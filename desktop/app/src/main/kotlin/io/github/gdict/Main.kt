package io.github.gdict

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.gdict.core.DesktopLogger
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictPersistence
import io.github.gdict.core.DictionaryManager
import io.github.gdict.core.GdictLogger
import io.github.gdict.data.DesktopBookmarkRepository
import io.github.gdict.data.DesktopDictionaryRepository
import io.github.gdict.data.DesktopHistoryRepository
import io.github.gdict.data.DesktopSettingsRepository
import io.github.gdict.data.JsonFileStorageBackend
import io.github.gdict.ui.DesktopApp
import io.github.gdict.ui.webview.preInitCef
import io.github.gdict.ui.webview.preCreateBrowserPanel
import io.github.gdict.ui.AppLanguage
import io.github.gdict.ui.getStringResourcesForLanguage
import io.github.gdict.ui.resolveEffectiveLanguage
import io.github.gdict.ui.theme.GdictTheme
import io.github.gdict.tts.TtsManager
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.DictionaryViewModel
import io.github.gdict.viewmodel.FlashcardViewModel
import io.github.gdict.viewmodel.SearchViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.io.File

fun main() = application {
    GdictLogger.setLogger(DesktopLogger())
    val log = GdictLogger.get()

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        log.e("UncaughtException", "Uncaught exception in thread '${thread.name}'", throwable)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        try { io.github.gdict.ui.webview.shutdownBrowser() } catch (_: Throwable) {}
    })

    val dataDir = File(System.getProperty("user.home"), ".gdict")
    dataDir.mkdirs()

    val storage = JsonFileStorageBackend(dataDir)

    val persistenceBackend = object : io.github.gdict.core.PersistenceBackend {
        private val dictFile = File(dataDir, "dictionaries.json")
        override fun loadDictionaries(): String? {
            return if (dictFile.exists()) dictFile.readText(Charsets.UTF_8) else null
        }
        override fun saveDictionaries(json: String) {
            dictFile.writeText(json, Charsets.UTF_8)
        }
    }

    val fileSystemAccess = object : io.github.gdict.core.FileSystemAccess {
        override fun selectDictionaryFiles(): List<String>? {
            val chooser = javax.swing.JFileChooser()
            chooser.dialogTitle = "Select MDX Dictionary Files"
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("MDX Dictionary Files", "mdx")
            chooser.isMultiSelectionEnabled = true
            val result = chooser.showOpenDialog(null)
            return if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                chooser.selectedFiles.map { it.absolutePath }
            } else null
        }

        override fun selectDictionaryDirectory(): String? {
            val chooser = javax.swing.JFileChooser()
            chooser.dialogTitle = "Select Dictionary Directory"
            chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
            val result = chooser.showOpenDialog(null)
            return if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile.absolutePath
            } else null
        }

        override fun listFilesInDirectory(dirPath: String): List<String> {
            return File(dirPath).listFiles()?.map { it.absolutePath } ?: emptyList()
        }
    }

    val fileImporter = DictFileImporter(fileSystemAccess)
    val persistence = DictPersistence(dataDir, persistenceBackend)

    val dictionaryManager = try {
        DictionaryManager(dataDir, persistence, fileImporter)
    } catch (e: Throwable) {
        log.e("Main", "DictionaryManager init failed: ${e.javaClass.simpleName}: ${e.message}", e)
        DictionaryManager(dataDir, persistence, fileImporter)
    }

    val dictionaryRepo = DesktopDictionaryRepository(dictionaryManager)
    val bookmarkRepo = DesktopBookmarkRepository(storage)
    val historyRepo = DesktopHistoryRepository(storage)
    val settingsRepo = DesktopSettingsRepository(storage)

    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val searchViewModel = SearchViewModel(dictionaryRepo, historyRepo, coroutineScope)
    val bookmarkViewModel = BookmarkViewModel(bookmarkRepo, coroutineScope)
    val dictionaryViewModel = DictionaryViewModel(dictionaryRepo, coroutineScope)
    val flashcardViewModel = FlashcardViewModel(bookmarkRepo)
    val settingsViewModel = SettingsViewModel(settingsRepo, historyRepo, bookmarkRepo)

    TtsManager.setAudioPlayer { audioData ->
        io.github.gdict.ui.webview.DesktopAudioPlayer.play(audioData)
    }

    preInitCef()
    coroutineScope.launch(Dispatchers.IO) {
        preCreateBrowserPanel()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Gdict Desktop",
        icon = painterResource("icon.png"),
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        window.minimumSize = Dimension(800, 600)
        val languageCode by settingsViewModel.language.collectAsState()
        val effectiveLanguage = resolveEffectiveLanguage(AppLanguage.fromCode(languageCode))
        val strings = getStringResourcesForLanguage(effectiveLanguage)

        GdictTheme {
            DesktopApp(
                searchViewModel = searchViewModel,
                bookmarkViewModel = bookmarkViewModel,
                dictionaryViewModel = dictionaryViewModel,
                flashcardViewModel = flashcardViewModel,
                settingsViewModel = settingsViewModel,
                dictionaryRepository = dictionaryRepo,
                strings = strings
            )
        }
    }
}
