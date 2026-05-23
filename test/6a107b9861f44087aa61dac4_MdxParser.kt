/**
 * MDX Parser for Kotlin
 * 解析 MDict 2.0 格式的 .mdx 字典文件
 *
 * MDX 文件结构 (从偏移 0 开始):
 * ┌─────────────────────────────────────────┐
 * │ Header Section                           │
 * │   4 bytes  : header 字节数 (大端序)       │
 * │   N bytes  : header 文本 (UTF-16LE)      │
 * │   4 bytes  : adler32 校验值 (小端序)     │
 * ├─────────────────────────────────────────┤
 * │ Key Section                              │
 * │   40 bytes : 5 个 uint64 (可能被加密)    │
 * │   Key Block Info (zlib 压缩, 可能加密)   │
 * │   Key Block Data (LZO/zlib 压缩)        │
 * ├─────────────────────────────────────────┤
 * │ Record Section                           │
 * │   4 x uint64 : blocks数/entries数/大小   │
 * │   Record Block Info                     │
 * │   Record Block Data (词条 HTML 内容)     │
 * └─────────────────────────────────────────┘
 *
 * 加密说明:
 *   Encrypted=0 : 无加密
 *   Encrypted=1 : Salsa20 加密 record section
 *   Encrypted=2 : RIPEMD-128 + XOR 加密 key info block
 *
 * 压缩类型 (每个 block 前 4 字节标识):
 *   0x00000000 : 无压缩
 *   0x01000000 : LZO 压缩
 *   0x02000000 : zlib 压缩
 *
 * 依赖:
 *   - Kotlin stdlib (必须)
 *   - Java ZLIB / java.util.zip (内置, 必须)
 *   - LZO 库 (可选): org.anarres.lzo:lzo-core
 *     用于解压 LZO 压缩的 block, 如果字典全部使用 zlib 则不需要
 *
 * 使用示例:
 *   val parser = MdxParser("dictionary.mdx")
 *   parser.parse()
 *   println("词条数: ${parser.keywords.size}")
 *   val entry = parser.getEntry("hello")
 *
 * @author Auto-generated
 * @version 1.0
 */

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Adler32
import java.util.zip.Inflater

/**
 * MDX 字典解析器
 *
 * 负责解析 .mdx 格式的字典文件，提取元数据和关键词索引。
 * 解析完成后可通过 [keywords] 遍历所有词条，或通过 [getEntry] 查询特定词条。
 *
 * @param filePath MDX 文件的绝对路径或相对路径
 *
 * @property metadata   字典元数据 (标题、编码、版本、描述等)
 * @property keywords   所有关键词列表，每项为 (recordOffset, keywordText)
 * @property encoding   字典内容的字符编码 (如 "UTF-8", "UTF-16", "GB18030")
 * @property version    MDict 引擎版本号 (如 2.0)
 */
class MdxParser(private val filePath: String) {

    // ==================== 公开属性 ====================

    /** 字典元数据，从 Header 的 XML 属性中提取 */
    var metadata: Map<String, String> = emptyMap()
        private set

    /**
     * 所有关键词列表
     *
     * 每个元素是一个 Pair:
     *   - first  (Long)   : 该词条在 Record Section 中的字节偏移量
     *   - second (String) : 关键词文本
     *
     * 关键词按字母顺序排列，同一个词的不同词形 (如 read/reads/reading)
     * 会作为独立条目出现，各自指向不同的 record offset。
     */
    var keywords: List<Pair<Long, String>> = emptyList()
        private set

    /** 字典内容的字符编码，默认 "UTF-8" */
    var encoding: String = "UTF-8"
        private set

    /** MDict 引擎版本号，默认 2.0 */
    var version: Double = 2.0
        private set

    // ==================== 私有属性 ====================

    /**
     * 加密标志位
     *
     * 从 Header 的 Encrypted 属性解析:
     *   0 = 无加密
     *   1 = record section 使用 Salsa20 加密 (需要用户提供注册码)
     *   2 = key info block 使用 RIPEMD-128 + XOR 加密
     *
     * 注意: encrypt & 0x01 表示 record 加密, encrypt & 0x02 表示 key info 加密
     * 两者可以同时存在 (值为 3)
     */
    private var encrypt: Int = 0

