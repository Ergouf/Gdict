package io.github.gdict.core

import io.github.gdict.core.GdictLogger.Companion.get as log

class DictSearchEngine {

    data class SearchResult(
        val word: String,
        val definition: String,
        val dictionaryName: String,
        val css: String = ""
    )

    companion object {
        private val PIPE_PATTERN = Regex("\\|")
        fun cleanWord(word: String?): String = word?.replace(PIPE_PATTERN, "") ?: ""
    }

    fun searchWord(
        query: String,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedDicts: Map<Long, MdxParser>,
        cssCache: MutableMap<Long, String>,
        loadedMdds: Map<Long, MdxParser>,
        cssKeysCache: MutableMap<Long, List<String>>? = null
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<SearchResult>()
        log().i("DictSearchEngine", "searchWord('$query') called, loadedDicts=${loadedDicts.size}, enabledDicts=${dictionaries.count { it.isEnabled }}")

        val snapshot = dictionaries.filter { it.isEnabled }

        for (dict in snapshot) {
            val parser = loadedDicts[dict.id]
            if (parser != null) {
                log().d("DictSearchEngine", "  Searching '${dict.name}' (id=${dict.id}) using parser title='${parser.title}' words=${parser.wordCount}")
                val dictResults = searchWithParser(parser, dict, query, cssCache, loadedMdds, cssKeysCache)
                results.addAll(dictResults)
            } else {
                log().w("DictSearchEngine", "  No parser for '${dict.name}' (id=${dict.id})")
            }
        }

        log().i("DictSearchEngine", "searchWord('$query') returned ${results.size} results")
        return results
    }

    private fun searchWithParser(
        parser: MdxParser,
        dict: DictionaryManager.DictEntry,
        query: String,
        cssCache: MutableMap<Long, String>,
        loadedMdds: Map<Long, MdxParser>,
        cssKeysCache: MutableMap<Long, List<String>>? = null
    ): List<SearchResult> {
        return try {
            val results = mutableListOf<SearchResult>()
            log().i("DictSearchEngine", "    [SEARCH] dict='${dict.name}' query='$query' parserHash=${parser.hashCode()} parserTitle='${parser.title}' parserFile='${parser.fileName}' parserWords=${parser.wordCount}")
            val css = buildCss(parser, dict.id, cssCache, loadedMdds, cssKeysCache)
            if (css.isNotEmpty()) {
                log().i("DictSearchEngine", "    [SEARCH] CSS loaded for '${dict.name}': ${css.length} chars")
            }

            val exact = parser.readArticles(query)
            log().d("DictSearchEngine", "    '${dict.name}' exact match: ${exact.size} articles")
            for ((word, def) in exact) {
                val defHash = def?.hashCode() ?: 0
                val preview = def?.take(60)?.replace("\n", "\\n") ?: "(null)"
                log().d("DictSearchEngine", "      ['$word'] defHash=$defHash preview='$preview'")
                results.add(SearchResult(word = cleanWord(word), definition = def ?: "", dictionaryName = dict.name, css = css))
            }

            if (results.isEmpty()) {
                val predictive = parser.readArticlesPredictive(query)
                log().d("DictSearchEngine", "    '${dict.name}' predictive: ${predictive.size} articles")
                for ((word, def) in predictive) {
                    val defHash = def?.hashCode() ?: 0
                    log().d("DictSearchEngine", "      ['$word'] defHash=$defHash")
                    results.add(SearchResult(word = cleanWord(word), definition = def ?: "", dictionaryName = dict.name, css = css))
                }
            }

            results
        } catch (e: Exception) {
            log().e("DictSearchEngine", "Search FAILED for ${dict.name}: ${e.message}")
            emptyList()
        }
    }

