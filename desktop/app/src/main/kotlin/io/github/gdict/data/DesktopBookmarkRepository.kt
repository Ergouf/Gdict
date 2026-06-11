package io.github.gdict.data

import io.github.gdict.core.FsrsAlgorithm
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.core.model.ReviewStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DesktopBookmarkRepository(private val storage: StorageBackend) : BookmarkRepository {

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    override val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val _bookmarksByWord = MutableStateFlow<Map<String, BookmarkItem>>(emptyMap())
    override val bookmarksByWord: StateFlow<Map<String, BookmarkItem>> = _bookmarksByWord.asStateFlow()

    @Volatile private var diskLoaded = false

    /**
     * Loads bookmarks from disk on a background dispatcher. The in-memory
     * StateFlow starts empty so the UI can render instantly; results land
     * here once parsing completes. Safe to call multiple times.
     */
    fun loadAsync(scope: CoroutineScope) {
        if (diskLoaded) return
        diskLoaded = true
        scope.launch(Dispatchers.IO) {
            val items = loadBookmarks()
            _bookmarks.value = items
            _bookmarksByWord.value = items.associateBy { it.word }
        }
    }

    private fun loadBookmarks(): List<BookmarkItem> {
        val json = storage.getString("bookmarks") ?: return emptyList()
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

    private fun saveBookmarks() {
        val arr = JSONArray()
        for (item in _bookmarks.value) {
            val obj = JSONObject().apply {
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
        storage.putString("bookmarks", arr.toString())
    }

    private fun updateBookmarks(newList: List<BookmarkItem>) {
        _bookmarks.value = newList
        _bookmarksByWord.value = newList.associateBy { it.word }
    }

    override fun addBookmark(word: String, definition: String, dictionaryName: String) {
        val item = BookmarkItem(word = word, definition = definition, dictionaryName = dictionaryName)
        updateBookmarks(_bookmarks.value.filter { !(it.word == word && it.dictionaryName == dictionaryName) } + item)
        saveBookmarks()
    }

    override fun removeBookmark(item: BookmarkItem) {
        updateBookmarks(_bookmarks.value.filter { it.id != item.id })
        saveBookmarks()
    }

    override fun clearBookmarks() {
        updateBookmarks(emptyList())
        saveBookmarks()
    }

    override fun getDueBookmarks(): List<BookmarkItem> {
        return _bookmarks.value.filter { it.isDue }
    }

    override fun getNewBookmarks(): List<BookmarkItem> {
        return _bookmarks.value.filter { it.isNew }
    }

    override fun getReviewStats(): ReviewStats {
        val all = _bookmarks.value
        val new = all.count { it.isNew }
        val due = all.count { it.isDue && !it.isNew }
        val learned = all.count { !it.isNew && !it.isDue }
        return ReviewStats(total = all.size, new = new, due = due, learned = learned)
    }

    override fun getSchedulingForBookmark(item: BookmarkItem): Map<Rating, SchedulingCard> {
        val now = System.currentTimeMillis()
        return if (item.isNew) {
            FsrsAlgorithm.scheduleNew(now)
        } else {
            FsrsAlgorithm.schedule(
                item.difficulty,
                item.stability,
                item.nextReview - item.nextReview % FsrsAlgorithm.DAY_MS,
                now
            )
        }
    }

    override fun applyReview(item: BookmarkItem, card: SchedulingCard) {
        val updated = item.copy(
            difficulty = card.difficulty,
            stability = card.stability,
            nextReview = card.nextReview,
            reviewCount = item.reviewCount + 1
        )
        updateBookmarks(_bookmarks.value.map { if (it.id == item.id) updated else it })
        saveBookmarks()
    }
}