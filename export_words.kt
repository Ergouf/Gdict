import java.io.File
import java.io.RandomAccessFile
import java.io.Closeable
import java.io.ByteArrayOutputStream
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
            println("WARNING: Encrypted=2 detected but RipeMD128 not available in export script")
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
            1 -> { println("WARNING: LZO not supported"); ByteArray(0) }
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
