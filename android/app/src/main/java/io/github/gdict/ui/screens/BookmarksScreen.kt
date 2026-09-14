package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gdict.R
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.SettingsViewModel

@Composable
fun BookmarksScreen(
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onWordClick: (word: String, definition: String, dictionaryName: String, css: String) -> Unit = { _, _, _, _ -> },
    onFlashcardClick: () -> Unit = {}
) {
    val bookmarks by bookmarkViewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    var bookmarkToDelete by remember { mutableStateOf<BookmarkItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.my_vocabulary),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (bookmarks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookmarks, key = { it.id }) { item ->
                    BookmarkItemRow(
                        item = item,
                        darkMode = darkMode,
                        onClick = { onWordClick(item.word, item.definition, item.dictionaryName, "") },
                        onDelete = { bookmarkToDelete = item }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlashcardPromoRow(darkMode = darkMode, onClick = onFlashcardClick)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (darkMode) GdictColors.DarkSurface else GdictColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.no_vocabulary_yet),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = stringResource(R.string.add_words_to_favorites_first),
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText
                    )
                }
            }
        }
    }

    bookmarkToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { bookmarkToDelete = null },
            title = { Text(stringResource(R.string.remove_bookmark), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.remove_bookmark_confirm, item.word)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookmarkViewModel.removeBookmark(item)
                        bookmarkToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.remove), color = GdictColors.CoralAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun BookmarkItemRow(
    item: BookmarkItem,
    darkMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val separator = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(0.5.dp, separator, shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            tint = GdictColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (item.dictionaryName.isNotBlank()) {
                Text(
                    text = item.dictionaryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_remove),
                tint = secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FlashcardPromoRow(
    darkMode: Boolean,
    onClick: () -> Unit
) {
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val separator = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(0.5.dp, separator, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            tint = GdictColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.flashcard),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = stringResource(R.string.flashcard_promo_desc),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}
