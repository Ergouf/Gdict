package io.github.gdict.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DictSearchEngineTest {

    private lateinit var engine: DictSearchEngine

    @Before
    fun setUp() {
        engine = DictSearchEngine()
    }

    @Test
    fun testSearchWordBlankQueryReturnsEmpty() {
        val result = engine.searchWord(
            query = "",
            dictionaries = emptyList(),
            loadedDicts = emptyMap(),
            cssCache = mutableMapOf(),
            loadedMdds = emptyMap()
        )
        assertTrue("Blank query should return empty results", result.isEmpty())
    }

    @Test
    fun testSearchWordWhitespaceQueryReturnsEmpty() {
        val result = engine.searchWord(
            query = "   ",
            dictionaries = emptyList(),
            loadedDicts = emptyMap(),
            cssCache = mutableMapOf(),
            loadedMdds = emptyMap()
        )
        assertTrue("Whitespace query should return empty results", result.isEmpty())
    }

    @Test
    fun testSearchWordNoDictionariesReturnsEmpty() {
        val result = engine.searchWord(
            query = "hello",
            dictionaries = emptyList(),
            loadedDicts = emptyMap(),
            cssCache = mutableMapOf(),
            loadedMdds = emptyMap()
        )
        assertTrue("No dictionaries should return empty results", result.isEmpty())
    }

    @Test
    fun testSearchWordNoLoadedParsersReturnsEmpty() {
        val dict = DictionaryManager.DictEntry(
            id = 1L,
            name = "TestDict",
            path = "/test.mdx",
            dictFilePath = "/test.mdx",
            isEnabled = true
        )
        val result = engine.searchWord(
            query = "hello",
            dictionaries = listOf(dict),
            loadedDicts = emptyMap(),
            cssCache = mutableMapOf(),
            loadedMdds = emptyMap()
        )
        assertTrue("No loaded parsers should return empty results", result.isEmpty())
    }

    @Test
    fun testSearchWordDisabledDictionarySkipped() {
        val dict = DictionaryManager.DictEntry(
            id = 1L,
            name = "TestDict",
            path = "/test.mdx",
            dictFilePath = "/test.mdx",
            isEnabled = false
        )
        val result = engine.searchWord(
            query = "hello",
            dictionaries = listOf(dict),
            loadedDicts = emptyMap(),
            cssCache = mutableMapOf(),
            loadedMdds = emptyMap()
        )
        assertTrue("Disabled dictionary should be skipped", result.isEmpty())
    }

    @Test
    fun testSearchWordMixedEnabledDisabledOnlySearchesEnabled() {
        val enabledDict = DictionaryManager.DictEntry(
            id = 1L,
            name = "EnabledDict",
            path = "/enabled.mdx",
            dictFilePath = "/enabled.mdx",
            isEnabled = true
        )
        val disabledDict = DictionaryManager.DictEntry(
            id = 2L,
            name = "DisabledDict",
            path = "/disabled.mdx",
            dictFilePath = "/disabled.mdx",
            isEnabled = false
        )
        val result = engine.searchWord(
            query = "hello",
            dictionaries = listOf(enabledDict, disabledDict),
            loadedDicts = emptyMap(),
            cssCache = mutableMapOf(),
            loadedMdds = emptyMap()
        )
        assertTrue("Only enabled dict should be searched (but no parser so empty)", result.isEmpty())
    }

    @Test
    fun testGetAudioResourceNoMddsReturnsNull() {
        val dict = DictionaryManager.DictEntry(
            id = 1L,
            name = "TestDict",
            path = "/test.mdx",
            dictFilePath = "/test.mdx",
            isEnabled = true
        )
        val result = engine.getAudioResource(
            word = "hello",
            dictionaries = listOf(dict),
            loadedMdds = emptyMap()
        )
        assertNull("No MDDs should return null audio", result)
    }

    @Test
    fun testGetAudioResourceEmptyDictionariesReturnsNull() {
        val result = engine.getAudioResource(
            word = "hello",
            dictionaries = emptyList(),
            loadedMdds = emptyMap()
        )
        assertNull("Empty dictionaries should return null audio", result)
    }

    @Test
    fun testGetAudioResourceByPathNoMddsReturnsNull() {
        val dict = DictionaryManager.DictEntry(
            id = 1L,
            name = "TestDict",
            path = "/test.mdx",
            dictFilePath = "/test.mdx",
            isEnabled = true
        )
        val result = engine.getAudioResourceByPath(
            path = "/audio/hello.mp3",
            dictionaries = listOf(dict),
            loadedMdds = emptyMap()
        )
        assertNull("No MDDs should return null audio by path", result)
    }

    @Test
    fun testGetAudioResourceByPathEmptyDictionariesReturnsNull() {
        val result = engine.getAudioResourceByPath(
            path = "/audio/hello.mp3",
            dictionaries = emptyList(),
            loadedMdds = emptyMap()
        )
        assertNull("Empty dictionaries should return null audio by path", result)
    }

    @Test
    fun testGetRandomWordsNoDictionariesReturnsEmpty() {
        val result = engine.getRandomWords(
            count = 5,
            dictionaries = emptyList(),
            loadedDicts = emptyMap()
        )
        assertTrue("No dictionaries should return empty word list", result.isEmpty())
    }

    @Test
    fun testGetRandomWordsDisabledDictSkipped() {
        val dict = DictionaryManager.DictEntry(
            id = 1L,
            name = "TestDict",
            path = "/test.mdx",
            dictFilePath = "/test.mdx",
            isEnabled = false
        )
        val result = engine.getRandomWords(
            count = 5,
            dictionaries = listOf(dict),
            loadedDicts = emptyMap()
        )
        assertTrue("Disabled dictionary should be skipped for random words", result.isEmpty())
    }

    @Test
    fun testGetRandomWordsNoLoadedParsersReturnsEmpty() {
        val dict = DictionaryManager.DictEntry(
            id = 1L,
            name = "TestDict",
            path = "/test.mdx",
            dictFilePath = "/test.mdx",
            isEnabled = true
        )
        val result = engine.getRandomWords(
            count = 5,
            dictionaries = listOf(dict),
            loadedDicts = emptyMap()
        )
        assertTrue("No loaded parsers should return empty word list", result.isEmpty())
    }

    @Test
    fun testBuildCssCachesResult() {
        val cssCache = mutableMapOf<Long, String>()
        cssCache[1L] = "cached css"
        val result = engine.buildCss(
            parser = createMockParser(),
            dictId = 1L,
            cssCache = cssCache,
            loadedMdds = emptyMap(),
            cssKeysCache = null
        )
        assertEquals("cached css", result)
    }

    @Test
    fun testGetCssFromMddNoMddReturnsEmpty() {
        val result = engine.getCssFromMdd(
            dictId = 1L,
            loadedMdds = emptyMap(),
            cssKeysCache = null
        )
        assertEquals("No MDD should return empty CSS", "", result)
    }

    private fun createMockParser(): MdxParser {
        val tempFile = java.io.File.createTempFile("test_mdx", ".mdx")
        tempFile.deleteOnExit()
        return MdxParser(tempFile)
    }
}
