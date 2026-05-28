package io.github.gdict.data

import android.content.Context
import io.github.gdict.core.model.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class AndroidHistoryRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("gdict_data", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    init {
        _history.value = loadHistory()
    }

    fun addToHistory(word: String) {
        val item = HistoryItem(word = word)
        _history.value = listOf(item) + _history.value.filter { it.word != word }
        saveHistory(_history.value)
    }

    fun removeFromHistory(item: HistoryItem) {
        _history.value = _history.value.filter { it.word != item.word }
        saveHistory(_history.value)
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory(emptyList())
    }

    private fun saveHistory(items: List<HistoryItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("word", item.word)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    private fun loadHistory(): List<HistoryItem> {
        val json = prefs.getString("history", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryItem(
                    word = obj.getString("word"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
