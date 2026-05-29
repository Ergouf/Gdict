package io.github.gdict.data

import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.model.Dictionary
import io.github.gdict.core.model.SearchResultItem
import kotlinx.coroutines.flow.StateFlow

interface DictionaryRepository {
    val dictionaries: StateFlow<List<Dictionary>>
    suspend fun searchWord(word: String): List<SearchResultItem>
    fun scanDirectory(path: String): List<DictFileImporter.DictCandidate>
    suspend fun addDictionary(name: String, path: String, companionFiles: List<String> = emptyList())
    fun removeDictionary(dictionary: Dictionary)
    fun diagnoseDictionaries(): String
    fun testMddResourcesAndHtml(): String
    fun toggleDictionary(dictionary: Dictionary)
    fun getCssForDictionary(dictionaryName: String): String
    suspend fun searchSuggestions(prefix: String, limit: Int = 10): List<String>
    suspend fun getRandomWords(count: Int = 5): List<Pair<String, String>>
    suspend fun getAudioResource(word: String): ByteArray?
    suspend fun getAudioResourceByPath(path: String): ByteArray?
    fun getAudioResourceByPathSync(path: String): ByteArray?
}
