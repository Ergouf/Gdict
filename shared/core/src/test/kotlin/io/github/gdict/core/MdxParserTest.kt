package io.github.gdict.core

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Random
import java.util.regex.Pattern

class MdxParserTest {

    private var parser: MdxParser? = null

    private val mdxPath = System.getProperty("mdx.file.path")
        ?: run {
            val propFile = File("test_mdx_path.properties")
            if (propFile.exists()) {
                propFile.readLines().firstOrNull { it.startsWith("mdx.file.path=") }?.substringAfter("=")
            } else null
        }

    @Before
    fun setUp() {
        if (mdxPath == null) {
            println("WARNING: No MDX file path configured. Set -Dmdx.file.path=<path> to run tests.")
            return
        }
        val file = File(mdxPath)
        if (!file.exists()) {
            println("WARNING: MDX file not found at: $mdxPath")
            println("Skipping MdxParser test - file not available")
            return
        }
        println("Parsing MDX file: ${file.name} (${file.length()} bytes)")
        parser = MdxParser(file)
    }

    @After
    fun tearDown() {
        parser?.close()
    }

    @Test
    fun testParseHeader() {
        val p = parser ?: return
        println("=== Header ===")
        println("  title: ${p.title}")
        println("  encoding: ${p.encoding}")
        println("  isKeyCaseSensitive: ${p.isKeyCaseSensitive}")

        assertTrue("Title should not be empty", p.title.isNotEmpty())
        assertTrue("Encoding should not be empty", p.encoding.isNotEmpty())
    }

    @Test
    fun testKeywordsLoaded() {
        val p = parser ?: return
        println("=== Keywords ===")
        println("  wordCount: ${p.wordCount}")

        assertTrue("wordCount should be > 0", p.wordCount > 0)

        val keywords = p.getAllKeywords()
        assertTrue("Keywords list should not be empty", keywords.isNotEmpty())
        assertEquals("wordCount should match keywords size", p.wordCount, keywords.size)

        println("  First 5 keywords: ${keywords.take(5)}")
        println("  Last 5 keywords: ${keywords.takeLast(5)}")

        for (kw in keywords.take(5)) {
            assertFalse("Keyword should not be empty", kw.isEmpty())
            assertFalse("Keyword should not contain garbled chars (0xFFFD)", kw.contains('\uFFFD'))
        }
    }

    @Test
    fun testSearchExactWord() {
        val p = parser ?: return
        val keywords = p.getAllKeywords()
        if (keywords.isEmpty()) return

        val testWord = keywords.first()
        println("=== Search exact word: '$testWord' ===")

        val results = p.readArticles(testWord)
        println("  Results count: ${results.size}")

        assertTrue("Should find results for '$testWord'", results.isNotEmpty())

        for ((word, definition) in results) {
            println("  Word: $word")
            println("  Definition length: ${definition?.length ?: 0}")
            assertNotNull("Definition should not be null for '$word'", definition)
            if (definition != null) {
                assertTrue("Definition should not be empty for '$word'", definition.isNotEmpty())
                assertFalse(
                    "Definition should not be garbled (contain 0xFFFD) for '$word'",
                    definition.contains('\uFFFD')
                )
            }
        }
    }

    @Test
    fun testSearchCommonEnglishWord() {
        val p = parser ?: return
        val testWords = listOf("the", "a", "is", "hello", "word", "book", "test")

        println("=== Search common English words ===")
        var foundAny = false

        for (testWord in testWords) {
            val results = p.readArticles(testWord)
            if (results.isNotEmpty()) {
                foundAny = true
                println("  '$testWord': ${results.size} results")
                for ((word, definition) in results) {
                    assertNotNull("Definition for '$word' should not be null", definition)
                    if (definition != null && definition.isNotEmpty()) {
                        assertFalse(
                            "Definition for '$word' should not be garbled",
                            definition.contains('\uFFFD')
                        )
                        println("    definition preview: ${definition.take(80)}...")
                    }
                }
            } else {
                println("  '$testWord': no results")
            }
        }

        assertTrue("At least one common word should be found in dictionary", foundAny)
    }

