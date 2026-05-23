import java.io.File
import java.io.RandomAccessFile
import java.io.Closeable
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Adler32
import java.util.zip.Inflater
import kotlin.random.Random

class MdxExport(private val mdxFile: File) : Closeable {
    var title: String = ""
    var encoding: String = "UTF-8"
    var wordCount: Int = 0

    private var engineVersion: Double = 2.0
    private var bpu: Int = 1
    private var numberWidth: Int = 8
    private var encrypt: Int = 0

    data class KeywordEntry(val word: String, val recordOffset: Long, val recordSize: Int)
    data class RecordBlockInfo(val recordStartOffset: Long, val compressedOffset: Long, val compressedSize: Long, val decompressedSize: Long)

    private val keywordIndex = mutableListOf<KeywordEntry>()
    private val recordBlockInfos = mutableListOf<RecordBlockInfo>()

    private val raf: RandomAccessFile = RandomAccessFile(mdxFile, "r")

    init { parse() }

    override fun close() { if (!raf.fd.valid()) return; raf.close() }

    fun transformHtml(raw: String): String {
        var result = raw
        result = result.replace(Regex("<SEP[^>]*>([^<]*)</SEP>", RegexOption.IGNORE_CASE)) {
            match -> val content = match.groupValues[1].trim(); if (content.isEmpty()) " " else " $content "
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
        if (keywordIndex.isEmpty()) return results
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
            for (entry in keywordIndex) {
                if (entry.word.lowercase() == lower) {
                    results[entry.word] = readRecord(entry.recordOffset, entry.recordSize)
                }
            }
        }
        return results
    }

    fun getRandomWords(count: Int): List<KeywordEntry> =
        if (keywordIndex.size <= count) keywordIndex.toList()
        else keywordIndex.shuffled(Random(42)).take(count)

    fun getAllKeywords(): List<String> = keywordIndex.map { it.word }

    private fun parse() {
        parseHeader()
        parseKeywordSection()
        parseRecordSection()
        println("Parsed: title='$title' words=$wordCount blocks=${recordBlockInfos.size} encoding='$encoding'")
    }

    private fun parseHeader() {
        val headerLen = readIntBE()
        if (headerLen <= 0 || headerLen > 100 * 1024 * 1024)
            throw IllegalArgumentException("Invalid MDX: header length $headerLen")
        val headerBytes = ByteArray(headerLen)
        raf.readFully(headerBytes)
        raf.readIntLE()
        val headerStr = String(headerBytes, Charsets.UTF_16LE)
        title = extractAttr(headerStr, "Title") ?: mdxFile.nameWithoutExtension
        encoding = extractAttr(headerStr, "Encoding") ?: "UTF-8"
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
    }

