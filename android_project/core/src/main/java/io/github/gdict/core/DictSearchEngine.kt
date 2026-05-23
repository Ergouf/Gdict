package io.github.gdict.core

import android.util.Log
import androidx.annotation.WorkerThread

class DictSearchEngine {

    data class SearchResult(
        val word: String,
        val definition: String,
        val dictionaryName: String,
        val css: String = ""
    )

    @WorkerThread
    fun searchWord(
        query: String,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedDicts: Map<Long, MdxParser>,
        cssCache: MutableMap<Long, String>,
        loadedMdds: Map<Long, MdxParser>
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<SearchResult>()
        Log.i("DictSearchEngine", "searchWord('$query') called, loadedDicts=${loadedDicts.size}, enabledDicts=${dictionaries.count { it.isEnabled }}")

        val snapshot = dictionaries.filter { it.isEnabled }

        for (dict in snapshot) {
            val parser = loadedDicts[dict.id]
            if (parser != null) {
                Log.d("DictSearchEngine", "  Searching '${dict.name}' (id=${dict.id}) using parser title='${parser.title}' words=${parser.wordCount}")
                val dictResults = searchWithParser(parser, dict, query, cssCache, loadedMdds)
                results.addAll(dictResults)
            } else {
                Log.w("DictSearchEngine", "  No parser for '${dict.name}' (id=${dict.id})")
            }
        }

        Log.i("DictSearchEngine", "searchWord('$query') returned ${results.size} results")
        return results
    }

    private fun searchWithParser(
        parser: MdxParser,
        dict: DictionaryManager.DictEntry,
        query: String,
        cssCache: MutableMap<Long, String>,
        loadedMdds: Map<Long, MdxParser>
    ): List<SearchResult> {
        return try {
            val results = mutableListOf<SearchResult>()
            Log.i("DictSearchEngine", "    [SEARCH] dict='${dict.name}' query='$query' parserHash=${parser.hashCode()} parserTitle='${parser.title}' parserFile='${parser.fileName}' parserWords=${parser.wordCount}")
            val css = buildCss(parser, dict.id, cssCache, loadedMdds)
            if (css.isNotEmpty()) {
                Log.i("DictSearchEngine", "    [SEARCH] CSS loaded for '${dict.name}': ${css.length} chars")
            }

            val exact = parser.readArticles(query)
            Log.d("DictSearchEngine", "    '${dict.name}' exact match: ${exact.size} articles")
            for ((word, def) in exact) {
                val defHash = def?.hashCode() ?: 0
                val preview = def?.take(60)?.replace("\n", "\\n") ?: "(null)"
                Log.d("DictSearchEngine", "      ['$word'] defHash=$defHash preview='$preview'")
                results.add(SearchResult(word = word ?: query, definition = def ?: "", dictionaryName = dict.name, css = css))
            }

            if (results.isEmpty()) {
                val predictive = parser.readArticlesPredictive(query)
                Log.d("DictSearchEngine", "    '${dict.name}' predictive: ${predictive.size} articles")
                for ((word, def) in predictive) {
                    val defHash = def?.hashCode() ?: 0
                    Log.d("DictSearchEngine", "      ['$word'] defHash=$defHash")
                    results.add(SearchResult(word = word ?: query, definition = def ?: "", dictionaryName = dict.name, css = css))
                }
            }

            results
        } catch (e: Exception) {
            Log.e("DictSearchEngine", "Search FAILED for ${dict.name}: ${e.message}")
            emptyList()
        }
    }

