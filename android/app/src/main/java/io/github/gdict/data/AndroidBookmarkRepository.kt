package io.github.gdict.data

import android.content.Context
import io.github.gdict.core.FsrsAlgorithm
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.core.model.ReviewStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class AndroidBookmarkRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("gdict_data", Context.MODE_PRIVATE)

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    init {
        _bookmarks.value = loadBookmarks()
    }

    fun addBookmark(word: String, definition: String, dictionaryName: String = "") {
        val item = BookmarkItem(word = word, definition = definition, dictionaryName = dictionaryName)
        _bookmarks.value = _bookmarks.value.filter { !(it.word == word && it.dictionaryName == dictionaryName) } + item
        saveBookmarks(_bookmarks.value)
    }

    fun removeBookmark(item: BookmarkItem) {
        _bookmarks.value = _bookmarks.value.filter { it.id != item.id }
        saveBookmarks(_bookmarks.value)
    }

    fun clearBookmarks() {
        _bookmarks.value = emptyList()
        saveBookmarks(emptyList())
    }

    fun getDueBookmarks(): List<BookmarkItem> {
        return _bookmarks.value.filter { it.isDue }
    }

    fun getNewBookmarks(): List<BookmarkItem> {
        return _bookmarks.value.filter { it.isNew }
    }

    fun getReviewStats(): ReviewStats {
        val all = _bookmarks.value
        val new = all.count { it.isNew }
        val due = all.count { it.isDue && !it.isNew }
        val learned = all.count { !it.isNew && !it.isDue }
        return ReviewStats(total = all.size, new = new, due = due, learned = learned)
    }

    fun getSchedulingForBookmark(item: BookmarkItem): Map<Rating, SchedulingCard> {
        val now = System.currentTimeMillis()
        return if (item.isNew) {
            FsrsAlgorithm.scheduleNew(now)
        } else {
            FsrsAlgorithm.schedule(item.difficulty, item.stability, item.nextReview - item.nextReview % FsrsAlgorithm.DAY_MS, now)
        }
    }

    fun applyReview(item: BookmarkItem, card: SchedulingCard) {
        val updated = item.copy(
            difficulty = card.difficulty,
            stability = card.stability,
            nextReview = card.nextReview,
            reviewCount = item.reviewCount + 1
        )
        _bookmarks.value = _bookmarks.value.map {
            if (it.id == item.id) updated else it
        }
        saveBookmarks(_bookmarks.value)
    }

    private fun saveBookmarks(items: List<BookmarkItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("word", item.word)
                put("definition", item.definition)
                put("dictionaryName", item.dictionaryName)
                put("timestamp", item.timestamp)
                put("difficulty", item.difficulty)
                put("stability", item.stability)
                put("nextReview", item.nextReview)
                put("reviewCount", item.reviewCount)
            }
            arr.put(obj)
        }
        prefs.edit().putString("bookmarks", arr.toString()).apply()
    }

    private fun loadBookmarks(): List<BookmarkItem> {
        val json = prefs.getString("bookmarks", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                BookmarkItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    word = obj.getString("word"),
                    definition = obj.optString("definition", ""),
                    dictionaryName = obj.optString("dictionaryName", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    difficulty = obj.optDouble("difficulty", 0.0),
                    stability = obj.optDouble("stability", 0.0),
                    nextReview = obj.optLong("nextReview", 0L),
                    reviewCount = obj.optInt("reviewCount", 0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
