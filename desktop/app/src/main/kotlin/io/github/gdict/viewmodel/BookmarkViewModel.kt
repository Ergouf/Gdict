package io.github.gdict.viewmodel

import io.github.gdict.data.BookmarkRepository
import io.github.gdict.core.model.BookmarkItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BookmarkViewModel(
    private val bookmarkRepo: BookmarkRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    val bookmarks: StateFlow<List<BookmarkItem>> = bookmarkRepo.bookmarks
        .stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val bookmarksByWord: StateFlow<Map<String, BookmarkItem>> = bookmarkRepo.bookmarksByWord
        .stateIn(coroutineScope, SharingStarted.Lazily, emptyMap())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleBookmark(word: String, definition: String, dictionaryName: String = "") {
        try {
            val existing = bookmarks.value.find { it.word == word && it.dictionaryName == dictionaryName }
            if (existing != null) {
                bookmarkRepo.removeBookmark(existing)
            } else {
                bookmarkRepo.addBookmark(word, definition, dictionaryName)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Bookmark operation failed: ${e.message}"
        }
    }

    fun addBookmark(word: String, definition: String = "", dictionaryName: String = "") {
        bookmarkRepo.addBookmark(word, definition, dictionaryName)
    }

    fun removeBookmark(item: BookmarkItem) {
        bookmarkRepo.removeBookmark(item)
    }
}
