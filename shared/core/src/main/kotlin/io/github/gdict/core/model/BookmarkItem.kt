package io.github.gdict.core.model

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
