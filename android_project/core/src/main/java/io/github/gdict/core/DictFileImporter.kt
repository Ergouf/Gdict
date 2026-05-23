package io.github.gdict.core

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import java.io.File

class DictFileImporter(private val context: Context) {

    data class CopyResult(val files: List<File>, val primaryFile: File?)

    data class DictCandidate(
        val name: String,
        val fileUri: String,
        val displayName: String,
        val companionFiles: List<String> = emptyList()
    )

    fun addOrUpdateDictionary(
        name: String,
        sourceUri: String,
        companionUris: List<String> = emptyList(),
        nextId: Long
    ): Pair<DictionaryManager.DictEntry, List<File>> {
        val id = nextId
        val dictDir = File(context.filesDir, "dictionaries/$id")
        dictDir.mkdirs()

        Log.i("DictFileImporter", "=== addOrUpdateDictionary '$name' id=$id ===")
        Log.i("DictFileImporter", "  sourceUri: $sourceUri")
        Log.i("DictFileImporter", "  companionUris: ${companionUris.size}")

        val copyResult = copyDictionaryFiles(sourceUri, dictDir)
        val copiedFiles = copyResult.files.toMutableList()
        val primaryFile = copyResult.primaryFile
        Log.i("DictFileImporter", "  Primary copy: ${primaryFile?.name} (${primaryFile?.length()} bytes)")
        Log.i("DictFileImporter", "  Total copied: ${copiedFiles.size} files")
        for (f in copiedFiles) {
            Log.i("DictFileImporter", "    -> ${f.name} (${f.length()} bytes)")
        }

        for (companionUri in companionUris) {
            try {
                val compFile = copyToInternal(Uri.parse(companionUri), dictDir)
                if (compFile != null) {
                    copiedFiles.add(compFile)
                    Log.i("DictFileImporter", "  Companion: ${compFile.name} (${compFile.length()} bytes)")
                } else {
                    Log.w("DictFileImporter", "  Companion copy failed: $companionUri")
                }
            } catch (e: Exception) {
                Log.e("DictFileImporter", "  Companion exception: ${e.message}")
            }
        }

        var mdxFile = if (primaryFile != null && primaryFile.name.lowercase().endsWith(".mdx")) {
            Log.i("DictFileImporter", "  Using primary file as MDX: ${primaryFile.name}")
            primaryFile
        } else {
            copiedFiles.firstOrNull { it.name.lowercase().endsWith(".mdx") }
        }
        if (mdxFile == null) {
            Log.w("DictFileImporter", "  No .mdx file found by extension, trying content detection...")
            for (i in copiedFiles.indices) {
                val file = copiedFiles[i]
                val detected = detectMdxOrMddExtension(file)
                if (detected != null) {
                    val newName = sanitizeFileName(file.name) + detected
                    val newFile = File(file.parentFile, newName)
                    if (file.renameTo(newFile)) {
                        Log.i("DictFileImporter", "  Detected MDX/MDD by header, renamed: ${file.name} -> $newName")
                        copiedFiles[i] = newFile
                        if (primaryFile == file) mdxFile = newFile
                    } else {
                        Log.w("DictFileImporter", "  Failed to rename ${file.name} to $newName")
                    }
                }
            }
            if (mdxFile == null || !mdxFile.exists()) {
                mdxFile = copiedFiles.firstOrNull { it.name.lowercase().endsWith(".mdx") }
            }
        }
        if (mdxFile == null) {
            Log.e("DictFileImporter", "  NO .mdx file found among ${copiedFiles.size} copied files!")
            for (f in copiedFiles) {
                Log.e("DictFileImporter", "    existing file: ${f.name}")
            }
            dictDir.deleteRecursively()
            throw RuntimeException("未能从导入路径中找到 .mdx 词典文件，请确认选择了正确的 .mdx 文件")
        }

        val mdxTitle = readMdxHeaderTitle(mdxFile)
        Log.i("DictFileImporter", "  MDX file verified: '${mdxFile.name}' (${mdxFile.length()} bytes) title='$mdxTitle'")

        val entry = DictionaryManager.DictEntry(
            id = id,
            name = name,
            path = sourceUri,
            dictFilePath = mdxFile.absolutePath,
            isEnabled = true
        )
        return Pair(entry, copiedFiles)
    }

