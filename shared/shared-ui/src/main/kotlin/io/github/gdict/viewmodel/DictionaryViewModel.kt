package io.github.gdict.viewmodel

import io.github.gdict.core.DictFileImporter
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.core.model.Dictionary
import io.github.gdict.core.GdictLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionaryViewModel(
    private val dictionaryRepo: DictionaryRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val logger = GdictLogger.get()

    val dictionaries: StateFlow<List<Dictionary>> = dictionaryRepo.dictionaries
        .stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

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

    fun scanDirectory(dirPath: String): List<DictFileImporter.DictCandidate> {
        return try {
            dictionaryRepo.scanDirectory(dirPath)
        } catch (e: Exception) {
            _errorMessage.value = "Scan directory failed: ${e.message}"
            emptyList()
        }
    }

    fun addDictionary(name: String, path: String, companionFiles: List<String> = emptyList()) {
        coroutineScope.launch {
            try {
                dictionaryRepo.addDictionary(name, path, companionFiles)
            } catch (e: Throwable) {
                _errorMessage.value = "Add dictionary failed: ${e.javaClass.simpleName} - ${e.message}"
                logger.e("VM", "addDictionary failed", e)
            }
        }
    }

    fun batchImport(candidates: List<DictFileImporter.DictCandidate>, onComplete: () -> Unit) {
        coroutineScope.launch {
            _importing.value = true
            var successCount = 0
            var failCount = 0
            try {
                withContext(Dispatchers.IO) {
                    for (candidate in candidates) {
                        try {
                            dictionaryRepo.addDictionary(candidate.name, candidate.filePath, candidate.companionFiles)
                            successCount++
                            logger.i("VM", "Imported OK: ${candidate.name}")
                        } catch (e: Throwable) {
                            failCount++
                            val errType = e.javaClass.simpleName
                            val errMsg = e.message ?: "(no message)"
                            logger.e("VM", "Import failed for ${candidate.name}: $errType - $errMsg", e)
                            _errorMessage.value = "Import failed for ${candidate.name}: $errType - $errMsg"
                        }
                    }
                }
                if (failCount > 0) {
                    _errorMessage.value = "Import result: $successCount succeeded, $failCount failed"
                }
            } catch (e: Throwable) {
                _errorMessage.value = "Import exception: ${e.javaClass.simpleName} - ${e.message}"
                logger.e("VM", "batchImport crashed", e)
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
            _errorMessage.value = "Remove dictionary failed: ${e.message}"
        }
    }

    fun toggleDictionary(dictionary: Dictionary) {
        try {
            dictionaryRepo.toggleDictionary(dictionary)
        } catch (e: Exception) {
            _errorMessage.value = "Toggle dictionary failed: ${e.message}"
        }
    }

    fun diagnoseDictionaries() {
        coroutineScope.launch {
            _diagnosticResult.value = withContext(Dispatchers.IO) {
                dictionaryRepo.diagnoseDictionaries() + "\n\n" + dictionaryRepo.testMddResourcesAndHtml()
            }
        }
    }
}
