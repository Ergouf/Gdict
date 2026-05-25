package io.github.gdict.core.model

data class HistoryItem(
    val word: String,
    val timestamp: Long = System.currentTimeMillis()
)