    /** 整个 MDX 文件的原始字节数据，解析时一次性读入内存 */
    private lateinit var data: ByteArray

    /**
     * 整型数字的字节宽度
     *
     * MDict v2.0 使用 8 字节 (uint64) 存储数字 (偏移量、大小等)
     * MDict v1.x 使用 4 字节 (uint32)
     */
    private val numberWidth: Int = 8

    // ==================== 主入口 ====================

    /**
     * 解析 MDX 文件的主入口方法
     *
     * 执行流程:
     * 1. 将整个文件读入内存
     * 2. 解析 Header Section (提取元数据)
     * 3. 解析 Key Section (提取所有关键词及其 record 偏移量)
     *
     * 解析完成后，可通过 [keywords] 访问所有词条，
     * 通过 [metadata] 访问字典信息。
     *
     * @throws IllegalArgumentException 如果文件不存在或 header 校验失败
     */
    fun parse() {
        // 将整个文件读入内存 (MDX 文件通常几十 MB，可以接受)
        val file = File(filePath)
        require(file.exists()) { "MDX 文件不存在: $filePath" }
        file.inputStream().use { input ->
            data = input.readBytes()
        }

        // 使用 ByteBuffer 进行结构化读取，MDX 使用大端序 (Big-Endian)
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        // 第一步: 解析 Header，提取字典元数据和编码信息
        parseHeader(buffer)

        // 第二步: 解析 Key Section，提取所有关键词
        parseKeys(buffer)
    }

    // ==================== Header 解析 ====================

    /**
     * 解析 Header Section
     *
     * Header 的二进制布局:
     *   [0..3]   : 4 字节 uint32 BE, 表示 header 文本的字节长度
     *   [4..4+N] : N 字节 header 文本 (UTF-16LE 编码, 以 \x00\x00 结尾)
     *   [4+N..7+N]: 4 字节 uint32 LE, header 文本的 adler32 校验值
     *
     * Header 文本是一个 XML 片段，格式为:
     *   <Dictionary GeneratedByEngineVersion="2.0" ... Description="..." Title="..." />
     *
     * 其中 Description 字段可能包含多行 HTML 内容。
     *
     * @param buffer 文件数据的 ByteBuffer，调用时位置在文件开头 (offset 0)
     */
    private fun parseHeader(buffer: ByteBuffer) {
        // 读取 header 文本的字节长度 (大端序 uint32)
        val headerSize = buffer.int

        // 读取 header 文本 (UTF-16LE 编码)
        val headerBytes = ByteArray(headerSize)
        buffer.get(headerBytes)

        // 读取并验证 adler32 校验值 (小端序 uint32)
        // 校验范围是整个 header 文本字节
        val adler32Check = buffer.int
        val calculatedAdler = adler32(headerBytes)
        require(adler32Check == calculatedAdler) {
            "Header 校验失败: 期望 0x${adler32Check.toString(16)}, 实际 0x${calculatedAdler.toString(16)}"
        }

        // 将 header 文本从 UTF-16LE 解码为字符串
        // 最后 2 字节是 \x00\x00 (UTF-16 的 null 终止符)，需要去掉
        val headerText = String(headerBytes, 0, headerBytes.size - 2, Charsets.UTF_16LE)

        // 使用正则表达式提取所有 key="value" 属性对
        // 注意: Description 的值可能包含引号和换行，使用非贪婪匹配 .*?
        val attrPattern = """(\w+)="(.*?)"""".toRegex()
        metadata = attrPattern.findAll(headerText).associate { match ->
            val key = match.groupValues[1]
            // 反转义 HTML 实体字符 (MDX header 中使用 XML 实体编码特殊字符)
            var value = match.groupValues[2]
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
            key to value
        }

        // 从元数据中提取解析所需的关键属性
        encoding = metadata["Encoding"] ?: "UTF-8"
        version = metadata["GeneratedByEngineVersion"]?.toDoubleOrNull() ?: 2.0
        encrypt = metadata["Encrypted"]?.toIntOrNull() ?: 0
    }

    // ==================== Key Section 解析 ====================

