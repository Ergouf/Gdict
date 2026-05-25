package io.github.gdict.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.gdict.GdictApplication
import io.github.gdict.data.AppRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as GdictApplication).repository

    val darkMode: StateFlow<Boolean> = repository.darkMode

    private val _scanPopup = kotlinx.coroutines.flow.MutableStateFlow(false)
    val scanPopup: kotlinx.coroutines.flow.StateFlow<Boolean> = _scanPopup

    private val _errorMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val errorMessage: kotlinx.coroutines.flow.StateFlow<String?> = _errorMessage

    fun setDarkMode(enabled: Boolean) {
        repository.setDarkMode(enabled)
    }

    fun setScanPopup(enabled: Boolean) {
        _scanPopup.value = enabled
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearAllData() {
        try {
            repository.clearHistory()
            repository.clearBookmarks()
        } catch (e: Exception) {
            _errorMessage.value = "清除数据失败: ${e.message}"
        }
    }

    suspend fun getAudioResource(word: String): ByteArray? {
        return repository.getAudioResource(word)
    }

    suspend fun getAudioResourceByPath(path: String): ByteArray? {
        return repository.getAudioResourceByPath(path)
    }

    fun getResourceByPathSync(path: String): ByteArray? {
        return repository.getResourceByPathSync(path)
    }
}
