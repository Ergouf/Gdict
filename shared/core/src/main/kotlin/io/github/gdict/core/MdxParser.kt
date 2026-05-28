package io.github.gdict.core

import io.github.gdict.core.GdictLogger.Companion.get as log
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Adler32
import java.util.zip.Inflater

/**
 * MDX词典文件解析器
 *
 * 支持MDX V1.2和V2.0两种规范，可解析MDX词典文件和MDD资源文件。
 *
 * MDX文件整体结构（从文件头到文件尾）：
 * ┌──────────────────────────────────────────────┐
 * │ 1. Header Section   - 词典元信息（XML格式）   │
 * │ 2. Keyword Section  - 关键词索引 + 关键词块    │
 * │ 3. Record Section   - 释义记录索引 + 记录数据块 │
 * └──────────────────────────────────────────────┘
 *
 * V1.2和V2.0的主要区别：
 * - V1.2: 整数字段为4字节BE（Big-Endian），关键词索引不压缩
 * - V2.0: 整数字段为8字节BE（64位），关键词索引经过压缩
 *
 * 压缩块的前8字节为压缩头：
 *   [0..3] 压缩类型（小端序）：0=不压缩, 1=LZO, 2=zlib
 *   [4..7] Adler32校验和（大端序）
 *   [8..]  实际压缩数据
 */
class MdxParser(private val mdxFile: File) : Closeable {

    var title: String = ""
    var encoding: String = "UTF-8"
    var isKeyCaseSensitive: Boolean = false
    var wordCount: Int = 0

    private var engineVersion: Double = 2.0
    private var bpu: Int = 1
    private var numberWidth: Int = 8
    private var encrypt: Int = 0

    private val keywordIndex = mutableListOf<KeywordEntry>()
    private val recordBlockInfos = mutableListOf<RecordBlockInfo>()
    private val lowercaseWordMap = mutableMapOf<String, MutableList<Int>>()
    private var sortedLowercaseEntries: List<Pair<String, Int>> = emptyList()

    private val raf: RandomAccessFile = RandomAccessFile(mdxFile, "r")
    @Volatile
    private var closed = false
    @Volatile
    private var parseFailed = false

    var isResourceMode = false
        private set
    private var keywordSectionStart: Long = -1
    private var keywordSectionEnd: Long = -1

    init {
        try {
            parse()
        } catch (e: Exception) {
            log().e(TAG, "解析MDX文件失败: ${e.message}", e)
            parseFailed = true
            close()
        }
    }

    override fun close() {
        synchronized(raf) {
            if (closed) return
            closed = true
            try { raf.close() } catch (_: Exception) {}
        }
    }

    val companionCss: String by lazy {
        loadCompanionCss()
    }

    private fun loadCompanionCss(): String {
        val sb = StringBuilder()
        val parentDir = mdxFile.parentFile ?: return ""
        val baseName = mdxFile.nameWithoutExtension

        val cssCandidates = listOf(
            File(parentDir, "$baseName.css"),
            File(parentDir, "${mdxFile.name}.css")
        ) + (parentDir.listFiles()?.filter { it.isFile && it.name.lowercase().endsWith(".css") } ?: emptyList()).distinctBy { it.absolutePath }

        for (cssFile in cssCandidates) {
            if (cssFile.exists() && cssFile.length() > 0 && cssFile.length() < 5 * 1024 * 1024) {
                try {
                    sb.append(cssFile.readText(Charsets.UTF_8))
                    sb.append("\n")
                    log().i(TAG, "Loaded CSS from: ${cssFile.name} (${cssFile.length()} bytes)")
                } catch (e: Exception) {
                    log().w(TAG, "Failed to read CSS: ${cssFile.name}: ${e.message}")
                }
            }
        }

        return sb.toString()
    }

    fun transformHtml(raw: String): String = transformHtmlStatic(raw)

    fun readArticles(word: String): Map<String, String?> {
        val results = mutableMapOf<String, String?>()
        if (closed || parseFailed || keywordIndex.isEmpty()) return results

        val idx = findFirstKeywordIndex(word)
        if (idx != null) {
            var i = idx
            while (i < keywordIndex.size && keywordIndex[i].word == word) {
                val entry = keywordIndex[i]
                results[entry.word] = readRecord(entry.recordOffset, entry.recordSize)
                i++
            }
        }

        if (results.isEmpty()) {
            val lower = word.lowercase()
            val indices = lowercaseWordMap[lower]
            if (indices != null) {
                for (i in indices) {
                    val entry = keywordIndex[i]
                    results[entry.word] = readRecord(entry.recordOffset, entry.recordSize)
                }
            }
        }

        return results
    }