    /**
     * 解析 Key Section (使用 "brutal force" 方法)
     *
     * Key Section 的布局:
     *   [0..39]    : 5 个 uint64 (共 40 字节)
     *               依次为: numKeyBlocks, numEntries, keyBlockInfoDecompSize,
     *                     keyBlockInfoCompSize, keyBlockSize
     *               当 encrypt & 0x01 时这些数字被 Salsa20 加密，无法直接读取
     *               因此使用 "brutal" 方法: 跳过这些数字，搜索下一个标记
     *
     *   [40..?]    : Key Block Info (zlib 压缩)
     *               以 4 字节 0x02000000 (zlib 标记) 开头
     *               如果 encrypt & 0x02，需要先解密再解压
     *               解压后包含每个 key block 的压缩大小和解压大小
     *
     *   [?..?]     : Key Block Data
     *               所有 key block 的压缩数据依次排列
     *               每个 block 以 4 字节压缩类型标记开头
     *
     * @param buffer 文件数据的 ByteBuffer，调用时位置在 Key Section 开头
     */
    private fun parseKeys(buffer: ByteBuffer) {
        val keyBlockOffset = buffer.position()

        // 跳过前 40 字节 (5 个可能被加密的 uint64) + 4 字节 adler32 = 44 字节
        // 这些数字在 Encrypted=2 时无法直接解密，所以我们不读取它们
        buffer.position(keyBlockOffset + 44)

        // ---- 搜索 Key Block Info 的起始位置 ----
        // Key Block Info 以 0x02000000 (zlib 标记) 开头
        // 我们逐字节扫描，直到找到这个 4 字节标记
        val keyBlockInfoStart = buffer.position()
        var keyBlockInfoEnd = keyBlockInfoStart

        while (buffer.remaining() >= 4) {
            val pos = buffer.position()
            val marker = buffer.int
            if (marker == 0x02000000.toInt()) {
                // 找到 zlib 标记，记录 Key Block Info 的结束位置
                // (这个标记实际上是下一个 section 的开始，所以 info 在此之前)
                keyBlockInfoEnd = pos
                break
            }
            // 没找到，回退 3 字节继续搜索 (因为已经读了 4 字节)
            buffer.position(pos + 1)
        }

        // 读取 Key Block Info 的原始字节
        val keyBlockInfo = ByteArray(keyBlockInfoEnd - keyBlockInfoStart)
        buffer.position(keyBlockInfoStart)
        buffer.get(keyBlockInfo)
        buffer.position(keyBlockInfoEnd)

        // ---- 解密 Key Block Info (如果需要) ----
        // Encrypted=2 时，Key Block Info 使用 RIPEMD-128 + XOR 加密
        // 解密后前 8 字节不变 (4字节 zlib 标记 + 4字节 adler32)
        val decryptedInfo = if (encrypt and 0x02 != 0) {
            mdxDecrypt(keyBlockInfo)
        } else {
            keyBlockInfo
        }

        // ---- zlib 解压 Key Block Info ----
        // 跳过前 8 字节 (4字节压缩类型标记 + 4字节 adler32 校验)
        val decompressedInfo = zlibDecompress(decryptedInfo.copyOfRange(8, decryptedInfo.size))

        // ---- 解析 Key Block Info 列表 ----
        // 解压后的数据包含多个条目，每个条目描述一个 Key Block:
        //   - numEntries     : uint64, 该 block 包含的关键词数量
        //   - textHeadSize   : uint16, 关键词头部文本的字节数
        //   - textHead       : textHeadSize 字节的关键词头部文本
        //   - textTailSize   : uint16, 关键词尾部文本的字节数
        //   - textTail       : textTailSize 字节的关键词尾部文本
        //   - compressedSize : uint64, 该 block 压缩后的字节数
        //   - decompressedSize: uint64, 该 block 解压后的字节数
        val blockInfoList = parseKeyBlockInfo(decompressedInfo)

        // ---- 读取 Key Block Data ----
        // 所有 Key Block 的压缩数据依次排列
        val totalSize = blockInfoList.sumOf { it.first.toInt() }
        val keyBlocksData = ByteArray(totalSize)
        buffer.get(keyBlocksData)

        // ---- 解压并解析所有关键词 ----
        keywords = parseKeyBlocks(keyBlocksData, blockInfoList)
    }

