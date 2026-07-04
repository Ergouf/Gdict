package io.github.gdict.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.gdict.GdictApplication
import androidx.compose.ui.res.stringResource
import io.github.gdict.R
import io.github.gdict.ui.screens.BookmarksScreen
import io.github.gdict.ui.screens.DictionariesScreen
import io.github.gdict.ui.screens.FlashcardScreen
import io.github.gdict.ui.screens.SearchScreen
import io.github.gdict.ui.screens.SettingsScreen
import io.github.gdict.ui.screens.WordDetailScreen
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.theme.GdictTheme
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.DictionaryViewModel
import io.github.gdict.viewmodel.FlashcardViewModel
import io.github.gdict.viewmodel.SearchViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Search : Screen("search", R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search)
    object Bookmarks : Screen("bookmarks", R.string.nav_favorites, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
    object Learning : Screen("learning", R.string.nav_learning, Icons.Filled.School, Icons.Outlined.School)
    object Profile : Screen("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun GdictApp(
    settingsViewModel: SettingsViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    flashcardViewModel: FlashcardViewModel = viewModel(),
    dictionaryViewModel: DictionaryViewModel = viewModel()
) {
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)

    GdictTheme(darkTheme = darkMode) {
        // Acrylic: 顶层必须是透明，渐变背景由各 Screen 自己画在根 Column 上；
        // 没画渐变的页面由 Scaffold 的 containerColor 作浅色 fallback，避免透明露底。
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            GdictAppContent(
                settingsViewModel = settingsViewModel,
                searchViewModel = searchViewModel,
                bookmarkViewModel = bookmarkViewModel,
                flashcardViewModel = flashcardViewModel,
                dictionaryViewModel = dictionaryViewModel
            )
        }
    }
}

@Composable
private fun GdictAppContent(
    settingsViewModel: SettingsViewModel,
    searchViewModel: SearchViewModel,
    bookmarkViewModel: BookmarkViewModel,
    flashcardViewModel: FlashcardViewModel,
    dictionaryViewModel: DictionaryViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)

    val screens = listOf(
        Screen.Search,
        Screen.Bookmarks,
        Screen.Learning,
        Screen.Profile
    )

    val isDetailPage = currentDestination?.route?.startsWith("word_detail/") == true ||
                       currentDestination?.route?.startsWith("dictionaries") == true

    Scaffold(
        // containerColor 透明，让各 Screen 自己画渐变能透过来
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isDetailPage) {
                GdictBottomBar(
                    screens = screens,
                    currentDestination = currentDestination,
                    darkMode = darkMode,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) {
                SearchScreen(
                    searchViewModel = searchViewModel,
                    settingsViewModel = settingsViewModel,
                    onWordClick = { word, definition, dictName, _ ->
                        val encodedDef = Uri.encode(definition)
                        val encodedDict = Uri.encode(dictName)
                        navController.navigate("word_detail/$word/$encodedDef/$encodedDict")
                    }
                )
            }
            composable(Screen.Bookmarks.route) {
                BookmarksScreen(
                    bookmarkViewModel = bookmarkViewModel,
                    settingsViewModel = settingsViewModel,
                    onWordClick = { word, definition, dictName, _ ->
                        val encodedDef = Uri.encode(definition)
                        val encodedDict = Uri.encode(dictName)
                        navController.navigate("word_detail/$word/$encodedDef/$encodedDict")
                    },
                    onFlashcardClick = {
                        navController.navigate(Screen.Learning.route)
                    }
                )
            }
            composable(Screen.Learning.route) {
                FlashcardScreen(
                    flashcardViewModel = flashcardViewModel,
                    settingsViewModel = settingsViewModel,
                    bookmarkViewModel = bookmarkViewModel
                )
            }
            composable(Screen.Profile.route) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateToDictionaries = { navController.navigate("dictionaries") }
                )
            }
            composable("dictionaries") {
                DictionariesScreen(dictionaryViewModel = dictionaryViewModel, settingsViewModel = settingsViewModel)
            }
            composable(
                route = "word_detail/{word}/{definition}/{dictionaryName}",
                arguments = listOf(
                    navArgument("word") { type = NavType.StringType },
                    navArgument("definition") { type = NavType.StringType },
                    navArgument("dictionaryName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val word = backStackEntry.arguments?.getString("word") ?: ""
                val definition = Uri.decode(backStackEntry.arguments?.getString("definition") ?: "")
                val dictionaryName = Uri.decode(backStackEntry.arguments?.getString("dictionaryName") ?: "")
                val isBookmarked by bookmarkViewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
                val css = searchViewModel.getCssForDictionary(dictionaryName)
                val entryCoroutineScope = rememberCoroutineScope()

                WordDetailScreen(
                    word = word,
                    definition = definition,
                    dictionaryName = dictionaryName,
                    css = css,
                    isBookmarked = isBookmarked.any { it.word == word && it.dictionaryName == dictionaryName },
                    onBack = { navController.popBackStack() },
                    onToggleBookmark = { bookmarkViewModel.toggleBookmark(word, definition, dictionaryName) },
                    onEntryClick = { entryWord ->
                        entryCoroutineScope.launch {
                            val results = searchViewModel.searchWordForResult(entryWord)
                            val result = results.find { it.dictionaryName == dictionaryName }
                                ?: results.firstOrNull()
                            if (result != null) {
                                val encodedDef = Uri.encode(result.definition)
                                val encodedDict = Uri.encode(result.dictionaryName)
                                navController.navigate("word_detail/${Uri.encode(entryWord)}/$encodedDef/$encodedDict")
                            }
                        }
                    },
                    dictionaryRepository = (LocalContext.current.applicationContext as GdictApplication).dictionaryRepository,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun GdictBottomBar(
    screens: List<Screen>,
    currentDestination: androidx.navigation.NavDestination?,
    darkMode: Boolean,
    onNavigate: (Screen) -> Unit
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    // 底栏区域背景：与各页面渐变底部一致，遮住窗口白色背景（主题为 Material.Light，
    // 窗口始终白底，浮动导航栏周围的透明区会露出白色，夜间模式下尤其突兀）。
    val barAreaGradient = if (darkMode) {
        Brush.verticalGradient(
            0.0f to GdictColors.DarkBackground,
            1.0f to GdictColors.DarkSurfaceVariant
        )
    } else {
        Brush.verticalGradient(
            0.0f to GdictColors.Background,
            1.0f to Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(barAreaGradient)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(4.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(32.dp))
                .background(glassBg)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                GdictBottomNavItem(
                    screen = screen,
                    isSelected = isSelected,
                    darkMode = darkMode,
                    onClick = { onNavigate(screen) }
                )
            }
        }
    }
}

@Composable
fun GdictBottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    darkMode: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val capsuleBg = if (isSelected) GdictColors.BluePrimaryLight.copy(alpha = 0.35f) else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(capsuleBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = stringResource(screen.titleResId),
                tint = if (isSelected) GdictColors.Primary else GdictColors.OnSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = stringResource(screen.titleResId),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) GdictColors.Primary else GdictColors.OnSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
