package io.github.gdict.data

import android.content.Context
import android.net.Uri
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictionaryManager
import io.github.gdict.core.model.Dictionary
import io.github.gdict.core.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DictionaryRepository(private val context: Context) {
    private val dictionaryManager = DictionaryManager(context)

    private val _dictionaries = MutableStateFlow<List<Dictionary>>(emptyList())
    val dictionaries: StateFlow<List<Dictionary>> = _dictionaries.asStateFlow()

    init {
        _dictionaries.value = dictionaryManager.getDictionaries().map { entry ->
            Dictionary(
                id = entry.id,
                name = entry.name,
                path = entry.path,
                isEnabled = entry.isEnabled
            )
        }
    }

    suspend fun searchWord(word: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        dictionaryManager.searchWord(word).map { result ->
            SearchResultItem(
                word = result.word,
                definition = result.definition,
                dictionaryName = result.dictionaryName,
                css = result.css
            )
        }
    }

    fun scanDirectory(uri: Uri): List<DictFileImporter.DictCandidate> {
        return dictionaryManager.scanDirectory(uri)
    }

    suspend fun addDictionary(name: String, path: String, companionFiles: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        val entry = dictionaryManager.addOrUpdateDictionary(name, path, companionFiles)
        val newDict = Dictionary(
            id = entry.id,
            name = entry.name,
            path = entry.path
        )
        _dictionaries.value = _dictionaries.value + newDict
    }

    fun removeDictionary(dictionary: Dictionary) {
        dictionaryManager.removeDictionary(dictionary.id)
        _dictionaries.value = _dictionaries.value.filter { it.id != dictionary.id }
    }

    fun diagnoseDictionaries(): String {
        return dictionaryManager.diagnoseAllDictionaries()
    }

    fun testMddResourcesAndHtml(): String {
        return dictionaryManager.testMddResourcesAndHtml()
    }

    fun toggleDictionary(dictionary: Dictionary) {
        dictionaryManager.toggleDictionary(dictionary.id, !dictionary.isEnabled)
        _dictionaries.value = _dictionaries.value.map {
            if (it.id == dictionary.id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun getCssForDictionary(dictionaryName: String): String {
        val dict = dictionaryManager.getDictionaries().find { it.name == dictionaryName }
        if (dict == null) return ""
        val parser = dictionaryManager.getParserForDictionary(dict.id)
        val fileCss = parser?.companionCss ?: ""
        val mddCss = dictionaryManager.getCssFromMdd(dict.id)
        return fileCss + mddCss
    }

    suspend fun searchSuggestions(prefix: String, limit: Int = 10): List<String> = withContext(Dispatchers.IO) {
        dictionaryManager.searchSuggestions(prefix, limit)
    }

    suspend fun getRandomWords(count: Int = 5): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        dictionaryManager.getRandomWords(count)
    }

    suspend fun getAudioResource(word: String): ByteArray? = withContext(Dispatchers.IO) {
        dictionaryManager.getAudioResource(word)
    }

    suspend fun getAudioResourceByPath(path: String): ByteArray? = withContext(Dispatchers.IO) {
        dictionaryManager.getAudioResourceByPath(path)
    }

    fun getAudioResourceByPathSync(path: String): ByteArray? {
        return dictionaryManager.getAudioResourceByPath(path)
    }
}
