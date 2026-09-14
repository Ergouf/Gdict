package io.github.gdict.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gdict.R
import io.github.gdict.core.model.HistoryItem
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.util.HtmlUtils
import io.github.gdict.viewmodel.SearchViewModel
import io.github.gdict.viewmodel.SettingsViewModel

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onWordClick: (word: String, definition: String, dictionaryName: String, css: String) -> Unit = { _, _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by searchViewModel.searchResults.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by searchViewModel.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val suggestions by searchViewModel.suggestions.collectAsStateWithLifecycle(initialValue = emptyList())
    val errorMessage by searchViewModel.errorMessage.collectAsStateWithLifecycle(initialValue = null)
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val wordOfTheDay by searchViewModel.wordOfTheDay.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        if (wordOfTheDay.isEmpty()) searchViewModel.loadWordOfTheDay()
    }

    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val separator = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_search),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            SearchBar(
                query = searchQuery,
                darkMode = darkMode,
                onQueryChange = {
                    searchQuery = it
                    searchViewModel.onSearchQueryChanged(it.trim())
                },
                onSearch = {
                    if (searchQuery.isNotBlank()) searchViewModel.searchWord(searchQuery.trim())
                }
            )
        }

        errorMessage?.let { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .border(0.5.dp, separator, RoundedCornerShape(14.dp))
                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GdictColors.CoralAccent,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = searchViewModel::clearError) {
                    Text(stringResource(R.string.close))
                }
            }
        }

        when {
            searchResults.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = searchResults,
                        key = { index, item -> "${item.word}_${item.dictionaryName}_$index" }
                    ) { _, result ->
                        SearchResultRow(
                            word = result.word,
                            definition = result.definition,
                            dictionaryName = result.dictionaryName,
                            darkMode = darkMode,
                            onClick = {
                                onWordClick(result.word, result.definition, result.dictionaryName, result.css)
                            }
                        )
                    }
                }
            }

            searchQuery.isNotBlank() -> {
                EmptySearchResult(
                    query = searchQuery,
                    suggestions = suggestions,
                    darkMode = darkMode,
                    onSuggestionClick = { suggestion ->
                        searchQuery = suggestion
                        searchViewModel.searchWord(suggestion)
                    }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (history.isNotEmpty()) {
                        item {
                            RecentSearchSection(
                                history = history,
                                darkMode = darkMode,
                                onClear = searchViewModel::clearHistory,
                                onWordClick = { word ->
                                    searchQuery = word
                                    searchViewModel.searchWord(word)
                                }
                            )
                        }
                    }
                    item {
                        WordOfTheDaySection(
                            words = wordOfTheDay,
                            darkMode = darkMode,
                            onWordClick = { word ->
                                searchQuery = word
                                searchViewModel.searchWord(word)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    darkMode: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val background = if (darkMode) GdictColors.DarkGlassSurfaceStrong else GdictColors.GlassSurfaceStrong
    val idleBorder = if (darkMode) GdictColors.DarkGlassSeparator else GdictColors.GlassSeparator
    val border by animateColorAsState(
        targetValue = if (isFocused) GdictColors.Primary.copy(alpha = 0.55f) else idleBorder,
        animationSpec = tween(160),
        label = "searchBorder"
    )
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(1.dp, shape)
            .clip(shape)
            .background(background)
            .border(0.5.dp, border, shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(9.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = secondaryText
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    word: String,
    definition: String,
    dictionaryName: String,
    darkMode: Boolean,
    onClick: () -> Unit
) {
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val separator = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val preview = remember(definition) { HtmlUtils.stripHtmlForPreview(definition) }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(0.5.dp, separator, shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (dictionaryName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dictionaryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (preview.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    suggestions: List<String>,
    darkMode: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_results_for, query),
            style = MaterialTheme.typography.titleMedium,
            color = textColor
        )
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.did_you_mean),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            suggestions.take(5).forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = GdictColors.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentSearchSection(
    history: List<HistoryItem>,
    darkMode: Boolean,
    onClear: () -> Unit,
    onWordClick: (String) -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.clear))
            }
        }
        history.take(5).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onWordClick(item.word) }
                    .padding(horizontal = 4.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun WordOfTheDaySection(
    words: List<Pair<String, String>>,
    darkMode: Boolean,
    onWordClick: (String) -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.word_of_the_day),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val displayWords = if (words.isEmpty()) {
            listOf(
                stringResource(R.string.word_of_the_day_welcome) to stringResource(R.string.word_of_the_day_welcome_desc),
                stringResource(R.string.word_of_the_day_dictionary) to stringResource(R.string.word_of_the_day_dictionary_desc)
            )
        } else {
            words
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(displayWords) { (word, meaning) ->
                WordOfDayCard(
                    word = word,
                    meaning = meaning,
                    darkMode = darkMode,
                    onClick = { onWordClick(word) }
                )
            }
        }
    }
}

@Composable
private fun WordOfDayCard(
    word: String,
    meaning: String,
    darkMode: Boolean,
    onClick: () -> Unit
) {
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondaryText = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val separator = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(190.dp)
            .height(118.dp)
            .clip(shape)
            .background(surface)
            .border(0.5.dp, separator, shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = meaning,
            style = MaterialTheme.typography.bodySmall,
            color = secondaryText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
