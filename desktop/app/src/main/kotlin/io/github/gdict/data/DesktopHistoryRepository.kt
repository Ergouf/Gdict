package io.github.gdict.data

import io.github.gdict.core.model.HistoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DesktopHistoryRepository(private val storage: StorageBackend) : HistoryRepository {

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    override val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    @Volatile private var diskLoaded = false
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Loads search history from disk on a background dispatcher. StateFlow
     * starts empty so the UI can render instantly. Safe to call multiple times.
     */
    fun loadAsync(scope: CoroutineScope) {
        if (diskLoaded) return
        diskLoaded = true
        scope.launch(Dispatchers.IO) {
            _history.value = loadHistory()
        }
    }

    private fun loadHistory(): List<HistoryItem> {
        val json = storage.getString("history") ?: return emptyList()
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

    private fun saveHistory() {
        val arr = JSONArray()
        for (item in _history.value) {
            val obj = JSONObject().apply {
                put("word", item.word)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        storage.putString("history", arr.toString())
    }

    override fun addToHistory(word: String) {
        val now = System.currentTimeMillis()
        val item = HistoryItem(word = word, timestamp = now)
        _history.value = (listOf(item) + _history.value.filter { it.word != word }).take(500)
        saveHistoryAsync()
    }

    override fun removeFromHistory(item: HistoryItem) {
        _history.value = _history.value.filter { it.word != item.word }
        saveHistoryAsync()
    }

    override fun clearHistory() {
        _history.value = emptyList()
        saveHistoryAsync()
    }

    private fun saveHistoryAsync() {
        scope.launch { saveHistory() }
    }
}