    fun buildCss(
        parser: MdxParser,
        dictId: Long,
        cssCache: MutableMap<Long, String>,
        loadedMdds: Map<Long, MdxParser>,
        cssKeysCache: MutableMap<Long, List<String>>? = null
    ): String {
        cssCache[dictId]?.let { return it }
        val sb = StringBuilder()
        sb.append(parser.companionCss)
        val mddParser = loadedMdds[dictId]
        if (mddParser != null && mddParser.wordCount > 0) {
            try {
                val cssKeys = cssKeysCache?.get(dictId) ?: mddParser.findResourceKeys(".css").also {
                    cssKeysCache?.put(dictId, it)
                }
                for (key in cssKeys) {
                    try {
                        val cssBytes = mddParser.readResourceBytesByKey(key)
                        if (cssBytes != null && cssBytes.isNotEmpty()) {
                            sb.append(String(cssBytes, Charsets.UTF_8)).append("\n")
                            log().i("DictSearchEngine", "  Loaded CSS from MDD: $key (${cssBytes.size} bytes)")
                        }
                    } catch (e: Exception) {
                        log().w("DictSearchEngine", "  Failed to read CSS resource $key: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                log().w("DictSearchEngine", "  Failed to extract CSS from MDD: ${e.message}")
            }
        }
        val result = sb.toString()
        cssCache[dictId] = result
        return result
    }

    fun getCssFromMdd(dictId: Long, loadedMdds: Map<Long, MdxParser>, cssKeysCache: MutableMap<Long, List<String>>? = null): String {
        val mddParser = loadedMdds[dictId] ?: return ""
        log().i("DictSearchEngine", "getCssFromMdd: dictId=$dictId, mddParser.title='${mddParser.title}' words=${mddParser.wordCount}")
        val cssKeys = cssKeysCache?.get(dictId) ?: mddParser.findResourceKeys(".css").also {
            cssKeysCache?.put(dictId, it)
        }
        log().i("DictSearchEngine", "getCssFromMdd: found ${cssKeys.size} CSS keys: $cssKeys")
        if (cssKeys.isEmpty()) return ""
        val sb = StringBuilder()
        for (key in cssKeys) {
            try {
                val data = mddParser.readResourceBytesByKey(key)
                if (data != null && data.isNotEmpty()) {
                    sb.append(String(data, Charsets.UTF_8))
                    sb.append("\n")
                    log().i("DictSearchEngine", "Loaded CSS from MDD: '$key' (${data.size} bytes)")
                } else {
                    log().w("DictSearchEngine", "CSS key '$key' returned ${if (data == null) "null" else "empty"}")
                }
            } catch (e: Exception) {
                log().w("DictSearchEngine", "Failed to read CSS '$key' from MDD: ${e.message}")
            }
        }
        return sb.toString()
    }

    fun getAudioResource(
        word: String,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedMdds: Map<Long, MdxParser>
    ): ByteArray? {
        val snapshot = dictionaries.filter { it.isEnabled }
        val extensions = listOf("mp3", "wav", "ogg", "spx")
        val audioPatterns = mutableListOf<String>()
        for (ext in extensions) {
            audioPatterns.add("\\$word.$ext")
            audioPatterns.add("\\${word.lowercase()}.$ext")
            for (prefix in listOf("uk", "us", "gb", "en", "audio", "sound", "pron")) {
                audioPatterns.add("\\$prefix\\$word.$ext")
                audioPatterns.add("\\$prefix\\${word.lowercase()}.$ext")
                audioPatterns.add("\\$prefix\\${word.lowercase()}_$ext.$ext")
                audioPatterns.add("\\${word.lowercase()}_$prefix.$ext")
            }
        }
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (pattern in audioPatterns) {
                val data = mddParser.readResourceBytes(pattern)
                if (data != null && data.isNotEmpty()) {
                    log().i("DictSearchEngine", "Audio found for '$word' in '${dict.name}': pattern='$pattern' size=${data.size}")
                    return data
                }
            }
        }
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (ext in extensions) {
                val suffix = "\\$word.$ext"
                val matches = mddParser.findResourceKeys(suffix)
                for (match in matches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
                        log().i("DictSearchEngine", "Audio found (fuzzy) for '$word' in '${dict.name}': key='$match' size=${data.size}")
                        return data
                    }
                }
                val lowerSuffix = "\\${word.lowercase()}.$ext"
                val lowerMatches = mddParser.findResourceKeys(lowerSuffix)
                for (match in lowerMatches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
                        log().i("DictSearchEngine", "Audio found (fuzzy-lower) for '$word' in '${dict.name}': key='$match' size=${data.size}")
                        return data
                    }
                }
            }
        }
        return null
    }

    fun getAudioResourceByPath(
        path: String,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedMdds: Map<Long, MdxParser>
    ): ByteArray? {
        val snapshot = dictionaries.filter { it.isEnabled }
        val normalizedPath = path.replace("/", "\\")
        val pathWithBackslash = if (normalizedPath.startsWith("\\")) normalizedPath else "\\$normalizedPath"

        val pathVariants = mutableListOf<String>()
        pathVariants.add(pathWithBackslash)
        pathVariants.add(normalizedPath)
        pathVariants.add(path)
        pathVariants.add("\\$path")
        val fileName = path.substringAfterLast("/").substringAfterLast("\\")
        if (fileName != path) {
            pathVariants.add("\\$fileName")
            for (prefix in listOf("uk", "us", "gb", "en", "audio", "sound", "pron")) {
                pathVariants.add("\\$prefix\\$fileName")
            }
        }

        log().d("DictSearchEngine", "getAudioResourceByPath('$path') enabledDicts=${snapshot.size} loadedMdds=${loadedMdds.size} variants=${pathVariants.distinct().size}")
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id]
            if (mddParser == null) {
                log().w("DictSearchEngine", "  MDD not loaded for '${dict.name}' (id=${dict.id})")
                continue
            }
            log().d("DictSearchEngine", "  Trying '${dict.name}' MDD (words=${mddParser.wordCount})")
            for (variant in pathVariants.distinct()) {
                val data = mddParser.readResourceBytes(variant)
                if (data != null && data.isNotEmpty()) {
                    log().i("DictSearchEngine", "Audio found by path '$path' in '${dict.name}' variant='$variant' size=${data.size}")
                    return data
                }
            }
        }
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (suffix in pathVariants.distinct().map { it.lowercase() }) {
                val matches = mddParser.findResourceKeys(suffix)
                for (match in matches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
                        log().i("DictSearchEngine", "Audio found (fuzzy) by path '$path' in '${dict.name}': key='$match' size=${data.size}")
                        return data
                    }
                }
            }
        }
        return null
    }

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
