package io.github.gdict.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DesktopSettingsRepository(private val storage: StorageBackend) : SettingsRepository {

    private val _darkMode = MutableStateFlow(storage.getBoolean("darkMode", false))
    override val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _scanPopup = MutableStateFlow(storage.getBoolean("scanPopup", false))
    override val scanPopup: StateFlow<Boolean> = _scanPopup.asStateFlow()

    private val _language = MutableStateFlow(storage.getString("language") ?: "")
    override val language: StateFlow<String> = _language.asStateFlow()

    private val _cardScale = MutableStateFlow(storage.getString("cardScale")?.toFloatOrNull() ?: 1.0f)
    override val cardScale: StateFlow<Float> = _cardScale.asStateFlow()

    private val _detailZoom = MutableStateFlow(storage.getString("detailZoom")?.toFloatOrNull() ?: 1.0f)
    override val detailZoom: StateFlow<Float> = _detailZoom.asStateFlow()

    override fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        storage.putBoolean("darkMode", enabled)
    }

    override fun setScanPopup(enabled: Boolean) {
        _scanPopup.value = enabled
        storage.putBoolean("scanPopup", enabled)
    }

    override fun setLanguage(tag: String) {
        _language.value = tag
        storage.putString("language", tag)
    }

    override fun setCardScale(scale: Float) {
        _cardScale.value = scale
        storage.putString("cardScale", scale.toString())
    }

    override fun setDetailZoom(zoom: Float) {
        _detailZoom.value = zoom
        storage.putString("detailZoom", zoom.toString())
    }
}