package io.github.gdict.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gdict.GdictApplication
import io.github.gdict.data.BookmarkRepository
import io.github.gdict.core.model.BookmarkItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val bookmarkRepo: BookmarkRepository = (application as GdictApplication).bookmarkRepository

    val bookmarks: StateFlow<List<BookmarkItem>> = bookmarkRepo.bookmarks
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
            _errorMessage.value = "收藏操作失败: ${e.message}"
        }
    }

    fun addBookmark(word: String, definition: String = "", dictionaryName: String = "") {
        bookmarkRepo.addBookmark(word, definition, dictionaryName)
    }

    fun removeBookmark(item: BookmarkItem) {
        bookmarkRepo.removeBookmark(item)
    }
}
