package io.github.gdict.ui

import android.net.Uri
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.github.gdict.ui.screens.BookmarksScreen
import io.github.gdict.ui.screens.DictionariesScreen
import io.github.gdict.ui.screens.HistoryScreen
import io.github.gdict.ui.screens.SearchScreen
import io.github.gdict.ui.screens.SettingsScreen
import io.github.gdict.ui.screens.WordDetailScreen
import io.github.gdict.ui.theme.GdictTheme
import io.github.gdict.viewmodel.AppViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Search : Screen("search", "搜索", Icons.Filled.Search, Icons.Outlined.Search)
    object Bookmarks : Screen("bookmarks", "生词本", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
    object History : Screen("history", "历史", Icons.Filled.History, Icons.Outlined.History)
    object Settings : Screen("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun GdictApp(
    viewModel: AppViewModel = viewModel()
) {
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)

    GdictTheme(darkTheme = darkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            GdictAppContent(viewModel = viewModel)
        }
    }
}

@Composable
private fun GdictAppContent(
    viewModel: AppViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = listOf(
        Screen.Search,
        Screen.Bookmarks,
        Screen.History,
        Screen.Settings
    )

    val isDetailPage = currentDestination?.route?.startsWith("word_detail/") == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isDetailPage) {
                GdictBottomBar(
                    screens = screens,
                    currentDestination = currentDestination,
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
                    viewModel = viewModel,
                    onWordClick = { word, definition, dictName, _ ->
                        val encodedDef = Uri.encode(definition)
                        val encodedDict = Uri.encode(dictName)
                        navController.navigate("word_detail/$word/$encodedDef/$encodedDict")
                    }
                )
            }
            composable(Screen.Bookmarks.route) {
                BookmarksScreen(
                    viewModel = viewModel,
                    onWordClick = { word, definition, dictName, _ ->
                        val encodedDef = Uri.encode(definition)
                        val encodedDict = Uri.encode(dictName)
                        navController.navigate("word_detail/$word/$encodedDef/$encodedDict")
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onWordClick = { word ->
                        viewModel.searchWord(word)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToDictionaries = { navController.navigate("dictionaries") }
                )
            }
            composable("dictionaries") {
                DictionariesScreen(viewModel = viewModel)
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
                val isBookmarked = viewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList()).value.any { it.word == word }
                val css = viewModel.getCssForDictionary(dictionaryName)

                WordDetailScreen(
                    word = word,
                    definition = definition,
                    dictionaryName = dictionaryName,
                    css = css,
                    isBookmarked = isBookmarked,
                    onBack = { navController.popBackStack() },
                    onToggleBookmark = { viewModel.toggleBookmark(word, definition, dictionaryName) }
                )
            }
        }
    }
}

@Composable
fun GdictBottomBar(
    screens: List<Screen>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    GdictBottomNavItem(
                        screen = screen,
                        isSelected = isSelected,
                        onClick = { onNavigate(screen) }
                    )
                }
            }
        }
    }
}

@Composable
fun GdictBottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = screen.title,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        if (isSelected) {
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
        }
    }
}
