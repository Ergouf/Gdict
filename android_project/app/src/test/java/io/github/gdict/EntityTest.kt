
package io.github.gdict

import io.github.gdict.data.entity.BookmarkItem
import io.github.gdict.data.entity.Dictionary
import io.github.gdict.data.entity.HistoryItem
import org.junit.Test
import org.junit.Assert.*

class EntityTest {

    @Test
    fun testDictionaryCreation() {
        val dictionary = Dictionary(
            id = 1L,
            name = "测试词典",
            path = "/sdcard/dictionaries/test.dsl",
            isEnabled = true
        )

        assertEquals(1L, dictionary.id)
        assertEquals("测试词典", dictionary.name)
        assertEquals("/sdcard/dictionaries/test.dsl", dictionary.path)
        assertTrue(dictionary.isEnabled)
    }

    @Test
    fun testDictionaryToggle() {
        var dictionary = Dictionary(
            id = 1L,
            name = "测试词典",
            path = "/path",
            isEnabled = true
        )

        assertTrue(dictionary.isEnabled)
        
        dictionary = dictionary.copy(isEnabled = false)
        
        assertFalse(dictionary.isEnabled)
    }

    @Test
    fun testHistoryItemCreation() {
        val historyItem = HistoryItem(
            word = "hello",
            timestamp = System.currentTimeMillis()
        )

        assertEquals("hello", historyItem.word)
        assertTrue(historyItem.timestamp > 0)
    }

    @Test
    fun testBookmarkItemCreation() {
        val bookmarkItem = BookmarkItem(
            word = "world",
            definition = "世界",
            timestamp = System.currentTimeMillis()
        )

        assertEquals("world", bookmarkItem.word)
        assertEquals("世界", bookmarkItem.definition)
        assertTrue(bookmarkItem.timestamp > 0)
    }
}

