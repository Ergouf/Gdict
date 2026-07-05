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

        private val AUDIO_EXTENSIONS = listOf("mp3", "wav", "ogg", "spx")
        private val AUDIO_PREFIXES = listOf("uk", "us", "gb", "en", "audio", "sound", "pron")
        private val AUDIO_PATTERN_CACHE = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
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

        val snapshot = dictionaries.filter { it.isEnabled }
        if (snapshot.size <= 1) {
            val results = mutableListOf<SearchResult>()
            for (dict in snapshot) {
                val parser = loadedDicts[dict.id]
                if (parser != null) {
                    results.addAll(searchWithParser(parser, dict, query, cssCache, loadedMdds, cssKeysCache))
                }
            }
            return results
        }

        // 并行搜索多个词典
        val tasks = snapshot.mapNotNull { dict ->
            val parser = loadedDicts[dict.id] ?: return@mapNotNull null
            java.util.concurrent.Callable {
                searchWithParser(parser, dict, query, cssCache, loadedMdds, cssKeysCache)
            }
        }

        val executor = java.util.concurrent.Executors.newFixedThreadPool(
            minOf(tasks.size, Runtime.getRuntime().availableProcessors())
        )
        try {
            val futures = executor.invokeAll(tasks)
            return futures.flatMap { it.get() }
        } catch (e: Exception) {
            log().e("DictSearchEngine", "Parallel search failed: ${e.message}")
            return emptyList()
        } finally {
            executor.shutdown()
        }
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
            val css = buildCss(parser, dict.id, cssCache, loadedMdds, cssKeysCache)

            val exact = parser.readArticles(query)
            for ((word, def) in exact) {
                results.add(SearchResult(word = cleanWord(word), definition = def ?: "", dictionaryName = dict.name, css = css))
            }

            if (results.isEmpty()) {
                val predictive = parser.readArticlesPredictive(query)
                for ((word, def) in predictive) {
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
        if (mddParser != null && (mddParser.wordCount > 0 || mddParser.isResourceMode)) {
            try {
                val cssKeys = cssKeysCache?.get(dictId) ?: mddParser.findResourceKeys(".css").also {
                    cssKeysCache?.put(dictId, it)
                }
                for (key in cssKeys) {
                    try {
                        val cssBytes = mddParser.readResourceBytesByKey(key)
                        if (cssBytes != null && cssBytes.isNotEmpty()) {
                            sb.append(String(cssBytes, Charsets.UTF_8)).append("\n")
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        val result = sb.toString()
        cssCache[dictId] = result
        return result
    }

    fun getCssFromMdd(dictId: Long, loadedMdds: Map<Long, MdxParser>, cssKeysCache: MutableMap<Long, List<String>>? = null): String {
        val mddParser = loadedMdds[dictId] ?: return ""
        val cssKeys = cssKeysCache?.get(dictId) ?: mddParser.findResourceKeys(".css").also {
            cssKeysCache?.put(dictId, it)
        }
        if (cssKeys.isEmpty()) return ""
        val sb = StringBuilder()
        for (key in cssKeys) {
            try {
                val data = mddParser.readResourceBytesByKey(key)
                if (data != null && data.isNotEmpty()) {
                    sb.append(String(data, Charsets.UTF_8))
                    sb.append("\n")
                }
            } catch (_: Exception) {}
        }
        return sb.toString()
    }

    fun getAudioResource(
        word: String,
        dictionaries: List<DictionaryManager.DictEntry>,
        loadedMdds: Map<Long, MdxParser>
    ): ByteArray? {
        val snapshot = dictionaries.filter { it.isEnabled }
        val audioPatterns = AUDIO_PATTERN_CACHE.getOrPut(word) {
            buildAudioPatterns(word)
        }
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (pattern in audioPatterns) {
                val data = mddParser.readResourceBytes(pattern)
                if (data != null && data.isNotEmpty()) {
                    return data
                }
            }
        }
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (ext in AUDIO_EXTENSIONS) {
                val suffix = "\\$word.$ext"
                val matches = mddParser.findResourceKeys(suffix)
                for (match in matches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
                        return data
                    }
                }
                val lowerSuffix = "\\${word.lowercase()}.$ext"
                val lowerMatches = mddParser.findResourceKeys(lowerSuffix)
                for (match in lowerMatches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
                        return data
                    }
                }
            }
        }
        return null
    }

    private fun buildAudioPatterns(word: String): List<String> {
        val patterns = mutableListOf<String>()
        val upperWord = word.uppercase()
        for (ext in AUDIO_EXTENSIONS) {
            patterns.add("\\$word.$ext")
            patterns.add("\\${word.lowercase()}.$ext")
            patterns.add("\\$upperWord.$ext")
            patterns.add("$word.$ext")
            patterns.add("${word.lowercase()}.$ext")
            patterns.add("$upperWord.$ext")
            for (prefix in AUDIO_PREFIXES) {
                patterns.add("\\$prefix\\$word.$ext")
                patterns.add("\\$prefix\\${word.lowercase()}.$ext")
                patterns.add("\\$prefix\\$upperWord.$ext")
                patterns.add("\\$prefix\\${word.lowercase()}_$ext.$ext")
                patterns.add("\\${word.lowercase()}_$prefix.$ext")
                patterns.add("$prefix/$word.$ext")
                patterns.add("$prefix/${word.lowercase()}.$ext")
                patterns.add("$prefix/$upperWord.$ext")
            }
        }
        return patterns
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
        pathVariants.add("/$path")
        pathVariants.add(path.uppercase())
        pathVariants.add("\\${path.uppercase()}")
        val fileName = path.substringAfterLast("/").substringAfterLast("\\")
        if (fileName != path) {
            pathVariants.add("\\$fileName")
            pathVariants.add("/$fileName")
            pathVariants.add(fileName)
            pathVariants.add("\\${fileName.uppercase()}")
            pathVariants.add(fileName.uppercase())
            for (prefix in listOf("uk", "us", "gb", "en", "audio", "sound", "pron")) {
                pathVariants.add("\\$prefix\\$fileName")
                pathVariants.add("$prefix/$fileName")
                pathVariants.add("$prefix\\$fileName")
                pathVariants.add("\\$prefix\\${fileName.uppercase()}")
                pathVariants.add("$prefix/${fileName.uppercase()}")
            }
        }

        val distinctVariants = pathVariants.distinct()
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (variant in distinctVariants) {
                val data = mddParser.readResourceBytes(variant)
                if (data != null && data.isNotEmpty()) {
                    return data
                }
            }
        }
        val lowerVariants = distinctVariants.map { it.lowercase() }.distinct()
        for (dict in snapshot) {
            val mddParser = loadedMdds[dict.id] ?: continue
            for (suffix in lowerVariants) {
                val matches = mddParser.findResourceKeys(suffix)
                for (match in matches) {
                    val data = mddParser.readResourceBytesByKey(match)
                    if (data != null && data.isNotEmpty()) {
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
