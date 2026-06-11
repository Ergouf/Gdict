package io.github.gdict.core

import io.github.gdict.core.GdictLogger.Companion.get as log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class DictionaryManager(
    private val dataDir: File,
    private val persistence: DictPersistence,
    private val fileImporter: DictFileImporter
) {

    data class DictEntry(
        val id: Long,
        val name: String,
        val path: String,
        val dictFilePath: String,
        val isEnabled: Boolean = true
    )

    data class SearchResult(
        val word: String,
        val definition: String,
        val dictionaryName: String,
        val css: String = ""
    )

    private val searchEngine = DictSearchEngine()

    private val dictionaries = mutableListOf<DictEntry>()
    private val loadedDicts = java.util.concurrent.ConcurrentHashMap<Long, MdxParser>()
    private val loadedMdds = java.util.concurrent.ConcurrentHashMap<Long, MdxParser>()
    private val cssCache = java.util.concurrent.ConcurrentHashMap<Long, String>()
    private val resourceCache = java.util.concurrent.ConcurrentHashMap<String, OptionalByteArray>()
    private val cssKeysCache = java.util.concurrent.ConcurrentHashMap<Long, List<String>>()

    private val loadLock = Mutex()
    @Volatile private var loadStarted = false
    @Volatile private var loadCompleted = false

    class OptionalByteArray(val value: ByteArray?) {
        companion object {
            private val NULL = OptionalByteArray(null)
            fun wrap(value: ByteArray?): OptionalByteArray = if (value == null) NULL else OptionalByteArray(value)
        }
    }

    init {
        val persisted = persistence.loadPersistedDictionaries()
        synchronized(this) {
            dictionaries.addAll(persisted)
        }
    }

    /**
     * Lazily loads MDX/MDD parsers for all enabled dictionaries on a background
     * dispatcher. Safe to call multiple times; subsequent calls are no-ops.
     *
     * Synchronous callers that need to wait for parsing to finish can pass a
     * CoroutineScope and check [isLoadCompleted] afterwards.
     */
    fun loadAllAsync(scope: CoroutineScope) {
        if (loadStarted) return
        loadStarted = true
        scope.launch(Dispatchers.IO) {
            val started = System.nanoTime()
            loadLock.withLock {
                val snapshot = synchronized(this@DictionaryManager) {
                    dictionaries.filter { it.isEnabled }.toList()
                }
                for (entry in snapshot) {
                    if (Thread.currentThread().isInterrupted) break
                    loadDictionary(entry)
                }
                loadCompleted = true
                val elapsedMs = (System.nanoTime() - started) / 1_000_000
                log().i(
                    "DictMgr",
                    "loadAllAsync: loaded ${loadedDicts.size}/${snapshot.size} dictionary parsers in ${elapsedMs}ms"
                )
            }
        }
    }

    fun isLoadCompleted(): Boolean = loadCompleted

    fun isDictLoaded(id: Long): Boolean = loadedDicts.containsKey(id)

    fun addOrUpdateDictionary(name: String, sourcePath: String, companionPaths: List<String> = emptyList()): DictEntry {
        try {
            val id = synchronized(this) {
                val existingIds = dictionaries.map { it.id }.toSet() + loadedDicts.keys
                persistence.nextId(existingIds)
            }
            val (entry, _) = fileImporter.addOrUpdateDictionary(name, sourcePath, companionPaths, id, dataDir)

            synchronized(this) {
                val removed = dictionaries.filter { it.name == name || it.path == sourcePath }
                for (old in removed) {
                    loadedDicts.remove(old.id)?.close()
                }
                dictionaries.removeAll { it.name == name || it.path == sourcePath }
                dictionaries.add(entry)
                removed
            }
            loadDictionary(entry)
            val loadedParser = loadedDicts[entry.id]
            if (loadedParser != null) {
                log().i("DictMgr", "  VERIFIED: '${entry.name}' → parser title='${loadedParser.title}' words=${loadedParser.wordCount}")
                synchronized(this) {
                    persistence.saveDictionaries(dictionaries.toList())
                }
            } else {
                log().e("DictMgr", "  FAILED: '${entry.name}' parser not loaded after loadDictionary!")
                val dictDir = File(dataDir, "dictionaries/${entry.id}")
                dictDir.deleteRecursively()
                throw RuntimeException("词典 '${name}' 加载失败，无法读取词典数据")
            }
            return entry
        } catch (e: Throwable) {
            log().e("DictMgr", "addOrUpdateDictionary CRASHED for '$name': ${e.javaClass.name} - ${e.message}", e)
            throw RuntimeException("导入 '$name' 时出错: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    private fun loadDictionary(entry: DictEntry) {
        val mdxFile = File(entry.dictFilePath)
        log().i("DictMgr", "Loading '${entry.name}' (id=${entry.id}) from: ${entry.dictFilePath}")
        if (!mdxFile.exists()) {
            log().e("DictMgr", "  File not found: ${entry.dictFilePath}")
            return
        }
        if (!mdxFile.name.lowercase().endsWith(".mdx")) {
            log().e("DictMgr", "  Not an .mdx file: ${entry.dictFilePath}")
            return
        }
        if (mdxFile.length() == 0L) {
            log().e("DictMgr", "  File is empty!")
            return
        }

        try {
            val parser = MdxParser(mdxFile)
            log().i("DictMgr", "  PARSER IDENTITY: hashCode=${parser.hashCode()} filePath='${parser.filePath}' fileName='${parser.fileName}' fileSize=${parser.fileSize} title='${parser.title}' words=${parser.wordCount}")

            if (parser.wordCount <= 0) {
                log().e("DictMgr", "  Loaded '${entry.name}' but wordCount=${parser.wordCount}, keywords empty!")
                parser.close()
                return
            }
            loadedDicts.remove(entry.id)?.close()
            loadedDicts[entry.id] = parser

            val mddFile = fileImporter.findCompanionMdd(mdxFile)
            log().i("DictMgr", "  MDD lookup for '${mdxFile.name}': ${if (mddFile != null) "'${mddFile.name}' (${mddFile.length()} bytes)" else "NOT FOUND"}")
            if (mddFile != null) {
                try {
                    log().i("DictMgr", "  Loading MDD: '${mddFile.name}' size=${mddFile.length()} bytes...")
                    val mddParser = MdxParser(mddFile)
                    log().i("DictMgr", "  MDD parsed: wordCount=${mddParser.wordCount} title='${mddParser.title}'")
                    if (mddParser.wordCount > 0) {
                        loadedMdds.remove(entry.id)?.close()
                        loadedMdds[entry.id] = mddParser
                        log().i("DictMgr", "  MDD loaded: '${mddFile.name}' resources=${mddParser.wordCount}")
                    } else {
                        log().w("DictMgr", "  MDD '${mddFile.name}' has wordCount=0, treating as empty")
                        mddParser.close()
                    }
                } catch (e: OutOfMemoryError) {
                    log().e("DictMgr", "  OOM loading MDD '${mddFile.name}' (${mddFile.length()} bytes): ${e.message}")
                    System.gc()
                } catch (e: Exception) {
                    log().e("DictMgr", "  Failed to load MDD '${mddFile.name}': ${e.javaClass.simpleName}: ${e.message}", e)
                }
            }

            log().i("DictMgr", "  LOADED OK: '${entry.name}' → title='${parser.title}' words=${parser.wordCount} encoding='${parser.encoding}' file=${mdxFile.name}")
        } catch (e: Exception) {
            log().e("DictMgr", "  FAILED to load ${entry.name}: ${e.javaClass.simpleName}: ${e.message}", e)
        } catch (e: OutOfMemoryError) {
            log().e("DictMgr", "  OUT OF MEMORY loading ${entry.name}: ${e.message}")
            System.gc()
        }
    }

    fun removeDictionary(id: Long) {
        loadedDicts.remove(id)?.close()
        loadedMdds.remove(id)?.close()
        cssCache.remove(id)
        cssKeysCache.remove(id)
        resourceCache.clear()
        synchronized(this) {
            dictionaries.removeAll { it.id == id }
        }
        val dictDir = File(dataDir, "dictionaries/$id")
        if (dictDir.exists()) dictDir.deleteRecursively()
        persistence.saveDictionaries(dictionaries.toList())
    }

    fun toggleDictionary(id: Long, enabled: Boolean) {
        val index = dictionaries.indexOfFirst { it.id == id }
        if (index >= 0) {
            synchronized(this) {
                dictionaries[index] = dictionaries[index].copy(isEnabled = enabled)
            }
            if (enabled) {
                loadDictionary(dictionaries[index])
            }
            persistence.saveDictionaries(dictionaries.toList())
        }
    }

    fun getDictionaries(): List<DictEntry> = synchronized(this) { dictionaries.toList() }

    fun getParserForDictionary(id: Long): MdxParser? = loadedDicts[id]

    fun getCssFromMdd(dictId: Long): String {
        val mddParser = loadedMdds[dictId] ?: return ""
        return searchEngine.getCssFromMdd(dictId, mapOf(dictId to mddParser), cssKeysCache)
    }

    fun searchWord(query: String): List<SearchResult> {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val dictsSnapshot = loadedDicts.toMap()
        val mddsSnapshot = loadedMdds.toMap()
        return searchEngine.searchWord(query, snapshot, dictsSnapshot, cssCache, mddsSnapshot, cssKeysCache).map {
            SearchResult(it.word, it.definition, it.dictionaryName, it.css)
        }
    }

    fun getAudioResource(word: String): ByteArray? {
        resourceCache[word]?.value?.let { return it }
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val mddsSnapshot = loadedMdds.toMap()
        val result = searchEngine.getAudioResource(word, snapshot, mddsSnapshot)
        if (result != null) {
            resourceCache[word] = OptionalByteArray.wrap(result)
        }
        return result
    }

    fun getAudioResourceByPath(path: String): ByteArray? {
        resourceCache[path]?.value?.let { return it }
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val mddsSnapshot = loadedMdds.toMap()
        val result = searchEngine.getAudioResourceByPath(path, snapshot, mddsSnapshot)
        if (result != null) {
            resourceCache[path] = OptionalByteArray.wrap(result)
        }
        return result
    }

    fun searchSuggestions(prefix: String, limit: Int = 10): List<String> {
        if (prefix.isBlank()) return emptyList()
        val results = mutableSetOf<String>()
        for ((_, parser) in loadedDicts) {
            val matches = parser.readArticlesPredictive(prefix)
            results.addAll(matches.keys)
            if (results.size >= limit) break
        }
        return results.take(limit)
    }

    fun getRandomWords(count: Int = 5): List<Pair<String, String>> {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val dictsSnapshot = loadedDicts.toMap()
        return searchEngine.getRandomWords(count, snapshot, dictsSnapshot)
    }

    fun scanDirectory(dirPath: String): List<DictFileImporter.DictCandidate> {
        return fileImporter.scanDirectory(dirPath)
    }

    fun diagnoseAllDictionaries(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Gdict Dictionary Diagnostics ===")
        sb.appendLine("Total dicts: ${synchronized(this) { dictionaries.size }}, Loaded parsers: ${loadedDicts.size}")
        sb.appendLine()

        val dictSnapshot = synchronized(this) { dictionaries.toList() }
        for ((i, dict) in dictSnapshot.withIndex()) {
            sb.appendLine("--- Dict[$i] ---")
            sb.appendLine("  id=${dict.id} name='${dict.name}'")
            sb.appendLine("  path='${dict.path}'")
            sb.appendLine("  dictFilePath='${dict.dictFilePath}'")
            sb.appendLine("  isEnabled=${dict.isEnabled}")

            val mdxFile = File(dict.dictFilePath)
            sb.appendLine("  fileExists=${mdxFile.exists()} fileSize=${mdxFile.length()}")

            val dictDir = mdxFile.parentFile
            val dictDirFiles = dictDir?.listFiles()?.sortedBy { it.name }
            sb.appendLine("  Files in dictDir (${dictDirFiles?.size ?: 0}): ${dictDirFiles?.joinToString { "${it.name}(${it.length()})" }}")

            val parser = loadedDicts[dict.id]
            if (parser != null) {
                sb.appendLine("  parser: title='${parser.title}' words=${parser.wordCount}")
                sb.appendLine("          encoding='${parser.encoding}' caseSensitive=${parser.isKeyCaseSensitive}")

                try {
                    val testWord = "read"
                    val articles = parser.readArticles(testWord)
                    sb.appendLine("  search('$testWord'): ${articles.size} results")
                    for ((word, def) in articles) {
                        val preview = def?.take(80)?.replace("\n", "\\n") ?: "(null)"
                        val hash = def?.hashCode() ?: 0
                        sb.appendLine("    ['$word'] hash=$hash len=${def?.length ?: 0} preview='$preview'")
                    }
                } catch (e: Exception) {
                    sb.appendLine("  search ERROR: ${e.javaClass.simpleName}: ${e.message}")
                }

                try {
                    val first3Keywords = parser.getAllKeywords().take(3)
                    sb.appendLine("  first keywords: $first3Keywords")
                } catch (_: Exception) {}
            } else {
                sb.appendLine("  parser: NULL (not loaded!)")
            }
            sb.appendLine()
        }

        sb.appendLine("=== Parser identity check ===")
        val parserIdentities = mutableMapOf<Int, String>()
        for ((id, parser) in loadedDicts) {
            val identity = "${parser.title}|${parser.wordCount}|${parser.hashCode()}|${parser.fileName}"
            parserIdentities[id.toInt()] = identity
            sb.appendLine("  parser[$id] → $identity")
        }
        val uniqueParsers = parserIdentities.values.toSet()
        sb.appendLine("  Unique parser identities: ${uniqueParsers.size}/${parserIdentities.size}")
        if (uniqueParsers.size < parserIdentities.size) {
            sb.appendLine("  WARNING: Duplicate parser detected!")
        }
        return sb.toString()
    }

    fun testMddResourcesAndHtml(): String {
        val sb = StringBuilder()
        sb.appendLine("=== MDD Resource & HTML Render Test ===")
        sb.appendLine()

        synchronized(this) {
            for ((id, dict) in dictionaries.withIndex()) {
                if (!dict.isEnabled) continue
                sb.appendLine("--- Dict[$id]: ${dict.name} ---")

                val parser = loadedDicts[dict.id]
                if (parser == null) {
                    sb.appendLine("  MDX parser not loaded!")
                    sb.appendLine()
                    continue
                }

                val mddParser = loadedMdds[dict.id]
                if (mddParser == null) {
                    sb.appendLine("  No MDD file (images may show as transparent)")
                } else {
                    sb.appendLine("  MDD loaded: ${mddParser.wordCount} resources, resourceMode=${mddParser.isResourceMode}")
                    val imageKeys = mddParser.findResourceKeys(".png").take(5)
                    if (imageKeys.isNotEmpty()) {
                        sb.appendLine("  Sample images: $imageKeys")
                        for (key in imageKeys.take(3)) {
                            val data = mddParser.readResourceBytesByKey(key)
                            sb.appendLine("    '$key' → ${data?.size ?: 0} bytes")
                        }
                    } else {
                        sb.appendLine("  No .png resources found in MDD")
                    }
                }
                sb.appendLine()

                val cssContent = getCssFromMdd(dict.id)
                val cssLen = cssContent.length
                sb.appendLine("  CSS from MDD: ${if (cssLen > 0) "$cssLen chars" else "EMPTY!"}")

                val companionCss = parser.companionCss
                val compLen = companionCss.length
                sb.appendLine("  companionCss: ${if (compLen > 0) "$compLen chars" else "EMPTY!"}")

                try {
                    val testWords = listOf("hello", "test", "the", "a", "bye")
                    for (word in testWords) {
                        val articles = parser.readArticles(word)
                        if (articles.isNotEmpty()) {
                            for ((w, def) in articles) {
                                if (def == null || def.isBlank()) continue
                                sb.appendLine("  Test word: '$w'")
                                val hasImg = Regex("""<img[^>]+src=["']([^"']+)["']""").containsMatchIn(def)
                                val cssLinks = Regex("""<link[^>]+href=["']([^"']+\.css)["']""").findAll(def).map { it.groupValues[1] }.distinct().toList()
                                sb.appendLine("     - Has <img>: $hasImg | CSS links: $cssLinks")
                                break
                            }
                            break
                        }
                    }
                } catch (e: Exception) {
                    sb.appendLine("  Test search error: ${e.message}")
                }
                sb.appendLine()
            }
        }
        return sb.toString()
    }
}
