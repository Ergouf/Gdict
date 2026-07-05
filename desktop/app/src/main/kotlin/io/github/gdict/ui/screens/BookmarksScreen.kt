@file:OptIn(ExperimentalComposeUiApi::class)

package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.strings.StringResources
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.SettingsViewModel

@Composable
fun BookmarksScreen(
    bookmarkViewModel: BookmarkViewModel,
    settingsViewModel: SettingsViewModel,
    strings: StringResources,
    onWordClick: (word: String, definition: String, dictionaryName: String, css: String) -> Unit = { _, _, _, _ -> },
    onFlashcardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bookmarks by bookmarkViewModel.bookmarks.collectAsState()
    val darkMode by settingsViewModel.darkMode.collectAsState()
    var bookmarkToDelete by remember { mutableStateOf<BookmarkItem?>(null) }

    val bgGradient = if (darkMode) {
        Brush.verticalGradient(
            0.0f to GdictColors.DarkBackground,
            1.0f to GdictColors.DarkSurfaceVariant
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0xFFDCEBFF),
            0.6f to Color(0xFFEDF4FF),
            1.0f to Color(0xFFFFFFFF)
        )
    }
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val titleColor = if (darkMode) GdictColors.DarkOnBackground else GdictColors.HeadingDark

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = with(density) { windowInfo.containerSize.width.toFloat() }
    val screenHeightPx = with(density) { windowInfo.containerSize.height.toFloat() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    strings.myVocabulary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }

            if (bookmarks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bookmarks, key = { it.id }) { item ->
                        BookmarkItemCard(
                            item = item,
                            darkMode = darkMode,
                            textColor = textColor,
                            subtitleColor = subtitleColor,
                            onClick = { onWordClick(item.word, item.definition, item.dictionaryName, "") },
                            onDelete = { bookmarkToDelete = item }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        FlashcardPromoCard(
                            strings = strings,
                            darkMode = darkMode,
                            onClick = onFlashcardClick
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(GdictColors.PrimarySoft.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = GdictColors.PrimarySoft.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            strings.noVocabularyYet,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            strings.addWordsToFavoritesFirst,
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtitleColor
                        )
                    }
                }
            }
        }
    }

    bookmarkToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { bookmarkToDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(strings.removeBookmark, fontWeight = FontWeight.Bold) },
            text = { Text(strings.removeBookmarkConfirm(item.word)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookmarkViewModel.removeBookmark(item)
                        bookmarkToDelete = null
                    }
                ) {
                    Text(strings.remove, color = GdictColors.CoralAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun BookmarkItemCard(
    item: BookmarkItem,
    darkMode: Boolean,
    textColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val iconContainerColor = GdictColors.Primary.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .background(glassBg)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = item.dictionaryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = subtitleColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FlashcardPromoCard(
    strings: StringResources,
    darkMode: Boolean,
    onClick: () -> Unit = {}
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val iconContainerColor = GdictColors.Primary.copy(alpha = 0.12f)
    val titleColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .background(glassBg)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings.flashcard,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    strings.flashcardReviewDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = GdictColors.Primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