    fun copyDictionaryFiles(sourceUri: String, targetDir: File): CopyResult {
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
                    Log.i("DictFileImporter", "  resolveDocumentName: '$realName' for uri: $sourceUri")
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
            Log.e("DictFileImporter", "copyDictionaryFiles FAILED: ${e.message}", e)
        }
        return CopyResult(copied, primaryFile)
    }

    fun resolveDocumentName(uri: Uri): String {
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
                            Log.i("DictFileImporter", "resolveDocumentName: got '$name' via OpenableColumns")
                            return name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("DictFileImporter", "resolveDocumentName: OpenableColumns query failed: ${e.message}")
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
                            Log.i("DictFileImporter", "resolveDocumentName: got '$name' via DocumentsContract")
                            return name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("DictFileImporter", "resolveDocumentName: DocumentsContract query failed: ${e.message}")
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
                            Log.i("DictFileImporter", "resolveDocumentName: got '$name' via MediaStore")
                            return name
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("DictFileImporter", "resolveDocumentName: MediaStore query failed: ${e.message}")
            }
        }

        if (uri.authority == "com.android.providers.downloads.documents") {
            try {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.startsWith("raw:")) {
                    val filePath = docId.substringAfter("raw:")
                    val fileName = File(filePath).name
                    if (fileName.isNotBlank() && fileName.contains(".")) {
                        Log.i("DictFileImporter", "resolveDocumentName: got '$fileName' via raw path")
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
                            Log.i("DictFileImporter", "resolveDocumentName: got '$name' via Downloads")
                            return name
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("DictFileImporter", "resolveDocumentName: Downloads query failed: ${e.message}")
            }
        }

        if (uri.scheme == "content") {
            try {
                val docId = DocumentsContract.getDocumentId(uri)
                val decoded = try { java.net.URLDecoder.decode(docId, "UTF-8") } catch (_: Exception) { docId }
                val candidate = decoded.substringAfterLast('/')
                if (candidate.contains(".") && !candidate.matches(Regex("^\\d+$")) && !candidate.contains(":")) {
                    Log.i("DictFileImporter", "resolveDocumentName: got '$candidate' from documentId")
                    return candidate
                }
            } catch (_: Exception) {}
        }

        val lastSegment = uri.lastPathSegment ?: "unknown"
        val decoded = try { java.net.URLDecoder.decode(lastSegment, "UTF-8") } catch (_: Exception) { lastSegment }
        val candidate = decoded.substringAfterLast('/')
        Log.w("DictFileImporter", "resolveDocumentName: fallback to lastPathSegment '$candidate'")
        return candidate
    }

    fun isDictionaryFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mdx") || lower.endsWith(".mdd") ||
               lower.endsWith(".dsl") || lower.endsWith(".dsl.dz") ||
               lower.endsWith(".bgl") || lower.endsWith(".lsa") ||
               lower.endsWith(".lsd") || lower.endsWith(".slob") ||
               lower.endsWith(".zim") || lower.endsWith(".stardict") ||
               lower.endsWith(".ifo") || lower.endsWith(".idx") ||
               lower.endsWith(".dict") || lower.endsWith(".css")
    }

    fun detectMdxOrMddExtension(file: File): String? {
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

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[:\\\\/*?|<>]"), "_")
    }

    fun readMdxHeaderTitle(file: File): String {
        return readMdxHeaderTitleStatic(file)
    }

    companion object {
        fun readMdxHeaderTitleStatic(file: File): String {
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
    }

    fun copyToInternal(uri: Uri, displayName: String, targetDir: File): File? {
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
            Log.e("DictFileImporter", "copyToInternal FAILED '$displayName': ${e.message}")
            null
        }
    }

    fun copyToInternal(uri: Uri, targetDir: File): File? {
        val name = resolveDocumentName(uri)
        return copyToInternal(uri, name, targetDir)
    }

    fun findCompanionMdd(mdxFile: File): File? {
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
            Log.e("DictFileImporter", "scanDirectory failed: ${e.message}")
        }
        return candidates
    }
}
