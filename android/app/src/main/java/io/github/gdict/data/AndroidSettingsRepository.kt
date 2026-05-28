package io.github.gdict.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidSettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("gdict_data", Context.MODE_PRIVATE)

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _scanPopup = MutableStateFlow(prefs.getBoolean("scan_popup", false))
    val scanPopup: StateFlow<Boolean> = _scanPopup.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "") ?: "")
    val language: StateFlow<String> = _language.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setScanPopup(enabled: Boolean) {
        _scanPopup.value = enabled
        prefs.edit().putBoolean("scan_popup", enabled).apply()
    }

    fun setLanguage(tag: String) {
        _language.value = tag
        prefs.edit().putString("language", tag).apply()
    }
}
