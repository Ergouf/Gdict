package io.github.gdict.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictPersistence
import io.github.gdict.core.DictionaryManager
import io.github.gdict.core.model.Dictionary
import io.github.gdict.core.model.SearchResultItem
import io.github.gdict.platform.AndroidPersistenceBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class AndroidDictionaryRepository(private val context: Context) {

    private val persistenceBackend = AndroidPersistenceBackend(context)
    private val persistence = DictPersistence(File(context.filesDir, "dictionaries"), persistenceBackend)
    private val fileImporter = DictFileImporter(object : io.github.gdict.core.FileSystemAccess {
        override fun selectDictionaryFiles(): List<String>? = null
        override fun selectDictionaryDirectory(): String? = null
        override fun listFilesInDirectory(dirPath: String): List<String> =
            File(dirPath).listFiles()?.map { it.absolutePath } ?: emptyList()
    })
    private val dictionaryManager = DictionaryManager(File(context.filesDir, "dictionaries"), persistence, fileImporter)

    private val _dictionaries = MutableStateFlow<List<Dictionary>>(emptyList())
    val dictionaries: StateFlow<List<Dictionary>> = _dictionaries.asStateFlow()

    init {
        _dictionaries.value = dictionaryManager.getDictionaries().map { entry ->
            Dictionary(id = entry.id, name = entry.name, path = entry.path, isEnabled = entry.isEnabled)
        }
    }

    fun scanSafDirectory(treeUri: Uri): List<DictFileImporter.DictCandidate> {
        android.util.Log.i("DictRepo", "scanSafDirectory: treeUri=$treeUri")

        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            android.util.Log.i("DictRepo", "takePersistableUriPermission OK")
        } catch (e: Exception) {
            android.util.Log.w("DictRepo", "takePersistableUriPermission failed: ${e.message}")
        }

        val candidates = mutableListOf<DictFileImporter.DictCandidate>()
        val filesByBaseName = mutableMapOf<String, MutableList<Pair<String, String>>>()

        val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (treeDoc == null) {
            android.util.Log.e("DictRepo", "DocumentFile.fromTreeUri returned null!")
            return candidates
        }
        if (!treeDoc.isDirectory) {
            android.util.Log.e("DictRepo", "treeDoc is not a directory! exists=${treeDoc.exists()}, canRead=${treeDoc.canRead()}")
            return candidates
        }

        val children = treeDoc.listFiles()
        android.util.Log.i("DictRepo", "listFiles returned ${children.size} entries")

        for (doc in children) {
            val displayName = doc.name ?: continue
            android.util.Log.d("DictRepo", "  found: name='$displayName' isFile=${doc.isFile} isDir=${doc.isDirectory}")

            if (!doc.isFile) continue
            if (!fileImporter.isDictionaryFile(displayName)) {
                android.util.Log.d("DictRepo", "    skipped (not dict file)")
                continue
            }

            android.util.Log.i("DictRepo", "    MATCH: '$displayName' -> ${doc.uri}")
            val lowerName = displayName.lowercase()
            val suffixLen = when {
                lowerName.endsWith(".mdx") -> 4
                lowerName.endsWith(".mdd") -> 4
                lowerName.endsWith(".dsl") -> 4
                lowerName.endsWith(".bgl") -> 4
                lowerName.endsWith(".lsa") -> 4
                lowerName.endsWith(".slob") -> 5
                lowerName.endsWith(".css") -> 4
                else -> 0
            }
            val baseName = displayName.dropLast(suffixLen)
            filesByBaseName.getOrPut(baseName) { mutableListOf() }
                .add(displayName to doc.uri.toString())
        }

        android.util.Log.i("DictRepo", "Grouped into ${filesByBaseName.size} base names: ${filesByBaseName.keys}")

        for ((baseName, files) in filesByBaseName) {
            val mdxFile = files.firstOrNull { it.first.lowercase().endsWith(".mdx") }
            if (mdxFile != null) {
                candidates.add(DictFileImporter.DictCandidate(
                    name = baseName.ifEmpty { mdxFile.first.removeSuffix(".mdx") },
                    filePath = mdxFile.second,
                    displayName = mdxFile.first,
                    companionFiles = files.filter { it !== mdxFile }.map { it.second }
                ))
                android.util.Log.i("DictRepo", "Candidate: name='$baseName' mdx='${mdxFile.first}' companions=${files.size - 1}")
            }
        }

        android.util.Log.i("DictRepo", "scanSafDirectory result: ${candidates.size} candidates")
        return candidates
    }

    suspend fun searchWord(word: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        dictionaryManager.searchWord(word).map { result ->
            SearchResultItem(word = result.word, definition = result.definition, dictionaryName = result.dictionaryName, css = result.css)
        }
    }

    fun scanDirectory(path: String): List<DictFileImporter.DictCandidate> {
        return dictionaryManager.scanDirectory(path)
    }

    suspend fun addDictionary(name: String, path: String, companionFiles: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        val resolvedPath = resolveSafUriToFile(path)
        val resolvedCompanions = companionFiles.map { resolveSafUriToFile(it) }
        val entry = dictionaryManager.addOrUpdateDictionary(name, resolvedPath, resolvedCompanions)
        val newDict = Dictionary(id = entry.id, name = entry.name, path = entry.path)
        _dictionaries.value = _dictionaries.value + newDict
    }

    private fun resolveSafUriToFile(uriString: String): String {
        if (!uriString.startsWith("content://")) return uriString
        val uri = Uri.parse(uriString)
        val fileName = getFileNameFromUri(uri) ?: "unknown"
        val tempFile = File(context.cacheDir, "saf_import_${System.currentTimeMillis()}_$fileName")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("DictRepo", "resolveSafUriToFile failed for $uriString: ${e.message}", e)
            tempFile.delete()
            return uriString
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
            }
        }
        if (name == null) {
            name = uri.lastPathSegment?.substringAfterLast("/")
        }
        return name
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
