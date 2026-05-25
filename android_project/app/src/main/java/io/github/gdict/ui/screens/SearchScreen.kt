package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gdict.core.model.HistoryItem
import io.github.gdict.core.model.SearchResultItem
import io.github.gdict.ui.theme.GdictColors
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
    val errorMessage by searchViewModel.errorMessage.collectAsStateWithLifecycle(initialValue = null)
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val wordOfTheDay by searchViewModel.wordOfTheDay.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        if (wordOfTheDay.isEmpty()) {
            searchViewModel.loadWordOfTheDay()
        }
    }

    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.LightGray
    val cardColor = if (darkMode) GdictColors.DarkSurface else Color.White
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.DarkGray

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GdictColors.NavyBlue)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    "Home & Search",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                SearchBar(
                    query = searchQuery,
                    cardColor = cardColor,
                    onQueryChange = {
                        searchQuery = it
                        searchViewModel.onSearchQueryChanged(it.trim())
                    },
                    onSearch = {
                        if (searchQuery.isNotEmpty()) {
                            searchViewModel.searchWord(searchQuery)
                        }
                    }
                )
            }
        }

        errorMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GdictColors.CoralAccent.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GdictColors.CoralAccent,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.TextButton(onClick = { searchViewModel.clearError() }) {
                        Text("Dismiss", color = GdictColors.MediumGray)
                    }
                }
            }
        }

        if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                items(items = searchResults, key = { "${it.word}_${it.dictionaryName}" }) { result ->
                    SearchResultCard(
                        word = result.word,
                        definition = result.definition,
                        dictionaryName = result.dictionaryName,
                        cardColor = cardColor,
                        textColor = textColor,
                        onClick = { onWordClick(result.word, result.definition, result.dictionaryName, result.css) }
                    )
                }
            }
        } else if (searchQuery.isNotEmpty()) {
            val suggestions by searchViewModel.suggestions.collectAsStateWithLifecycle(initialValue = emptyList())
            EmptySearchResult(
                query = searchQuery,
                suggestions = suggestions,
                textColor = textColor,
                onSuggestionClick = { suggestion ->
                    searchQuery = suggestion
                    searchViewModel.searchWord(suggestion)
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (history.isNotEmpty()) {
                    item {
                        RecentSearchSection(
                            history = history,
                            textColor = textColor,
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
                        textColor = textColor,
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

@Composable
private fun SearchBar(
    query: String,
    cardColor: Color,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "搜索",
            tint = GdictColors.MediumGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Search",
                        color = GdictColors.MediumGray,
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "清除",
                    tint = GdictColors.MediumGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    word: String,
    definition: String,
    dictionaryName: String,
    cardColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GdictColors.NavyBlue
                    )
                    if (dictionaryName.isNotEmpty()) {
                        Text(
                            text = dictionaryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = GdictColors.MediumGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            if (definition.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentSearchSection(
    history: List<HistoryItem>,
    textColor: Color,
    onWordClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "Recent searches",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        history.take(5).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onWordClick(item.word) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = GdictColors.MediumGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun WordOfTheDaySection(
    words: List<Pair<String, String>>,
    textColor: Color,
    onWordClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            "Word of the Day",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (words.isEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listOf(
                    Pair("Welcome", "Tap to explore"),
                    Pair("Dictionary", "Add dictionaries to start")
                )) { (word, meaning) ->
                    WordOfDayCard(
                        word = word,
                        meaning = meaning,
                        onClick = { onWordClick(word) }
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(words) { (word, dictName) ->
                    WordOfDayCard(
                        word = word,
                        meaning = dictName,
                        onClick = { onWordClick(word) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordOfDayCard(
    word: String,
    meaning: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GdictColors.NavyBlue
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    suggestions: List<String>,
    textColor: Color,
    onSuggestionClick: (String) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "No exact match found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                "Did you mean?",
                style = MaterialTheme.typography.bodyMedium,
                color = GdictColors.MediumGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = suggestion
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GdictColors.LightGray)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = GdictColors.DarkGray
        )
    }
}

