package io.github.gdict.core

import io.github.gdict.core.GdictLogger.Companion.get as log
import java.io.File

interface FileSystemAccess {
    fun selectDictionaryFiles(): List<String>?
    fun selectDictionaryDirectory(): String?
    fun listFilesInDirectory(dirPath: String): List<String>
}

class DictFileImporter(private val fileSystemAccess: FileSystemAccess) {

    data class CopyResult(val files: List<File>, val primaryFile: File?)

    data class DictCandidate(
        val name: String,
        val filePath: String,
        val displayName: String,
        val companionFiles: List<String> = emptyList()
    )

    fun addOrUpdateDictionary(
        name: String,
        sourcePath: String,
        companionPaths: List<String> = emptyList(),
        nextId: Long,
        dataDir: File
    ): Pair<DictionaryManager.DictEntry, List<File>> {
        val id = nextId
        val dictDir = File(dataDir, "dictionaries/$id")
        dictDir.mkdirs()

        log().i("DictFileImporter", "=== addOrUpdateDictionary '$name' id=$id ===")
        log().i("DictFileImporter", "  sourcePath: $sourcePath")
        log().i("DictFileImporter", "  companionPaths: ${companionPaths.size}")

        val copyResult = copyDictionaryFiles(sourcePath, dictDir)
        val copiedFiles = copyResult.files.toMutableList()
        val primaryFile = copyResult.primaryFile
        log().i("DictFileImporter", "  Primary copy: ${primaryFile?.name} (${primaryFile?.length()} bytes)")
        log().i("DictFileImporter", "  Total copied: ${copiedFiles.size} files")
        for (f in copiedFiles) {
            log().i("DictFileImporter", "    -> ${f.name} (${f.length()} bytes)")
        }

        for (companionPath in companionPaths) {
            try {
                val compFile = copyFileToInternal(companionPath, dictDir)
                if (compFile != null) {
                    copiedFiles.add(compFile)
                    log().i("DictFileImporter", "  Companion: ${compFile.name} (${compFile.length()} bytes)")
                } else {
                    log().w("DictFileImporter", "  Companion copy failed: $companionPath")
                }
            } catch (e: Exception) {
                log().e("DictFileImporter", "  Companion exception: ${e.message}")
            }
        }

        var mdxFile = if (primaryFile != null && primaryFile.name.lowercase().endsWith(".mdx")) {
            log().i("DictFileImporter", "  Using primary file as MDX: ${primaryFile.name}")
            primaryFile
        } else {
            copiedFiles.firstOrNull { it.name.lowercase().endsWith(".mdx") }
        }
        if (mdxFile == null) {
            log().w("DictFileImporter", "  No .mdx file found by extension, trying content detection...")
            for (i in copiedFiles.indices) {
                val file = copiedFiles[i]
                val detected = detectMdxOrMddExtension(file)
                if (detected != null) {
                    val newName = sanitizeFileName(file.name) + detected
                    val newFile = File(file.parentFile, newName)
                    if (file.renameTo(newFile)) {
                        log().i("DictFileImporter", "  Detected MDX/MDD by header, renamed: ${file.name} -> $newName")
                        copiedFiles[i] = newFile
                        if (primaryFile == file) mdxFile = newFile
                    } else {
                        log().w("DictFileImporter", "  Failed to rename ${file.name} to $newName")
                    }
                }
            }
            if (mdxFile == null || !mdxFile.exists()) {
                mdxFile = copiedFiles.firstOrNull { it.name.lowercase().endsWith(".mdx") }
            }
        }
        if (mdxFile == null) {
            log().e("DictFileImporter", "  NO .mdx file found among ${copiedFiles.size} copied files!")
            for (f in copiedFiles) {
                log().e("DictFileImporter", "    existing file: ${f.name}")
            }
            dictDir.deleteRecursively()
            throw RuntimeException("未能从导入路径中找到 .mdx 词典文件，请确认选择了正确的 .mdx 文件")
        }

        val mdxTitle = readMdxHeaderTitle(mdxFile)
        log().i("DictFileImporter", "  MDX file verified: '${mdxFile.name}' (${mdxFile.length()} bytes) title='$mdxTitle'")

        val entry = DictionaryManager.DictEntry(
            id = id,
            name = name,
            path = sourcePath,
            dictFilePath = mdxFile.absolutePath,
            isEnabled = true
        )
        return Pair(entry, copiedFiles)
    }

    fun copyDictionaryFiles(sourcePath: String, targetDir: File): CopyResult {
        val copied = mutableListOf<File>()
        var primaryFile: File? = null
        try {
            val file = File(sourcePath)
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
        } catch (e: Exception) {
            log().e("DictFileImporter", "copyDictionaryFiles FAILED: ${e.message}", e)
        }
        return CopyResult(copied, primaryFile)
    }

    fun copyFileToInternal(sourcePath: String, targetDir: File): File? {
        return try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return null
            val targetFile = File(targetDir, sourceFile.name)
            sourceFile.copyTo(targetFile, overwrite = true)
            if (targetFile.exists() && targetFile.length() > 0) targetFile
            else { targetFile.delete(); null }
        } catch (e: Exception) {
            log().e("DictFileImporter", "copyFileToInternal FAILED '$sourcePath': ${e.message}")
            null
        }
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

    fun scanDirectory(dirPath: String): List<DictCandidate> {
        val candidates = mutableListOf<DictCandidate>()
        try {
            val dir = File(dirPath)
            if (!dir.isDirectory) return candidates

            val filesByBaseName = mutableMapOf<String, MutableList<Pair<String, String>>>()
            dir.listFiles()?.forEach { file ->
                if (!isDictionaryFile(file.name)) return@forEach
                val lowerName = file.name.lowercase()
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
                val baseName = file.name.dropLast(suffixLen)
                filesByBaseName.getOrPut(baseName) { mutableListOf() }
                    .add(file.name to file.absolutePath)
            }

            for ((baseName, files) in filesByBaseName) {
                val mdxFile = files.firstOrNull { it.first.lowercase().endsWith(".mdx") }
                if (mdxFile != null) {
                    candidates.add(DictCandidate(
                        name = baseName.ifEmpty { mdxFile.first.removeSuffix(".mdx") },
                        filePath = mdxFile.second,
                        displayName = mdxFile.first,
                        companionFiles = files.filter { it !== mdxFile }.map { it.second }
                    ))
                }
            }
        } catch (e: Exception) {
            log().e("DictFileImporter", "scanDirectory failed: ${e.message}")
        }
        return candidates
    }
}
