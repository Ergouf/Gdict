package io.github.gdict.viewmodel

import io.github.gdict.data.BookmarkRepository
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.core.model.ReviewStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlashcardViewModel(
    private val bookmarkRepo: BookmarkRepository
) {
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

    val isSessionComplete: Boolean
        get() = _currentCardIndex.value >= _dueBookmarks.value.size && _dueBookmarks.value.isNotEmpty()

    val currentReviewItem: BookmarkItem?
        get() {
            val items = _dueBookmarks.value
            val index = _currentCardIndex.value
            return if (index < items.size) items[index] else null
        }

    fun startReviewSession() {
        val due = bookmarkRepo.getDueBookmarks()
        val new = bookmarkRepo.getNewBookmarks()
        val combined = (due + new).distinctBy { it.id }
        _dueBookmarks.value = combined
        _currentCardIndex.value = 0
        _sessionReviewed.value = 0
        if (combined.isNotEmpty()) {
            _currentScheduling.value = bookmarkRepo.getSchedulingForBookmark(combined[0])
        }
        refreshReviewStats()
    }

    fun rateCurrentCard(rating: Rating) {
        val items = _dueBookmarks.value
        val index = _currentCardIndex.value
        if (index >= items.size) return

        val item = items[index]
        val scheduling = _currentScheduling.value[rating] ?: return

        bookmarkRepo.applyReview(item, scheduling)
        _sessionReviewed.value = _sessionReviewed.value + 1

        val nextIndex = index + 1
        if (nextIndex < items.size) {
            _currentCardIndex.value = nextIndex
            _currentScheduling.value = bookmarkRepo.getSchedulingForBookmark(items[nextIndex])
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
            _currentScheduling.value = bookmarkRepo.getSchedulingForBookmark(items[nextIndex])
        } else {
            _currentCardIndex.value = items.size
            _currentScheduling.value = emptyMap()
        }
    }

    fun refreshReviewStats() {
        _reviewStats.value = bookmarkRepo.getReviewStats()
    }
}
