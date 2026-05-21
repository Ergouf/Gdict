package io.github.gdict.core

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
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
        val dictionaryName: String
    )

    private val dictionaries = mutableListOf<DictEntry>()
    private val loadedDicts = mutableMapOf<Long, MdxParser>()
    private val idCounter = AtomicLong(1)
    private val prefs = context.getSharedPreferences("dict_manager", Context.MODE_PRIVATE)

    init {
        loadPersistedDictionaries()
    }

    private fun nextId(): Long {
        return System.currentTimeMillis() * 1000 + idCounter.getAndIncrement()
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
                    DocumentsContract.isTreeUri(uri)
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
            val parser = MdxParser(mdxFile)
            if (parser.wordCount <= 0) {
                android.util.Log.e("DictMgr", "  Loaded '${entry.name}' but wordCount=${parser.wordCount}, keywords empty!")
                android.util.Log.e("DictMgr", "  Title='${parser.title}' Encoding='${parser.encoding}' CaseSensitive=${parser.isKeyCaseSensitive}")
                parser.close()
                return
            }
            synchronized(this) {
                loadedDicts[entry.id] = parser
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
            dictionaries.removeAll { it.id == id }
        }
        val dictDir = File(context.filesDir, "dictionaries/$id")
        if (dictDir.exists()) dictDir.deleteRecursively()
        saveDictionaries()
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
                            sb.appendLine("    ['$word'] hash=$hash preview='$preview'")
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
                val identity = "${parser.title}|${parser.wordCount}|${parser.hashCode()}"
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

            val exact = parser.readArticles(query)
            android.util.Log.d("DictMgr", "    '${dict.name}' exact match: ${exact.size} articles")
            for ((word, def) in exact) {
                val defHash = def?.hashCode() ?: 0
                val preview = def?.take(60)?.replace("\n", "\\n") ?: "(null)"
                android.util.Log.d("DictMgr", "      ['$word'] defHash=$defHash preview='$preview'")
                results.add(SearchResult(word = word ?: query, definition = def ?: "", dictionaryName = dict.name))
            }

            if (results.isEmpty()) {
                val predictive = parser.readArticlesPredictive(query)
                android.util.Log.d("DictMgr", "    '${dict.name}' predictive: ${predictive.size} articles")
                for ((word, def) in predictive) {
                    val defHash = def?.hashCode() ?: 0
                    android.util.Log.d("DictMgr", "      ['$word'] defHash=$defHash")
                    results.add(SearchResult(word = word ?: query, definition = def ?: "", dictionaryName = dict.name))
                }
            }

            results
        } catch (e: Exception) {
            android.util.Log.e("DictMgr", "Search FAILED for ${dict.name}: ${e.message}")
            emptyList()
        }
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
                    val baseName = displayName.removeSuffix(".mdx").removeSuffix(".mdd")
                        .removeSuffix(".dsl").removeSuffix(".bgl")
                        .removeSuffix(".lsa").removeSuffix(".slob")
                        .removeSuffix(".css")
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