    /**
     * 解析 Key Block Info 列表
     *
     * 将解压后的 Key Block Info 数据解析为 (compressedSize, decompressedSize) 列表。
     *
     * 每个条目的结构:
     *   ┌──────────────┬────────────┬──────────────┬────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
     *   │ numEntries   │ textHeadSz │ textHead     │ textTailSz │ textTail     │ compressedSz │ decompSz     │              │
     *   │ uint64       │ uint16     │ N bytes      │ uint16     │ M bytes      │ uint64       │ uint64       │              │
     *   └──────────────┴────────────┴──────────────┴────────────┴──────────────┴──────────────┴──────────────┘
     *
     * 注意: textHead 和 textTail 的字节数取决于编码:
     *   - UTF-16: 字节数 = (size + textTerm) * 2
     *   - 其他:   字节数 = (size + textTerm)
     *   其中 textTerm = 1 (表示有一个终止字节)
     *
     * @param data 解压后的 Key Block Info 数据
     * @return Key Block 信息列表，每项为 (compressedSize, decompressedSize)
     */
    private fun parseKeyBlockInfo(data: ByteArray): List<Pair<Long, Long>> {
        val list = mutableListOf<Pair<Long, Long>>()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        // 判断编码是否为 UTF-16 (影响 textHead/textTail 的字节长度计算)
        val isUtf16 = encoding == "UTF-16"
        // textTerm: 终止符占用的额外字节 (MDX v2.0 中固定为 1)
        val textTerm = 1

        while (buffer.remaining() >= numberWidth * 2 + 4) {
            // 读取 numEntries (当前 block 包含多少个关键词)
            // 我们不直接使用这个值，但需要跳过它
            buffer.long

            // 读取 textHeadSize (关键词头部文本的长度)
            val ths = buffer.short.toInt() and 0xFFFF
            // 计算头部文本实际占用的字节数 (包含终止符)
            val thsBytes = (ths + textTerm) * if (isUtf16) 2 else 1
            if (buffer.remaining() < thsBytes + 2) break
            // 跳过头部文本数据
            buffer.position(buffer.position() + thsBytes)

            // 读取 textTailSize (关键词尾部文本的长度)
            val tts = buffer.short.toInt() and 0xFFFF
            // 计算尾部文本实际占用的字节数 (包含终止符)
            val ttsBytes = (tts + textTerm) * if (isUtf16) 2 else 1
            if (buffer.remaining() < ttsBytes + numberWidth * 2) break
            // 跳过尾部文本数据
            buffer.position(buffer.position() + ttsBytes)

            // 读取 compressedSize 和 decompressedSize
            val cs = buffer.long  // 该 block 压缩后的字节数
            val ds = buffer.long  // 该 block 解压后的字节数
            list.add(cs to ds)
        }

        return list
    }

    /**
     * 解压并解析所有 Key Blocks
     *
     * Key Block Data 中，每个 block 的布局:
     *   [0..3]       : 4 字节压缩类型标记
     *   [4..7]       : 4 字节 adler32 校验值 (解压后数据的校验)
     *   [8..8+cs-1] : cs 字节压缩数据
     *
     * 解压后，每个关键词的布局:
     *   [0..7]       : 8 字节 uint64 BE, 该词条在 Record Section 中的偏移量
     *   [8..?]       : 关键词文本 (以 \x00 终止, UTF-8 编码)
     *
     * @param data         所有 Key Block 的原始压缩数据
     * @param blockInfoList Key Block 信息列表 (每项包含压缩/解压大小)
     * @return 关键词列表，每项为 (recordOffset, keywordText)
     */
    private fun parseKeyBlocks(
        data: ByteArray,
        blockInfoList: List<Pair<Long, Long>>
    ): List<Pair<Long, String>> {
        val allKeywords = mutableListOf<Pair<Long, String>>()
        var offset = 0  // 当前在 data 中的字节偏移

        for ((cs, ds) in blockInfoList) {
            val blockSize = cs.toInt()
            val blockData = data.copyOfRange(offset, offset + blockSize)

            // 读取前 4 字节，判断压缩类型
            val blockType = ByteBuffer.wrap(blockData, 0, 4)
                .order(ByteOrder.BIG_ENDIAN).int

            // 根据压缩类型解压 (跳过前 8 字节的类型标记和校验值)
            val decompressed = when (blockType) {
                0x00000000 ->
                    // 无压缩: 数据直接存储在标记之后
                    blockData.copyOfRange(8, blockData.size)
                0x01000000 ->
                    // LZO 压缩: 需要外部 LZO 库
                    lzoDecompress(blockData.copyOfRange(8, blockData.size), ds.toInt())
                0x02000000 ->
                    // zlib 压缩: 使用 Java 内置的 Inflater
                    zlibDecompress(blockData.copyOfRange(8, blockData.size))
                else ->
                    // 未知类型: 尝试直接作为原始数据读取
                    blockData.copyOfRange(8, blockData.size)
            }

            // 从解压后的数据中提取关键词
            val blockKeywords = splitKeyBlock(decompressed)
            allKeywords.addAll(blockKeywords)

            // 移动到下一个 block
            offset += blockSize
        }

        return allKeywords
    }

