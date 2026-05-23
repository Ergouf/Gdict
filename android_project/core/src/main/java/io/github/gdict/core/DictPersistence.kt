package io.github.gdict.core

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DictPersistence(private val context: Context) {

    private val prefs = context.getSharedPreferences("dict_manager", Context.MODE_PRIVATE)
    private val idCounter = AtomicLong(1)

    fun nextId(existingIds: Set<Long>): Long {
        var candidate: Long
        do {
            candidate = System.currentTimeMillis() * 1000 + idCounter.getAndIncrement()
        } while (candidate in existingIds)
        return candidate
    }

    fun saveDictionaries(dictionaries: List<DictionaryManager.DictEntry>) {
        val json = JSONArray()
        for (dict in dictionaries) {
            val obj = JSONObject().apply {
                put("id", dict.id)
                put("name", dict.name)
                put("path", dict.path)
                put("dictFilePath", dict.dictFilePath)
                put("isEnabled", dict.isEnabled)
            }
            json.put(obj)
        }
        prefs.edit().putString("dictionaries", json.toString()).apply()
    }

    fun loadPersistedDictionaries(): List<DictionaryManager.DictEntry> {
        val jsonStr = prefs.getString("dictionaries", null) ?: return emptyList()
        val result = mutableListOf<DictionaryManager.DictEntry>()
        try {
            val json = JSONArray(jsonStr)
            Log.i("DictPersistence", "Loading ${json.length()} persisted dictionaries from SharedPreferences...")
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val entry = DictionaryManager.DictEntry(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    path = obj.getString("path"),
                    dictFilePath = obj.getString("dictFilePath"),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                val mdxFile = File(entry.dictFilePath)
                val mdxTitle = DictFileImporter.readMdxHeaderTitleStatic(mdxFile)
                Log.i("DictPersistence", "  Persisted[$i]: '${entry.name}' (id=${entry.id}) file=${mdxFile.name} exists=${mdxFile.exists()} size=${mdxFile.length()} mdxTitle='$mdxTitle'")

                val dictDir = File(context.filesDir, "dictionaries/${entry.id}")
                if (!dictDir.exists()) {
                    Log.w("DictPersistence", "  Skipping '${entry.name}': directory not found at ${dictDir.absolutePath}")
                    continue
                }
                if (!mdxFile.exists() || mdxFile.length() == 0L) {
                    Log.w("DictPersistence", "  Skipping '${entry.name}': MDX file not found or empty at ${entry.dictFilePath}")
                    continue
                }
                result.add(entry)
            }
            Log.i("DictPersistence", "Loaded ${result.size}/${json.length()} persisted dictionaries")
        } catch (e: Exception) {
            Log.e("DictPersistence", "loadPersistedDictionaries FAILED: ${e.message}")
        }
        return result
    }
}
