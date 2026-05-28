package io.github.gdict.data

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val darkMode: StateFlow<Boolean>
    val scanPopup: StateFlow<Boolean>
    val language: StateFlow<String>
    val cardScale: StateFlow<Float>
    val detailZoom: StateFlow<Float>
    fun setDarkMode(enabled: Boolean)
    fun setScanPopup(enabled: Boolean)
    fun setLanguage(tag: String)
    fun setCardScale(scale: Float)
    fun setDetailZoom(zoom: Float)
}