    /**
     * 从单个 Key Block 的解压数据中提取所有关键词
     *
     * 数据格式 (连续排列，无分隔符):
     *   ┌──────────┬───────────────────┬──────────┬───────────────────┐
     *   │ offset   │ keyword text \0    │ offset   │ keyword text \0    │ ...
     *   │ 8 bytes  │ variable          │ 8 bytes  │ variable          │
     *   └──────────┴───────────────────┴──────────┴───────────────────┘
     *
     * 每个关键词:
     *   - 8 字节 uint64 BE: 该词条在 Record Section 中的起始偏移量
     *   - 变长文本: 关键词字符串，以 \x00 (null 字节) 终止
     *
     * @param data 解压后的 Key Block 数据
     * @return 关键词列表，每项为 (recordOffset, keywordText)
     */
    private fun splitKeyBlock(data: ByteArray): List<Pair<Long, String>> {
        val list = mutableListOf<Pair<Long, String>>()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        while (buffer.remaining() > numberWidth) {
            // 读取 8 字节的 record offset (大端序 uint64)
            val recordOffset = buffer.long

            // 读取以 null (\x00) 终止的关键词文本
            val textStart = buffer.position()
            var textEnd = textStart
            while (textEnd < data.size && data[textEnd] != 0.toByte()) {
                textEnd++
            }

            // 将关键词文本按指定编码解码为字符串
            val keyword = String(data, textStart, textEnd - textStart, Charsets.UTF_8)
            // 跳过 null 终止符
            buffer.position(textEnd + 1)

            list.add(recordOffset to keyword)
        }

        return list
    }

    // ==================== Record Section 解析 ====================

    /**
     * 根据关键词查询词条内容 (HTML 代码)
     *
     * 查找逻辑:
     * 1. 在 keywords 列表中查找匹配的关键词 (不区分大小写)
     * 2. 获取该关键词的 record offset
     * 3. 获取下一个关键词的 record offset 作为结束位置
     * 4. 从 Record Section 中提取这段数据
     *
     * @param keyword 要查询的关键词
     * @return 词条的 HTML 内容字符串，如果未找到则返回 null
     */
    fun getEntry(keyword: String): String? {
        // 在关键词列表中查找 (不区分大小写)
        val index = keywords.indexOfFirst { it.second.equals(keyword, ignoreCase = true) }
        if (index == -1) return null

        // 当前词条的 record 起始偏移
        val (offset, _) = keywords[index]
        // 下一个词条的 record 起始偏移 (即当前词条的结束位置)
        val endOffset = if (index + 1 < keywords.size) keywords[index + 1].first else null

        return getRecord(offset, endOffset)
    }

