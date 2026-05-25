package io.github.gdict.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.gdict.GdictApplication
import io.github.gdict.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo: SettingsRepository = (application as GdictApplication).settingsRepository

    val darkMode: StateFlow<Boolean> = settingsRepo.darkMode

    val scanPopup: StateFlow<Boolean> = settingsRepo.scanPopup

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun setDarkMode(enabled: Boolean) {
        settingsRepo.setDarkMode(enabled)
    }

    fun setScanPopup(enabled: Boolean) {
        settingsRepo.setScanPopup(enabled)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearAllData() {
        try {
            val app = getApplication<GdictApplication>()
            app.historyRepository.clearHistory()
            app.bookmarkRepository.clearBookmarks()
        } catch (e: Exception) {
            _errorMessage.value = "清除数据失败: ${e.message}"
        }
    }
}
