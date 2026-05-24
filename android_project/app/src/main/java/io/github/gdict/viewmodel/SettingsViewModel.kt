package io.github.gdict.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gdict.GdictApplication
import io.github.gdict.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as GdictApplication).repository

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _scanPopup = MutableStateFlow(false)
    val scanPopup: StateFlow<Boolean> = _scanPopup.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
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
