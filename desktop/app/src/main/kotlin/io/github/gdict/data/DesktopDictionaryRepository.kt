package io.github.gdict.data

import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictionaryManager
import io.github.gdict.core.model.Dictionary
import io.github.gdict.core.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DesktopDictionaryRepository(
    private val dictionaryManager: DictionaryManager
) : DictionaryRepository {

    private val _dictionaries = MutableStateFlow<List<Dictionary>>(emptyList())
    override val dictionaries: StateFlow<List<Dictionary>> = _dictionaries.asStateFlow()

    init {
        refreshDictionaries()
    }

    private fun refreshDictionaries() {
        _dictionaries.value = dictionaryManager.getDictionaries().map { entry ->
            Dictionary(
                id = entry.id,
                name = entry.name,
                path = entry.path,
                isEnabled = entry.isEnabled
            )
        }
    }

    override suspend fun searchWord(word: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        dictionaryManager.searchWord(word).map { result ->
            SearchResultItem(
                word = result.word,
                definition = result.definition,
                dictionaryName = result.dictionaryName,
                css = result.css
            )
        }
    }

    override fun scanDirectory(path: String): List<DictFileImporter.DictCandidate> {
        return dictionaryManager.scanDirectory(path)
    }

    override suspend fun addDictionary(name: String, path: String, companionFiles: List<String>) = withContext(Dispatchers.IO) {
        val entry = dictionaryManager.addOrUpdateDictionary(name, path, companionFiles)
        val newDict = Dictionary(id = entry.id, name = entry.name, path = entry.path)
        _dictionaries.value = _dictionaries.value + newDict
    }

    override fun removeDictionary(dictionary: Dictionary) {
        dictionaryManager.removeDictionary(dictionary.id)
        _dictionaries.value = _dictionaries.value.filter { it.id != dictionary.id }
    }

    override fun diagnoseDictionaries(): String {
        return dictionaryManager.diagnoseAllDictionaries()
    }

    override fun testMddResourcesAndHtml(): String {
        return dictionaryManager.testMddResourcesAndHtml()
    }

    override fun toggleDictionary(dictionary: Dictionary) {
        dictionaryManager.toggleDictionary(dictionary.id, !dictionary.isEnabled)
        _dictionaries.value = _dictionaries.value.map {
            if (it.id == dictionary.id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    override fun getCssForDictionary(dictionaryName: String): String {
        val dict = dictionaryManager.getDictionaries().find { it.name == dictionaryName }
        if (dict == null) return ""
        val parser = dictionaryManager.getParserForDictionary(dict.id)
        val fileCss = parser?.companionCss ?: ""
        val mddCss = dictionaryManager.getCssFromMdd(dict.id)
        return fileCss + mddCss
    }

    override suspend fun searchSuggestions(prefix: String, limit: Int): List<String> = withContext(Dispatchers.IO) {
        dictionaryManager.searchSuggestions(prefix, limit)
    }

    override suspend fun getRandomWords(count: Int): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        dictionaryManager.getRandomWords(count)
    }

    override suspend fun getAudioResource(word: String): ByteArray? = withContext(Dispatchers.IO) {
        dictionaryManager.getAudioResource(word)
    }

    override suspend fun getAudioResourceByPath(path: String): ByteArray? = withContext(Dispatchers.IO) {
        dictionaryManager.getAudioResourceByPath(path)
    }

    override fun getAudioResourceByPathSync(path: String): ByteArray? {
        return dictionaryManager.getAudioResourceByPath(path)
    }
}