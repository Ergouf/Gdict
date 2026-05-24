package io.github.gdict.core

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.File

class DictionaryManager(private val context: Context) {

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

    private val persistence = DictPersistence(context)
    private val fileImporter = DictFileImporter(context)
    private val searchEngine = DictSearchEngine()

    private val dictionaries = mutableListOf<DictEntry>()
    private val loadedDicts = mutableMapOf<Long, MdxParser>()
    private val loadedMdds = mutableMapOf<Long, MdxParser>()
    private val cssCache = mutableMapOf<Long, String>()

    init {
        val persisted = persistence.loadPersistedDictionaries()
        synchronized(this) {
            dictionaries.addAll(persisted)
        }
        for (entry in persisted) {
            if (entry.isEnabled) {
                loadDictionary(entry)
            }
        }
    }

    fun addOrUpdateDictionary(name: String, sourceUri: String, companionUris: List<String> = emptyList()): DictEntry {
        try {
            val id = synchronized(this) {
                val existingIds = dictionaries.map { it.id }.toSet() + loadedDicts.keys
                persistence.nextId(existingIds)
            }
            val (entry, _) = fileImporter.addOrUpdateDictionary(name, sourceUri, companionUris, id)

            synchronized(this) {
                val removed = dictionaries.filter { it.name == name || it.path == sourceUri }
                for (old in removed) {
                    loadedDicts.remove(old.id)?.close()
                }
                dictionaries.removeAll { it.name == name || it.path == sourceUri }
                dictionaries.add(entry)
            }
            loadDictionary(entry)
            synchronized(this) {
                val loadedParser = loadedDicts[entry.id]
                if (loadedParser != null) {
                    Log.i("DictMgr", "  VERIFIED: '${entry.name}' → parser title='${loadedParser.title}' words=${loadedParser.wordCount}")
                    persistence.saveDictionaries(dictionaries.toList())
                } else {
                    Log.e("DictMgr", "  FAILED: '${entry.name}' parser not loaded after loadDictionary!")
                    val dictDir = File(context.filesDir, "dictionaries/${entry.id}")
                    dictDir.deleteRecursively()
                    throw RuntimeException("词典 '${name}' 加载失败，无法读取词典数据")
                }
            }
            return entry
        } catch (e: Throwable) {
            Log.e("DictMgr", "addOrUpdateDictionary CRASHED for '$name': ${e.javaClass.name} - ${e.message}", e)
            throw RuntimeException("导入 '$name' 时出错: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    private fun loadDictionary(entry: DictEntry) {
        val mdxFile = File(entry.dictFilePath)
        Log.i("DictMgr", "Loading '${entry.name}' (id=${entry.id}) from: ${entry.dictFilePath}")
        if (!mdxFile.exists()) {
            Log.e("DictMgr", "  File not found: ${entry.dictFilePath}")
            return
        }
        if (!mdxFile.name.lowercase().endsWith(".mdx")) {
            Log.e("DictMgr", "  Not an .mdx file: ${entry.dictFilePath}")
            return
        }
        if (mdxFile.length() == 0L) {
            Log.e("DictMgr", "  File is empty!")
            return
        }

        try {
            val parser = MdxParser(mdxFile)
            Log.i("DictMgr", "  PARSER IDENTITY: hashCode=${parser.hashCode()} filePath='${parser.filePath}' fileName='${parser.fileName}' fileSize=${parser.fileSize} title='${parser.title}' words=${parser.wordCount}")

            if (parser.wordCount <= 0) {
                Log.e("DictMgr", "  Loaded '${entry.name}' but wordCount=${parser.wordCount}, keywords empty!")
                parser.close()
                return
            }
            synchronized(this) {
                loadedDicts.remove(entry.id)?.close()
                loadedDicts[entry.id] = parser
            }

            val mddFile = fileImporter.findCompanionMdd(mdxFile)
            Log.i("DictMgr", "  MDD lookup for '${mdxFile.name}': ${if (mddFile != null) "'${mddFile.name}' (${mddFile.length()} bytes)" else "NOT FOUND"}")
            if (mddFile != null) {
                try {
                    Log.i("DictMgr", "  Loading MDD: '${mddFile.name}' size=${mddFile.length()} bytes...")
                    val mddParser = MdxParser(mddFile)
                    Log.i("DictMgr", "  MDD parsed: wordCount=${mddParser.wordCount} title='${mddParser.title}'")
                    if (mddParser.wordCount > 0) {
                        synchronized(this) {
                            loadedMdds.remove(entry.id)?.close()
                            loadedMdds[entry.id] = mddParser
                        }
                        Log.i("DictMgr", "  MDD loaded: '${mddFile.name}' resources=${mddParser.wordCount}")
                    } else {
                        Log.w("DictMgr", "  MDD '${mddFile.name}' has wordCount=0, treating as empty")
                        mddParser.close()
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e("DictMgr", "  OOM loading MDD '${mddFile.name}' (${mddFile.length()} bytes): ${e.message}")
                    System.gc()
                } catch (e: Exception) {
                    Log.e("DictMgr", "  Failed to load MDD '${mddFile.name}': ${e.javaClass.simpleName}: ${e.message}", e)
                }
            }

            Log.i("DictMgr", "  LOADED OK: '${entry.name}' → title='${parser.title}' words=${parser.wordCount} encoding='${parser.encoding}' file=${mdxFile.name}")
        } catch (e: Exception) {
            Log.e("DictMgr", "  FAILED to load ${entry.name}: ${e.javaClass.simpleName}: ${e.message}", e)
        } catch (e: OutOfMemoryError) {
            Log.e("DictMgr", "  OUT OF MEMORY loading ${entry.name}: ${e.message}")
            System.gc()
        }
    }

    fun removeDictionary(id: Long) {
        synchronized(this) {
            loadedDicts.remove(id)?.close()
            loadedMdds.remove(id)?.close()
            cssCache.remove(id)
            dictionaries.removeAll { it.id == id }
        }
        val dictDir = File(context.filesDir, "dictionaries/$id")
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

    fun getParserForDictionary(id: Long): MdxParser? = synchronized(this) { loadedDicts[id] }

    fun getCssFromMdd(dictId: Long): String {
        val mddParser = synchronized(this) { loadedMdds[dictId] } ?: return ""
        return searchEngine.getCssFromMdd(dictId, mapOf(dictId to mddParser))
    }

    @WorkerThread
    fun searchWord(query: String): List<SearchResult> {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val dictsSnapshot = synchronized(this) { loadedDicts.toMap() }
        val mddsSnapshot = synchronized(this) { loadedMdds.toMap() }
        return searchEngine.searchWord(query, snapshot, dictsSnapshot, cssCache, mddsSnapshot).map {
            SearchResult(it.word, it.definition, it.dictionaryName, it.css)
        }
    }

    @WorkerThread
    fun getAudioResource(word: String): ByteArray? {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val mddsSnapshot = synchronized(this) { loadedMdds.toMap() }
        return searchEngine.getAudioResource(word, snapshot, mddsSnapshot)
    }

    @WorkerThread
    fun getAudioResourceByPath(path: String): ByteArray? {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val mddsSnapshot = synchronized(this) { loadedMdds.toMap() }
        return searchEngine.getAudioResourceByPath(path, snapshot, mddsSnapshot)
    }

    @WorkerThread
    fun getRandomWords(count: Int = 5): List<Pair<String, String>> {
        val snapshot = synchronized(this) { dictionaries.filter { it.isEnabled }.toList() }
        val dictsSnapshot = synchronized(this) { loadedDicts.toMap() }
        return searchEngine.getRandomWords(count, snapshot, dictsSnapshot)
    }

    fun scanDirectory(uri: Uri): List<DictFileImporter.DictCandidate> {
        return fileImporter.scanDirectory(uri)
    }

    fun diagnoseAllDictionaries(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Gdict Dictionary Diagnostics ===")
        sb.appendLine("Total dicts: ${dictionaries.size}, Loaded parsers: ${loadedDicts.size}")
        sb.appendLine()

        synchronized(this) {
            for ((i, dict) in dictionaries.withIndex()) {
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

                try {
                    val uri2 = android.net.Uri.parse(dict.path)
                    if (dict.path.startsWith("content://") && DocumentsContract.isTreeUri(uri2)) {
                        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                            uri2,
                            DocumentsContract.getTreeDocumentId(uri2)
                        )
                        val cursor = context.contentResolver.query(
                            childrenUri,
                            arrayOf(
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                DocumentsContract.Document.COLUMN_SIZE
                            ),
                            null,
                            null,
                            null
                        )
                        val fileList = mutableListOf<String>()
                        cursor?.use {
                            while (it.moveToNext()) {
                                val name = it.getString(1)
                                val size = it.getLong(2)
                                if (name != null) {
                                    fileList.add("$name($size)")
                                }
                            }
                        }
                        sb.appendLine("  Files in original dir (${fileList.size}): ${fileList.joinToString()}")
                    }
                } catch (e: Exception) {
                    sb.appendLine("  Original dir list failed: ${e.message}")
                }

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
                            if (def != null && def.length <= 2000) {
                                val rawEscaped = def
                                    .replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")
                                    .replace("\n", "\\n")
                                    .replace("\r", "\\r")
                                sb.appendLine("    RAW(first500): '${rawEscaped.take(500)}'")
                            } else if (def != null) {
                                val rawEscaped = def.take(500)
                                    .replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")
                                    .replace("\n", "\\n")
                                sb.appendLine("    RAW(first500): '${rawEscaped}' ... (truncated, total=${def.length})")
                            }
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
                    val mdxPath = dict.dictFilePath
                    val mdxFile = File(mdxPath)
                    val parentDir = mdxFile.parentFile
                    sb.appendLine("  MDX path: $mdxPath")
                    sb.appendLine("  MDX exists: ${mdxFile.exists()} (${mdxFile.length()} bytes)")
                    if (parentDir != null && parentDir.exists()) {
                        sb.appendLine("  Files in dir: ${parentDir.listFiles()?.map { "${it.name} (${it.length()}" }?.joinToString(", ") ?: "empty"}")
                        val baseName = mdxFile.nameWithoutExtension
                        val candidates = listOf(
                            File(parentDir, "$baseName.mdd"),
                            File(parentDir, "${mdxFile.name}.mdd")
                        )
                        for (cand in candidates) {
                            sb.appendLine("  MDD candidate '${cand.name}': exists=${cand.exists()}, size=${if (cand.exists()) cand.length() else "N/A"}")
                            if (cand.exists() && cand.length() > 0) {
                                try {
                                    sb.appendLine("  Attempting to load MDD...")
                                    val testMdd = MdxParser(cand)
                                    sb.appendLine("  MDD parsed: wordCount=${testMdd.wordCount}, title='${testMdd.title}', resourceMode=${testMdd.isResourceMode}")
                                    sb.appendLine("  ${testMdd.diagnose().replace("\n", "\n  ")}")
                                    if (testMdd.wordCount > 0) {
                                        val sampleKeys = testMdd.findResourceKeys(".png").take(3)
                                        sb.appendLine("  Sample PNG keys: $sampleKeys")
                                        for (key in sampleKeys) {
                                            val data = testMdd.readResourceBytesByKey(key)
                                            sb.appendLine("    '$key' → ${data?.size ?: 0} bytes")
                                        }
                                    } else if (testMdd.isResourceMode) {
                                        sb.appendLine("  Stream mode active, testing resource lookup...")
                                        val testPaths = listOf("\\cepd18.css", "\\95D5C9C1.png")
                                        for (tp in testPaths) {
                                            val rd = testMdd.readResourceBytes(tp)
                                            sb.appendLine("    '$tp' → ${rd?.size ?: "not found"} bytes")
                                        }
                                        val pngKeys = testMdd.findResourceKeys(".css").take(5)
                                        sb.appendLine("  CSS keys found: $pngKeys")
                                        val pngKeys2 = testMdd.findResourceKeys(".png").take(5)
                                        sb.appendLine("  PNG keys found: $pngKeys2")
                                    }
                                    testMdd.close()
                                } catch (e: OutOfMemoryError) {
                                    sb.appendLine("  OOM loading MDD (${cand.length()} bytes): ${e.message}")
                                } catch (e: Exception) {
                                    sb.appendLine("  Error loading MDD: ${e.javaClass.simpleName}: ${e.message}")
                                }
                            }
                        }
                    }
                } else {
                    sb.appendLine("  MDD loaded: ${mddParser.wordCount} resources, resourceMode=${mddParser.isResourceMode}")
                    val imageKeys = mddParser.findResourceKeys(".png").take(5)
                    if (imageKeys.isNotEmpty()) {
                        sb.appendLine("  Sample images: $imageKeys")
                        for (key in imageKeys.take(3)) {
                            val data = mddParser.readResourceBytesByKey(key)
                            sb.appendLine("    '$key' → ${data?.size ?: 0} bytes")
                        }
                    } else if (mddParser.isResourceMode) {
                        sb.appendLine("  Stream mode: testing resource lookup...")
                        val testPaths = listOf("\\cepd18.css", "\\95D5C9C1.png", "\\images\\test.png")
                        for (tp in testPaths) {
                            val rd = mddParser.readResourceBytes(tp)
                            sb.appendLine("    '$tp' → ${rd?.size ?: "not found"} bytes")
                        }
                    } else {
                        sb.appendLine("  No .png resources found in MDD")
                    }
                }
                sb.appendLine()

                val cssKeys = mddParser?.findResourceKeys(".css") ?: emptyList()
                sb.appendLine("  CSS keys in MDD (endsWith .css): ${cssKeys.size} → $cssKeys")

                if (mddParser != null) {
                    val cssLikeKeys = mddParser.findResourceKeys("css").filter { !it.lowercase().endsWith(".css") }
                    sb.appendLine("  CSS-like keys (contains 'css'): ${cssLikeKeys.size} → ${cssLikeKeys.take(10)}")
                    val sampleKeys = mddParser.getAllKeywords().take(10)
                    sb.appendLine("  Sample MDD keywords: $sampleKeys")
                }

                val cssContent = getCssFromMdd(dict.id)
                val cssLen = cssContent.length
                sb.appendLine("  CSS from MDD: ${if (cssLen > 0) "$cssLen chars" else "EMPTY!"}")
                if (cssLen > 0 && cssLen <= 500) {
                    sb.appendLine("     Preview: ${cssContent.take(200).replace("\n", "\\n")}")
                } else if (cssLen > 500) {
                    sb.appendLine("     First 200 chars: ${cssContent.take(200).replace("\n", "\\n")}")
                }

                val companionCss = parser?.companionCss ?: ""
                val compLen = companionCss.length
                sb.appendLine("  companionCss: ${if (compLen > 0) "$compLen chars" else "EMPTY!"}")
                if (compLen > 0 && compLen <= 500) {
                    sb.appendLine("     Preview: ${companionCss.take(200).replace("\n", "\\n")}")
                } else if (compLen > 500) {
                    sb.appendLine("     First 200 chars: ${companionCss.take(200).replace("\n", "\\n")}")
                }

                try {
                    val testWords = listOf("hello", "test", "the", "a", "bye")
                    for (word in testWords) {
                        val articles = parser.readArticles(word)
                        if (articles.isNotEmpty()) {
                            for ((w, def) in articles) {
                                if (def == null || def.isBlank()) continue
                                sb.appendLine("  Test word: '$w'")
                                val hasIpa = def.contains("<ipa>", ignoreCase = true)
                                val hasImg = Regex("""<img[^>]+src=["']([^"']+)["']""").containsMatchIn(def)
                                val cssLinks = Regex("""<link[^>]+href=["']([^"']+\.css)["']""").findAll(def).map { it.groupValues[1] }.distinct().toList()
                                sb.appendLine("     - Has <ipa>: $hasIpa | Has <img>: $hasImg | CSS links: $cssLinks")
                                val imgSrcs = Regex("""<img[^>]+src=["']([^"']+)["']""").findAll(def).map { it.groupValues[1] }.distinct().toList()
                                sb.appendLine("     - All img srcs: $imgSrcs")
                                val soundHrefs = Regex("""href=["']sound://([^"']+)["']""").findAll(def).map { it.groupValues[1] }.distinct().toList()
                                sb.appendLine("     - All sound:// hrefs: $soundHrefs")
                                val customTags = Regex("""<([a-zA-Z][a-zA-Z0-9_-]*)[^>]*>""").findAll(def).map { it.groupValues[1].lowercase() }.distinct().sorted().toList()
                                sb.appendLine("     - All custom tags: $customTags")
                                sb.appendLine("     - FULL HTML:")
                                sb.appendLine(def)
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

        sb.appendLine("=== transformMdxTags Coverage Check ===")
        val knownTags = listOf(
            "<SEP>", "<hw>", "<inf>", "<ex>", "<hit>",
            "<ipa>", "<prongrp>", "<inflection>", "<capvar>",
            "<sense-block>", "<sense-body>", "<sense-head>",
            "<di-head>", "<di-title>", "<di-body>", "<di-info>",
            "<arl>", "<base>", "<results>", "<forms>", "<inflections>",
            "<pron>", "<ussymbol>", "<soundfile>"
        )
        sb.appendLine("Tags handled by transformMdxTags: ${knownTags.size}")
        for (tag in knownTags) {
            sb.appendLine("  $tag")
        }
        return sb.toString()
    }
}
