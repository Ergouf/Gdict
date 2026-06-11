package io.github.gdict.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class JsonFileStorageBackend(dataDir: File) : StorageBackend {

    private val storageFile = File(dataDir, "storage.json")
    private val data = JSONObject()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var pendingWriteJob: Job? = null
    @Volatile private var dirty: Boolean = false
    private val writeLock = Any()
    private val debounceMillis: Long = 80

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

    /**
     * Coalesces in-memory mutations and flushes the storage file at most
     * once per [debounceMillis] window. Writes go to a sibling .tmp file
     * and are atomically renamed in place, so a crash mid-write can never
     * leave a truncated storage.json on disk.
     */
    private fun requestSave() {
        synchronized(writeLock) {
            dirty = true
            val existing = pendingWriteJob
            if (existing != null && existing.isActive) return
            pendingWriteJob = ioScope.launch {
                delay(debounceMillis)
                flushNow()
            }
        }
    }

    private fun flushNow() {
        val snapshot: String
        synchronized(writeLock) {
            if (!dirty) return
            snapshot = data.toString()
            dirty = false
        }
        val parent = storageFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmp = File(parent, "${storageFile.name}.tmp")
        try {
            tmp.writeText(snapshot, Charsets.UTF_8)
            if (storageFile.exists() && !storageFile.delete()) {
                // On Windows, renameTo can fail if the target exists, so delete first.
                storageFile.delete()
            }
            if (!tmp.renameTo(storageFile)) {
                // Fallback: copy + delete tmp.
                storageFile.writeText(snapshot, Charsets.UTF_8)
                tmp.delete()
            }
        } catch (e: Throwable) {
            // Best-effort; ensure tmp is cleaned up to avoid stale leftovers.
            try { tmp.delete() } catch (_: Throwable) {}
        }
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
        requestSave()
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
        requestSave()
    }

    override fun remove(key: String) {
        data.remove(key)
        requestSave()
    }

    /**
     * Forces any pending debounced write to disk immediately. Call before
     * shutdown or tests that need the file to be consistent with memory.
     */
    fun flush() {
        flushNow()
    }
}