package io.github.gdict.data

import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictionaryManager
import io.github.gdict.core.GdictLogger
import io.github.gdict.core.model.Dictionary
import io.github.gdict.core.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

class DesktopDictionaryRepository(
    private val dictionaryManager: DictionaryManager
) : DictionaryRepository {

    private val _dictionaries = MutableStateFlow<List<Dictionary>>(emptyList())
    override val dictionaries: StateFlow<List<Dictionary>> = _dictionaries.asStateFlow()

    private val log = GdictLogger.get()

    // LRU cache for searchWord(word) results. Bounded at [searchCacheCapacity]
    // so memory stays flat even with broad queries. LinkedHashMap is thread-safe
    // for reads when we guard mutation with [cacheLock].
    private val searchCacheLock = Any()
    private val searchCache = LinkedHashMap<String, List<SearchResultItem>>(
        SEARCH_CACHE_CAPACITY, 0.75f, true
    )
    private val suggestionCache = LinkedHashMap<String, List<String>>(
        SEARCH_CACHE_CAPACITY, 0.75f, true
    )

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
        // Dictionary set changed, drop stale entries to avoid serving results
        // from a now-disabled or removed dict.
        invalidateSearchCache()
    }

    private fun evictIfNeeded(cache: LinkedHashMap<String, *>) {
        if (cache.size > SEARCH_CACHE_CAPACITY) {
            val overflow = cache.size - SEARCH_CACHE_CAPACITY
            val keys = cache.keys.iterator()
            var removed = 0
            while (keys.hasNext() && removed < overflow) {
                keys.next()
                keys.remove()
                removed++
            }
        }
    }

    override suspend fun searchWord(word: String): List<SearchResultItem> {
        val normalized = word.trim()
        if (normalized.isEmpty()) return emptyList()
        // Fast path: cache hit.
        synchronized(searchCacheLock) {
            searchCache[normalized]?.let { cached ->
                log.i("SearchCache", "hit word='$normalized' results=${cached.size}")
                return cached
            }
        }
        val started = System.nanoTime()
        val results = withContext(Dispatchers.IO) {
            dictionaryManager.searchWord(normalized).map { result ->
                SearchResultItem(
                    word = result.word,
                    definition = result.definition,
                    dictionaryName = result.dictionaryName,
                    css = result.css
                )
            }
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        log.i("SearchCache", "miss word='$normalized' took ${elapsedMs}ms, results=${results.size}")
        synchronized(searchCacheLock) {
            searchCache[normalized] = results
            evictIfNeeded(searchCache)
        }
        return results
    }

    override fun scanDirectory(path: String): List<DictFileImporter.DictCandidate> {
        return dictionaryManager.scanDirectory(path)
    }

    override suspend fun addDictionary(name: String, path: String, companionFiles: List<String>) = withContext(Dispatchers.IO) {
        val entry = dictionaryManager.addOrUpdateDictionary(name, path, companionFiles)
        val newDict = Dictionary(id = entry.id, name = entry.name, path = entry.path)
        _dictionaries.value = _dictionaries.value + newDict
        invalidateSearchCache()
    }

    override fun removeDictionary(dictionary: Dictionary) {
        dictionaryManager.removeDictionary(dictionary.id)
        _dictionaries.value = _dictionaries.value.filter { it.id != dictionary.id }
        invalidateSearchCache()
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
        invalidateSearchCache()
    }

    private fun invalidateSearchCache() {
        synchronized(searchCacheLock) {
            searchCache.clear()
            suggestionCache.clear()
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

    override suspend fun searchSuggestions(prefix: String, limit: Int): List<String> {
        val normalized = prefix.trim()
        if (normalized.isEmpty()) return emptyList()
        val key = "$normalized|$limit"
        synchronized(searchCacheLock) {
            suggestionCache[key]?.let { return it }
        }
        val results = withContext(Dispatchers.IO) {
            dictionaryManager.searchSuggestions(normalized, limit)
        }
        synchronized(searchCacheLock) {
            suggestionCache[key] = results
            evictIfNeeded(suggestionCache)
        }
        return results
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

    override fun extractExamples(definition: String): List<String> {
        return dictionaryManager.extractExamples(definition)
    }

    override fun extractSynonyms(definition: String): List<String> {
        return dictionaryManager.extractSynonyms(definition)
    }

    companion object {
        // Bounded LRU size. Each entry is a small (key, list) pair; with 500
        // entries of ~10 results each this is well under a few MB.
        private const val SEARCH_CACHE_CAPACITY = 500
    }
}