    fun buildCss(
        parser: MdxParser,
        dictId: Long,
        cssCache: MutableMap<Long, String>,
        loadedMdds: Map<Long, MdxParser>
    ): String {
        cssCache[dictId]?.let { return it }
        val sb = StringBuilder()
        sb.append(parser.companionCss)
        val mddParser = loadedMdds[dictId]
        if (mddParser != null && mddParser.wordCount > 0) {
            try {
                val cssKeys = mddParser.findResourceKeys(".css")
                for (key in cssKeys) {
                    try {
                        val cssBytes = mddParser.readResourceBytesByKey(key)
                        if (cssBytes != null && cssBytes.isNotEmpty()) {
                            sb.append(String(cssBytes, Charsets.UTF_8)).append("\n")
                            Log.i("DictSearchEngine", "  Loaded CSS from MDD: $key (${cssBytes.size} bytes)")
                        }
                    } catch (e: Exception) {
                        Log.w("DictSearchEngine", "  Failed to read CSS resource $key: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("DictSearchEngine", "  Failed to extract CSS from MDD: ${e.message}")
            }
        }
        val result = sb.toString()
        cssCache[dictId] = result
        return result
    }

    fun getCssFromMdd(dictId: Long, loadedMdds: Map<Long, MdxParser>): String {
        val mddParser = loadedMdds[dictId] ?: return ""
        Log.i("DictSearchEngine", "getCssFromMdd: dictId=$dictId, mddParser.title='${mddParser.title}' words=${mddParser.wordCount}")
        val cssKeys = mddParser.findResourceKeys(".css")
        Log.i("DictSearchEngine", "getCssFromMdd: found ${cssKeys.size} CSS keys: $cssKeys")
        if (cssKeys.isEmpty()) return ""
        val sb = StringBuilder()
        for (key in cssKeys) {
            try {
                val data = mddParser.readResourceBytesByKey(key)
                if (data != null && data.isNotEmpty()) {
                    sb.append(String(data, Charsets.UTF_8))
                    sb.append("\n")
                    Log.i("DictSearchEngine", "Loaded CSS from MDD: '$key' (${data.size} bytes)")
                } else {
                    Log.w("DictSearchEngine", "CSS key '$key' returned ${if (data == null) "null" else "empty"}")
                }
            } catch (e: Exception) {
                Log.w("DictSearchEngine", "Failed to read CSS '$key' from MDD: ${e.message}")
            }
        }
        return sb.toString()
    }

    @WorkerThread
    fun getAudioResource(
        word: String,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedMdds: Map<Long, MdxParser>
    ): ByteArray? {
        val snapshot = dictionaries.filter { it.isEnabled }
        val audioPatterns = listOf(
            "\\$word.mp3",
            "\\$word.wav",
            "\\$word.ogg",
            "\\$word.spx",
            "\\${word.lowercase()}.mp3",
            "\\${word.lowercase()}.wav",
            "\\${word.lowercase()}.ogg",
            "\\${word.lowercase()}.spx"
        )
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (pattern in audioPatterns) {
                val data = mddParser.readResourceBytes(pattern)
                if (data != null && data.isNotEmpty()) {
                    Log.i("DictSearchEngine", "Audio found for '$word' in '${dict.name}': pattern='$pattern' size=${data.size}")
                    return data
                }
            }
        }
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (suffix in audioPatterns) {
                val matches = mddParser.findResourceKeys(suffix)
                for (match in matches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
                        Log.i("DictSearchEngine", "Audio found (fuzzy) for '$word' in '${dict.name}': key='$match' size=${data.size}")
                        return data
                    }
                }
            }
        }
        return null
    }

    @WorkerThread
    fun getAudioResourceByPath(
        path: String,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedMdds: Map<Long, MdxParser>
    ): ByteArray? {
        val snapshot = dictionaries.filter { it.isEnabled }
        val normalizedPath = path.replace("/", "\\")
        val pathWithBackslash = if (normalizedPath.startsWith("\\")) normalizedPath else "\\$normalizedPath"
        Log.d("DictSearchEngine", "getAudioResourceByPath('$path') enabledDicts=${snapshot.size} loadedMdds=${loadedMdds.size}")
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id]
            if (mddParser == null) {
                Log.w("DictSearchEngine", "  MDD not loaded for '${dict.name}' (id=${dict.id})")
                continue
            }
            Log.d("DictSearchEngine", "  Trying '${dict.name}' MDD (words=${mddParser.wordCount})")
            val data = mddParser.readResourceBytes(pathWithBackslash)
            if (data != null && data.isNotEmpty()) {
                Log.i("DictSearchEngine", "Audio found by path '$path' in '${dict.name}' size=${data.size}")
                return data
            }
            val data2 = mddParser.readResourceBytes(normalizedPath)
            if (data2 != null && data2.isNotEmpty()) {
                Log.i("DictSearchEngine", "Audio found by path '$path' in '${dict.name}' size=${data2.size}")
                return data2
            }
        }
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            val suffix = pathWithBackslash.lowercase()
            val matches = mddParser.findResourceKeys(suffix)
            for (match in matches) {
                val data = mddParser.readResourceBytesByKey(match)
                if (data != null && data.isNotEmpty()) {
                    Log.i("DictSearchEngine", "Audio found (fuzzy) by path '$path' in '${dict.name}': key='$match' size=${data.size}")
                    return data
                }
            }
        }
        return null
    }

    @WorkerThread
    fun getRandomWords(
        count: Int = 5,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedDicts: Map<Long, MdxParser>
    ): List<Pair<String, String>> {
        val snapshot = dictionaries.filter { it.isEnabled }
        val allWords = mutableListOf<Pair<String, String>>()
        for (dict in snapshot) {
            val parser = loadedDicts[dict.id] ?: continue
            val keywords = parser.getAllKeywords()
            if (keywords.isEmpty()) continue
            val step = (keywords.size / count).coerceAtLeast(1)
            val start = (0 until step).random()
            for (i in 0 until count) {
                val idx = start + i * step
                if (idx < keywords.size) {
                    allWords.add(Pair(keywords[idx], dict.name))
                }
            }
        }
        return allWords.shuffled().take(count)
    }
}
