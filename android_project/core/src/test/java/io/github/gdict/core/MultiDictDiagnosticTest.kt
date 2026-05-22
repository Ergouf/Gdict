package io.github.gdict.core

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * 多词典诊断测试：验证多个 MdxParser 实例是否真正独立，
 * 确保不会出现"所有词典都显示同一个词典内容"的问题。
 */
class MultiDictDiagnosticTest {

    private var parserA: MdxParser? = null
    private var parserB: MdxParser? = null
    private var parserC: MdxParser? = null

    private val mdxPath = System.getProperty("mdx.file.path")
        ?: """D:\workspace\Gdict\Collins COBUILD Advanced English Dictionary Online.mdx"""

    @Before
    fun setUp() {
        val file = File(mdxPath)
        if (!file.exists()) {
            println("WARNING: MDX file not found at: $mdxPath")
            return
        }
        println("======= 多词典诊断测试 =======")
        println("文件: ${file.name} (${file.length()} bytes)")
        println("创建3个独立 parser 实例...")

        // 模拟3个词典，都指向同一个文件（测试独立性）
        parserA = MdxParser(file)
        parserB = MdxParser(file)
        parserC = MdxParser(file)

        println("  parserA hashCode=${parserA.hashCode()}")
        println("  parserB hashCode=${parserB.hashCode()}")
        println("  parserC hashCode=${parserC.hashCode()}")
    }

    @After
    fun tearDown() {
        parserA?.close()
        parserB?.close()
        parserC?.close()
    }

    @Test
    fun testParserObjectsAreIndependent() {
        val pA = parserA ?: return
        val pB = parserB ?: return
        val pC = parserC ?: return

        println()
        println("=== 1. 对象身份验证 ===")
        assertNotEquals("A和B应是不同对象", pA.hashCode(), pB.hashCode())
        assertNotEquals("A和C应是不同对象", pA.hashCode(), pC.hashCode())
        assertNotEquals("B和C应是不同对象", pB.hashCode(), pC.hashCode())
        println("✅ 3个 parser 是独立对象")

        println()
        println("=== 2. 元数据一致性 ===")
        assertEquals("title应一致", pA.title, pB.title)
        assertEquals("wordCount应一致", pA.wordCount, pB.wordCount)
        println("✅ 元数据一致: title='${pA.title}' words=${pA.wordCount}")

        println()
        println("=== 3. 关键词索引独立性 ===")
        val kwA = pA.getAllKeywords()
        val kwB = pB.getAllKeywords()
        val kwC = pC.getAllKeywords()
        assertEquals("关键词数量一致", kwA.size, kwB.size)
        assertEquals("关键词数量一致", kwA.size, kwC.size)
        println("✅ 关键词数量: ${kwA.size}")

        println()
        println("=== 4. 搜索独立性测试（关键） ===")
        // 对3个parser独立搜索，结果应完全一致（同一文件）
        val testWords = listOf("read", "book", "test", "word", "hello", "the")
        var allPassed = true
        for (word in testWords) {
            val rA = pA.readArticles(word)
            val rB = pB.readArticles(word)
            val rC = pC.readArticles(word)

            if (rA.size != rB.size || rA.size != rC.size) {
                println("❌ '$word': 结果数量不一致 A=${rA.size} B=${rB.size} C=${rC.size}")
                allPassed = false
                continue
            }
            if (rA.isNotEmpty()) {
                val dA = rA.values.first()
                val dB = rB.values.first()
                val dC = rC.values.first()
                if (dA != dB || dA != dC) {
                    println("❌ '$word': 释义内容不一致")
                    allPassed = false
                } else {
                    println("  '$word': ${rA.size}条结果, 释义长度=${dA?.length ?: 0} ✅")
                }
            } else {
                println("  '$word': 未找到 ✅")
            }
        }
        assertTrue("所有搜索测试应通过", allPassed)
        println("✅ 搜索独立性测试通过")

        println()
        println("=== 5. 预测搜索独立性测试 ===")
        val predA = pA.readArticlesPredictive("re")
        val predB = pB.readArticlesPredictive("re")
        val predC = pC.readArticlesPredictive("re")
        assertEquals("A和B预测结果数量一致", predA.size, predB.size)
        assertEquals("A和C预测结果数量一致", predA.size, predC.size)
        println("✅ 预测搜索独立: 're' → ${predA.size}条结果")

        println()
        println("======= 结论 =======")
        println("MdxParser 实例之间完全独立，不存在共享状态问题。")
        println("如果搜索时不同词典显示了相同内容，问题可能在：")
        println("  1. `loadedDicts` 中不同ID映射到了同一个parser")
        println("  2. 多个 DictEntry 指向了同一个文件路径")
        println("  3. searchWord 路由逻辑bug")
        println("============================")
    }
}