    /**
     * 从 Record Section 中提取指定范围的数据
     *
     * Record Section 的布局:
     *   [0..7]       : uint64, record block 数量
     *   [8..15]      : uint64, record 条目总数
     *   [16..23]     : uint64, record block info 的字节数
     *   [24..31]     : uint64, record block data 的总字节数
     *   [32..?]      : record block info (每个 block 的压缩/解压大小)
     *   [?..?]       : record block data (压缩的 HTML 内容)
     *
     * 每个 record block 的布局与 key block 类似:
     *   [0..3]  : 压缩类型标记
     *   [4..7]  : adler32 校验值
     *   [8..?]  : 压缩数据
     *
     * @param startOffset 词条在解压后 record 数据中的起始字节偏移
     * @param endOffset   词条的结束字节偏移 (下一个词条的起始位置), null 表示到末尾
     * @return 词条的 HTML 内容字符串
     */
    private fun getRecord(startOffset: Long, endOffset: Long?): String? {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        // 定位到 record section
        // 需要先跳过 header 和 key section，计算 record section 的起始位置
        // 完整实现需要记录 key section 结束时的 buffer 位置
        // TODO: 完整实现 record section 定位和解压逻辑

        return null
    }

    // ==================== 工具方法 ====================

    /**
     * 计算数据的 Adler32 校验值
     *
     * Adler32 是一种快速校验和算法，MDX 用它来验证数据完整性。
     * 比 CRC32 更快但可靠性稍低。
     *
     * @param data 要计算校验值的数据
     * @return 32 位无符号校验值 (以 Int 表示)
     */
    private fun adler32(data: ByteArray): Int {
        val adler = Adler32()
        adler.update(data)
        return adler.value.toInt()
    }

    /**
     * zlib 解压 (raw deflate 模式)
     *
     * 使用 Java 内置的 Inflater 进行解压。
     * 注意: 使用 raw deflate 模式 (nowrap=true)，因为 MDX 中的 zlib 数据
     * 不包含 zlib 头部 (2 字节) 和尾部 (4 字节 adler32)，
     * 校验值是单独存储在 block header 中的。
     *
     * @param data zlib 压缩的数据 (raw deflate 格式)
     * @return 解压后的原始数据
     */
    private fun zlibDecompress(data: ByteArray): ByteArray {
        // nowrap=true 表示 raw deflate 格式 (无 zlib header/trailer)
        val inflater = Inflater(true)
        inflater.setInput(data)

        // 使用 4KB 缓冲区逐步解压
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)

        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            output.write(buffer, 0, count)
        }
        inflater.end()

        return output.toByteArray()
    }

    /**
     * LZO 解压
     *
     * LZO 是一种快速的压缩算法，MDX v1.x 常用。
     * 需要添加外部依赖: org.anarres.lzo:lzo-core
     *
     * 如果使用该库，实现示例:
     * ```
     * import org.anarres.lzo.LzoDecompressor1x
     * import org.anarres.lzo.LzoInputStream
     *
     * val decompressor = LzoDecompressor1x()
     * // LZO 需要在压缩数据前添加头部: 0xF0 + uint32 BE 原始大小
     * val header = byteArrayOf(0xF0.toByte()) +
     *     ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(uncompressedSize).array()
     * val input = LzoInputStream(header + data ByteArrayInputStream(), decompressor)
     * return input.readBytes()
     * ```
     *
     * @param data LZO 压缩的数据
     * @param uncompressedSize 解压后的预期大小 (字节)
     * @return 解压后的原始数据
     * @throws UnsupportedOperationException 如果未添加 LZO 库依赖
     */
    private fun lzoDecompress(data: ByteArray, uncompressedSize: Int): ByteArray {
        throw UnsupportedOperationException(
            "LZO 解压需要添加依赖: org.anarres.lzo:lzo-core\n" +
            "在 build.gradle 中添加: implementation 'org.anarres.lzo:lzo-core:1.0.6'"
        )
    }

    /**
     * MDX 解密 (Encrypted=2 时使用)
     *
     * 解密算法:
     * 1. 从密文第 4-7 字节 + 固定值 0x3695 生成 RIPEMD-128 密钥 (16 字节)
     * 2. 前 8 字节保持不变 (4字节类型标记 + 4字节校验值)
     * 3. 从第 8 字节开始，逐字节进行 XOR 解密:
     *    - 将当前字节的高 4 位和低 4 位交换 (nibble swap)
     *    - 与前一个密文字节 XOR
     *    - 与字节索引 (mod 256) XOR
     *    - 与密钥字节 (循环使用 16 字节密钥) XOR
     *
     * @param data 加密的 Key Block Info 数据
     * @return 解密后的数据 (前 8 字节与输入相同)
     */
    private fun mdxDecrypt(data: ByteArray): ByteArray {
        // 复制数据，前 8 字节保持不变
        val result = data.copyOf()

        // 构造密钥输入: 密文[4:8] + 固定值 0x3695 (小端序 uint32)
        val keyBytes = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x3695)
            .array()
        val key = ripemd128(data.copyOfRange(4, 8) + keyBytes)

        // 从第 8 字节开始逐字节解密
        var previous: Int = 0x36  // 初始值固定为 0x36
        for (i in 8 until result.size) {
            val b = result[i].toInt() and 0xFF  // 当前密文字节

            // 步骤 1: 高低 4 位交换 (nibble swap)
            // 例如: 0xAB -> 0xBA
            val swapped = ((b shr 4) or (b shl 4)) and 0xFF

            // 步骤 2: XOR 解密
            // 与前一个密文字节、字节索引、循环密钥进行 XOR
            val decrypted = swapped xor previous xor (i and 0xFF) xor (key[i % key.size].toInt() and 0xFF)
            result[i] = decrypted.toByte()

            // 保存当前密文字节 (注意是解密前的原始值) 供下一个字节使用
            previous = b
        }

        return result
    }

    /**
     * RIPEMD-128 哈希函数
     *
     * 生成 128 位 (16 字节) 的哈希值，用于 MDX 解密的密钥派生。
     *
     * 注意: Java 标准库不包含 RIPEMD-128，这里使用 SHA-1 截断为 16 字节作为替代。
     * 对于正确解密，应使用完整的 RIPEMD-128 实现。
     * 可选方案:
     *   - 使用 Bouncy Castle 库: Security.addProvider(BouncyCastleProvider())
     *   - 或自行实现 RIPEMD-128 算法
     *
     * @param data 要哈希的输入数据
     * @return 16 字节的哈希值
     */
    private fun ripemd128(data: ByteArray): ByteArray {
        // TODO: 替换为完整的 RIPEMD-128 实现
        // 临时使用 SHA-1 并截断为 16 字节 (不完全兼容，可能导致解密失败)
        val md = java.security.MessageDigest.getInstance("SHA-1")
        return md.digest(data).copyOfRange(0, 16)
    }
}

