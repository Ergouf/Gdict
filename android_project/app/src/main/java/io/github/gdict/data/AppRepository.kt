package io.github.gdict.data

import android.content.Context
import android.net.Uri
import io.github.gdict.core.DictionaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val dictionaryName: String
)

class AppRepository(private val context: Context) {
    private val dictionaryManager = DictionaryManager(context)

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
    }

    suspend fun searchWord(word: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        dictionaryManager.searchWord(word).map { result ->
            SearchResultItem(
                word = result.word,
                definition = result.definition,
                dictionaryName = result.dictionaryName
            )
        }
    }

    fun scanDirectory(uri: Uri): List<DictionaryManager.DictCandidate> {
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

    fun toggleDictionary(dictionary: Dictionary) {
        dictionaryManager.toggleDictionary(dictionary.id, !dictionary.isEnabled)
        _dictionaries.value = _dictionaries.value.map {
            if (it.id == dictionary.id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun addToHistory(word: String) {
        val item = HistoryItem(word = word)
        _history.value = listOf(item) + _history.value.filter { it.word != word }
    }

    fun removeFromHistory(item: HistoryItem) {
        _history.value = _history.value.filter { it.word != item.word }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun addBookmark(word: String, definition: String, dictionaryName: String = "") {
        val item = BookmarkItem(word = word, definition = definition, dictionaryName = dictionaryName)
        _bookmarks.value = _bookmarks.value.filter { it.word != word } + item
    }

    fun removeBookmark(item: BookmarkItem) {
        _bookmarks.value = _bookmarks.value.filter { it.word != item.word }
    }
}
