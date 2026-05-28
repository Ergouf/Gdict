package io.github.gdict.data

import io.github.gdict.core.model.HistoryItem
import kotlinx.coroutines.flow.StateFlow

interface HistoryRepository {
    val history: StateFlow<List<HistoryItem>>
    fun addToHistory(word: String)
    fun removeFromHistory(item: HistoryItem)
    fun clearHistory()
}
