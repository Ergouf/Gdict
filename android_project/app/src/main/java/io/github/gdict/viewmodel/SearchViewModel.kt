package io.github.gdict.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gdict.GdictApplication
import io.github.gdict.R
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.data.HistoryRepository
import io.github.gdict.core.model.Dictionary
import io.github.gdict.core.model.HistoryItem
import io.github.gdict.core.model.SearchResultItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val dictionaryRepo: DictionaryRepository = (application as GdictApplication).dictionaryRepository
    private val historyRepo: HistoryRepository = (application as GdictApplication).historyRepository

    val history: StateFlow<List<HistoryItem>> = historyRepo.history
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _wordOfTheDay = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val wordOfTheDay: StateFlow<List<Pair<String, String>>> = _wordOfTheDay.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val cssCache = mutableMapOf<String, String>()

    init {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    performSearch(query)
                    loadSuggestions(query)
                } else {
                    _searchResults.value = emptyList()
                    _suggestions.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private var searchVersion = 0L

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun searchWord(word: String) {
        if (word.isNotBlank()) {
            _searchQuery.value = word
        }
    }

    private fun performSearch(word: String) {
        val currentVersion = ++searchVersion
        viewModelScope.launch {
            try {
                val results = dictionaryRepo.searchWord(word)
                if (currentVersion == searchVersion) {
                    _searchResults.value = results
                    for (result in results) {
                        if (result.css.isNotEmpty()) {
                            cssCache[result.dictionaryName] = result.css
                        }
                    }
                    if (results.isNotEmpty()) {
                        historyRepo.addToHistory(word)
                    }
                }
            } catch (e: Exception) {
                if (currentVersion == searchVersion) {
                    _errorMessage.value = getApplication<GdictApplication>().getString(R.string.search_failed, e.message)
                }
            }
        }
    }

    private fun loadSuggestions(query: String) {
        viewModelScope.launch {
            try {
                _suggestions.value = dictionaryRepo.searchSuggestions(query, 10)
            } catch (_: Exception) {
            }
        }
    }

    fun addToHistory(word: String) {
        historyRepo.addToHistory(word)
    }

    fun removeFromHistory(item: HistoryItem) {
        historyRepo.removeFromHistory(item)
    }

    fun clearHistory() {
        historyRepo.clearHistory()
    }

    fun loadWordOfTheDay() {
        viewModelScope.launch {
            try {
                val words = dictionaryRepo.getRandomWords(5)
                _wordOfTheDay.value = words
            } catch (_: Exception) {
            }
        }
    }

    fun getCssForDictionary(dictionaryName: String): String {
        cssCache[dictionaryName]?.let { return it }
        val css = dictionaryRepo.getCssForDictionary(dictionaryName)
        if (css.isNotEmpty()) {
            cssCache[dictionaryName] = css
        }
        return css
    }

    suspend fun searchWordForResult(word: String): List<SearchResultItem> {
        return try {
            dictionaryRepo.searchWord(word)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