// ==================== 使用示例 ====================

/**
 * 命令行入口
 *
 * 用法:
 *   kotlinc MdxParser.kt -include-runtime -d MdxParser.jar
 *   kotlin -jar MdxParser.jar /path/to/dictionary.mdx
 *
 * 或在 IDE 中直接运行 main 函数，修改 filePath 变量即可。
 */
fun main(args: Array<String>) {
    // 从命令行参数获取文件路径，或使用默认值
    if (args.isEmpty()) {
        println("用法: kotlin MdxParserKt <mdx文件路径>")
        println("示例: kotlin MdxParserKt dictionary.mdx")
        println()
        println("功能说明:")
        println("  - 解析 MDX 字典文件的 Header 和 Key Section")
        println("  - 输出字典元信息 (标题、编码、词条数等)")
        println("  - 显示前 10 个词条")
        println("  - 搜索指定单词")
        return
    }

    val filePath = args[0]

    // 创建解析器并解析
    println("正在解析: $filePath")
    val parser = MdxParser(filePath)
    parser.parse()

    // 输出字典基本信息
    println("\n=== 字典信息 ===")
    println("标题: ${parser.metadata["Title"]}")
    println("编码: ${parser.encoding}")
    println("版本: ${parser.version}")
    println("加密: ${parser.metadata["Encrypted"]}")
    println("词条数: ${parser.keywords.size}")

    // 显示前 10 个词条
    println("\n=== 前 10 个词条 ===")
    parser.keywords.take(10).forEach { (offset, keyword) ->
        println("  $keyword (record offset: $offset)")
    }

    // 搜索示例
    println("\n=== 搜索示例 ===")
    val searchWords = listOf("hello", "world", "apple")
    for (word in searchWords) {
        val found = parser.keywords.find { it.second.equals(word, ignoreCase = true) }
        if (found != null) {
            println("  ✓ 找到: \"${found.second}\" (offset: ${found.first})")
        } else {
            println("  ✗ 未找到: \"$word\"")
        }
    }
}
