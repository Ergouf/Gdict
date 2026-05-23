package io.github.gdict.core

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.annotation.WorkerThread
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DictionaryManager(private val context: Context) {

    data class DictEntry(
        val id: Long,
        val name: String,
        val path: String,
        val dictFilePath: String,
        val isEnabled: Boolean = true
    )

    data class SearchResult(
        val word: String,
        val definition: String,
        val dictionaryName: String,
        val css: String = ""
    )

    private val dictionaries = mutableListOf<DictEntry>()
    private val loadedDicts = mutableMapOf<Long, MdxParser>()
    private val loadedMdds = mutableMapOf<Long, MdxParser>()
    private val cssCache = mutableMapOf<Long, String>()
    private val idCounter = AtomicLong(1)
    private val prefs = context.getSharedPreferences("dict_manager", Context.MODE_PRIVATE)

    init {
        loadPersistedDictionaries()
    }

    private fun nextId(): Long {
        synchronized(this) {
            var candidate: Long
            do {
                candidate = System.currentTimeMillis() * 1000 + idCounter.getAndIncrement()
            } while (dictionaries.any { it.id == candidate } || loadedDicts.containsKey(candidate))
            return candidate
        }
    }

    private fun saveDictionaries() {
        val json = JSONArray()
        synchronized(this) {
            for (dict in dictionaries) {
                val obj = JSONObject().apply {
                    put("id", dict.id)
                    put("name", dict.name)
                    put("path", dict.path)
                    put("dictFilePath", dict.dictFilePath)
                    put("isEnabled", dict.isEnabled)
                }
                json.put(obj)
            }
        }
        prefs.edit().putString("dictionaries", json.toString()).apply()
    }

    private fun loadPersistedDictionaries() {
        val jsonStr = prefs.getString("dictionaries", null) ?: return
        try {
            val json = JSONArray(jsonStr)
            android.util.Log.i("DictMgr", "Loading ${json.length()} persisted dictionaries from SharedPreferences...")
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val entry = DictEntry(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    path = obj.getString("path"),
                    dictFilePath = obj.getString("dictFilePath"),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                val mdxFile = File(entry.dictFilePath)
                val mdxTitle = readMdxHeaderTitle(mdxFile)
                android.util.Log.i("DictMgr", "  Persisted[$i]: '${entry.name}' (id=${entry.id}) file=${mdxFile.name} exists=${mdxFile.exists()} size=${mdxFile.length()} mdxTitle='$mdxTitle'")

                val dictDir = File(context.filesDir, "dictionaries/${entry.id}")
                if (!dictDir.exists()) {
                    android.util.Log.w("DictMgr", "  Skipping '${entry.name}': directory not found at ${dictDir.absolutePath}")
                    continue
                }
                if (!mdxFile.exists() || mdxFile.length() == 0L) {
                    android.util.Log.w("DictMgr", "  Skipping '${entry.name}': MDX file not found or empty at ${entry.dictFilePath}")
                    continue
                }
                synchronized(this) {
                    dictionaries.add(entry)
                }
                if (entry.isEnabled) {
                    loadDictionary(entry)
                }
            }
            android.util.Log.i("DictMgr", "Loaded ${dictionaries.size}/${json.length()} persisted dictionaries (${loadedDicts.size} parsers ready)")
        } catch (e: Exception) {
            android.util.Log.e("DictMgr", "loadPersistedDictionaries FAILED: ${e.message}")
        }
    }

    fun addOrUpdateDictionary(name: String, sourceUri: String, companionUris: List<String> = emptyList()): DictEntry {
        try {
            return addOrUpdateDictionaryImpl(name, sourceUri, companionUris)
        } catch (e: Throwable) {
            android.util.Log.e("DictMgr", "addOrUpdateDictionary CRASHED for '$name': ${e.javaClass.name} - ${e.message}", e)
            throw RuntimeException("导入 '$name' 时出错: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    private fun addOrUpdateDictionaryImpl(name: String, sourceUri: String, companionUris: List<String> = emptyList()): DictEntry {
        val id = nextId()
        val dictDir = File(context.filesDir, "dictionaries/$id")
        dictDir.mkdirs()

        android.util.Log.i("DictMgr", "=== addOrUpdateDictionary '$name' id=$id ===")
        android.util.Log.i("DictMgr", "  sourceUri: $sourceUri")
        android.util.Log.i("DictMgr", "  companionUris: ${companionUris.size}")

        val copyResult = copyDictionaryFiles(sourceUri, dictDir)
        val copiedFiles = copyResult.files.toMutableList()
        val primaryFile = copyResult.primaryFile
        android.util.Log.i("DictMgr", "  Primary copy: ${primaryFile?.name} (${primaryFile?.length()} bytes)")
        android.util.Log.i("DictMgr", "  Total copied: ${copiedFiles.size} files")
        for (f in copiedFiles) {
            android.util.Log.i("DictMgr", "    -> ${f.name} (${f.length()} bytes)")
        }

        for (companionUri in companionUris) {
            try {
                val compFile = copyToInternal(Uri.parse(companionUri), dictDir)
                if (compFile != null) {
                    copiedFiles.add(compFile)
                    android.util.Log.i("DictMgr", "  Companion: ${compFile.name} (${compFile.length()} bytes)")
                } else {
                    android.util.Log.w("DictMgr", "  Companion copy failed: $companionUri")
                }
            } catch (e: Exception) {
                android.util.Log.e("DictMgr", "  Companion exception: ${e.message}")
            }
        }

        var mdxFile = if (primaryFile != null && primaryFile.name.lowercase().endsWith(".mdx")) {
            android.util.Log.i("DictMgr", "  Using primary file as MDX: ${primaryFile.name}")
            primaryFile
        } else {
            copiedFiles.firstOrNull { it.name.lowercase().endsWith(".mdx") }
        }
        if (mdxFile == null) {
            android.util.Log.w("DictMgr", "  No .mdx file found by extension, trying content detection...")
            for (i in copiedFiles.indices) {
                val file = copiedFiles[i]
                val detected = detectMdxOrMddExtension(file)
                if (detected != null) {
                    val newName = sanitizeFileName(file.name) + detected
                    val newFile = File(file.parentFile, newName)
                    if (file.renameTo(newFile)) {
                        android.util.Log.i("DictMgr", "  Detected MDX/MDD by header, renamed: ${file.name} -> $newName")
                        copiedFiles[i] = newFile
                        if (primaryFile == file) mdxFile = newFile
                    } else {
                        android.util.Log.w("DictMgr", "  Failed to rename ${file.name} to $newName")
                    }
                }
            }
            if (mdxFile == null || !mdxFile.exists()) {
                mdxFile = copiedFiles.firstOrNull { it.name.lowercase().endsWith(".mdx") }
            }
        }
        if (mdxFile == null) {
            android.util.Log.e("DictMgr", "  NO .mdx file found among ${copiedFiles.size} copied files!")
            for (f in copiedFiles) {
                android.util.Log.e("DictMgr", "    existing file: ${f.name}")
            }
            dictDir.deleteRecursively()
            throw RuntimeException("未能从导入路径中找到 .mdx 词典文件，请确认选择了正确的 .mdx 文件")
        }

        val mdxTitle = readMdxHeaderTitle(mdxFile)
        android.util.Log.i("DictMgr", "  MDX file verified: '${mdxFile.name}' (${mdxFile.length()} bytes) title='$mdxTitle'")

        val entry = DictEntry(
            id = id,
            name = name,
            path = sourceUri,
            dictFilePath = mdxFile.absolutePath,
            isEnabled = true
        )
        synchronized(this) {
            val removed = dictionaries.filter { it.name == name || it.path == sourceUri }
            for (old in removed) {
                loadedDicts.remove(old.id)?.close()
            }
            dictionaries.removeAll { it.name == name || it.path == sourceUri }
            dictionaries.add(entry)
        }
        loadDictionary(entry)
        synchronized(this) {
            val loadedParser = loadedDicts[entry.id]
            if (loadedParser != null) {
                android.util.Log.i("DictMgr", "  VERIFIED: '${entry.name}' → parser title='${loadedParser.title}' words=${loadedParser.wordCount} file=${mdxFile.name}")
                saveDictionaries()
            } else {
                android.util.Log.e("DictMgr", "  FAILED: '${entry.name}' parser not loaded after loadDictionary!")
                dictDir.deleteRecursively()
                throw RuntimeException("词典 '${name}' 加载失败，无法读取词典数据")
            }
        }
        return entry
    }

    private data class CopyResult(val files: List<File>, val primaryFile: File?)

    private fun copyDictionaryFiles(sourceUri: String, targetDir: File): CopyResult {
        val copied = mutableListOf<File>()
        var primaryFile: File? = null
        try {
            val uri = Uri.parse(sourceUri)
            if (sourceUri.startsWith("content://")) {
                val isTree = try {
                    DocumentsContract.isTreeUri(uri) && !uri.toString().contains("/document/", ignoreCase = true)
                } catch (_: Exception) { false }

                if (isTree) {
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        uri, DocumentsContract.getTreeDocumentId(uri)
                    )
                    val cursor = context.contentResolver.query(
                        childrenUri, arrayOf(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                        ), null, null, null
                    )
                    cursor?.use { c ->
                        while (c.moveToNext()) {
                            val docId = c.getString(0)
                            val displayName = c.getString(1)
                            if (displayName != null && isDictionaryFile(displayName)) {
                                val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                                copyToInternal(childUri, displayName, targetDir)?.let { f ->
                                    copied.add(f)
                                    if (primaryFile == null && f.name.lowercase().endsWith(".mdx")) primaryFile = f
                                }
                            }
                        }
                    }
                } else {
                    val realName = resolveDocumentName(uri)
                    android.util.Log.i("DictMgr", "  resolveDocumentName: '$realName' for uri: $sourceUri")
                    copyToInternal(uri, realName, targetDir)?.let { f ->
                        copied.add(f)
                        primaryFile = f
                    }
                }
            } else {
                val file = File(sourceUri)
                if (file.isDirectory) {
                    file.listFiles()?.forEach { f ->
                        if (isDictionaryFile(f.name)) {
                            f.copyTo(File(targetDir, f.name), overwrite = true)
                            val target = File(targetDir, f.name)
                            copied.add(target)
                            if (primaryFile == null && f.name.lowercase().endsWith(".mdx")) primaryFile = target
                        }
                    }
                } else if (isDictionaryFile(file.name)) {
                    file.copyTo(File(targetDir, file.name), overwrite = true)
                    val target = File(targetDir, file.name)
                    copied.add(target)
                    primaryFile = target
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DictMgr", "copyDictionaryFiles FAILED: ${e.message}", e)
        }
        return CopyResult(copied, primaryFile)
    }

    private fun resolveDocumentName(uri: Uri): String {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (colIndex >= 0) {
                        val name = cursor.getString(colIndex)
                        if (!name.isNullOrBlank()) {
                            android.util.Log.i("DictMgr", "resolveDocumentName: got '$name' via OpenableColumns")
                            return name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DictMgr", "resolveDocumentName: OpenableColumns query failed: ${e.message}")
        }

        try {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (colIndex >= 0) {
                        val name = cursor.getString(colIndex)
                        if (!name.isNullOrBlank()) {
                            android.util.Log.i("DictMgr", "resolveDocumentName: got '$name' via DocumentsContract")
                            return name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DictMgr", "resolveDocumentName: DocumentsContract query failed: ${e.message}")
        }

        if (uri.authority == "com.android.providers.media.documents") {
            try {
                val docId = DocumentsContract.getDocumentId(uri)
                val id = docId.split(":").lastOrNull() ?: docId
                context.contentResolver.query(
                    android.provider.MediaStore.Files.getContentUri("external"),
                    arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME),
                    "${android.provider.MediaStore.MediaColumns._ID} = ?",
                    arrayOf(id),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrBlank()) {
                            android.util.Log.i("DictMgr", "resolveDocumentName: got '$name' via MediaStore")
                            return name
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DictMgr", "resolveDocumentName: MediaStore query failed: ${e.message}")
            }
        }

        if (uri.authority == "com.android.providers.downloads.documents") {
            try {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.startsWith("raw:")) {
                    val filePath = docId.substringAfter("raw:")
                    val fileName = File(filePath).name
                    if (fileName.isNotBlank() && fileName.contains(".")) {
                        android.util.Log.i("DictMgr", "resolveDocumentName: got '$fileName' via raw path")
                        return fileName
                    }
                }
                val id = docId.split(":").lastOrNull() ?: docId
                val downloadUri = android.provider.MediaStore.Downloads.getContentUri("external")
                context.contentResolver.query(
                    downloadUri,
                    arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME),
                    "${android.provider.MediaStore.MediaColumns._ID} = ?",
                    arrayOf(id),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrBlank()) {
                            android.util.Log.i("DictMgr", "resolveDocumentName: got '$name' via Downloads")
                            return name
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DictMgr", "resolveDocumentName: Downloads query failed: ${e.message}")
            }
        }

        if (uri.scheme == "content") {
            try {
                val docId = DocumentsContract.getDocumentId(uri)
                val decoded = try { java.net.URLDecoder.decode(docId, "UTF-8") } catch (_: Exception) { docId }
                val candidate = decoded.substringAfterLast('/')
                if (candidate.contains(".") && !candidate.matches(Regex("^\\d+$")) && !candidate.contains(":")) {
                    android.util.Log.i("DictMgr", "resolveDocumentName: got '$candidate' from documentId")
                    return candidate
                }
            } catch (_: Exception) {}
        }

        val lastSegment = uri.lastPathSegment ?: "unknown"
        val decoded = try { java.net.URLDecoder.decode(lastSegment, "UTF-8") } catch (_: Exception) { lastSegment }
        val candidate = decoded.substringAfterLast('/')
        android.util.Log.w("DictMgr", "resolveDocumentName: fallback to lastPathSegment '$candidate'")
        return candidate
    }

    private fun isDictionaryFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mdx") || lower.endsWith(".mdd") ||
               lower.endsWith(".dsl") || lower.endsWith(".dsl.dz") ||
               lower.endsWith(".bgl") || lower.endsWith(".lsa") ||
               lower.endsWith(".lsd") || lower.endsWith(".slob") ||
               lower.endsWith(".zim") || lower.endsWith(".stardict") ||
               lower.endsWith(".ifo") || lower.endsWith(".idx") ||
               lower.endsWith(".dict") || lower.endsWith(".css")
    }

    private fun detectMdxOrMddExtension(file: File): String? {
        if (file.length() < 12) return null
        try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val b = ByteArray(4)
                raf.readFully(b)
                val headerLen = (b[0].toInt() and 0xFF shl 24) or (b[1].toInt() and 0xFF shl 16) or
                        (b[2].toInt() and 0xFF shl 8) or (b[3].toInt() and 0xFF)
                if (headerLen <= 0 || headerLen > 100 * 1024 * 1024 || headerLen + 8 > file.length()) return null
                val readLen = minOf(headerLen, 4096)
                val headerBytes = ByteArray(readLen)
                raf.readFully(headerBytes)
                val headerStr = String(headerBytes, Charsets.UTF_16LE)
                if (headerStr.contains("GeneratedByEngineVersion", ignoreCase = true)) {
                    return ".mdx"
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[:\\\\/*?|<>]"), "_")
    }

    private fun readMdxHeaderTitle(file: File): String {
        if (file.length() < 12) return "(invalid: too small)"
        try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val b = ByteArray(4)
                raf.readFully(b)
                val headerLen = (b[0].toInt() and 0xFF shl 24) or (b[1].toInt() and 0xFF shl 16) or
                        (b[2].toInt() and 0xFF shl 8) or (b[3].toInt() and 0xFF)
                if (headerLen <= 0 || headerLen > 100 * 1024 * 1024) return "(invalid headerLen=$headerLen)"
                val readLen = minOf(headerLen, 4096)
                val headerBytes = ByteArray(readLen)
                raf.readFully(headerBytes)
                val headerStr = String(headerBytes, Charsets.UTF_16LE)
                val titleMatch = Regex("""<Title[^>]*>([^<]*)</Title>""", RegexOption.IGNORE_CASE).find(headerStr)
                return titleMatch?.groupValues?.get(1)?.trim() ?: "(no Title in header)"
            }
        } catch (e: Exception) {
            return "(read error: ${e.message})"
        }
    }

    private fun copyToInternal(uri: Uri, displayName: String, targetDir: File): File? {
        return try {
            val targetFile = File(targetDir, displayName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) targetFile
            else { targetFile.delete(); null }
        } catch (e: Exception) {
            android.util.Log.e("DictMgr", "copyToInternal FAILED '$displayName': ${e.message}")
            null
        }
    }

    private fun copyToInternal(uri: Uri, targetDir: File): File? {
        val name = resolveDocumentName(uri)
        return copyToInternal(uri, name, targetDir)
    }

    private fun loadDictionary(entry: DictEntry) {
        val mdxFile = File(entry.dictFilePath)
        android.util.Log.i("DictMgr", "Loading '${entry.name}' (id=${entry.id}) from: ${entry.dictFilePath}")
        if (!mdxFile.exists()) {
            android.util.Log.e("DictMgr", "  File not found: ${entry.dictFilePath}")
            return
        }
        if (!mdxFile.name.lowercase().endsWith(".mdx")) {
            android.util.Log.e("DictMgr", "  Not an .mdx file: ${entry.dictFilePath}")
            return
        }
        android.util.Log.i("DictMgr", "  File size: ${mdxFile.length()} bytes")
        if (mdxFile.length() == 0L) {
            android.util.Log.e("DictMgr", "  File is empty!")
            return
        }

        try {
            val mdxCanonical = mdxFile.canonicalPath
            val headerPreview = try {
                java.io.RandomAccessFile(mdxFile, "r").use { raf ->
                    val b = ByteArray(16)
                    raf.readFully(b)
                    b.joinToString("") { "%02X".format(it) }
                }
            } catch (_: Exception) { "(unreadable)" }
            android.util.Log.i("DictMgr", "  canonicalPath=$mdxCanonical header=$headerPreview")

            val parser = MdxParser(mdxFile)
            android.util.Log.i("DictMgr", "  PARSER IDENTITY: hashCode=${parser.hashCode()} filePath='${parser.filePath}' fileName='${parser.fileName}' fileSize=${parser.fileSize} title='${parser.title}' words=${parser.wordCount}")

            if (parser.wordCount <= 0) {
                android.util.Log.e("DictMgr", "  Loaded '${entry.name}' but wordCount=${parser.wordCount}, keywords empty!")
                android.util.Log.e("DictMgr", "  Title='${parser.title}' Encoding='${parser.encoding}' CaseSensitive=${parser.isKeyCaseSensitive}")
                parser.close()
                return
            }
            synchronized(this) {
                loadedDicts.remove(entry.id)?.close()
                loadedDicts[entry.id] = parser
            }

            val mddFile = findCompanionMdd(mdxFile)
            android.util.Log.i("DictMgr", "  MDD lookup for '${mdxFile.name}': ${if (mddFile != null) "'${mddFile.name}' (${mddFile.length()} bytes)" else "NOT FOUND"}")
            if (mddFile != null) {
                try {
                    android.util.Log.i("DictMgr", "  Loading MDD: '${mddFile.name}' size=${mddFile.length()} bytes...")
                    val mddParser = MdxParser(mddFile)
                    android.util.Log.i("DictMgr", "  MDD parsed: wordCount=${mddParser.wordCount} title='${mddParser.title}'")
                    if (mddParser.wordCount > 0) {
                        synchronized(this) {
                            loadedMdds.remove(entry.id)?.close()
                            loadedMdds[entry.id] = mddParser
                        }
                        android.util.Log.i("DictMgr", "  ✅ MDD loaded: '${mddFile.name}' resources=${mddParser.wordCount}")
                    } else {
                        android.util.Log.w("DictMgr", "  ⚠️ MDD '${mddFile.name}' has wordCount=0, treating as empty (size=${mddFile.length()})")
                        mddParser.close()
                    }
                } catch (e: OutOfMemoryError) {
                    android.util.Log.e("DictMgr", "  ❌ OOM loading MDD '${mddFile.name}' (${mddFile.length()} bytes): ${e.message}")
                    System.gc()
                } catch (e: Exception) {
                    android.util.Log.e("DictMgr", "  ❌ Failed to load MDD '${mddFile.name}': ${e.javaClass.simpleName}: ${e.message}", e)
                }
            }

            android.util.Log.i("DictMgr", "  LOADED OK: '${entry.name}' → title='${parser.title}' words=${parser.wordCount} encoding='${parser.encoding}' file=${mdxFile.name}")
        } catch (e: Exception) {
            android.util.Log.e("DictMgr", "  FAILED to load ${entry.name}: ${e.javaClass.simpleName}: ${e.message}", e)
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("DictMgr", "  OUT OF MEMORY loading ${entry.name}: ${e.message}")
            System.gc()
        }
    }

    fun removeDictionary(id: Long) {
        synchronized(this) {
            loadedDicts.remove(id)?.close()
            loadedMdds.remove(id)?.close()
            cssCache.remove(id)
            dictionaries.removeAll { it.id == id }
        }
        val dictDir = File(context.filesDir, "dictionaries/$id")
        if (dictDir.exists()) dictDir.deleteRecursively()
        saveDictionaries()
    }

    private fun findCompanionMdd(mdxFile: File): File? {
        val parentDir = mdxFile.parentFile ?: return null
        val baseName = mdxFile.nameWithoutExtension
        val candidates = listOf(
            File(parentDir, "$baseName.mdd"),
            File(parentDir, "${mdxFile.name}.mdd")
        )
        for (mdd in candidates) {
            if (mdd.exists() && mdd.length() > 0) return mdd
        }
        return null
    }

    @WorkerThread
    fun getAudioResource(word: String): ByteArray? {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val audioPatterns = listOf(
            "\\$word.mp3",
            "\\$word.wav",
            "\\$word.ogg",
            "\\$word.spx",
            "\\${word.lowercase()}.mp3",
            "\\${word.lowercase()}.wav",
            "\\${word.lowercase()}.ogg",
            "\\${word.lowercase()}.spx"
        )
        for (dict in snapshot) {
            val mddParser = synchronized(this) { loadedMdds[dict.id] } ?: continue
            for (pattern in audioPatterns) {
                val data = mddParser.readResourceBytes(pattern)
                if (data != null && data.isNotEmpty()) {
                    android.util.Log.i("DictMgr", "Audio found for '$word' in '${dict.name}': pattern='$pattern' size=${data.size}")
                    return data
                }
            }
        }
        for (dict in snapshot) {
            val mddParser = synchronized(this) { loadedMdds[dict.id] } ?: continue
            for (suffix in audioPatterns) {
                val matches = mddParser.findResourceKeys(suffix)
                for (match in matches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
                        android.util.Log.i("DictMgr", "Audio found (fuzzy) for '$word' in '${dict.name}': key='$match' size=${data.size}")
                        return data
                    }
                }
            }
        }
        return null
    }

    @WorkerThread
    fun getAudioResourceByPath(path: String): ByteArray? {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val normalizedPath = path.replace("/", "\\")
        val pathWithBackslash = if (normalizedPath.startsWith("\\")) normalizedPath else "\\$normalizedPath"
        android.util.Log.d("DictMgr", "getAudioResourceByPath('$path') enabledDicts=${snapshot.size} loadedMdds=${loadedMdds.size}")
        for (dict in snapshot) {
            val mddParser = synchronized(this) { loadedMdds[dict.id] }
            if (mddParser == null) {
                android.util.Log.w("DictMgr", "  MDD not loaded for '${dict.name}' (id=${dict.id})")
                continue
            }
            android.util.Log.d("DictMgr", "  Trying '${dict.name}' MDD (words=${mddParser.wordCount})")
            val data = mddParser.readResourceBytes(pathWithBackslash)
            if (data != null && data.isNotEmpty()) {
                android.util.Log.i("DictMgr", "Audio found by path '$path' in '${dict.name}' size=${data.size}")
                return data
            }
            val data2 = mddParser.readResourceBytes(normalizedPath)
            if (data2 != null && data2.isNotEmpty()) {
                android.util.Log.i("DictMgr", "Audio found by path '$path' in '${dict.name}' size=${data2.size}")
                return data2
            }
        }
        for (dict in snapshot) {
            val mddParser = synchronized(this) { loadedMdds[dict.id] } ?: continue
            val suffix = pathWithBackslash.lowercase()
            val matches = mddParser.findResourceKeys(suffix)
            for (match in matches) {
                val data = mddParser.readResourceBytesByKey(match)
                if (data != null && data.isNotEmpty()) {
                    android.util.Log.i("DictMgr", "Audio found (fuzzy) by path '$path' in '${dict.name}': key='$match' size=${data.size}")
                    return data
                }
            }
        }
        return null
    }

    fun diagnoseAllDictionaries(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Gdict Dictionary Diagnostics ===")
        sb.appendLine("Total dicts: ${dictionaries.size}, Loaded parsers: ${loadedDicts.size}")
        sb.appendLine()

        synchronized(this) {
            for ((i, dict) in dictionaries.withIndex()) {
                sb.appendLine("--- Dict[$i] ---")
                sb.appendLine("  id=${dict.id} name='${dict.name}'")
                sb.appendLine("  path='${dict.path}'")
                sb.appendLine("  dictFilePath='${dict.dictFilePath}'")
                sb.appendLine("  isEnabled=${dict.isEnabled}")

                val mdxFile = File(dict.dictFilePath)
                sb.appendLine("  fileExists=${mdxFile.exists()} fileSize=${mdxFile.length()}")

                val dictDir = mdxFile.parentFile
                val dictDirFiles = dictDir?.listFiles()?.sortedBy { it.name }
                sb.appendLine("  📂 Files in dictDir (${dictDirFiles?.size ?: 0}): ${dictDirFiles?.joinToString { "${it.name}(${it.length()})" }}")

                try {
                    val uri = android.net.Uri.parse(dict.path)
                    if (dict.path.startsWith("content://") && android.provider.DocumentsContract.isTreeUri(uri)) {
                        val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                            uri,
                            android.provider.DocumentsContract.getTreeDocumentId(uri)
                        )
                        val cursor = context.contentResolver.query(
                            childrenUri,
                            arrayOf(
                                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                android.provider.DocumentsContract.Document.COLUMN_SIZE
                            ),
                            null,
                            null,
                            null
                        )
                        val fileList = mutableListOf<String>()
                        cursor?.use {
                            while (it.moveToNext()) {
                                val name = it.getString(1)
                                val size = it.getLong(2)
                                if (name != null) {
                                    fileList.add("$name($size)")
                                }
                            }
                        }
                        sb.appendLine("  📂 Files in original dir (${fileList.size}): ${fileList.joinToString()}")
                    }
                } catch (e: Exception) {
                    sb.appendLine("  📂 Original dir list failed: ${e.message}")
                }

                val parser = loadedDicts[dict.id]
                if (parser != null) {
                    sb.appendLine("  parser: title='${parser.title}' words=${parser.wordCount}")
                    sb.appendLine("          encoding='${parser.encoding}' caseSensitive=${parser.isKeyCaseSensitive}")

                    try {
                        val testWord = "read"
                        val articles = parser.readArticles(testWord)
                        sb.appendLine("  search('$testWord'): ${articles.size} results")
                        for ((word, def) in articles) {
                            val preview = def?.take(80)?.replace("\n", "\\n") ?: "(null)"
                            val hash = def?.hashCode() ?: 0
                            sb.appendLine("    ['$word'] hash=$hash len=${def?.length ?: 0} preview='$preview'")
                            if (def != null && def.length <= 2000) {
                                val rawEscaped = def
                                    .replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")
                                    .replace("\n", "\\n")
                                    .replace("\r", "\\r")
                                sb.appendLine("    RAW(first500): '${rawEscaped.take(500)}'")
                            } else if (def != null) {
                                val rawEscaped = def.take(500)
                                    .replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")
                                    .replace("\n", "\\n")
                                sb.appendLine("    RAW(first500): '${rawEscaped}' ... (truncated, total=${def.length})")
                            }
                        }
                    } catch (e: Exception) {
                        sb.appendLine("  search ERROR: ${e.javaClass.simpleName}: ${e.message}")
                    }

                    try {
                        val first3Keywords = parser.getAllKeywords().take(3)
                        sb.appendLine("  first keywords: $first3Keywords")
                    } catch (_: Exception) {}
                } else {
                    sb.appendLine("  parser: NULL (not loaded!)")
                }
                sb.appendLine()
            }

            sb.appendLine("=== Parser identity check ===")
            val parserIdentities = mutableMapOf<Int, String>()
            for ((id, parser) in loadedDicts) {
                val identity = "${parser.title}|${parser.wordCount}|${parser.hashCode()}|${parser.fileName}"
                parserIdentities[id.toInt()] = identity
                sb.appendLine("  parser[$id] → $identity")
            }
            val uniqueParsers = parserIdentities.values.toSet()
            sb.appendLine("  Unique parser identities: ${uniqueParsers.size}/${parserIdentities.size}")
            if (uniqueParsers.size < parserIdentities.size) {
                sb.appendLine("  ⚠️ WARNING: Duplicate parser detected!")
            }
        }
        return sb.toString()
    }

    fun testMddResourcesAndHtml(): String {
        val sb = StringBuilder()
        sb.appendLine("=== MDD Resource & HTML Render Test ===")
        sb.appendLine()

        synchronized(this) {
            for ((id, dict) in dictionaries.withIndex()) {
                if (!dict.isEnabled) continue
                sb.appendLine("--- Dict[$id]: ${dict.name} ---")

                val parser = loadedDicts[dict.id]
                if (parser == null) {
                    sb.appendLine("  ⚠️ MDX parser not loaded!")
                    sb.appendLine()
                    continue
                }

                val mddParser = loadedMdds[dict.id]
                if (mddParser == null) {
                    sb.appendLine("  ℹ️ No MDD file (images may show as transparent)")
                    val mdxPath = dict.dictFilePath
                    val mdxFile = File(mdxPath)
                    val parentDir = mdxFile.parentFile
                    sb.appendLine("  🔍 MDX path: $mdxPath")
                    sb.appendLine("  🔍 MDX exists: ${mdxFile.exists()} (${mdxFile.length()} bytes)")
                    if (parentDir != null && parentDir.exists()) {
                        sb.appendLine("  📁 Files in dir: ${parentDir.listFiles()?.map { "${it.name} (${it.length()}" }?.joinToString(", ") ?: "empty"}")
                        val baseName = mdxFile.nameWithoutExtension
                        val candidates = listOf(
                            File(parentDir, "$baseName.mdd"),
                            File(parentDir, "${mdxFile.name}.mdd")
                        )
                        for (cand in candidates) {
                            sb.appendLine("  🔎 MDD candidate '${cand.name}': exists=${cand.exists()}, size=${if (cand.exists()) cand.length() else "N/A"}")
                            if (cand.exists() && cand.length() > 0) {
                                try {
                                    sb.appendLine("  🧪 Attempting to load MDD...")
                                    val testMdd = MdxParser(cand)
                                    sb.appendLine("  🧪 MDD parsed: wordCount=${testMdd.wordCount}, title='${testMdd.title}', resourceMode=${testMdd.isResourceMode}")
                                    sb.appendLine("  🧪 ${testMdd.diagnose().replace("\n", "\n  🧪 ")}")
                                    if (testMdd.wordCount > 0) {
                                        val sampleKeys = testMdd.findResourceKeys(".png").take(3)
                                        sb.appendLine("  🧪 Sample PNG keys: $sampleKeys")
                                        for (key in sampleKeys) {
                                            val data = testMdd.readResourceBytesByKey(key)
                                            sb.appendLine("  🧪   '$key' → ${data?.size ?: 0} bytes")
                                        }
                                    } else if (testMdd.isResourceMode) {
                                        sb.appendLine("  🧪 Stream mode active, testing resource lookup...")
                                        val testPaths = listOf("\\cepd18.css", "\\95D5C9C1.png")
                                        for (tp in testPaths) {
                                            val rd = testMdd.readResourceBytes(tp)
                                            sb.appendLine("  🧪   '$tp' → ${rd?.size ?: "not found"} bytes")
                                        }
                                        val pngKeys = testMdd.findResourceKeys(".css").take(5)
                                        sb.appendLine("  🧪 CSS keys found: $pngKeys")
                                        val pngKeys2 = testMdd.findResourceKeys(".png").take(5)
                                        sb.appendLine("  🧪 PNG keys found: $pngKeys2")
                                    }
                                    testMdd.close()
                                } catch (e: OutOfMemoryError) {
                                    sb.appendLine("  ❌ OOM loading MDD (${cand.length()} bytes): ${e.message}")
                                } catch (e: Exception) {
                                    sb.appendLine("  ❌ Error loading MDD: ${e.javaClass.simpleName}: ${e.message}")
                                }
                            }
                        }
                    }
                } else {
                    sb.appendLine("  ✅ MDD loaded: ${mddParser.wordCount} resources, resourceMode=${mddParser.isResourceMode}")
                    val imageKeys = mddParser.findResourceKeys(".png").take(5)
                    if (imageKeys.isNotEmpty()) {
                        sb.appendLine("  Sample images: $imageKeys")
                        for (key in imageKeys.take(3)) {
                            val data = mddParser.readResourceBytesByKey(key)
                            sb.appendLine("    '$key' → ${data?.size ?: 0} bytes")
                        }
                    } else if (mddParser.isResourceMode) {
                        sb.appendLine("  🔍 Stream mode: testing resource lookup...")
                        val testPaths = listOf("\\cepd18.css", "\\95D5C9C1.png", "\\images\\test.png")
                        for (tp in testPaths) {
                            val rd = mddParser.readResourceBytes(tp)
                            sb.appendLine("    '$tp' → ${rd?.size ?: "not found"} bytes")
                        }
                    } else {
                        sb.appendLine("  ⚠️ No .png resources found in MDD")
                    }
                }
            sb.appendLine()

            val cssKeys = mddParser?.findResourceKeys(".css") ?: emptyList()
            sb.appendLine("  🔍 CSS keys in MDD (endsWith .css): ${cssKeys.size} → $cssKeys")

            if (mddParser != null) {
                val cssLikeKeys = mddParser.findResourceKeys("css").filter { !it.lowercase().endsWith(".css") }
                sb.appendLine("  🔍 CSS-like keys (contains 'css'): ${cssLikeKeys.size} → ${cssLikeKeys.take(10)}")
                val sampleKeys = mddParser.getAllKeywords().take(10)
                sb.appendLine("  🔍 Sample MDD keywords: $sampleKeys")
            }

            val cssContent = getCssFromMdd(dict.id)
            val cssLen = cssContent.length
            sb.appendLine("  🎨 CSS from MDD: ${if (cssLen > 0) "$cssLen chars" else "EMPTY!"}")
            if (cssLen > 0 && cssLen <= 500) {
                sb.appendLine("     Preview: ${cssContent.take(200).replace("\n", "\\n")}")
            } else if (cssLen > 500) {
                sb.appendLine("     First 200 chars: ${cssContent.take(200).replace("\n", "\\n")}")
            }

            val companionCss = parser?.companionCss ?: ""
            val compLen = companionCss.length
            sb.appendLine("  📄 companionCss: ${if (compLen > 0) "$compLen chars" else "EMPTY!"}")
            if (compLen > 0 && compLen <= 500) {
                sb.appendLine("     Preview: ${companionCss.take(200).replace("\n", "\\n")}")
            } else if (compLen > 500) {
                sb.appendLine("     First 200 chars: ${companionCss.take(200).replace("\n", "\\n")}")
            }

            try {
                val testWords = listOf("hello", "test", "the", "a", "bye")
                for (word in testWords) {
                        val articles = parser.readArticles(word)
                        if (articles.isNotEmpty()) {
                            for ((w, def) in articles) {
                                if (def == null || def.isBlank()) continue
                                sb.appendLine("  📝 Test word: '$w'")
                                val hasIpa = def.contains("<ipa>", ignoreCase = true)
                                val hasImg = Regex("""<img[^>]+src=["']([^"']+)["']""").containsMatchIn(def)
                                val cssLinks = Regex("""<link[^>]+href=["']([^"']+\.css)["']""").findAll(def).map { it.groupValues[1] }.distinct().toList()
                                sb.appendLine("     - Has <ipa>: $hasIpa | Has <img>: $hasImg | CSS links: $cssLinks")
                                val imgSrcs = Regex("""<img[^>]+src=["']([^"']+)["']""").findAll(def).map { it.groupValues[1] }.distinct().toList()
                                sb.appendLine("     - All img srcs: $imgSrcs")
                                val soundHrefs = Regex("""href=["']sound://([^"']+)["']""").findAll(def).map { it.groupValues[1] }.distinct().toList()
                                sb.appendLine("     - All sound:// hrefs: $soundHrefs")
                                val customTags = Regex("""<([a-zA-Z][a-zA-Z0-9_-]*)[^>]*>""").findAll(def).map { it.groupValues[1].lowercase() }.distinct().sorted().toList()
                                sb.appendLine("     - All custom tags: $customTags")
                                sb.appendLine("     - FULL HTML:")
                                sb.appendLine(def)
                                break
                            }
                            break
                        }
                    }
                } catch (e: Exception) {
                    sb.appendLine("  ❌ Test search error: ${e.message}")
                }
                sb.appendLine()
            }
        }

        sb.appendLine("=== transformMdxTags Coverage Check ===")
        val knownTags = listOf(
            "<SEP>", "<hw>", "<inf>", "<ex>", "<hit>",
            "<ipa>", "<prongrp>", "<inflection>", "<capvar>",
            "<sense-block>", "<sense-body>", "<sense-head>",
            "<di-head>", "<di-title>", "<di-body>", "<di-info>",
            "<arl>", "<base>", "<results>", "<forms>", "<inflections>",
            "<pron>", "<ussymbol>", "<soundfile>"
        )
        sb.appendLine("Tags handled by transformMdxTags: ${knownTags.size}")
        for (tag in knownTags) {
            sb.appendLine("  ✅ $tag")
        }
        return sb.toString()
    }

    fun toggleDictionary(id: Long, enabled: Boolean) {
        val index = dictionaries.indexOfFirst { it.id == id }
        if (index >= 0) {
            synchronized(this) {
                dictionaries[index] = dictionaries[index].copy(isEnabled = enabled)
            }
            if (enabled) {
                loadDictionary(dictionaries[index])
            }
            saveDictionaries()
        }
    }

    fun getDictionaries(): List<DictEntry> = synchronized(this) { dictionaries.toList() }

    fun getParserForDictionary(id: Long): MdxParser? = synchronized(this) { loadedDicts[id] }

    fun getCssFromMdd(dictId: Long): String {
        val mddParser = synchronized(this) { loadedMdds[dictId] } ?: return ""
        android.util.Log.i("DictMgr", "getCssFromMdd: dictId=$dictId, mddParser.title='${mddParser.title}' words=${mddParser.wordCount}")
        val cssKeys = mddParser.findResourceKeys(".css")
        android.util.Log.i("DictMgr", "getCssFromMdd: found ${cssKeys.size} CSS keys: $cssKeys")
        if (cssKeys.isEmpty()) return ""
        val sb = StringBuilder()
        for (key in cssKeys) {
            try {
                val data = mddParser.readResourceBytesByKey(key)
                if (data != null && data.isNotEmpty()) {
                    sb.append(String(data, Charsets.UTF_8))
                    sb.append("\n")
                    android.util.Log.i("DictMgr", "Loaded CSS from MDD: '$key' (${data.size} bytes)")
                } else {
                    android.util.Log.w("DictMgr", "CSS key '$key' returned ${if (data == null) "null" else "empty"}")
                }
            } catch (e: Exception) {
                android.util.Log.w("DictMgr", "Failed to read CSS '$key' from MDD: ${e.message}")
            }
        }
        return sb.toString()
    }

    @WorkerThread
    fun getRandomWords(count: Int = 5): List<Pair<String, String>> {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val allWords = mutableListOf<Pair<String, String>>()
        for (dict in snapshot) {
            val parser = synchronized(this) { loadedDicts[dict.id] } ?: continue
            val keywords = parser.getAllKeywords()
            if (keywords.isEmpty()) continue
            val step = (keywords.size / count).coerceAtLeast(1)
            val start = (0 until step).random()
            for (i in 0 until count) {
                val idx = start + i * step
                if (idx < keywords.size) {
                    allWords.add(Pair(keywords[idx], dict.name))
                }
            }
        }
        return allWords.shuffled().take(count)
    }

    @WorkerThread
    fun searchWord(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<SearchResult>()
        android.util.Log.i("DictMgr", "searchWord('$query') called, loadedDicts=${loadedDicts.size}, enabledDicts=${dictionaries.count { it.isEnabled }}")

        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }

        for (dict in snapshot) {
            val parser = synchronized(this) { loadedDicts[dict.id] }
            if (parser != null) {
                android.util.Log.d("DictMgr", "  Searching '${dict.name}' (id=${dict.id}) using parser title='${parser.title}' words=${parser.wordCount}")
                val dictResults = searchWithParser(parser, dict, query)
                results.addAll(dictResults)
            } else {
                android.util.Log.w("DictMgr", "  No parser for '${dict.name}' (id=${dict.id})")
            }
        }

        android.util.Log.i("DictMgr", "searchWord('$query') returned ${results.size} results")
        return results
    }

    private fun searchWithParser(parser: MdxParser, dict: DictEntry, query: String): List<SearchResult> {
        return try {
            val results = mutableListOf<SearchResult>()
            android.util.Log.i("DictMgr", "    [SEARCH] dict='${dict.name}' parserHash=${parser.hashCode()} parserTitle='${parser.title}' parserFile='${parser.fileName}' parserWords=${parser.wordCount}")
            val css = buildCss(parser, dict.id)
            if (css.isNotEmpty()) {
                android.util.Log.i("DictMgr", "    [SEARCH] CSS loaded for '${dict.name}': ${css.length} chars")
            }

            val exact = parser.readArticles(query)
            android.util.Log.d("DictMgr", "    '${dict.name}' exact match: ${exact.size} articles")
            for ((word, def) in exact) {
                val defHash = def?.hashCode() ?: 0
                val preview = def?.take(60)?.replace("\n", "\\n") ?: "(null)"
                android.util.Log.d("DictMgr", "      ['$word'] defHash=$defHash preview='$preview'")
                results.add(SearchResult(word = word ?: query, definition = def ?: "", dictionaryName = dict.name, css = css))
            }

            if (results.isEmpty()) {
                val predictive = parser.readArticlesPredictive(query)
                android.util.Log.d("DictMgr", "    '${dict.name}' predictive: ${predictive.size} articles")
                for ((word, def) in predictive) {
                    val defHash = def?.hashCode() ?: 0
                    android.util.Log.d("DictMgr", "      ['$word'] defHash=$defHash")
                    results.add(SearchResult(word = word ?: query, definition = def ?: "", dictionaryName = dict.name, css = css))
                }
            }

            results
        } catch (e: Exception) {
            android.util.Log.e("DictMgr", "Search FAILED for ${dict.name}: ${e.message}")
            emptyList()
        }
    }

    private fun buildCss(parser: MdxParser, dictId: Long): String {
        cssCache[dictId]?.let { return it }
        val sb = StringBuilder()
        sb.append(parser.companionCss)
        val mddParser = synchronized(this) { loadedMdds[dictId] }
        if (mddParser != null && mddParser.wordCount > 0) {
            try {
                val cssKeys = mddParser.findResourceKeys(".css")
                for (key in cssKeys) {
                    try {
                        val cssBytes = mddParser.readResourceBytesByKey(key)
                        if (cssBytes != null && cssBytes.isNotEmpty()) {
                            sb.append(String(cssBytes, Charsets.UTF_8)).append("\n")
                            android.util.Log.i("DictMgr", "  Loaded CSS from MDD: $key (${cssBytes.size} bytes)")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("DictMgr", "  Failed to read CSS resource $key: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DictMgr", "  Failed to extract CSS from MDD: ${e.message}")
            }
        }
        val result = sb.toString()
        cssCache[dictId] = result
        return result
    }

    fun scanDirectory(uri: Uri): List<DictCandidate> {
        val candidates = mutableListOf<DictCandidate>()
        try {
            if (!DocumentsContract.isTreeUri(uri)) return candidates
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                uri, DocumentsContract.getTreeDocumentId(uri)
            )
            val cursor = context.contentResolver.query(
                childrenUri, arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ), null, null, null
            )
            val filesByBaseName = mutableMapOf<String, MutableList<Pair<String, String>>>()
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val docId = c.getString(0)
                    val displayName = c.getString(1) ?: continue
                    if (!isDictionaryFile(displayName)) continue
                    val lowerName = displayName.lowercase()
                    val suffixLen = when {
                        lowerName.endsWith(".mdx") -> 4
                        lowerName.endsWith(".mdd") -> 4
                        lowerName.endsWith(".dsl") -> 4
                        lowerName.endsWith(".bgl") -> 4
                        lowerName.endsWith(".lsa") -> 4
                        lowerName.endsWith(".slob") -> 5
                        lowerName.endsWith(".css") -> 4
                        else -> 0
                    }
                    val baseName = displayName.dropLast(suffixLen)
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                    filesByBaseName.getOrPut(baseName) { mutableListOf() }
                        .add(displayName to childUri.toString())
                }
            }
            for ((baseName, files) in filesByBaseName) {
                val mdxFile = files.firstOrNull { it.first.lowercase().endsWith(".mdx") }
                if (mdxFile != null) {
                    candidates.add(DictCandidate(
                        name = baseName.ifEmpty { mdxFile.first.removeSuffix(".mdx") },
                        fileUri = mdxFile.second,
                        displayName = mdxFile.first,
                        companionFiles = files.filter { it !== mdxFile }.map { it.second }
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DictMgr", "scanDirectory failed: ${e.message}")
        }
        return candidates
    }

    data class DictCandidate(
        val name: String,
        val fileUri: String,
        val displayName: String,
        val companionFiles: List<String> = emptyList()
    )
}
