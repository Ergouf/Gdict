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
                    try {
                        data.put(key, json.get(key))
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun save() {
        storageFile.writeText(data.toString(), Charsets.UTF_8)
    }

    override fun getString(key: String): String? {
        if (!data.has(key) || data.isNull(key)) return null
        return try {
            val value = data.get(key)
            when (value) {
                is String -> value
                else -> value.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun putString(key: String, value: String) {
        data.put(key, value)
        save()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        if (!data.has(key) || data.isNull(key)) return default
        return try {
            data.optBoolean(key, default)
        } catch (_: Exception) {
            default
        }
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