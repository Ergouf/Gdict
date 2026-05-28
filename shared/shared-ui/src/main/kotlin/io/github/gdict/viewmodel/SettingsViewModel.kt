package io.github.gdict.viewmodel

import io.github.gdict.data.BookmarkRepository
import io.github.gdict.data.HistoryRepository
import io.github.gdict.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val historyRepo: HistoryRepository,
    private val bookmarkRepo: BookmarkRepository
) {
    val darkMode: StateFlow<Boolean> = settingsRepo.darkMode

    val scanPopup: StateFlow<Boolean> = settingsRepo.scanPopup

    val language: StateFlow<String> = settingsRepo.language

    val cardScale: StateFlow<Float> = settingsRepo.cardScale

    val detailZoom: StateFlow<Float> = settingsRepo.detailZoom

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun setDarkMode(enabled: Boolean) {
        settingsRepo.setDarkMode(enabled)
    }

    fun setScanPopup(enabled: Boolean) {
        settingsRepo.setScanPopup(enabled)
    }

    fun setLanguage(tag: String) {
        settingsRepo.setLanguage(tag)
    }

    fun setCardScale(scale: Float) {
        settingsRepo.setCardScale(scale)
    }

    fun setDetailZoom(zoom: Float) {
        settingsRepo.setDetailZoom(zoom)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearAllData() {
        try {
            historyRepo.clearHistory()
            bookmarkRepo.clearBookmarks()
        } catch (e: Exception) {
            _errorMessage.value = "Clear data failed: ${e.message}"
        }
    }
}