    private fun parseKeywordSection() {
        val numKeyBlocks: Long; val totalEntries: Long
        val keyIndexDecompLen: Long; val keyIndexCompLen: Long; val keyBlocksLen: Long
        if (engineVersion >= 2.0) {
            numKeyBlocks = raf.readLongBE(); totalEntries = raf.readLongBE()
            keyIndexDecompLen = raf.readLongBE(); keyIndexCompLen = raf.readLongBE()
            keyBlocksLen = raf.readLongBE(); raf.readIntBE()
        } else {
            numKeyBlocks = raf.readIntBE().toLong(); totalEntries = raf.readIntBE().toLong()
            keyIndexCompLen = raf.readIntBE().toLong(); keyBlocksLen = raf.readIntBE().toLong()
            keyIndexDecompLen = 0
        }
        if (numKeyBlocks <= 0 || numKeyBlocks > 10000000) return
        val keyIndexRaw = ByteArray(keyIndexCompLen.toInt())
        raf.readFully(keyIndexRaw)
        if ((encrypt and 2) != 0 && keyIndexRaw.size > 8) {
            val checksumBytes = keyIndexRaw.copyOfRange(4, 8)
            val keyInput = checksumBytes + byteArrayOf(0x95.toByte(), 0x36.toByte(), 0x00.toByte(), 0x00.toByte())
            val key = RipeMD128.digest(keyInput)
            fastDecrypt(keyIndexRaw, 8, key)
        }
        val keyIndexData = if (engineVersion >= 2.0) decompressBlock(keyIndexRaw, keyIndexDecompLen.toInt()) else keyIndexRaw
        val blockMetas = decodeKeyBlockInfo(keyIndexData, numKeyBlocks.toInt())
        for ((blockIdx, meta) in blockMetas.withIndex()) {
            val blockCompData = ByteArray(meta.compSize.toInt())
            raf.readFully(blockCompData)
            val blockData = decompressBlock(blockCompData, meta.decompSize)
            val blockStream = ByteStream(blockData)
            while (blockStream.remaining >= numberWidth) {
                val recordOffset = if (engineVersion >= 2.0) blockStream.readLongBE() else blockStream.readIntBE().toLong()
                if (blockStream.remaining == 0) break
                val wordBytes = blockStream.readNullTerminated(bpu)
                val decodedWord = decodeKeyWord(wordBytes)
                keywordIndex.add(KeywordEntry(decodedWord, recordOffset, 0))
            }
        }
        keywordIndex.sortBy { it.word }
        wordCount = keywordIndex.size
    }

    private fun parseRecordSection() {
        val numRecordBlocks: Long; val numRecords: Long
        val recordInfoCompLen: Long; val recordDataSize: Long
        if (engineVersion >= 2.0) {
            numRecordBlocks = raf.readLongBE(); numRecords = raf.readLongBE()
            recordInfoCompLen = raf.readLongBE(); recordDataSize = raf.readLongBE()
            raf.readIntBE()
        } else {
            numRecordBlocks = raf.readIntBE().toLong(); numRecords = raf.readIntBE().toLong()
            recordInfoCompLen = raf.readIntBE().toLong(); recordDataSize = raf.readIntBE().toLong()
        }
        if (numRecordBlocks <= 0 || numRecordBlocks > 10000000L) return
        val infoRaw = ByteArray(recordInfoCompLen.toInt())
        raf.readFully(infoRaw)
        val infoData = decompressBlock(infoRaw, recordInfoCompLen.toInt())
        val infoStream = ByteStream(infoData)
        var prevEndOffset = 0L
        for (i in 0 until numRecordBlocks.toInt()) {
            if (infoStream.remaining < numberWidth * 2) break
            val compressedSize = if (engineVersion >= 2.0) infoStream.readLongBE() else infoStream.readIntBE().toLong()
            val decompressedSize = if (engineVersion >= 2.0) infoStream.readLongBE() else infoStream.readIntBE().toLong()
            recordBlockInfos.add(RecordBlockInfo(prevEndOffset, raf.filePointer, compressedSize, decompressedSize))
            raf.seek(raf.filePointer + compressedSize)
            prevEndOffset += decompressedSize
        }
    }

    private fun readRecord(offset: Long, size: Int): String? {
        if (recordBlockInfos.isEmpty()) return null
        val idx = findRecordBlockIndex(offset) ?: return null
        val rbInfo = recordBlockInfos[idx]
        if (rbInfo.compressedSize > Int.MAX_VALUE || rbInfo.decompressedSize > Int.MAX_VALUE) return null
        try {
            raf.seek(rbInfo.compressedOffset)
            val data = ByteArray(rbInfo.compressedSize.toInt())
            raf.readFully(data)
            val decompressed = decompressBlock(data, rbInfo.decompressedSize.toInt())
            val recordStart = (offset - rbInfo.recordStartOffset).toInt()
            if (recordStart < 0 || recordStart >= decompressed.size) return null
            val actualSize = if (size > 0) size.coerceAtMost(decompressed.size - recordStart) else findNullLength(decompressed, recordStart, bpu)
            if (actualSize <= 0) return null
            val bytes = decompressed.copyOfRange(recordStart, recordStart + actualSize)
            return decodeString(bytes)
        } catch (e: Exception) {
            println("readRecord error at $offset: ${e.message}")
            return null
        }
    }

