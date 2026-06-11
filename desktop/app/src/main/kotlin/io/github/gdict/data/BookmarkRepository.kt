package io.github.gdict.data

import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.core.model.ReviewStats
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import kotlinx.coroutines.flow.StateFlow

interface BookmarkRepository {
    val bookmarks: StateFlow<List<BookmarkItem>>
    val bookmarksByWord: StateFlow<Map<String, BookmarkItem>>
    fun addBookmark(word: String, definition: String, dictionaryName: String = "")
    fun removeBookmark(item: BookmarkItem)
    fun clearBookmarks()
    fun getDueBookmarks(): List<BookmarkItem>
    fun getNewBookmarks(): List<BookmarkItem>
    fun getReviewStats(): ReviewStats
    fun getSchedulingForBookmark(item: BookmarkItem): Map<Rating, SchedulingCard>
    fun applyReview(item: BookmarkItem, card: SchedulingCard)
}
