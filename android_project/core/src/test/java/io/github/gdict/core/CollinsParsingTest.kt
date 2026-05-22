package io.github.gdict.core

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class CollinsParsingTest {

    private var parser: MdxParser? = null

    private val mdxPath = System.getProperty("mdx.file.path")
        ?: """D:\workspace\Gdict\Collins COBUILD Advanced English Dictionary Online.mdx"""

    @Before
    fun setUp() {
        val file = File(mdxPath)
        if (!file.exists()) {
            println("WARNING: MDX file not found at: $mdxPath")
            return
        }
        println("======= Parsing MDX file: ${file.name} (${file.length()} bytes) =======")
        parser = MdxParser(file)
    }

    @After
    fun tearDown() {
        val p = parser
        if (p != null) {
            println("Final diagnostics:")
            println("  Keywords loaded: ${p.wordCount}")
            println("  Title: ${p.title}")
            println("  Encoding: ${p.encoding}")
        }
        parser?.close()
    }

    @Test
    fun testReadWordParsing() {
        val p = parser ?: return

        println("\n======= Testing 'read' word =======")

        val results = p.readArticles("read")
        println("  Results count: ${results.size}")

        assertTrue("Should find definition for 'read'", results.isNotEmpty())

        for ((word, definition) in results) {
            println("\n  Word: '$word'")
            println("  Definition length: ${definition?.length ?: 0}")
            if (definition != null) {
                // Check for garbled characters
                val hasGarbled = definition.contains('\uFFFD')
                println("  Has garbled chars (U+FFFD): $hasGarbled")

                // Check for binary garbage
                val garbledPattern = Regex("[\\x00-\\x08\\x0E-\\x1F]{3,}")
                val hasBinaryGarbage = garbledPattern.containsMatchIn(definition)
                println("  Has binary garbage: $hasBinaryGarbage")

                // Print full definition
                println("\n  ===== FULL DEFINITION =====")
                println(definition)
                println("  ===== END DEFINITION =====")

                // Check key expected content
                val expectedPhrases = listOf(
                    "Definition", "read", "verb",
                    "book", "article", "words"
                )
                for (phrase in expectedPhrases) {
                    val found = definition.contains(phrase, ignoreCase = true)
                    println("  Contains '$phrase': $found")
                }
            }
        }
    }
}