    @Test
    fun testPredictiveSearch() {
        val p = parser ?: return
        println("=== Predictive search ===")

        val results = p.readArticlesPredictive("ab")
        println("  Prefix 'ab': ${results.size} results")

        assertTrue("Predictive search for 'ab' should return results", results.isNotEmpty())

        for ((word, definition) in results.entries.take(3)) {
            assertTrue(
                "Predictive result '$word' should start with 'ab'",
                word.startsWith("ab", ignoreCase = true)
            )
            if (definition != null && definition.isNotEmpty()) {
                assertFalse(
                    "Predictive result for '$word' should not be garbled",
                    definition.contains('\uFFFD')
                )
            }
        }
    }

    @Test
    fun testDefinitionContainsHtml() {
        val p = parser ?: return
        val keywords = p.getAllKeywords()
        if (keywords.isEmpty()) return

        val testWord = keywords.first()
        val results = p.readArticles(testWord)
        if (results.isEmpty()) return

        val definition = results.values.first()
        if (definition == null || definition.isEmpty()) return

        println("=== HTML content check for '$testWord' ===")
        val hasHtmlTags = definition.contains("<") && definition.contains(">")
        println("  Contains HTML tags: $hasHtmlTags")
        println("  Definition preview: ${definition.take(200)}")

        val garbledPattern = Pattern.compile("[\\x00-\\x08\\x0E-\\x1F]{3,}")
        val hasGarbledBinary = garbledPattern.matcher(definition).find()
        assertFalse("Definition should not contain binary garbage", hasGarbledBinary)
    }

    @Test
    fun testKeywordEncoding() {
        val p = parser ?: return
        val keywords = p.getAllKeywords()
        if (keywords.isEmpty()) return

        println("=== Keyword encoding diagnostic ===")
        val nonAsciiKeywords = keywords.filter { kw -> kw.any { it.code > 127 } }
        println("  Total keywords: ${keywords.size}")
        println("  Non-ASCII keywords: ${nonAsciiKeywords.size}")

        for (kw in nonAsciiKeywords.take(10)) {
            val hexBytes = kw.toByteArray(Charsets.UTF_8).joinToString(" ") { "%02X".format(it) }
            val charCodes = kw.map { "U+%04X".format(it.code) }.joinToString(" ")
            val isDoubleEncoded = kw.contains('\u00C3') || kw.contains('\u00A9')
            println("  '$kw' hex=$hexBytes chars=$charCodes doubleEncoded=$isDoubleEncoded")
        }

        val doubleEncodedCount = nonAsciiKeywords.count { kw ->
            kw.contains('\u00C3') || kw.contains('\u00A9') || kw.contains('\u00A8')
        }
        println("  Double-encoded keywords: $doubleEncodedCount / ${nonAsciiKeywords.size}")

        if (doubleEncodedCount > 0) {
            println("  WARNING: Some keywords appear to be double-encoded (UTF-8 bytes read as Latin-1)")
        }

        assertEquals("No keywords should be double-encoded", 0, doubleEncodedCount)
    }

    @Test
    fun testMultipleWordsDefinitions() {
        val p = parser ?: return
        val keywords = p.getAllKeywords()
        if (keywords.size < 10) return

        println("=== Multiple words definition check ===")
        var successCount = 0
        var garbledCount = 0
        val sampleWords = keywords.filterIndexed { i, _ -> i % (keywords.size / 10) == 0 }.take(10)

        for (word in sampleWords) {
            val results = p.readArticles(word)
            val def = results.values.firstOrNull()
            if (def != null && def.isNotEmpty()) {
                successCount++
                if (def.contains('\uFFFD')) {
                    garbledCount++
                    println("  GARBLED: '$word' - ${def.take(50)}...")
                }
            }
        }

        println("  Sampled ${sampleWords.size} words: $successCount with definitions, $garbledCount garbled")
        assertTrue(
            "At most 20% of sampled definitions may be garbled (got $garbledCount/$successCount)",
            garbledCount <= successCount * 0.2
        )
    }
}