    fun readArticlesPredictive(prefix: String): Map<String, String?> {
        val results = mutableMapOf<String, String?>()
        if (closed || parseFailed || keywordIndex.isEmpty() || prefix.isEmpty()) return results

        val lower = prefix.lowercase()

        var lo = 0
        var hi = sortedLowercaseEntries.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sortedLowercaseEntries[mid].first < lower) {
                lo = mid + 1
            } else {
                hi = mid
            }
        }

        for (i in lo until sortedLowercaseEntries.size) {
            val (wordLower, origIdx) = sortedLowercaseEntries[i]
            if (!wordLower.startsWith(lower)) break
            val entry = keywordIndex[origIdx]
            results[entry.word] = readRecord(entry.recordOffset, entry.recordSize)
            if (results.size >= 20) break
        }

        return results
    }

    private fun findFirstKeywordIndex(word: String): Int? {
        var lo = 0
        var hi = keywordIndex.size - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cmp = keywordIndex[mid].word.compareTo(word)
            when {
                cmp < 0 -> lo = mid + 1
                cmp > 0 -> hi = mid - 1
                else -> {
                    found = mid
                    hi = mid - 1
                }
            }
        }
        return if (found >= 0) found else null
    }

    fun getAllKeywords(): List<String> = keywordIndex.map { it.word }

    fun readResourceBytes(key: String): ByteArray? {
        if (closed || parseFailed) return null
        if (!isResourceMode && keywordIndex.isEmpty()) return null
        if (isResourceMode) return readResourceBytesStream(key)
        val normalizedKey = if (key.startsWith("\\")) key else "\\$key"
        val idx = findFirstKeywordIndex(normalizedKey)
            ?: findFirstKeywordIndex(key)
            ?: return null
        val entry = keywordIndex[idx]
        return readRecordBytes(entry.recordOffset, entry.recordSize)
    }

    fun findResourceKeys(suffix: String): List<String> {
        if (closed || parseFailed) return emptyList()
        if (isResourceMode) return findResourceKeysStream(suffix)
        if (keywordIndex.isEmpty()) return emptyList()
        val lowerSuffix = suffix.lowercase()
        return keywordIndex.filter {
            it.word.lowercase().endsWith(lowerSuffix)
        }.map { it.word }
    }

    fun readResourceBytesByKey(key: String): ByteArray? {
        if (closed || parseFailed) return null
        if (isResourceMode) return readResourceBytesStream(key)
        if (keywordIndex.isEmpty()) return null
        val idx = findFirstKeywordIndex(key) ?: return null
        val entry = keywordIndex[idx]
        return readRecordBytes(entry.recordOffset, entry.recordSize)
    }

    private fun readRecordBytes(offset: Long, size: Int): ByteArray? {
        if (closed || parseFailed || recordBlockInfos.isEmpty()) return null
        val idx = findRecordBlockIndex(offset) ?: return null
        val rbInfo = recordBlockInfos[idx]
        if (rbInfo.compressedSize > Int.MAX_VALUE || rbInfo.decompressedSize > Int.MAX_VALUE) return null
        synchronized(raf) {
            try {
                raf.seek(rbInfo.compressedOffset)
                val data = ByteArray(rbInfo.compressedSize.toInt())
                raf.readFully(data)
                val decompressed = decompressBlock(data, rbInfo.decompressedSize.toInt())
                val recordStart = (offset - rbInfo.recordStartOffset).toInt()
                if (recordStart < 0 || recordStart >= decompressed.size) return null
                val actualSize = if (size > 0) {
                    size.coerceAtMost(decompressed.size - recordStart)
                } else {
                    findNullLength(decompressed, recordStart, bpu)
                }
                if (actualSize <= 0) return null
                return decompressed.copyOfRange(recordStart, recordStart + actualSize)
            } catch (e: Exception) {
                log().e(TAG, "readRecordBytes error at $offset: ${e.message}")
                return null
            }
        }
    }

    private fun readResourceBytesStream(key: String): ByteArray? {
        if (keywordSectionStart < 0 || recordBlockInfos.isEmpty()) return null
        val targetKey = if (key.startsWith("\\")) key else "\\$key"
        log().d(TAG, "Stream resource lookup: '$targetKey'")
        synchronized(raf) {
            val savedPosition = raf.filePointer
            try {
                raf.seek(keywordSectionStart)

                val numKeyBlocks: Long
                val keyIndexDecompLen: Long
                val keyIndexCompLen: Long

                if (engineVersion >= 2.0) {
                    numKeyBlocks = raf.readLongBE()
                    raf.readLongBE()
                    keyIndexDecompLen = raf.readLongBE()
                    keyIndexCompLen = raf.readLongBE()
                    raf.readLongBE()
                    raf.readIntBE()
                } else {
                    numKeyBlocks = raf.readIntBE().toLong()
                    raf.readIntBE()
                    keyIndexCompLen = raf.readIntBE().toLong()
                    raf.readIntBE()
                    keyIndexDecompLen = 0
                }

                log().d(TAG, "Stream header: numKeyBlocks=$numKeyBlocks idxComp=$keyIndexCompLen idxDecomp=$keyIndexDecompLen")

                if (numKeyBlocks <= 0 || numKeyBlocks > 10000000 || keyIndexCompLen <= 0) {
                    log().w(TAG, "Stream: invalid header values")
                    return null
                }

                if (keyIndexCompLen > 100 * 1024 * 1024) {
                    log().w(TAG, "Stream: keyIndexCompLen too large ($keyIndexCompLen), cannot stream")
                    return null
                }

                val keyIndexRaw = ByteArray(keyIndexCompLen.toInt())
                raf.readFully(keyIndexRaw)

                if ((encrypt and 2) != 0 && keyIndexRaw.size > 8) {
                    val checksumBytes = keyIndexRaw.copyOfRange(4, 8)
                    val keyInput = checksumBytes + byteArrayOf(0x95.toByte(), 0x36.toByte(), 0x00.toByte(), 0x00.toByte())
                    val decryptKey = RipeMD128.digest(keyInput)
                    fastDecrypt(keyIndexRaw, 8, decryptKey)
                    log().d(TAG, "Stream: key index decrypted")
                }

                val keyIndexData = if (engineVersion >= 2.0 && keyIndexDecompLen > 0) {
                    decompressBlock(keyIndexRaw, keyIndexDecompLen.toInt())
                } else keyIndexRaw

                val blockMetas = decodeKeyBlockInfo(keyIndexData, numKeyBlocks.toInt())
                log().d(TAG, "Stream mode: ${blockMetas.size} blocks, searching for '$targetKey'")

                for ((blockIdx, meta) in blockMetas.withIndex()) {
                    if (meta.compSize <= 0 || meta.compSize > 50 * 1024 * 1024) {
                        log().w(TAG, "Stream: skip block $blockIdx compSize=${meta.compSize}")
                        continue
                    }
                    val compData = ByteArray(meta.compSize.toInt())
                    raf.readFully(compData)
                    val blockData = decompressBlock(compData, meta.decompSize)
                    val stream = ByteStream(blockData)

                    while (stream.remaining >= numberWidth) {
                        val recordOffset = if (engineVersion >= 2.0) stream.readLongBE() else stream.readIntBE().toLong()
                        if (stream.remaining == 0) break
                        val wordBytes = stream.readNullTerminated(bpu)
                        val word = String(wordBytes, charset(encoding))
                        if (word.equals(targetKey, ignoreCase = true) || word.endsWith(targetKey, ignoreCase = true)) {
                            log().i(TAG, "Stream found: '$word' at offset=$recordOffset")
                            return readRecordBytes(recordOffset, 0)
                        }
                    }
                }
                log().w(TAG, "Stream lookup not found: '$targetKey'")
                return null
            } catch (e: Exception) {
                log().e(TAG, "Stream resource error: ${e.message}")
                return null
            } finally {
                raf.seek(savedPosition)
            }
        }
    }

    private fun findResourceKeysStream(suffix: String): List<String> {
        if (keywordSectionStart < 0) return emptyList()
        val results = mutableListOf<String>()
        val lowerSuffix = suffix.lowercase()
        synchronized(raf) {
            val savedPosition = raf.filePointer
            try {
                raf.seek(keywordSectionStart)

                val numKeyBlocks: Long
                val keyIndexDecompLen: Long
                val keyIndexCompLen: Long

                if (engineVersion >= 2.0) {
                    numKeyBlocks = raf.readLongBE()
                    raf.readLongBE()
                    keyIndexDecompLen = raf.readLongBE()
                    keyIndexCompLen = raf.readLongBE()
                    raf.readLongBE()
                    raf.readIntBE()
                } else {
                    numKeyBlocks = raf.readIntBE().toLong()
                    raf.readIntBE()
                    keyIndexCompLen = raf.readIntBE().toLong()
                    raf.readIntBE()
                    keyIndexDecompLen = 0
                }

                if (numKeyBlocks <= 0 || numKeyBlocks > 10000000 || keyIndexCompLen <= 0 || keyIndexCompLen > 100 * 1024 * 1024) return emptyList()

                val keyIndexRaw = ByteArray(keyIndexCompLen.toInt())
                raf.readFully(keyIndexRaw)

                if ((encrypt and 2) != 0 && keyIndexRaw.size > 8) {
                    val checksumBytes = keyIndexRaw.copyOfRange(4, 8)
                    val keyInput = checksumBytes + byteArrayOf(0x95.toByte(), 0x36.toByte(), 0x00.toByte(), 0x00.toByte())
                    val key = RipeMD128.digest(keyInput)
                    fastDecrypt(keyIndexRaw, 8, key)
                }

                val keyIndexData = if (engineVersion >= 2.0 && keyIndexDecompLen > 0) decompressBlock(keyIndexRaw, keyIndexDecompLen.toInt()) else keyIndexRaw

                val blockMetas = decodeKeyBlockInfo(keyIndexData, numKeyBlocks.toInt())
                for (meta in blockMetas) {
                    if (results.size >= 50 || meta.compSize <= 0 || meta.compSize > 10*1024*1024) continue
                    val compData = ByteArray(meta.compSize.toInt())
                    raf.readFully(compData)
                    val blockData = decompressBlock(compData, meta.decompSize)
                    val stream = ByteStream(blockData)
                    while (stream.remaining >= numberWidth && results.size < 50) {
                        if (engineVersion >= 2.0) stream.readLongBE() else stream.readIntBE()
                        if (stream.remaining == 0) break
                        val wordBytes = stream.readNullTerminated(bpu)
                        val word = String(wordBytes, charset(encoding))
                        if (word.lowercase().endsWith(lowerSuffix)) results.add(word)
                    }
                }
            } catch (e: Exception) {
                log().e(TAG, "findResourceKeysStream error: ${e.message}")
            } finally {
                raf.seek(savedPosition)
            }
        }
        return results
    }

    val filePath: String get() = mdxFile.absolutePath
    val fileName: String get() = mdxFile.name
    val fileSize: Long get() = mdxFile.length()

    fun diagnose(): String {
        val sb = StringBuilder()
        sb.appendLine("MdxParser Diagnostics:")
        sb.appendLine("  file='$mdxFile' (${mdxFile.length()} bytes)")
        sb.appendLine("  title='$title' encoding='$encoding' encrypt=$encrypt")
        sb.appendLine("  engineVersion=$engineVersion bpu=$bpu numberWidth=$numberWidth")
        sb.appendLine("  wordCount=$wordCount caseSensitive=$isKeyCaseSensitive")
        sb.appendLine("  keywordBlocks=${keywordIndex.size} recordBlocks=${recordBlockInfos.size}")
        sb.appendLine("  isResourceMode=$isResourceMode keywordSectionStart=$keywordSectionStart keywordSectionEnd=$keywordSectionEnd")
        sb.appendLine("  closed=$closed")

        if (keywordSectionStart > 0 && !closed) {
            try {
                synchronized(raf) {
                    raf.seek(keywordSectionStart)
                    if (engineVersion >= 2.0) {
                        val nkb = raf.readLongBE()
                        val te = raf.readLongBE()
                        val idxDec = raf.readLongBE()
                        val idxComp = raf.readLongBE()
                        val kbLen = raf.readLongBE()
                        val chk = raf.readIntBE()
                        sb.appendLine("  kwHeader: numKeyBlocks=$nkb totalEntries=$te idxDecomp=$idxDec idxComp=$idxComp keyBlocksLen=$kbLen checksum=$chk")
                        if (idxComp > 0 && idxComp < 100 * 1024 * 1024) {
                            val raw = ByteArray(idxComp.toInt())
                            raf.readFully(raw)
                            sb.appendLine("  kwIdxRaw first16: ${raw.take(16).joinToString(" ") { "%02X".format(it) }}")
                            if ((encrypt and 2) != 0 && raw.size > 8) {
                                val checksumBytes = raw.copyOfRange(4, 8)
                                val keyInput = checksumBytes + byteArrayOf(0x95.toByte(), 0x36.toByte(), 0x00.toByte(), 0x00.toByte())
                                val key = RipeMD128.digest(keyInput)
                                sb.appendLine("  decryptKey: ${key.joinToString(" ") { "%02X".format(it) }}")
                                fastDecrypt(raw, 8, key)
                                sb.appendLine("  kwIdxDecrypted first16: ${raw.take(16).joinToString(" ") { "%02X".format(it) }}")
                                val compType = (raw[0].toInt() and 0xFF) or ((raw[1].toInt() and 0xFF) shl 8) or ((raw[2].toInt() and 0xFF) shl 16) or ((raw[3].toInt() and 0xFF) shl 24)
                                sb.appendLine("  compType after decrypt: $compType")
                            }
                            try {
                                val decomp = decompressBlock(raw, idxDec.toInt())
                                sb.appendLine("  kwIdxDecomp: ${decomp.size} bytes (expected $idxDec)")
                                if (decomp.size > 0) {
                                    sb.appendLine("  kwIdxDecomp first32: ${decomp.take(32).joinToString(" ") { "%02X".format(it) }}")
                                    val metas = decodeKeyBlockInfo(decomp, nkb.toInt())
                                    sb.appendLine("  blockMetas from stream: ${metas.size}")
                                    if (metas.isNotEmpty()) {
                                        sb.appendLine("  meta[0]: numEntries=${metas[0].numEntries} compSize=${metas[0].compSize} decompSize=${metas[0].decompSize}")
                                    }
                                }
                            } catch (e: Exception) {
                                sb.appendLine("  kwIdxDecomp FAILED: ${e.javaClass.simpleName}: ${e.message}")
                            }
                        }
                    } else {
                        val nkb = raf.readIntBE()
                        val te = raf.readIntBE()
                        val idxComp = raf.readIntBE()
                        val kbLen = raf.readIntBE()
                        sb.appendLine("  kwHeader: numKeyBlocks=$nkb totalEntries=$te idxComp=$idxComp keyBlocksLen=$kbLen")
                        if (idxComp > 0 && idxComp < 100 * 1024 * 1024) {
                            val raw = ByteArray(idxComp)
                            raf.readFully(raw)
                            sb.appendLine("  kwIdxRaw first16: ${raw.take(16).joinToString(" ") { "%02X".format(it) }}")
                            try {
                                val metas = decodeKeyBlockInfo(raw, nkb)
                                sb.appendLine("  blockMetas from V1 idx: ${metas.size}")
                                if (metas.isNotEmpty()) {
                                    sb.appendLine("  meta[0]: numEntries=${metas[0].numEntries} compSize=${metas[0].compSize} decompSize=${metas[0].decompSize}")
                                }
                            } catch (e: Exception) {
                                sb.appendLine("  V1 idx parse FAILED: ${e.javaClass.simpleName}: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                sb.appendLine("  kwHeader read error: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        if (recordBlockInfos.isNotEmpty()) {
            sb.appendLine("  recordBlockInfo[0]: startOffset=${recordBlockInfos[0].recordStartOffset} " +
                "compOffset=${recordBlockInfos[0].compressedOffset} compSize=${recordBlockInfos[0].compressedSize} decompSize=${recordBlockInfos[0].decompressedSize}")
        }

        if (keywordIndex.isNotEmpty()) {
            val first = keywordIndex.first()
            sb.appendLine("  firstKeyword: word='${first.word}' recordOffset=${first.recordOffset} recordSize=${first.recordSize}")
            val last = keywordIndex.last()
            sb.appendLine("  lastKeyword: word='${last.word}' recordOffset=${last.recordOffset} recordSize=${last.recordSize}")

            val testResult = try {
                val articles = readArticles(first.word)
                val def = articles.values.firstOrNull()
                val preview = def?.take(100)?.replace("\n", "\\n") ?: "(null)"
                "readArticles('${first.word}') → ${articles.size} results, first_def='$preview'"
            } catch (e: Exception) {
                "readArticles ERROR: ${e.javaClass.simpleName}: ${e.message}"
            }
            sb.appendLine("  $testResult")
        }
        return sb.toString()
    }

    /**
     * 根据记录偏移和大小读取词条的释义内容
     *
     * 工作流程：
     * 1. 用二分查找定位该偏移所在的记录块（RecordBlock）
     * 2. 读取并解压整个记录块
     * 3. 在解压数据中按偏移截取对应词条内容
     *
     * 关键修复：UTF-16编码时，记录数据是按UTF-16LE存储的，
     * 每个字符占2字节，null终止符也是2字节（0x00 0x00）。
     * 必须确保偏移对齐到2字节边界。
     */
    private fun readRecord(offset: Long, size: Int): String? {
        if (closed || parseFailed || recordBlockInfos.isEmpty()) return null
        val idx = findRecordBlockIndex(offset) ?: return null
        val rbInfo = recordBlockInfos[idx]
        if (rbInfo.compressedSize > Int.MAX_VALUE || rbInfo.decompressedSize > Int.MAX_VALUE) {
            log().e(TAG, "readRecord: block size exceeds Int.MAX_VALUE")
            return null
        }
        synchronized(raf) {
            try {
                raf.seek(rbInfo.compressedOffset)
                val data = ByteArray(rbInfo.compressedSize.toInt())
                raf.readFully(data)
                val decompressed = decompressBlock(data, rbInfo.decompressedSize.toInt())
                val recordStart = (offset - rbInfo.recordStartOffset).toInt()
                if (recordStart < 0 || recordStart >= decompressed.size) return null

                val actualSize = if (size > 0) {
                    size.coerceAtMost(decompressed.size - recordStart)
                } else {
                    findNullLength(decompressed, recordStart, bpu)
                }
                if (actualSize <= 0) return null
                val bytes = decompressed.copyOfRange(recordStart, recordStart + actualSize)
                return decodeRecordString(bytes)
            } catch (e: Exception) {
                log().e(TAG, "readRecord error at $offset: ${e.message}")
                return null
            }
        }
    }

    private fun findNullLength(data: ByteArray, start: Int, bpu: Int): Int {
        var pos = start
        while (pos + bpu <= data.size) {
            var isNull = true
            for (i in 0 until bpu) {
                if (data[pos + i] != 0.toByte()) { isNull = false; break }
            }
            if (isNull) return pos - start
            pos += bpu
        }
        return data.size - start
    }

    private fun findRecordBlockIndex(offset: Long): Int? {
        var lo = 0
        var hi = recordBlockInfos.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (recordBlockInfos[mid].recordStartOffset <= offset) {
                if (mid == recordBlockInfos.lastIndex || recordBlockInfos[mid + 1].recordStartOffset > offset) {
                    return mid
                }
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return null
    }

    /**
     * 解码记录内容字符串
     *
     * 与关键词解码不同，记录内容（HTML释义）的编码处理更复杂：
     * - UTF-16LE: 直接按UTF-16LE解码，需处理可能的BOM
     * - UTF-8: 直接解码
     * - 其他编码: 使用对应字符集解码
     */
    private fun decodeRecordString(bytes: ByteArray): String {
        return try {
            when (encoding.uppercase().replace("-", "")) {
                "UTF8", "UTF" -> String(bytes, Charsets.UTF_8)
                "UTF16", "UTF16LE" -> {
                    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
                        String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
                    } else if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                        String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
                    } else {
                        String(bytes, Charsets.UTF_16LE)
                    }
                }
                "UTF16BE" -> String(bytes, Charsets.UTF_16BE)
                "GBK", "GB2312", "GB18030" -> String(bytes, charset("GB18030"))
                "BIG5", "BIG5HKSCS" -> String(bytes, charset("Big5"))
                else -> String(bytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            log().w(TAG, "解码字符串失败: ${e.message}")
            String(bytes, Charsets.ISO_8859_1)
        }
    }

    /**
     * 解码关键词文本
     *
     * 关键词在MDX文件中的存储方式：
     * - V1.2 UTF-8: 每个关键词以单个0x00结尾
     * - V1.2 UTF-16: 每个关键词以0x00 0x00结尾
     * - V2.0 UTF-8: 每个关键词以单个0x00结尾
     * - V2.0 UTF-16: 每个关键词以0x00 0x00结尾
     *
     * 关键修复：V2.0中关键词文本的长度字段(firstSize/lastSize)
     * 表示的是字符数而非字节数，需要乘以bpu才是实际字节数。
     */
    private fun decodeKeyWord(bytes: ByteArray): String {
        val decoded = try {
            when (encoding.uppercase().replace("-", "")) {
                "UTF8", "UTF" -> String(bytes, Charsets.UTF_8)
                "UTF16", "UTF16LE" -> String(bytes, Charsets.UTF_16LE)
                "UTF16BE" -> String(bytes, Charsets.UTF_16BE)
                "GBK", "GB2312", "GB18030" -> String(bytes, charset("GB18030"))
                "BIG5", "BIG5HKSCS" -> String(bytes, charset("Big5"))
                else -> String(bytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            String(bytes, Charsets.ISO_8859_1)
        }

        if (encoding.uppercase().replace("-", "") in listOf("UTF8", "UTF") && looksDoubleEncoded(decoded)) {
            return try {
                val rawBytes = decoded.toByteArray(Charsets.ISO_8859_1)
                val fixed = String(rawBytes, Charsets.UTF_8)
                if (!fixed.contains('\uFFFD')) fixed else decoded
            } catch (_: Exception) {
                decoded
            }
        }

        return decoded
    }

    private fun looksDoubleEncoded(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val c = s[i].code
            if (c in 0xC2..0xC3 && i + 1 < s.length) {
                val next = s[i + 1].code
                if (next in 0x80..0xBF) return true
            }
            i++
        }
        return false
    }

    private fun parse() {
        val fileSize = mdxFile.length()
        log().i(TAG, "开始解析文件: ${mdxFile.name} (${fileSize} bytes)")
        if (fileSize < 12) {
            throw IllegalArgumentException("文件太小: ${mdxFile.name} ($fileSize bytes < 12)")
        }
        parseHeader()
        keywordSectionStart = raf.filePointer
        parseKeywordSection()
        if (wordCount == 0 && mdxFile.length() > 10 * 1024 * 1024) {
            log().w(TAG, "大文件MDD关键词区为空，启用资源流式模式")
            isResourceMode = true
        }
        parseRecordSection()
        log().i(TAG, "解析完成: title='$title' words=$wordCount blocks=${recordBlockInfos.size} encoding='$encoding' version=$engineVersion bpu=$bpu resourceMode=$isResourceMode")
    }

    private fun parseHeader() {
        val headerLen = raf.readIntBE()
        if (headerLen <= 0 || headerLen > 100 * 1024 * 1024) {
            throw IllegalArgumentException("无效的MDX文件: header长度异常 $headerLen")
        }
        val headerBytes = ByteArray(headerLen)
        raf.readFully(headerBytes)
        raf.readIntLE()

        val headerStr = String(headerBytes, Charsets.UTF_16LE)

        title = extractAttr(headerStr, "Title") ?: mdxFile.nameWithoutExtension
        val rawEncoding = extractAttr(headerStr, "Encoding")?.ifBlank { null }
        encoding = rawEncoding ?: if (engineVersion >= 2.0) "UTF-16LE" else "UTF-8"
        isKeyCaseSensitive = extractAttr(headerStr, "KeyCaseSensitive")?.equals("Yes", ignoreCase = true) ?: false
        encrypt = extractAttr(headerStr, "Encrypted")?.let {
            when {
                it.equals("No", ignoreCase = true) -> 0
                it.equals("Yes", ignoreCase = true) -> 1
                else -> it.toIntOrNull() ?: 0
            }
        } ?: 0
        engineVersion = extractAttr(headerStr, "GeneratedByEngineVersion")?.toDoubleOrNull() ?: 2.0

        bpu = if (encoding.uppercase().replace("-", "").startsWith("UTF16")) 2 else 1
        numberWidth = if (engineVersion >= 2.0) 8 else 4

        log().i(TAG, "Header: title='$title' encoding='$encoding' (raw='$rawEncoding') encrypt=$encrypt caseSensitive=$isKeyCaseSensitive engineVer=$engineVersion headerLen=$headerLen bpu=$bpu numberWidth=$numberWidth")
    }

    private fun parseKeywordSection() {
        val numKeyBlocks: Long
        val totalEntries: Long
        val keyIndexDecompLen: Long
        val keyIndexCompLen: Long
        val keyBlocksLen: Long

        if (engineVersion >= 2.0) {
            numKeyBlocks = raf.readLongBE()
            totalEntries = raf.readLongBE()
            keyIndexDecompLen = raf.readLongBE()
            keyIndexCompLen = raf.readLongBE()
            keyBlocksLen = raf.readLongBE()
            raf.readIntBE()
            log().i(TAG, "Keyword V2: numKeyBlocks=$numKeyBlocks totalEntries=$totalEntries idxDecomp=$keyIndexDecompLen idxComp=$keyIndexCompLen keyBlocksLen=$keyBlocksLen")
        } else {
            numKeyBlocks = raf.readIntBE().toLong()
            totalEntries = raf.readIntBE().toLong()
            keyIndexCompLen = raf.readIntBE().toLong()
            keyBlocksLen = raf.readIntBE().toLong()
            keyIndexDecompLen = 0
            log().i(TAG, "Keyword V1: numKeyBlocks=$numKeyBlocks totalEntries=$totalEntries idxLen=$keyIndexCompLen keyBlocksLen=$keyBlocksLen")
        }

        val sectionEndPos = raf.filePointer + keyIndexCompLen + keyBlocksLen
        keywordSectionEnd = sectionEndPos

        if (numKeyBlocks <= 0 || numKeyBlocks > 10000000) {
            log().e(TAG, "无效的numKeyBlocks: $numKeyBlocks, seeking to record section")
            raf.seek(sectionEndPos)
            return
        }

        val fileSize = mdxFile.length()
        val currentPos = raf.filePointer
        val remainingBytes = fileSize - currentPos

        if (keyIndexCompLen <= 0 || keyIndexCompLen > remainingBytes) {
            log().e(TAG, "无效的keyIndexCompLen: $keyIndexCompLen (文件剩余: $remainingBytes bytes), seeking to record section")
            raf.seek(sectionEndPos)
            return
        }

        if (keyIndexDecompLen > 0 && keyIndexDecompLen > 200 * 1024 * 1024) {
            log().e(TAG, "keyIndexDecompLen过大: ${keyIndexDecompLen} (>200MB), 启用流式模式, seeking to record section")
            raf.seek(sectionEndPos)
            return
        }

        val safeCompLen = minOf(keyIndexCompLen.toInt(), remainingBytes.toInt())
        val keyIndexRaw = ByteArray(safeCompLen)
        raf.readFully(keyIndexRaw)

        if ((encrypt and 2) != 0 && keyIndexRaw.size > 8) {
            val checksumBytes = keyIndexRaw.copyOfRange(4, 8)
            val keyInput = checksumBytes + byteArrayOf(0x95.toByte(), 0x36.toByte(), 0x00.toByte(), 0x00.toByte())
            val key = RipeMD128.digest(keyInput)
            fastDecrypt(keyIndexRaw, 8, key)
            log().i(TAG, "Keyword index decrypted: keyIndexCompLen=$keyIndexCompLen")
        }

        val keyIndexData = if (engineVersion >= 2.0) {
            decompressBlock(keyIndexRaw, keyIndexDecompLen.toInt())
        } else {
            keyIndexRaw
        }

        var blockMetas = decodeKeyBlockInfo(keyIndexData, numKeyBlocks.toInt())

        val maxCompSize = keyBlocksLen + keyIndexCompLen + 1024
        val needsUtf16Fallback = blockMetas.any { it.compSize <= 0 || it.compSize > maxCompSize || it.decompSize <= 0 || it.decompSize > 500 * 1024 * 1024 }
        if (needsUtf16Fallback && bpu == 1) {
            log().w(TAG, "Key block info解析异常(compSize超出范围)，尝试UTF-16LE编码 (bpu=2)")
            val origBpu = bpu
            val origEncoding = encoding
            bpu = 2
            encoding = "UTF-16LE"
            blockMetas = decodeKeyBlockInfo(keyIndexData, numKeyBlocks.toInt())
            val stillBad = blockMetas.any { it.compSize <= 0 || it.compSize > maxCompSize || it.decompSize <= 0 || it.decompSize > 500 * 1024 * 1024 }
            if (stillBad) {
                log().w(TAG, "UTF-16LE回退也失败，恢复原编码")
                bpu = origBpu
                encoding = origEncoding
                blockMetas = decodeKeyBlockInfo(keyIndexData, numKeyBlocks.toInt())
            } else {
                log().i(TAG, "UTF-16LE回退成功! blockMetas=${blockMetas.size}, meta[0]: compSize=${blockMetas[0].compSize} decompSize=${blockMetas[0].decompSize}")
            }
        }

        log().i(TAG, "Key block info解析完成: ${blockMetas.size} blocks (encoding=$encoding bpu=$bpu)")

        val maxBlockSize = 50 * 1024 * 1024
        var totalAllocated = 0L

        for ((blockIdx, meta) in blockMetas.withIndex()) {
            if (meta.compSize <= 0 || meta.compSize > maxBlockSize) {
                log().w(TAG, "  Block $blockIdx: 跳过异常块 compSize=${meta.compSize} (max=$maxBlockSize)")
                continue
            }
            if (totalAllocated + meta.compSize > 200 * 1024 * 1024) {
                log().w(TAG, "  Block $blockIdx: 总分配超限，跳过 (已分配=${totalAllocated})")
                break
            }

            val blockCompData = ByteArray(meta.compSize.toInt())
            raf.readFully(blockCompData)
            totalAllocated += meta.compSize

            val blockData = decompressBlock(blockCompData, meta.decompSize)

            if (blockData.size != meta.decompSize) {
                log().w(TAG, "  Block $blockIdx: decomp size mismatch! expected=${meta.decompSize} actual=${blockData.size}")
            }

            val blockStream = ByteStream(blockData)

            var entriesInBlock = 0
            while (blockStream.remaining >= numberWidth) {
                val recordOffset = if (engineVersion >= 2.0) {
                    blockStream.readLongBE()
                } else {
                    blockStream.readIntBE().toLong()
                }
                if (blockStream.remaining == 0) break
                val wordBytes = blockStream.readNullTerminated(bpu)
                val decodedWord = decodeKeyWord(wordBytes)

                keywordIndex.add(KeywordEntry(
                    word = decodedWord,
                    recordOffset = recordOffset,
                    recordSize = 0
                ))
                entriesInBlock++
            }

            if (entriesInBlock != meta.numEntries) {
                log().w(TAG, "  Block $blockIdx: entry count mismatch! parsed=$entriesInBlock expected=${meta.numEntries} compSize=${meta.compSize} decompSize=${meta.decompSize}")
            }
            if (blockIdx < 5 || entriesInBlock != meta.numEntries) {
                log().d(TAG, "  Block $blockIdx: $entriesInBlock entries (meta=${meta.numEntries}) remaining=${blockStream.remaining}")
            }
        }

        keywordIndex.sortBy { it.word }

        for (i in keywordIndex.indices) {
            if (i < keywordIndex.size - 1) {
                val nextOffset = keywordIndex[i + 1].recordOffset
                val currOffset = keywordIndex[i].recordOffset
                if (nextOffset > currOffset) {
                    keywordIndex[i] = keywordIndex[i].copy(recordSize = (nextOffset - currOffset).toInt())
                }
            }
        }

        val totalDecompSize = recordBlockInfos.sumOf { it.decompressedSize }
        if (keywordIndex.isNotEmpty() && keywordIndex.last().recordSize == 0 && totalDecompSize > 0) {
            val lastOffset = keywordIndex.last().recordOffset
            keywordIndex[keywordIndex.lastIndex] = keywordIndex.last().copy(
                recordSize = (totalDecompSize - lastOffset).toInt().coerceAtLeast(0)
            )
        }

        for (i in keywordIndex.indices) {
            val lower = keywordIndex[i].word.lowercase()
            lowercaseWordMap.getOrPut(lower) { mutableListOf() }.add(i)
        }
        sortedLowercaseEntries = keywordIndex.indices.map { i ->
            Pair(keywordIndex[i].word.lowercase(), i)
        }.sortedBy { it.first }

        wordCount = keywordIndex.size
        log().i(TAG, "关键词加载完成: $wordCount")
        if (wordCount > 0) {
            log().d(TAG, "前5个词: ${keywordIndex.take(5).map { it.word }}")
            log().d(TAG, "后5个词: ${keywordIndex.takeLast(5).map { it.word }}")
        }

        raf.seek(keywordSectionEnd)
        log().i(TAG, "parseKeywordSection done, seeked to $keywordSectionEnd")
    }

    /**
     * 解码关键词块索引信息
     *
     * 关键修复：V2.0中firstSize/lastSize的单位是字符数（不是字节数），
     * 需要乘以bpu才能得到实际字节数。V1.2中则是字节数。
     *
     * 此外，V2.0中关键词文本后跟null终止符（宽度=bpu字节），
     * 而V1.2中关键词文本后不一定有null终止符。
     */
    private fun decodeKeyBlockInfo(data: ByteArray, numKeyBlocks: Int): List<BlockMeta> {
        val stream = ByteStream(data)
        val metas = mutableListOf<BlockMeta>()

        for (i in 0 until numKeyBlocks) {
            if (stream.remaining < numberWidth) break
            val numEntries = if (engineVersion >= 2.0) {
                stream.readLongBE().toInt()
            } else {
                stream.readIntBE()
            }

            val firstSize: Int
            val lastSize: Int

            if (engineVersion >= 2.0) {
                if (stream.remaining < 2) break
                firstSize = stream.readShortBE()
                val firstBytes = firstSize * bpu
                if (stream.remaining < firstBytes + bpu) break
                stream.skip(firstBytes + bpu)
                if (stream.remaining < 2) break
                lastSize = stream.readShortBE()
                val lastBytes = lastSize * bpu
                if (stream.remaining < lastBytes + bpu) break
                stream.skip(lastBytes + bpu)
            } else {
                if (stream.remaining < 1) break
                firstSize = stream.readUnsignedByte()
                if (stream.remaining < firstSize * bpu) break
                stream.skip(firstSize * bpu)
                if (stream.remaining < 1) break
                lastSize = stream.readUnsignedByte()
                if (stream.remaining < lastSize * bpu) break
                stream.skip(lastSize * bpu)
            }

            if (stream.remaining < numberWidth * 2) break
            val compSize = if (engineVersion >= 2.0) {
                stream.readLongBE()
            } else {
                stream.readIntBE().toLong()
            }
            val decompSize = if (engineVersion >= 2.0) {
                stream.readLongBE().toInt()
            } else {
                stream.readIntBE()
            }

            metas.add(BlockMeta(numEntries, compSize, decompSize))
        }
        return metas
    }

    private fun parseRecordSection() {
        val recordSectionStart = raf.filePointer
        log().i(TAG, "parseRecordSection starting at offset $recordSectionStart (keywordSectionEnd=$keywordSectionEnd)")

        val numRecordBlocks: Long
        val numEntries: Long
        val indexLen: Long
        val blocksLen: Long

        if (engineVersion >= 2.0) {
            numRecordBlocks = raf.readLongBE()
            numEntries = raf.readLongBE()
            indexLen = raf.readLongBE()
            blocksLen = raf.readLongBE()
        } else {
            numRecordBlocks = raf.readIntBE().toLong()
            numEntries = raf.readIntBE().toLong()
            indexLen = raf.readIntBE().toLong()
            blocksLen = raf.readIntBE().toLong()
        }

        log().i(TAG, "Record section: numRecordBlocks=$numRecordBlocks numEntries=$numEntries indexLen=$indexLen blocksLen=$blocksLen")

        if (numRecordBlocks <= 0 || numRecordBlocks > 10000000) {
            log().e(TAG, "Invalid numRecordBlocks=$numRecordBlocks, dumping raw bytes at offset $recordSectionStart")
            try {
                raf.seek(recordSectionStart)
                val dump = ByteArray(minOf(64, (mdxFile.length() - recordSectionStart).toInt()))
                raf.readFully(dump)
                log().e(TAG, "Raw bytes: ${dump.joinToString(" ") { "%02X".format(it) }}")
            } catch (_: Exception) {}
            return
        }

        val compSizes = LongArray(numRecordBlocks.toInt())
        val decompSizes = LongArray(numRecordBlocks.toInt())

        for (i in 0 until numRecordBlocks.toInt()) {
            if (engineVersion >= 2.0) {
                compSizes[i] = raf.readLongBE()
                decompSizes[i] = raf.readLongBE()
            } else {
                compSizes[i] = raf.readIntBE().toLong()
                decompSizes[i] = raf.readIntBE().toLong()
            }
        }

        var cumulativeOffset = 0L
        for (i in 0 until numRecordBlocks.toInt()) {
            val blockPos = raf.filePointer
            recordBlockInfos.add(RecordBlockInfo(
                recordStartOffset = cumulativeOffset,
                compressedOffset = blockPos,
                compressedSize = compSizes[i],
                decompressedSize = decompSizes[i]
            ))
            cumulativeOffset += decompSizes[i]
            raf.seek(blockPos + compSizes[i])
        }

        log().i(TAG, "Record blocks加载完成: ${recordBlockInfos.size}")
    }

    private fun fastDecrypt(buf: ByteArray, startOffset: Int, key: ByteArray) {
        var prev: Int = 0x36
        var relIdx = 0
        for (i in startOffset until buf.size) {
            val original = buf[i].toInt() and 0xFF
            val swappedNibble = ((original ushr 4) or (original shl 4)) and 0xFF
            val decrypted = (swappedNibble xor (prev xor (relIdx and 0xFF) xor (key[relIdx % key.size].toInt() and 0xFF))) and 0xFF
            prev = original
            buf[i] = decrypted.toByte()
            relIdx++
        }
    }

    private fun decompressBlock(data: ByteArray, expectedDecompSize: Int): ByteArray {
        if (data.size < 8) return data

        val compType = readCompType(data)
        val expectedChecksum = readAdler32(data)
        val compressedData = data.copyOfRange(8, data.size)

        log().d(TAG, "  decompressBlock: input=${data.size}B compType=$compType expectedDecomp=$expectedDecompSize compressedLen=${compressedData.size}")

        val result = when (compType) {
            0 -> {
                val raw = ByteArray(minOf(expectedDecompSize, compressedData.size))
                System.arraycopy(compressedData, 0, raw, 0, raw.size)
                raw
            }
            1 -> {
                try {
                    Lzo1xDecompressor.decompress(compressedData, expectedDecompSize)
                } catch (e: Exception) {
                    log().e(TAG, "LZO解压失败: ${e.message}")
                    ByteArray(0)
                }
            }
            2 -> {
                decompressZlib(compressedData, expectedDecompSize, expectedChecksum)
            }
            else -> {
                log().w(TAG, "未知压缩类型=$compType, 返回原始数据")
                val raw = ByteArray(minOf(expectedDecompSize, compressedData.size))
                System.arraycopy(compressedData, 0, raw, 0, raw.size)
                raw
            }
        }

        if (expectedChecksum != 0L && result.isNotEmpty()) {
            val actualChecksum = computeAdler32(result)
            if (actualChecksum != expectedChecksum) {
                log().w(TAG, "Adler32校验和不匹配: expected=$expectedChecksum actual=$actualChecksum")
            }
        }

        return result
    }

    private fun readCompType(data: ByteArray): Int {
        return (data[0].toInt() and 0xFF) or
               ((data[1].toInt() and 0xFF) shl 8) or
               ((data[2].toInt() and 0xFF) shl 16) or
               ((data[3].toInt() and 0xFF) shl 24)
    }

    private fun readAdler32(data: ByteArray): Long {
        if (data.size < 8) return 0L
        return ((data[4].toLong() and 0xFF) shl 24) or
               ((data[5].toLong() and 0xFF) shl 16) or
               ((data[6].toLong() and 0xFF) shl 8) or
               (data[7].toLong() and 0xFF)
    }

    private fun extractAttr(xml: String, attrName: String): String? {
        val pattern = "$attrName=\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(xml)?.groupValues?.get(1)
    }

    companion object {
        private const val TAG = "MdxParser"

        fun transformHtmlStatic(raw: String): String {
            var result = raw
            result = result.replace(Regex("<SEP[^>]*>([^<]*)</SEP>", RegexOption.IGNORE_CASE)) { match ->
                val content = match.groupValues[1].trim()
                if (content.isEmpty()) " " else " $content "
            }
            result = result.replace(Regex("<SEP\\s*/?>", RegexOption.IGNORE_CASE), " ")
            result = result.replace(Regex("</SEP>", RegexOption.IGNORE_CASE), "")
            result = result.replace(Regex("<hw>", RegexOption.IGNORE_CASE), "<b class='hw'>")
            result = result.replace(Regex("</hw>", RegexOption.IGNORE_CASE), "</b>")
            result = result.replace(Regex("<inf>", RegexOption.IGNORE_CASE), "<i class='inf'>")
            result = result.replace(Regex("</inf>", RegexOption.IGNORE_CASE), "</i>")
            result = result.replace(Regex("<ex>", RegexOption.IGNORE_CASE), "<span class='ex'>")
            result = result.replace(Regex("</ex>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<hit[^>]*>", RegexOption.IGNORE_CASE), "<div class='hit'>")
            result = result.replace(Regex("</hit>", RegexOption.IGNORE_CASE), "</div>")
            result = result.replace(Regex("<link\\s+rel=stylesheet[^>]*>", RegexOption.IGNORE_CASE), "")
            result = result.replace(Regex("<meta[^>]*>", RegexOption.IGNORE_CASE), "")
            result = result.replace(Regex("<soundfile>", RegexOption.IGNORE_CASE), "<span class='soundfile'>")
            result = result.replace(Regex("</soundfile>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<pronunciation-practice\\s*/?>", RegexOption.IGNORE_CASE), "")
            result = result.replace(Regex("<di-info\\s*/?>", RegexOption.IGNORE_CASE), "")
            result = result.replace(Regex("<sense-head>", RegexOption.IGNORE_CASE), "<div class='sense-head'>")
            result = result.replace(Regex("</sense-head>", RegexOption.IGNORE_CASE), "</div>")
            result = result.replace(Regex("<ipa>", RegexOption.IGNORE_CASE), "<span class='ipa'>")
            result = result.replace(Regex("</ipa>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<prongrp>", RegexOption.IGNORE_CASE), "<span class='prongrp'>")
            result = result.replace(Regex("</prongrp>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<inflection>", RegexOption.IGNORE_CASE), "<span class='inflection'>")
            result = result.replace(Regex("</inflection>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<capvar>", RegexOption.IGNORE_CASE), "<span class='capvar'>")
            result = result.replace(Regex("</capvar>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<sense-block>", RegexOption.IGNORE_CASE), "<span class='sense-block'>")
            result = result.replace(Regex("</sense-block>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<sense-body>", RegexOption.IGNORE_CASE), "<span class='sense-body'>")
            result = result.replace(Regex("</sense-body>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<di-head>", RegexOption.IGNORE_CASE), "<span class='di-head'>")
            result = result.replace(Regex("</di-head>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<di-title>", RegexOption.IGNORE_CASE), "<span class='di-title'>")
            result = result.replace(Regex("</di-title>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<di-body>", RegexOption.IGNORE_CASE), "<span class='di-body'>")
            result = result.replace(Regex("</di-body>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<arl>", RegexOption.IGNORE_CASE), "<span class='arl'>")
            result = result.replace(Regex("</arl>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<base>", RegexOption.IGNORE_CASE), "<span class='base'>")
            result = result.replace(Regex("</base>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<results>", RegexOption.IGNORE_CASE), "<span class='results'>")
            result = result.replace(Regex("</results>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<forms>", RegexOption.IGNORE_CASE), "<span class='forms'>")
            result = result.replace(Regex("</forms>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<inflections>", RegexOption.IGNORE_CASE), "<span class='inflections'>")
            result = result.replace(Regex("</inflections>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<pron>", RegexOption.IGNORE_CASE), "<span class='pron'>")
            result = result.replace(Regex("</pron>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<ussymbol>", RegexOption.IGNORE_CASE), "<span class='ussymbol'>")
            result = result.replace(Regex("</ussymbol>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("<sense-info>", RegexOption.IGNORE_CASE), "<span class='sense-info'>")
            result = result.replace(Regex("</sense-info>", RegexOption.IGNORE_CASE), "</span>")
            result = result.replace(Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
                "href=\"sound://${match.groupValues[1]}\" onclick=\"event.preventDefault(); window.location.href=this.href;\""
            }
            return result
        }

        private fun computeAdler32(data: ByteArray): Long {
            val adler = Adler32()
            adler.update(data)
            return adler.value
        }

        private fun decompressZlib(data: ByteArray, expectedSize: Int, expectedChecksum: Long): ByteArray {
            var bestResult: ByteArray? = null

            for (nowrap in listOf(false, true)) {
                try {
                    val inflater = Inflater(nowrap)
                    inflater.setInput(data)
                    val result = ByteArray(expectedSize)
                    val len = inflater.inflate(result)
                    inflater.end()
                    if (len > 0) {
                        val decompressed = result.copyOf(len)
                        val cksum = computeAdler32(decompressed)
                        log().d(TAG, "  zlib nowrap=$nowrap len=$len adler32=$cksum expected=$expectedChecksum")
                        if (expectedChecksum > 0 && cksum == expectedChecksum) {
                            log().d(TAG, "  Adler32 matched, using nowrap=$nowrap")
                            return decompressed
                        }
                        if (bestResult == null) bestResult = decompressed
                    }
                } catch (_: Exception) {}
            }

            return bestResult ?: data
        }

        private fun RandomAccessFile.readIntBE(): Int {
            val b = ByteArray(4)
            readFully(b)
            return (b[0].toInt() and 0xFF shl 24) or (b[1].toInt() and 0xFF shl 16) or
                   (b[2].toInt() and 0xFF shl 8) or (b[3].toInt() and 0xFF)
        }

        private fun RandomAccessFile.readIntLE(): Int {
            val b = ByteArray(4)
            readFully(b)
            return (b[0].toInt() and 0xFF) or (b[1].toInt() and 0xFF shl 8) or
                   (b[2].toInt() and 0xFF shl 16) or (b[3].toInt() shl 24)
        }

        private fun RandomAccessFile.readLongBE(): Long {
            val b = ByteArray(8)
            readFully(b)
            return (b[0].toLong() and 0xFF shl 56) or (b[1].toLong() and 0xFF shl 48) or
                   (b[2].toLong() and 0xFF shl 40) or (b[3].toLong() and 0xFF shl 32) or
                   (b[4].toLong() and 0xFF shl 24) or (b[5].toLong() and 0xFF shl 16) or
                   (b[6].toLong() and 0xFF shl 8) or (b[7].toLong() and 0xFF)
        }
    }

    private class ByteStream(private val data: ByteArray) {
        var pos: Int = 0
        val remaining: Int get() = data.size - pos

        fun readUnsignedByte(): Int {
            if (pos >= data.size) return 0
            val v = data[pos].toInt() and 0xFF
            pos++
            return v
        }

        fun readShortBE(): Int {
            if (pos + 2 > data.size) return 0
            val v = (data[pos].toInt() and 0xFF shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2
            return v
        }

        fun readIntBE(): Int {
            if (pos + 4 > data.size) return 0
            val v = (data[pos].toInt() and 0xFF shl 24) or (data[pos + 1].toInt() and 0xFF shl 16) or
                    (data[pos + 2].toInt() and 0xFF shl 8) or (data[pos + 3].toInt() and 0xFF)
            pos += 4
            return v
        }

        fun readLongBE(): Long {
            if (pos + 8 > data.size) return 0L
            val v = (data[pos].toLong() and 0xFF shl 56) or (data[pos + 1].toLong() and 0xFF shl 48) or
                    (data[pos + 2].toLong() and 0xFF shl 40) or (data[pos + 3].toLong() and 0xFF shl 32) or
                    (data[pos + 4].toLong() and 0xFF shl 24) or (data[pos + 5].toLong() and 0xFF shl 16) or
                    (data[pos + 6].toLong() and 0xFF shl 8) or (data[pos + 7].toLong() and 0xFF)
            pos += 8
            return v
        }

        fun skip(n: Int) {
            pos += n
        }

        fun readNullTerminated(bpu: Int): ByteArray {
            val start = pos
            var found = false
            while (pos + bpu <= data.size) {
                var isNull = true
                for (i in 0 until bpu) {
                    if (data[pos + i] != 0.toByte()) { isNull = false; break }
                }
                if (isNull) {
                    pos += bpu
                    found = true
                    break
                }
                pos += bpu
            }
            val len = if (found) pos - start - bpu else pos - start
            return if (len > 0) data.copyOfRange(start, start + len) else ByteArray(0)
        }
    }

    private data class BlockMeta(val numEntries: Int, val compSize: Long, val decompSize: Int)

    private data class KeywordEntry(val word: String, val recordOffset: Long, val recordSize: Int)

    private data class RecordBlockInfo(
        val recordStartOffset: Long,
        val compressedOffset: Long,
        val compressedSize: Long,
        val decompressedSize: Long
    )
}
