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
    val word: String,
    val definition: String = "",
    val dictionaryName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

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

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory(emptyList())
    }

    fun addBookmark(word: String, definition: String, dictionaryName: String = "") {
        val item = BookmarkItem(word = word, definition = definition, dictionaryName = dictionaryName)
        _bookmarks.value = _bookmarks.value.filter { it.word != word } + item
        saveBookmarks(_bookmarks.value)
    }

    fun removeBookmark(item: BookmarkItem) {
        _bookmarks.value = _bookmarks.value.filter { it.word != item.word }
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
                put("word", item.word)
                put("definition", item.definition)
                put("dictionaryName", item.dictionaryName)
                put("timestamp", item.timestamp)
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
                    word = obj.getString("word"),
                    definition = obj.optString("definition", ""),
                    dictionaryName = obj.optString("dictionaryName", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
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
}
