package io.github.gdict.core

import android.util.Log
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
    private var closed = false
    private var parseFailed = false

    init {
        try {
            parse()
        } catch (e: Exception) {
            Log.e(TAG, "解析MDX文件失败: ${e.message}", e)
            parseFailed = true
            close()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        try { raf.close() } catch (_: Exception) {}
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
        )
        for (cssFile in cssCandidates) {
            if (cssFile.exists() && cssFile.length() > 0 && cssFile.length() < 5 * 1024 * 1024) {
                try {
                    sb.append(cssFile.readText(Charsets.UTF_8))
                    Log.i(TAG, "Loaded CSS from: ${cssFile.name} (${cssFile.length()} bytes)")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read CSS: ${cssFile.name}: ${e.message}")
                }
            }
        }

        val mddCandidates = listOf(
            File(parentDir, "$baseName.mdd"),
            File(parentDir, "${mdxFile.name}.mdd")
        )
        for (mddFile in mddCandidates) {
            if (mddFile.exists() && mddFile.length() > 0 && mddFile.length() < 50 * 1024 * 1024) {
                try {
                    extractCssFromMdd(mddFile, sb)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read MDD: ${mddFile.name}: ${e.message}")
                }
            }
        }

        return sb.toString()
    }

    private fun extractCssFromMdd(mddFile: File, sb: StringBuilder) {
        val mddParser = try {
            MdxParser(mddFile)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse MDD for CSS: ${mddFile.name}: ${e.message}")
            return
        }

        try {
            if (mddParser.wordCount > 0) {
                val cssKeys = mddParser.findResourceKeys(".css")
                for (key in cssKeys) {
                    try {
                        val cssBytes = mddParser.readResourceBytesByKey(key)
                        if (cssBytes != null && cssBytes.isNotEmpty()) {
                            val cssText = String(cssBytes, Charsets.UTF_8)
                            sb.append(cssText).append("\n")
                            Log.i(TAG, "  Loaded CSS from MDD: $key (${cssBytes.size} bytes)")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "  Failed to read CSS resource $key: ${e.message}")
                    }
                }
                if (cssKeys.isEmpty()) {
                    Log.i(TAG, "  No CSS resources found in MDD: ${mddFile.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract CSS from MDD: ${mddFile.name}: ${e.message}")
        } finally {
            mddParser.close()
        }
    }

    fun transformHtml(raw: String): String {
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
        return result
    }

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
        if (closed || parseFailed || keywordIndex.isEmpty()) return null
        val normalizedKey = if (key.startsWith("\\")) key else "\\$key"
        val idx = findFirstKeywordIndex(normalizedKey)
            ?: findFirstKeywordIndex(key)
            ?: return null
        val entry = keywordIndex[idx]
        return readRecordBytes(entry.recordOffset, entry.recordSize)
    }

    fun findResourceKeys(suffix: String): List<String> {
        if (closed || parseFailed || keywordIndex.isEmpty()) return emptyList()
        val lowerSuffix = suffix.lowercase()
        return keywordIndex.filter {
            it.word.lowercase().endsWith(lowerSuffix)
        }.map { it.word }
    }

    fun readResourceBytesByKey(key: String): ByteArray? {
        if (closed || parseFailed || keywordIndex.isEmpty()) return null
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
                Log.e(TAG, "readRecordBytes error at $offset: ${e.message}")
                return null
            }
        }
    }

    val filePath: String get() = mdxFile.absolutePath
    val fileName: String get() = mdxFile.name
    val fileSize: Long get() = mdxFile.length()

    fun diagnose(): String {
        val sb = StringBuilder()
        sb.appendLine("MdxParser Diagnostics:")
        sb.appendLine("  file='$mdxFile' (${mdxFile.length()} bytes)")
        sb.appendLine("  title='$title' encoding='$encoding'")
        sb.appendLine("  engineVersion=$engineVersion bpu=$bpu numberWidth=$numberWidth")
        sb.appendLine("  wordCount=$wordCount caseSensitive=$isKeyCaseSensitive")
        sb.appendLine("  keywordBlocks=${keywordIndex.size} recordBlocks=${recordBlockInfos.size}")
        sb.appendLine("  closed=$closed")

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
            Log.e(TAG, "readRecord: block size exceeds Int.MAX_VALUE")
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
                Log.e(TAG, "readRecord error at $offset: ${e.message}")
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
            Log.w(TAG, "解码字符串失败: ${e.message}")
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
        parseHeader()
        parseKeywordSection()
        parseRecordSection()
        Log.i(TAG, "解析完成: title='$title' words=$wordCount blocks=${recordBlockInfos.size} encoding='$encoding' version=$engineVersion bpu=$bpu")
    }

    private fun parseHeader() {
        val headerLen = raf.readIntBE()
        if (headerLen <= 0 || headerLen > 100 * 1024 * 1024) {
            throw IllegalArgumentException("无效的MDX文件: header长度异常 $headerLen")
        }
        val headerBytes = ByteArray(headerLen)
        raf.readFully(headerBytes)
        val checksum = raf.readIntLE()

        val headerStr = String(headerBytes, Charsets.UTF_16LE)

        title = extractAttr(headerStr, "Title") ?: mdxFile.nameWithoutExtension
        encoding = extractAttr(headerStr, "Encoding") ?: "UTF-8"
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

        Log.i(TAG, "Header: title='$title' encoding='$encoding' encrypt=$encrypt caseSensitive=$isKeyCaseSensitive engineVer=$engineVersion headerLen=$headerLen bpu=$bpu numberWidth=$numberWidth")
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
            val kwChecksum = raf.readIntBE()
            Log.i(TAG, "Keyword V2: numKeyBlocks=$numKeyBlocks totalEntries=$totalEntries idxDecomp=$keyIndexDecompLen idxComp=$keyIndexCompLen keyBlocksLen=$keyBlocksLen")
        } else {
            numKeyBlocks = raf.readIntBE().toLong()
            totalEntries = raf.readIntBE().toLong()
            keyIndexCompLen = raf.readIntBE().toLong()
            keyBlocksLen = raf.readIntBE().toLong()
            keyIndexDecompLen = 0
            Log.i(TAG, "Keyword V1: numKeyBlocks=$numKeyBlocks totalEntries=$totalEntries idxLen=$keyIndexCompLen keyBlocksLen=$keyBlocksLen")
        }

        if (numKeyBlocks <= 0 || numKeyBlocks > 10000000) {
            Log.e(TAG, "无效的numKeyBlocks: $numKeyBlocks")
            return
        }

        val keyIndexRaw = ByteArray(keyIndexCompLen.toInt())
        raf.readFully(keyIndexRaw)

        if ((encrypt and 2) != 0 && keyIndexRaw.size > 8) {
            val checksumBytes = keyIndexRaw.copyOfRange(4, 8)
            val keyInput = checksumBytes + byteArrayOf(0x95.toByte(), 0x36.toByte(), 0x00.toByte(), 0x00.toByte())
            val key = RipeMD128.digest(keyInput)
            fastDecrypt(keyIndexRaw, 8, key)
            Log.i(TAG, "Keyword index decrypted: keyIndexCompLen=$keyIndexCompLen")
        }

        val keyIndexData = if (engineVersion >= 2.0) {
            decompressBlock(keyIndexRaw, keyIndexDecompLen.toInt())
        } else {
            keyIndexRaw
        }

        val blockMetas = decodeKeyBlockInfo(keyIndexData, numKeyBlocks.toInt())

        Log.i(TAG, "Key block info解析完成: ${blockMetas.size} blocks")

        for ((blockIdx, meta) in blockMetas.withIndex()) {
            val blockCompData = ByteArray(meta.compSize.toInt())
            raf.readFully(blockCompData)
            val blockData = decompressBlock(blockCompData, meta.decompSize)

            if (blockData.size != meta.decompSize) {
                Log.w(TAG, "  Block $blockIdx: decomp size mismatch! expected=${meta.decompSize} actual=${blockData.size}")
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
                Log.w(TAG, "  Block $blockIdx: entry count mismatch! parsed=$entriesInBlock expected=${meta.numEntries} compSize=${meta.compSize} decompSize=${meta.decompSize}")
            }
            if (blockIdx < 5 || entriesInBlock != meta.numEntries) {
                Log.d(TAG, "  Block $blockIdx: $entriesInBlock entries (meta=${meta.numEntries}) remaining=${blockStream.remaining}")
            }
        }

        keywordIndex.sortBy { it.word }

        for (i in keywordIndex.indices) {
            val lower = keywordIndex[i].word.lowercase()
            lowercaseWordMap.getOrPut(lower) { mutableListOf() }.add(i)
        }
        sortedLowercaseEntries = keywordIndex.indices.map { i ->
            Pair(keywordIndex[i].word.lowercase(), i)
        }.sortedBy { it.first }

        wordCount = keywordIndex.size
        Log.i(TAG, "关键词加载完成: $wordCount")
        if (wordCount > 0) {
            Log.d(TAG, "前5个词: ${keywordIndex.take(5).map { it.word }}")
            Log.d(TAG, "后5个词: ${keywordIndex.takeLast(5).map { it.word }}")
        }
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

        Log.i(TAG, "Record section: numRecordBlocks=$numRecordBlocks numEntries=$numEntries indexLen=$indexLen blocksLen=$blocksLen")

        if (numRecordBlocks <= 0 || numRecordBlocks > 10000000) return

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

        Log.i(TAG, "Record blocks加载完成: ${recordBlockInfos.size}")
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

        Log.d(TAG, "  decompressBlock: input=${data.size}B compType=$compType expectedDecomp=$expectedDecompSize compressedLen=${compressedData.size}")

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
                    Log.e(TAG, "LZO解压失败: ${e.message}")
                    ByteArray(0)
                }
            }
            2 -> {
                decompressZlib(compressedData, expectedDecompSize)
            }
            else -> {
                Log.w(TAG, "未知压缩类型=$compType, 返回原始数据")
                val raw = ByteArray(minOf(expectedDecompSize, compressedData.size))
                System.arraycopy(compressedData, 0, raw, 0, raw.size)
                raw
            }
        }

        if (expectedChecksum != 0L && result.isNotEmpty()) {
            val actualChecksum = computeAdler32(result)
            if (actualChecksum != expectedChecksum) {
                Log.w(TAG, "Adler32校验和不匹配: expected=$expectedChecksum actual=$actualChecksum")
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

    private fun computeAdler32(data: ByteArray): Long {
        val adler = Adler32()
        adler.update(data)
        return adler.value
    }

    private fun extractAttr(xml: String, attrName: String): String? {
        val pattern = "$attrName=\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(xml)?.groupValues?.get(1)
    }

    companion object {
        private const val TAG = "MdxParser"

        private fun decompressZlib(data: ByteArray, expectedSize: Int): ByteArray {
            for (nowrap in listOf(true, false)) {
                try {
                    val inflater = Inflater(nowrap)
                    inflater.setInput(data)
                    val result = ByteArray(expectedSize)
                    val len = inflater.inflate(result)
                    inflater.end()
                    if (len > 0) return result.copyOf(len)
                } catch (_: Exception) {
                    continue
                }
            }
            Log.w(TAG, "zlib解压失败: raw deflate和标准zlib均不可用")
            return data
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
