package io.github.gdict.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.ui.components.CollapsibleSidebar
import io.github.gdict.ui.components.SidebarItem
import io.github.gdict.ui.screens.BookmarksScreen
import io.github.gdict.ui.screens.DictionariesScreen
import io.github.gdict.ui.screens.FlashcardScreen
import io.github.gdict.ui.screens.SearchScreen
import io.github.gdict.ui.screens.SettingsScreen
import io.github.gdict.ui.screens.WordDetailScreen
import io.github.gdict.ui.strings.StringResources
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.DictionaryViewModel
import io.github.gdict.viewmodel.FlashcardViewModel
import io.github.gdict.viewmodel.SearchViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import io.github.gdict.core.GdictLogger

data class WordDetailNavState(
    val word: String,
    val definition: String,
    val dictionaryName: String,
    val css: String
)

@Composable
fun DesktopApp(
    searchViewModel: SearchViewModel,
    bookmarkViewModel: BookmarkViewModel,
    dictionaryViewModel: DictionaryViewModel,
    flashcardViewModel: FlashcardViewModel,
    settingsViewModel: SettingsViewModel,
    dictionaryRepository: DictionaryRepository,
    strings: StringResources
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDictionaries by remember { mutableIntStateOf(0) }
    var showFlashcard by remember { mutableStateOf(false) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }
    var wordDetailState by remember { mutableStateOf<WordDetailNavState?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val sidebarItems = listOf(
        SidebarItem(label = strings.navSearch, icon = Icons.Outlined.Search, route = "search"),
        SidebarItem(label = strings.navFavorites, icon = Icons.Outlined.BookmarkBorder, route = "bookmarks"),
        SidebarItem(label = strings.navLearning, icon = Icons.Outlined.MenuBook, route = "learning"),
        SidebarItem(label = strings.dictionaries, icon = Icons.Outlined.Book, route = "dictionaries"),
        SidebarItem(label = strings.navProfile, icon = Icons.Outlined.Person, route = "settings"),
    )

    val effectiveSelectedIndex = when {
        showDictionaries == 1 -> 3
        showFlashcard -> 2
        else -> selectedTab
    }

    val onWordClick: (String, String, String, String) -> Unit = { word, definition, dictName, css ->
        GdictLogger.get().i("DesktopApp", "onWordClick: word='$word', defLen=${definition.length}, dict='$dictName', cssLen=${css.length}")
        wordDetailState = WordDetailNavState(word, definition, dictName, css)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CollapsibleSidebar(
            items = sidebarItems,
            selectedIndex = effectiveSelectedIndex,
            isCollapsed = isSidebarCollapsed,
            onItemSelected = { index ->
                showDictionaries = 0
                showFlashcard = false
                wordDetailState = null
                when (sidebarItems[index].route) {
                    "dictionaries" -> showDictionaries = 1
                    "learning" -> showFlashcard = true
                    else -> selectedTab = index
                }
            },
            onToggleCollapse = { isSidebarCollapsed = !isSidebarCollapsed },
            modifier = Modifier.fillMaxHeight()
        )

        VerticalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val bookmarks by bookmarkViewModel.bookmarks.collectAsState()
            val detailShown = wordDetailState != null

            if (detailShown) {
                WordDetailScreen(
                    word = wordDetailState?.word ?: "",
                    definition = wordDetailState?.definition ?: "",
                    dictionaryName = wordDetailState?.dictionaryName ?: "",
                    css = wordDetailState?.css ?: "",
                    isBookmarked = wordDetailState?.let { s -> bookmarks.any { it.word == s.word } } ?: false,
                    onBack = { wordDetailState = null },
                    onToggleBookmark = {
                        val state = wordDetailState
                        if (state != null) {
                            if (bookmarks.any { it.word == state.word }) {
                                val bookmarkItem = bookmarks.first { it.word == state.word }
                                bookmarkViewModel.removeBookmark(bookmarkItem)
                            } else {
                                bookmarkViewModel.addBookmark(
                                    state.word,
                                    state.definition,
                                    state.dictionaryName
                                )
                            }
                        }
                    },
                    onEntryClick = { entry ->
                        GdictLogger.get().i("DesktopApp", "onEntryClick: entry='$entry'")
                        val currentDict = wordDetailState?.dictionaryName
                        coroutineScope.launch {
                            try {
                                val searchResults = searchViewModel.searchWordForResult(entry)
                                GdictLogger.get().i("DesktopApp", "onEntryClick: got ${searchResults.size} results for '$entry', currentDict='$currentDict'")
                                if (searchResults.isNotEmpty()) {
                                    val result = searchResults.find { it.dictionaryName == currentDict }
                                        ?: searchResults.first()
                                    wordDetailState = WordDetailNavState(
                                        result.word,
                                        result.definition,
                                        result.dictionaryName,
                                        result.css
                                    )
                                }
                            } catch (e: Throwable) {
                                GdictLogger.get().e("DesktopApp", "onEntryClick failed: ${e.javaClass.simpleName}: ${e.message}")
                            }
                        }
                    },
                    dictionaryRepository = dictionaryRepository,
                    settingsViewModel = settingsViewModel,
                    webViewVisible = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (showFlashcard) {
                FlashcardScreen(
                    flashcardViewModel = flashcardViewModel,
                    settingsViewModel = settingsViewModel,
                    bookmarkViewModel = bookmarkViewModel,
                    onBack = { showFlashcard = false }
                )
            } else if (showDictionaries == 1) {
                DictionariesScreen(
                    dictionaryViewModel = dictionaryViewModel,
                    settingsViewModel = settingsViewModel,
                    strings = strings,
                    onBack = { showDictionaries = 0 },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                when (selectedTab) {
                    0 -> SearchScreen(
                        searchViewModel = searchViewModel,
                        settingsViewModel = settingsViewModel,
                        onWordClick = onWordClick,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> BookmarksScreen(
                        bookmarkViewModel = bookmarkViewModel,
                        settingsViewModel = settingsViewModel,
                        onWordClick = onWordClick,
                        modifier = Modifier.fillMaxSize()
                    )
                    4 -> SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        strings = strings,
                        onNavigateToDictionaries = { showDictionaries = 1 },
                        onNavigateToFlashcard = { showFlashcard = true },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> SearchScreen(
                        searchViewModel = searchViewModel,
                        settingsViewModel = settingsViewModel,
                        onWordClick = onWordClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
