package io.github.gdict.core

import io.github.gdict.core.GdictLogger.Companion.get as log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

interface PersistenceBackend {
    fun loadDictionaries(): String?
    fun saveDictionaries(json: String)
}

class DictPersistence(private val dataDir: File, private val backend: PersistenceBackend) {

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
        backend.saveDictionaries(json.toString())
    }

    fun loadPersistedDictionaries(): List<DictionaryManager.DictEntry> {
        val jsonStr = backend.loadDictionaries() ?: return emptyList()
        val result = mutableListOf<DictionaryManager.DictEntry>()
        try {
            val json = JSONArray(jsonStr)
            log().i("DictPersistence", "Loading ${json.length()} persisted dictionaries...")
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
                log().i("DictPersistence", "  Persisted[$i]: '${entry.name}' (id=${entry.id}) file=${mdxFile.name} exists=${mdxFile.exists()} size=${mdxFile.length()} mdxTitle='$mdxTitle'")

                val dictDir = File(dataDir, "dictionaries/${entry.id}")
                if (!dictDir.exists()) {
                    log().w("DictPersistence", "  Skipping '${entry.name}': directory not found at ${dictDir.absolutePath}")
                    continue
                }
                if (!mdxFile.exists() || mdxFile.length() == 0L) {
                    log().w("DictPersistence", "  Skipping '${entry.name}': MDX file not found or empty at ${entry.dictFilePath}")
                    continue
                }
                result.add(entry)
            }
            log().i("DictPersistence", "Loaded ${result.size}/${json.length()} persisted dictionaries")
        } catch (e: Exception) {
            log().e("DictPersistence", "loadPersistedDictionaries FAILED: ${e.message}")
        }
        return result
    }
}
