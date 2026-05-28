package io.github.gdict.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gdict.GdictApplication
import io.github.gdict.R
import io.github.gdict.core.DictFileImporter
import io.github.gdict.data.AndroidDictionaryRepository
import io.github.gdict.core.model.Dictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dictionaryRepo: AndroidDictionaryRepository = (application as GdictApplication).dictionaryRepository

    val dictionaries: StateFlow<List<Dictionary>> = dictionaryRepo.dictionaries
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<String?>(null)
    val diagnosticResult: StateFlow<String?> = _diagnosticResult.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearDiagnosticResult() {
        _diagnosticResult.value = null
    }

    fun scanDirectory(uri: Uri): List<DictFileImporter.DictCandidate> {
        return try {
            dictionaryRepo.scanSafDirectory(uri)
        } catch (e: Exception) {
            _errorMessage.value = getApplication<GdictApplication>().getString(R.string.scan_dir_failed, e.message)
            emptyList()
        }
    }

    fun addDictionary(name: String, path: String, companionFiles: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                dictionaryRepo.addDictionary(name, path, companionFiles)
            } catch (e: Throwable) {
                _errorMessage.value = getApplication<GdictApplication>().getString(R.string.add_dict_failed, "${e.javaClass.simpleName} - ${e.message}")
                android.util.Log.e("VM", "addDictionary failed", e)
            }
        }
    }

    fun batchImport(candidates: List<DictFileImporter.DictCandidate>, onComplete: () -> Unit) {
        viewModelScope.launch {
            _importing.value = true
            var successCount = 0
            var failCount = 0
            try {
                withContext(Dispatchers.IO) {
                    for (candidate in candidates) {
                        try {
                            dictionaryRepo.addDictionary(candidate.name, candidate.filePath, candidate.companionFiles)
                            successCount++
                            android.util.Log.i("VM", "Imported OK: ${candidate.name}")
                        } catch (e: Throwable) {
                            failCount++
                            val errType = e.javaClass.simpleName
                            val errMsg = e.message ?: "(no message)"
                            android.util.Log.e("VM", "Import failed for ${candidate.name}: $errType - $errMsg", e)
                            _errorMessage.value = getApplication<GdictApplication>().getString(R.string.import_dict_failed, candidate.name, "$errType - $errMsg")
                        }
                    }
                }
                if (failCount > 0) {
                    _errorMessage.value = getApplication<GdictApplication>().getString(R.string.import_result, successCount, failCount)
                }
            } catch (e: Throwable) {
                _errorMessage.value = getApplication<GdictApplication>().getString(R.string.import_exception, "${e.javaClass.simpleName} - ${e.message}")
                android.util.Log.e("VM", "batchImport crashed", e)
            } finally {
                _importing.value = false
                onComplete()
            }
        }
    }

    fun removeDictionary(dictionary: Dictionary) {
        try {
            dictionaryRepo.removeDictionary(dictionary)
        } catch (e: Exception) {
            _errorMessage.value = getApplication<GdictApplication>().getString(R.string.remove_dict_failed, e.message)
        }
    }

    fun toggleDictionary(dictionary: Dictionary) {
        try {
            dictionaryRepo.toggleDictionary(dictionary)
        } catch (e: Exception) {
            _errorMessage.value = getApplication<GdictApplication>().getString(R.string.toggle_dict_failed, e.message)
        }
    }

    fun diagnoseDictionaries() {
        viewModelScope.launch {
            _diagnosticResult.value = withContext(Dispatchers.IO) {
                dictionaryRepo.diagnoseDictionaries() + "\n\n" + dictionaryRepo.testMddResourcesAndHtml()
            }
        }
    }
}
