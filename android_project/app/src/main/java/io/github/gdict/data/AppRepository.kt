package io.github.gdict.data

import android.content.Context
import android.net.Uri
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictionaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import io.github.gdict.core.FsrsAlgorithm
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard

data class Dictionary(
    val id: Long = 0,
    val name: String,
    val path: String,
    val isEnabled: Boolean = true
)

data class HistoryItem(
    val word: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BookmarkItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val word: String,
    val definition: String = "",
    val dictionaryName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val difficulty: Double = 0.0,
    val stability: Double = 0.0,
    val nextReview: Long = 0L,
    val reviewCount: Int = 0
) {
    val isNew: Boolean get() = reviewCount == 0
    val isDue: Boolean get() = nextReview <= System.currentTimeMillis()
}

data class SearchResultItem(
    val word: String,
    val definition: String,
    val dictionaryName: String,
    val css: String = ""
)

class AppRepository(private val context: Context) {
    private val dictionaryManager = DictionaryManager(context)

    private val prefs = context.getSharedPreferences("gdict_data", Context.MODE_PRIVATE)

    private val _dictionaries = MutableStateFlow<List<Dictionary>>(emptyList())
    val dictionaries: StateFlow<List<Dictionary>> = _dictionaries.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    init {
        _dictionaries.value = dictionaryManager.getDictionaries().map { entry ->
            Dictionary(
                id = entry.id,
                name = entry.name,
                path = entry.path,
                isEnabled = entry.isEnabled
            )
        }
        _history.value = loadHistory()
        _bookmarks.value = loadBookmarks()
    }

    suspend fun searchWord(word: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        dictionaryManager.searchWord(word).map { result ->
            SearchResultItem(
                word = result.word,
                definition = result.definition,
                dictionaryName = result.dictionaryName,
                css = result.css
            )
        }
    }

    fun scanDirectory(uri: Uri): List<DictFileImporter.DictCandidate> {
        return dictionaryManager.scanDirectory(uri)
    }

    suspend fun addDictionary(name: String, path: String, companionFiles: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        val entry = dictionaryManager.addOrUpdateDictionary(name, path, companionFiles)
        val newDict = Dictionary(
            id = entry.id,
            name = entry.name,
            path = entry.path
        )
        _dictionaries.value = _dictionaries.value + newDict
    }

    fun removeDictionary(dictionary: Dictionary) {
        dictionaryManager.removeDictionary(dictionary.id)
        _dictionaries.value = _dictionaries.value.filter { it.id != dictionary.id }
    }

    fun diagnoseDictionaries(): String {
        return dictionaryManager.diagnoseAllDictionaries()
    }

    fun testMddResourcesAndHtml(): String {
        return dictionaryManager.testMddResourcesAndHtml()
    }

    fun toggleDictionary(dictionary: Dictionary) {
        dictionaryManager.toggleDictionary(dictionary.id, !dictionary.isEnabled)
        _dictionaries.value = _dictionaries.value.map {
            if (it.id == dictionary.id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun addToHistory(word: String) {
        val item = HistoryItem(word = word)
        _history.value = listOf(item) + _history.value.filter { it.word != word }
        saveHistory(_history.value)
    }

    fun removeFromHistory(item: HistoryItem) {
        _history.value = _history.value.filter { it.word != item.word }
        saveHistory(_history.value)
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory(emptyList())
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

    private fun saveHistory(items: List<HistoryItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("word", item.word)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    private fun loadHistory(): List<HistoryItem> {
        val json = prefs.getString("history", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryItem(
                    word = obj.getString("word"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
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

    fun getCssForDictionary(dictionaryName: String): String {
        val dict = dictionaryManager.getDictionaries().find { it.name == dictionaryName }
        if (dict == null) return ""
        val parser = dictionaryManager.getParserForDictionary(dict.id)
        val fileCss = parser?.companionCss ?: ""
        val mddCss = dictionaryManager.getCssFromMdd(dict.id)
        return fileCss + mddCss
    }

    suspend fun searchSuggestions(prefix: String, limit: Int = 10): List<String> = withContext(Dispatchers.IO) {
        dictionaryManager.searchSuggestions(prefix, limit)
    }

    suspend fun getRandomWords(count: Int = 5): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        dictionaryManager.getRandomWords(count)
    }

    suspend fun getAudioResource(word: String): ByteArray? = withContext(Dispatchers.IO) {
        dictionaryManager.getAudioResource(word)
    }

    suspend fun getAudioResourceByPath(path: String): ByteArray? = withContext(Dispatchers.IO) {
        dictionaryManager.getAudioResourceByPath(path)
    }

    fun getResourceByPathSync(path: String): ByteArray? {
        return dictionaryManager.getAudioResourceByPath(path)
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
}

data class ReviewStats(
    val total: Int,
    val new: Int,
    val due: Int,
    val learned: Int
)
