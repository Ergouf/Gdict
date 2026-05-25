package io.github.gdict.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gdict.GdictApplication
import io.github.gdict.data.AppRepository
import io.github.gdict.data.HistoryItem
import io.github.gdict.data.SearchResultItem
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
    private val repository: AppRepository = (application as GdictApplication).repository

    val history: StateFlow<List<HistoryItem>> = repository.history
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
                val results = repository.searchWord(word)
                if (currentVersion == searchVersion) {
                    _searchResults.value = results
                    if (results.isNotEmpty()) {
                        repository.addToHistory(word)
                    }
                }
            } catch (e: Exception) {
                if (currentVersion == searchVersion) {
                    _errorMessage.value = "搜索失败: ${e.message}"
                }
            }
        }
    }

    private fun loadSuggestions(query: String) {
        viewModelScope.launch {
            try {
                _suggestions.value = repository.searchSuggestions(query, 10)
            } catch (_: Exception) {
            }
        }
    }

    fun addToHistory(word: String) {
        repository.addToHistory(word)
    }

    fun removeFromHistory(item: HistoryItem) {
        repository.removeFromHistory(item)
    }

    fun clearHistory() {
        repository.clearHistory()
    }

    fun loadWordOfTheDay() {
        viewModelScope.launch {
            try {
                val words = repository.getRandomWords(5)
                _wordOfTheDay.value = words
            } catch (_: Exception) {
            }
        }
    }

    fun getCssForDictionary(dictionaryName: String): String {
        return repository.getCssForDictionary(dictionaryName)
    }
}