    private fun decompressBlock(data: ByteArray, expectedDecompSize: Int): ByteArray {
        if (data.size < 8) return data
        val compType = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8) or
            ((data[2].toInt() and 0xFF) shl 16) or ((data[3].toInt() and 0xFF) shl 24)
        val expectedChecksum = if (data.size >= 8)
            ((data[4].toLong() and 0xFF) shl 24) or ((data[5].toLong() and 0xFF) shl 16) or
             ((data[6].toLong() and 0xFF) shl 8) or (data[7].toLong() and 0xFF) else 0L
        val compressedData = data.copyOfRange(8, data.size)
        val result = when (compType) {
            0 -> { val raw = ByteArray(minOf(expectedDecompSize, compressedData.size)); System.arraycopy(compressedData, 0, raw, 0, raw.size); raw }
            1 -> { try { Lzo1xDecompressor.decompress(compressedData, expectedDecompSize) } catch (e: Exception) { println("LZO decompress failed: ${e.message}"); ByteArray(0) } }
            2 -> decompressZlib(compressedData, expectedDecompSize)
            else -> { val raw = ByteArray(minOf(expectedDecompSize, compressedData.size)); System.arraycopy(compressedData, 0, raw, 0, raw.size); raw }
        }
        if (expectedChecksum != 0L && result.isNotEmpty()) {
            val actualChecksum = computeAdler32(result)
            if (actualChecksum != expectedChecksum)
                println("WARN: Adler32 mismatch: expected=$expectedChecksum actual=$actualChecksum")
        }
        return result
    }

    private fun decompressZlib(data: ByteArray, expectedSize: Int): ByteArray {
        try {
            val inflater = Inflater()
            inflater.setInput(data)
            val result = ByteArray(expectedSize)
            val len = inflater.inflate(result)
            inflater.end()
            if (len > 0) return result.copyOf(len)
        } catch (_: Exception) {}
        try {
            val inflater = Inflater(true)
            inflater.setInput(data)
            val result = ByteArray(expectedSize)
            val len = inflater.inflate(result)
            inflater.end()
            if (len > 0) return result.copyOf(len)
        } catch (e: Exception) {
            println("zlib failed: ${e.message}")
        }
        return data
    }

    private fun computeAdler32(data: ByteArray): Long {
        val adler = Adler32(); adler.update(data); return adler.value
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

    private fun decodeString(bytes: ByteArray): String = try {
        when (encoding.uppercase().replace("-", "")) {
            "UTF8", "UTF" -> String(bytes, Charsets.UTF_8)
            "UTF16", "UTF16LE" -> if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte())
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE) else String(bytes, Charsets.UTF_16LE)
            "GBK", "GB2312", "GB18030" -> String(bytes, charset("GB18030"))
            "BIG5" -> String(bytes, charset("Big5"))
            else -> String(bytes, Charsets.UTF_8)
        }
    } catch (e: Exception) { String(bytes, Charsets.ISO_8859_1) }

    private fun decodeKeyWord(bytes: ByteArray): String = try {
        when (encoding.uppercase().replace("-", "")) {
            "UTF8", "UTF" -> String(bytes, Charsets.UTF_8)
            "UTF16", "UTF16LE" -> String(bytes, Charsets.UTF_16LE)
            else -> String(bytes, Charsets.UTF_8)
        }
    } catch (e: Exception) { String(bytes, Charsets.ISO_8859_1) }

    private fun extractAttr(xml: String, attrName: String): String? =
        """$attrName="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE).find(xml)?.groupValues?.get(1)

    private fun findFirstKeywordIndex(word: String): Int? {
        var lo = 0; var hi = keywordIndex.size - 1; var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1; val cmp = keywordIndex[mid].word.compareTo(word)
            when { cmp < 0 -> lo = mid + 1; cmp > 0 -> hi = mid - 1; else -> { found = mid; hi = mid - 1 } }
        }
        return if (found >= 0) found else null
    }

    private fun findRecordBlockIndex(offset: Long): Int? {
        var lo = 0; var hi = recordBlockInfos.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (recordBlockInfos[mid].recordStartOffset <= offset) {
                if (mid == recordBlockInfos.lastIndex || recordBlockInfos[mid + 1].recordStartOffset > offset) return mid
                lo = mid + 1
            } else hi = mid - 1
        }
        return null
    }

    private fun findNullLength(data: ByteArray, start: Int, bpu: Int): Int {
        var pos = start
        while (pos + bpu <= data.size) {
            var isNull = true
            for (i in 0 until bpu) { if (data[pos + i] != 0.toByte()) { isNull = false; break } }
            if (isNull) return pos - start
            pos += bpu
        }
        return data.size - start
    }

    private data class BlockMeta(val numEntries: Int, val compSize: Long, val decompSize: Int)
    private fun decodeKeyBlockInfo(data: ByteArray, numKeyBlocks: Int): List<BlockMeta> {
        val stream = ByteStream(data); val metas = mutableListOf<BlockMeta>()
        for (i in 0 until numKeyBlocks) {
            if (stream.remaining < numberWidth) break
            val numEntries = if (engineVersion >= 2.0) stream.readLongBE().toInt() else stream.readIntBE()
            val firstSize: Int; val lastSize: Int
            if (engineVersion >= 2.0) {
                if (stream.remaining < 2) break; firstSize = stream.readShortBE()
                val firstBytes = firstSize * bpu; if (stream.remaining < firstBytes + bpu) break; stream.skip(firstBytes + bpu)
                if (stream.remaining < 2) break; lastSize = stream.readShortBE()
                val lastBytes = lastSize * bpu; if (stream.remaining < lastBytes + numberWidth * 2) break; stream.skip(lastBytes)
            } else {
                if (stream.remaining < numberWidth * 3) break
                firstSize = stream.readIntBE(); stream.skip(firstSize)
                lastSize = stream.readIntBE(); stream.skip(lastSize)
            }
            if (stream.remaining < numberWidth * 2) break
            val cs = if (engineVersion >= 2.0) stream.readLongBE() else stream.readIntBE().toLong()
            val ds = if (engineVersion >= 2.0) stream.readLongBE() else stream.readIntBE().toLong()
            metas.add(BlockMeta(numEntries, cs, ds.toInt()))
        }
        return metas
    }

    private class ByteStream(private val data: ByteArray) {
        var pos: Int = 0; val remaining: Int get() = data.size - pos
        fun skip(n: Int) { pos += n }
        fun readNullTerminated(bpu: Int): ByteArray {
            val start = pos
            while (pos + bpu <= data.size) {
                var isNull = true
                for (i in 0 until bpu) { if (data[pos + i] != 0.toByte()) { isNull = false; break } }
                if (isNull) { val result = data.copyOfRange(start, pos); pos += bpu; return result }
                pos += bpu
            }
            val result = data.copyOfRange(start, data.size); pos = data.size; return result
        }
        fun readIntBE(): Int {
            val b = ByteArray(4); System.arraycopy(data, pos, b, 0, 4); pos += 4
            return (b[0].toInt() and 0xFF shl 24) or (b[1].toInt() and 0xFF shl 16) or (b[2].toInt() and 0xFF shl 8) or (b[3].toInt() and 0xFF)
        }
        fun readShortBE(): Int {
            val b = ByteArray(2); System.arraycopy(data, pos, b, 0, 2); pos += 2
            return (b[0].toInt() and 0xFF shl 8) or (b[1].toInt() and 0xFF)
        }
        fun readLongBE(): Long {
            val b = ByteArray(8); System.arraycopy(data, pos, b, 0, 8); pos += 8
            return (b[0].toLong() and 0xFF shl 56) or (b[1].toLong() and 0xFF shl 48) or
                   (b[2].toLong() and 0xFF shl 40) or (b[3].toLong() and 0xFF shl 32) or
                   (b[4].toLong() and 0xFF shl 24) or (b[5].toLong() and 0xFF shl 16) or
                   (b[6].toLong() and 0xFF shl 8) or (b[7].toLong() and 0xFF)
        }
    }

    private fun RandomAccessFile.readIntBE(): Int {
        val b = ByteArray(4); readFully(b)
        return (b[0].toInt() and 0xFF shl 24) or (b[1].toInt() and 0xFF shl 16) or (b[2].toInt() and 0xFF shl 8) or (b[3].toInt() and 0xFF)
    }
    private fun RandomAccessFile.readIntLE(): Int {
        val b = ByteArray(4); readFully(b)
        return (b[0].toInt() and 0xFF) or (b[1].toInt() and 0xFF shl 8) or (b[2].toInt() and 0xFF shl 16) or (b[3] shl 24)
    }
    private fun RandomAccessFile.readLongBE(): Long {
        val b = ByteArray(8); readFully(b)
        return (b[0].toLong() and 0xFF shl 56) or (b[1].toLong() and 0xFF shl 48) or
               (b[2].toLong() and 0xFF shl 40) or (b[3].toLong() and 0xFF shl 32) or
               (b[4].toLong() and 0xFF shl 24) or (b[5].toLong() and 0xFF shl 16) or
               (b[6].toLong() and 0xFF shl 8) or (b[7].toLong() and 0xFF)
    }
}

object Lzo1xDecompressor {
    private const val M2_MAX_OFFSET = 0x0800
    private const val MAX_255_COUNT = (Int.MAX_VALUE / 255) - 2
    private const val MIN_ZERO_RUN_LENGTH = 4

    fun decompress(input: ByteArray, expectedSize: Int): ByteArray {
        val out = ByteArray(expectedSize)
        var ip = 0; var op = 0
        val ipEnd = input.size; val opEnd = expectedSize
        var t: Int; var next: Int = 0; var state = 0; var mPos: Int
        var gotoMatchNext = false

        if (ipEnd <= 2) return java.util.Arrays.copyOf(out, op)

        var bitstreamVersion = 0
        if (ipEnd >= 5 && (input[ip].toInt() and 0xFF) == 17) {
            bitstreamVersion = input[ip + 1].toInt() and 0xFF
            ip += 2
        }

        if ((input[ip].toInt() and 0xFF) > 17) {
            t = (input[ip].toInt() and 0xFF) - 17; ip++
            if (t < 4) { next = t; gotoMatchNext = true }
            else {
                if (op + t > opEnd || ip + t > ipEnd) return java.util.Arrays.copyOf(out, op)
                for (i in 0 until t) out[op++] = input[ip++]
                state = 4
            }
        }

        while (true) {
            if (gotoMatchNext) {
                gotoMatchNext = false; state = next; t = next
                if (t > 0) {
                    if (ip + t > ipEnd || op + t > opEnd) return java.util.Arrays.copyOf(out, op)
                    while (t > 0) { out[op++] = input[ip++]; t-- }
                }
                if (ip >= ipEnd) break
                t = input[ip++].toInt() and 0xFF
            } else {
                if (ip >= ipEnd) break
                t = input[ip++].toInt() and 0xFF
            }

            if (t < 16) {
                if (state == 0) {
                    if (t == 0) {
                        val ipLast = ip
                        while (ip < ipEnd && input[ip].toInt() and 0xFF == 0) ip++
                        if (ip >= ipEnd) break
                        val offset = ip - ipLast
                        if (offset > MAX_255_COUNT) return java.util.Arrays.copyOf(out, op)
                        t += (offset shl 8) - offset; t += 15 + (input[ip++].toInt() and 0xFF)
                    }
                    t += 3
                    if (op + t > opEnd || ip + t > ipEnd) return java.util.Arrays.copyOf(out, op)
                    for (i in 0 until t) out[op++] = input[ip++]
                    state = 4; continue
                } else if (state != 4) {
                    next = t and 3
                    mPos = op - 1 - (t shr 2) - ((input[ip++].toInt() and 0xFF) shl 2)
                    if (mPos < 0 || op + 2 > opEnd) return java.util.Arrays.copyOf(out, op)
                    out[op++] = out[mPos++]; out[op++] = out[mPos]
                    gotoMatchNext = true; continue
                } else {
                    next = t and 3
                    mPos = op - (1 + M2_MAX_OFFSET) - (t shr 2) - ((input[ip++].toInt() and 0xFF) shl 2)
                    t = 3
                }
            } else if (t >= 64) {
                next = t and 3
                mPos = op - 1 - ((t shr 2) and 7) - ((input[ip++].toInt() and 0xFF) shl 3)
                t = (t shr 5) - 1 + 2
            } else if (t >= 32) {
                t = (t and 31) + 2
                if (t == 2) {
                    val ipLast = ip
                    while (ip < ipEnd && input[ip].toInt() and 0xFF == 0) ip++
                    if (ip >= ipEnd) return java.util.Arrays.copyOf(out, op)
                    val offset = ip - ipLast
                    if (offset > MAX_255_COUNT) return java.util.Arrays.copyOf(out, op)
                    t += (offset shl 8) - offset; t += 31 + (input[ip++].toInt() and 0xFF)
                    if (ip + 2 > ipEnd) return java.util.Arrays.copyOf(out, op)
                }
                next = (input[ip].toInt() and 0xFF) or ((input[ip + 1].toInt() and 0xFF) shl 8)
                mPos = op - 1 - (next shr 2); next = next and 3; ip += 2
            } else {
                if (ip + 2 > ipEnd) return java.util.Arrays.copyOf(out, op)
                next = (input[ip].toInt() and 0xFF) or ((input[ip + 1].toInt() and 0xFF) shl 8)
                if ((next and 0xFFFC) == 0xFFFC && (t and 0xF8) == 0x18 && bitstreamVersion != 0) {
                    if (ip + 3 > ipEnd) return java.util.Arrays.copyOf(out, op)
                    t = (t and 7) or ((input[ip + 2].toInt() and 0xFF) shl 3); t += MIN_ZERO_RUN_LENGTH
                    if (op + t > opEnd) return java.util.Arrays.copyOf(out, op)
                    for (i in 0 until t) out[op++] = 0
                    next = next and 3; ip += 3; gotoMatchNext = true; continue
                } else {
                    mPos = op - ((t and 8) shl 11); t = (t and 7) + 2
                    if (t == 2) {
                        val ipLast = ip
                        while (ip < ipEnd && input[ip].toInt() and 0xFF == 0) ip++
                        if (ip >= ipEnd) return java.util.Arrays.copyOf(out, op)
                        val offset = ip - ipLast
                        if (offset > MAX_255_COUNT) return java.util.Arrays.copyOf(out, op)
                        t += (offset shl 8) - offset; t += 7 + (input[ip++].toInt() and 0xFF)
                        if (ip + 2 > ipEnd) return java.util.Arrays.copyOf(out, op)
                        next = (input[ip].toInt() and 0xFF) or ((input[ip + 1].toInt() and 0xFF) shl 8)
                    }
                    ip += 2; mPos -= next shr 2; next = next and 3
                    if (mPos == op) return java.util.Arrays.copyOf(out, op)
                    mPos -= 0x4000
                }
            }

            if (mPos < 0 || op + t > opEnd) return java.util.Arrays.copyOf(out, op)
            out[op++] = out[mPos++]; out[op++] = out[mPos++]
            var count = t - 2
            while (count > 0) { out[op++] = out[mPos++]; count-- }

            state = next; t = next
            if (t > 0) {
                if (ip + t > ipEnd || op + t > opEnd) return java.util.Arrays.copyOf(out, op)
                while (t > 0) { out[op++] = input[ip++]; t-- }
            }
        }

        return java.util.Arrays.copyOf(out, op)
    }
}

object RipeMD128 {
    private const val BLOCK_SIZE = 64

    fun digest(data: ByteArray): ByteArray {
        var h0 = 0x67452301; var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt(); var h3 = 0x10325476
        val padded = padMessage(data)

        for (offset in padded.indices step BLOCK_SIZE) {
            val block = padded.copyOfRange(offset, offset + BLOCK_SIZE)
            val x = ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN)
            var a = h0; var b = h1; var c = h2; var d = h3
            var ap = h0; var bp = h1; var cp = h2; var dp = h3

            for (j in 0 until 64) {
                var t = add(a, f(j, b, c, d), x.getInt(RL[j] * 4), K(j)); t = rol(SL[j], t)
                a = d; d = c; c = b; b = t
                t = add(ap, f(63 - j, bp, cp, dp), x.getInt(RR[j] * 4), Kp(j)); t = rol(SR[j], t)
                ap = dp; dp = cp; cp = bp; bp = t
            }

            val t = add(h1, c, dp); h1 = add(h2, d, ap)
            h2 = add(h3, a, bp); h3 = add(h0, b, cp); h0 = t
        }

        return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(h0).putInt(h1).putInt(h2).putInt(h3).array()
    }

    private fun f(j: Int, x: Int, y: Int, z: Int): Int = when {
        j < 16 -> x xor y xor z
        j < 32 -> (x and y) or (z and x.inv())
        j < 48 -> (x or y.inv()) xor z
        else -> (x and z) or (y and z.inv())
    }
    private fun K(j: Int): Int = when { j < 16 -> 0; j < 32 -> 0x5A827999; j < 48 -> 0x6ED9EBA1.toInt(); else -> 0x8F1BBCDC.toInt() }
    private fun Kp(j: Int): Int = when { j < 16 -> 0x50A28BE6.toInt(); j < 32 -> 0x5C4DD124.toInt(); j < 48 -> 0x6D703EF3.toInt(); else -> 0 }
    private fun rol(s: Int, x: Int): Int = (x shl s) or (x ushr (32 - s))
    private fun add(vararg args: Int): Int { var sum = 0; for (arg in args) sum += arg; return sum and 0xFFFFFFFF.toInt() }

    private fun padMessage(data: ByteArray): ByteArray {
        val bitLen = data.size * 8L
        val padLen = (BLOCK_SIZE - ((data.size + 9) % BLOCK_SIZE)) % BLOCK_SIZE + 9
        val result = ByteArray(data.size + padLen)
        System.arraycopy(data, 0, result, 0, data.size)
        result[data.size] = 0x80.toByte()
        ByteBuffer.wrap(result, result.size - 8, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(bitLen)
        return result
    }

    private val RL = intArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,7,4,13,1,10,6,15,3,12,0,9,5,2,14,11,8,3,10,14,4,9,15,8,1,2,7,0,6,13,11,5,12,1,9,11,10,0,8,12,4,13,3,7,15,14,5,6,2)
    private val RR = intArrayOf(5,14,7,0,9,2,11,4,13,6,15,8,1,10,3,12,6,11,3,7,0,13,5,10,14,15,8,12,4,9,1,2,15,5,1,3,7,14,6,9,11,8,12,2,10,0,4,13,8,6,4,1,3,11,15,0,5,12,2,13,9,7,10,14)
    private val SL = intArrayOf(11,14,15,12,5,8,7,9,11,13,14,15,6,7,9,8,7,6,8,13,11,9,7,15,7,12,15,9,11,7,13,12,11,13,6,7,14,9,13,15,14,8,13,6,5,12,7,5,11,12,14,15,14,15,9,8,9,14,5,6,8,6,5,12)
    private val SR = intArrayOf(8,9,9,11,13,15,15,5,7,7,8,11,14,14,12,6,9,13,15,7,12,8,9,11,7,7,12,7,6,15,13,11,9,7,15,11,8,6,6,14,12,13,5,14,13,13,7,5,15,5,8,11,14,14,6,14,6,9,12,9,12,5,15,8)
}

fun main() {
    val mdxPath = args.getOrElse(0) { "D:\\workspace\\Gdict\\Cambridge_English_Pronouncing_Dictionary_18th.mdx" }
    val outputDir = File(args.getOrElse(1) { "D:\\workspace\\Gdict\\test_export" })
    outputDir.mkdirs()

    val mdxFile = File(mdxPath)
    if (!mdxFile.exists()) {
        println("ERROR: MDX file not found: $mdxPath")
        return
    }

    println("Loading: ${mdxFile.name} (${mdxFile.length()} bytes)")
    MdxExport(mdxFile).use { parser ->
    println("Title: '${parser.title}' Words: ${parser.wordCount} Encoding: '${parser.encoding}'")

    if (parser.wordCount == 0) {
        println("ERROR: No words loaded!")
        return
    }

    val words = parser.getRandomWords(10)
    println("\n=== Exporting ${words.size} random words ===\n")

    for ((idx, entry) in words.withIndex()) {
        val word = entry.word
        println("--- [${idx + 1}/10] $word ---")

        val articles = parser.readArticles(word)
        if (articles.isEmpty()) {
            println("  WARNING: No articles found for '$word'")
            continue
        }

        val safeName = word.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val baseDir = File(outputDir, safeName)
        baseDir.mkdirs()

        for ((articleWord, rawHtml) in articles) {
            if (rawHtml == null) {
                println("  WARNING: Null content for article '$articleWord'")
                continue
            }

            val transformedHtml = parser.transformHtml(rawHtml)

            File(baseDir, "${safeName}_raw.html").writeText(
                buildString {
                    appendLine("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>$word (RAW)</title></head><body>")
                    appendLine("<h1>$word — Raw HTML from MDX</h1>")
                    appendLine("<pre style='white-space:pre-wrap;word-break:break-all'>")
                    append(rawHtml.take(5000))
                    appendLine("</pre></body></html>")
                }
            )

            File(baseDir, "${safeName}_rendered.html").writeText(
                buildString {
                    appendLine("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>$word (Rendered)</title>")
                    appendLine("<style>")
                    appendLine("body{font-family:'Times New Roman',serif;max-width:600px;margin:20px auto;padding:15px;background:#fff;color:#333}")
                    appendLine(".hw{font-size:26px;font-weight:bold;color:#000}.inf{font-style:italic}.inf b{color:#c00}")
                    appendLine(".pron{color:#06c;font-size:17px;font-family:'Lucida Sans Unicode',Arial,sans-serif}")
                    appendLine(".ipa{font-family:'Lucida Sans Unicode',Arial,sans-serif}.ex{color:#666}")
                    appendLine(".tense-section{margin:10px 0;padding:8px;border-left:3px solid #06c;background:#f0f8ff}")
                    appendLine(".tense-label{font-weight:bold;color:#06c}.label{display:inline-block;padding:2px 6px;border-radius:3px;font-size:12px;margin-right:5px}")
                    appendLine(".label-uk{background:#ffe0e0;color:#c00}.label-us{background:#e0e0ff;color:#00c}")
                    appendLine(".sense-block{margin:15px 0;padding:10px;background:#fff;border-radius:4px}")
                    appendLine("</style></head><body>")
                    appendLine(transformedHtml)
                    appendLine("</body></html>")
                }
            )

            val preview = transformedHtml.replace(Regex("<[^>]+>")).take(200)
            val size = rawHtml.length
            println("  Article: '$articleWord' ($size chars)")
            println("  Preview: $preview...")
        }
        println()
    }

    println("Done! Output: ${outputDir.absolutePath}")
    }
}
