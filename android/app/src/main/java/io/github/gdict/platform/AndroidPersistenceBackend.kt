package io.github.gdict.platform

import android.content.Context
import io.github.gdict.core.PersistenceBackend

class AndroidPersistenceBackend(private val context: Context) : PersistenceBackend {

    companion object {
        private const val PREFS_NAME = "gdict_persistence"
        private const val KEY_DICTIONARIES = "dictionaries_json"
    }

    override fun loadDictionaries(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DICTIONARIES, null)
    }

    override fun saveDictionaries(json: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DICTIONARIES, json)
            .apply()
    }
}
