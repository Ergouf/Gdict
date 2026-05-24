package io.github.gdict.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gdict.GdictApplication
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictionaryManager
import io.github.gdict.data.AppRepository
import io.github.gdict.data.BookmarkItem
import io.github.gdict.data.Dictionary
import io.github.gdict.data.HistoryItem
import io.github.gdict.data.SearchResultItem
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import io.github.gdict.data.ReviewStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as GdictApplication).repository

    val dictionaries: StateFlow<List<Dictionary>> = repository.dictionaries
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.history
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bookmarks: StateFlow<List<BookmarkItem>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _scanPopup = MutableStateFlow(false)
    val scanPopup: StateFlow<Boolean> = _scanPopup.asStateFlow()

    private var searchVersion = 0L

    private val _diagnosticResult = MutableStateFlow<String?>(null)
    val diagnosticResult: StateFlow<String?> = _diagnosticResult.asStateFlow()

    private val _wordOfTheDay = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val wordOfTheDay: StateFlow<List<Pair<String, String>>> = _wordOfTheDay.asStateFlow()

    private val _reviewStats = MutableStateFlow(ReviewStats(0, 0, 0, 0))
    val reviewStats: StateFlow<ReviewStats> = _reviewStats.asStateFlow()

    private val _dueBookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val dueBookmarks: StateFlow<List<BookmarkItem>> = _dueBookmarks.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _currentScheduling = MutableStateFlow<Map<Rating, SchedulingCard>>(emptyMap())
    val currentScheduling: StateFlow<Map<Rating, SchedulingCard>> = _currentScheduling.asStateFlow()

    private val _sessionReviewed = MutableStateFlow(0)
    val sessionReviewed: StateFlow<Int> = _sessionReviewed.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun searchWord(word: String) {
        if (word.isNotBlank()) {
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
    }

    fun toggleBookmark(word: String, definition: String, dictionaryName: String = "") {
        try {
            val existing = repository.bookmarks.value.find { it.word == word && it.dictionaryName == dictionaryName }
            if (existing != null) {
                repository.removeBookmark(existing)
            } else {
                repository.addBookmark(word, definition, dictionaryName)
            }
        } catch (e: Exception) {
            _errorMessage.value = "收藏操作失败: ${e.message}"
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
    }

    fun setScanPopup(enabled: Boolean) {
        _scanPopup.value = enabled
    }

    fun scanDirectory(uri: Uri): List<DictFileImporter.DictCandidate> {
        return try {
            repository.scanDirectory(uri)
        } catch (e: Exception) {
            _errorMessage.value = "扫描目录失败: ${e.message}"
            emptyList()
        }
    }

    fun addDictionary(name: String, path: String, companionFiles: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                repository.addDictionary(name, path, companionFiles)
            } catch (e: Throwable) {
                _errorMessage.value = "添加词典失败: ${e.javaClass.simpleName} - ${e.message}"
                android.util.Log.e("VM", "addDictionary failed", e)
            }
        }
    }

    fun batchImport(candidates: List<DictFileImporter.DictCandidate>, onComplete: () -> Unit) {
        viewModelScope.launch {
            _importing.value = true
            var successCount = 0
            var failCount = 0
            try {
                withContext(Dispatchers.IO) {
                    for (candidate in candidates) {
                        try {
                            repository.addDictionary(candidate.name, candidate.fileUri, candidate.companionFiles)
                            successCount++
                            android.util.Log.i("VM", "Imported OK: ${candidate.name}")
                        } catch (e: Throwable) {
                            failCount++
                            val errType = e.javaClass.simpleName
                            val errMsg = e.message ?: "(no message)"
                            android.util.Log.e("VM", "Import failed for ${candidate.name}: $errType - $errMsg", e)
                            _errorMessage.value = "导入 ${candidate.name} 失败: $errType - $errMsg"
                        }
                    }
                }
                if (failCount > 0) {
                    _errorMessage.value = "导入完成: 成功 $successCount, 失败 $failCount"
                }
            } catch (e: Throwable) {
                _errorMessage.value = "批量导入异常: ${e.javaClass.simpleName} - ${e.message}"
                android.util.Log.e("VM", "batchImport crashed", e)
            } finally {
                _importing.value = false
                onComplete()
            }
        }
    }

    fun removeDictionary(dictionary: Dictionary) {
        try {
            repository.removeDictionary(dictionary)
        } catch (e: Exception) {
            _errorMessage.value = "移除词典失败: ${e.message}"
        }
    }

    fun toggleDictionary(dictionary: Dictionary) {
        try {
            repository.toggleDictionary(dictionary)
        } catch (e: Exception) {
            _errorMessage.value = "切换词典状态失败: ${e.message}"
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

    fun addBookmark(word: String, definition: String = "", dictionaryName: String = "") {
        repository.addBookmark(word, definition, dictionaryName)
    }

    fun removeBookmark(item: BookmarkItem) {
        repository.removeBookmark(item)
    }

    fun clearAllData() {
        repository.clearHistory()
        repository.clearBookmarks()
    }

    fun diagnoseDictionaries() {
        viewModelScope.launch {
            _diagnosticResult.value = withContext(Dispatchers.IO) {
                repository.diagnoseDictionaries() + "\n\n" + repository.testMddResourcesAndHtml()
            }
        }
    }

    fun clearDiagnosticResult() {
        _diagnosticResult.value = null
    }

    fun getCssForDictionary(dictionaryName: String): String {
        return repository.getCssForDictionary(dictionaryName)
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

    suspend fun getAudioResource(word: String): ByteArray? {
        return repository.getAudioResource(word)
    }

    suspend fun getAudioResourceByPath(path: String): ByteArray? {
        return repository.getAudioResourceByPath(path)
    }

    fun getResourceByPathSync(path: String): ByteArray? {
        return repository.getResourceByPathSync(path)
    }

    fun startReviewSession() {
        val due = repository.getDueBookmarks()
        val new = repository.getNewBookmarks()
        val combined = (due + new).distinctBy { it.id }
        _dueBookmarks.value = combined
        _currentCardIndex.value = 0
        _sessionReviewed.value = 0
        if (combined.isNotEmpty()) {
            _currentScheduling.value = repository.getSchedulingForBookmark(combined[0])
        }
        refreshReviewStats()
    }

    fun rateCurrentCard(rating: Rating) {
        val items = _dueBookmarks.value
        val index = _currentCardIndex.value
        if (index >= items.size) return

        val item = items[index]
        val scheduling = _currentScheduling.value[rating] ?: return

        repository.applyReview(item, scheduling)
        _sessionReviewed.value = _sessionReviewed.value + 1

        val nextIndex = index + 1
        if (nextIndex < items.size) {
            _currentCardIndex.value = nextIndex
            _currentScheduling.value = repository.getSchedulingForBookmark(items[nextIndex])
        } else {
            _currentCardIndex.value = items.size
            _currentScheduling.value = emptyMap()
        }
        refreshReviewStats()
    }

    fun skipCurrentCard() {
        val items = _dueBookmarks.value
        val index = _currentCardIndex.value
        val nextIndex = index + 1
        if (nextIndex < items.size) {
            _currentCardIndex.value = nextIndex
            _currentScheduling.value = repository.getSchedulingForBookmark(items[nextIndex])
        } else {
            _currentCardIndex.value = items.size
            _currentScheduling.value = emptyMap()
        }
    }

    fun refreshReviewStats() {
        _reviewStats.value = repository.getReviewStats()
    }

    val isSessionComplete: Boolean
        get() = _currentCardIndex.value >= _dueBookmarks.value.size && _dueBookmarks.value.isNotEmpty()

    val currentReviewItem: BookmarkItem?
        get() {
            val items = _dueBookmarks.value
            val index = _currentCardIndex.value
            return if (index < items.size) items[index] else null
        }
}
