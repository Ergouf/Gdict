package io.github.gdict.data

import org.json.JSONObject
import java.io.File

class JsonFileStorageBackend(dataDir: File) : StorageBackend {

    private val storageFile = File(dataDir, "storage.json")
    private val data = JSONObject()

    init {
        if (storageFile.exists()) {
            try {
                val json = JSONObject(storageFile.readText(Charsets.UTF_8))
                for (key in json.keys()) {
                    data.put(key, json.get(key))
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun save() {
        storageFile.writeText(data.toString(), Charsets.UTF_8)
    }

    override fun getString(key: String): String? {
        return if (data.has(key) && !data.isNull(key)) data.getString(key) else null
    }

    override fun putString(key: String, value: String) {
        data.put(key, value)
        save()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return if (data.has(key) && !data.isNull(key)) data.optBoolean(key, default) else default
    }

    override fun putBoolean(key: String, value: Boolean) {
        data.put(key, value)
        save()
    }

    override fun remove(key: String) {
        data.remove(key)
        save()
    }
}