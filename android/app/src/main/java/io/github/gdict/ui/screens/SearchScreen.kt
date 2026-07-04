package io.github.gdict.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import io.github.gdict.R
import io.github.gdict.core.model.HistoryItem
import io.github.gdict.core.model.SearchResultItem
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.components.pageEnterAnimation
import io.github.gdict.ui.components.pressScale
import io.github.gdict.ui.components.staggerEnterAnimation
import io.github.gdict.ui.theme.GdictColors
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import io.github.gdict.viewmodel.SearchViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import io.github.gdict.util.HtmlUtils
import kotlin.math.roundToInt

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

    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val bgGradient = if (darkMode) {
        Brush.verticalGradient(
            0.0f to GdictColors.DarkBackground,
            1.0f to GdictColors.DarkSurfaceVariant
        )
    } else {
        // 蓝白渐变：顶部淡蓝 → 中下部缓慢过渡到白，蓝色更持久
        Brush.verticalGradient(
            0.0f to Color(0xFFDCEBFF),
            0.6f to Color(0xFFEDF4FF),
            1.0f to Color(0xFFFFFFFF)
        )
    }
    val cardColor = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val titleColor = if (darkMode) GdictColors.DarkOnBackground else GdictColors.HeadingDark

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
        Column(
            modifier = Modifier
            .fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .pageEnterAnimation()
        ) {
            Text(
                stringResource(R.string.nav_search),
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
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
                    if (searchQuery.isNotEmpty()) {
                        searchViewModel.searchWord(searchQuery)
                    }
                }
            )
        }

        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .border(0.5.dp, GdictColors.BlueHighlightBorder, RoundedCornerShape(18.dp))
                    .background(GdictColors.BlueSurfaceGlass)
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
                    TextButton(onClick = { searchViewModel.clearError() }) {
                        Text("Dismiss", color = subtitleColor)
                    }
                }
            }
        }

        if (searchResults.isNotEmpty()) {
            var reorderedResults by remember { mutableStateOf(searchResults) }
            var contentScale by remember { mutableFloatStateOf(1f) }

            LaunchedEffect(searchResults) {
                reorderedResults = searchResults
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            contentScale = (contentScale * zoom).coerceIn(0.7f, 2.0f)
                        }
                    }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    itemsIndexed(
                        items = reorderedResults,
                        key = { _, item -> "${item.word}_${item.dictionaryName}" }
                    ) { index, result ->
                        DraggableSearchResultCard(
                            word = result.word,
                            definition = result.definition,
                            dictionaryName = result.dictionaryName,
                            cardColor = cardColor,
                            textColor = textColor,
                            subtitleColor = subtitleColor,
                            contentScale = contentScale,
                            index = index,
                            totalItems = reorderedResults.size,
                            darkMode = darkMode,
                            onReorder = { fromIndex, toIndex ->
                                if (fromIndex != toIndex && fromIndex in reorderedResults.indices && toIndex in reorderedResults.indices) {
                                    val mutable = reorderedResults.toMutableList()
                                    val item = mutable.removeAt(fromIndex)
                                    mutable.add(toIndex, item)
                                    reorderedResults = mutable
                                }
                            },
                            onClick = { onWordClick(result.word, result.definition, result.dictionaryName, result.css) }
                        )
                    }
                }
            }
        } else if (searchQuery.isNotEmpty()) {
            val suggestions by searchViewModel.suggestions.collectAsStateWithLifecycle(initialValue = emptyList())
            EmptySearchResult(
                query = searchQuery,
                suggestions = suggestions,
                textColor = textColor,
                subtitleColor = subtitleColor,
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
                            subtitleColor = subtitleColor,
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
                        subtitleColor = subtitleColor,
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
    val searchBarBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val focusInteractionSource = remember { MutableInteractionSource() }
    val isFocused by focusInteractionSource.collectIsFocusedAsState()
    val focusedBorder by animateColorAsState(
        targetValue = if (isFocused) GdictColors.Primary.copy(alpha = 0.6f) else borderColor,
        animationSpec = tween(200),
        label = "focusBorder"
    )
    val focusedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.2.dp else 0.5.dp,
        animationSpec = tween(200),
        label = "focusBorderWidth"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(1.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(focusedBorderWidth, focusedBorder, RoundedCornerShape(28.dp))
            .background(searchBarBg)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = stringResource(R.string.search_hint),
            tint = Color(0xFF4A5568),
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
            interactionSource = focusInteractionSource,
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        stringResource(R.string.search_hint),
                        color = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.BluePlaceholder,
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotEmpty()) {
            val clearPressed = remember { MutableInteractionSource() }
            val clearIsPressed by clearPressed.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .pressScale(clearIsPressed, 0.95f)
                    .shadow(1.dp, androidx.compose.foundation.shape.CircleShape)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .border(0.5.dp, borderColor, androidx.compose.foundation.shape.CircleShape)
                    .background(searchBarBg)
                    .clickable(
                        interactionSource = clearPressed,
                        indication = null,
                        onClick = { onQueryChange("") }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DraggableSearchResultCard(
    word: String,
    definition: String,
    dictionaryName: String,
    cardColor: Color,
    textColor: Color,
    subtitleColor: Color,
    contentScale: Float,
    index: Int,
    totalItems: Int,
    darkMode: Boolean,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onClick: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    val pressInteractionSource = remember { MutableInteractionSource() }
    val isPressed by pressInteractionSource.collectIsPressedAsState()
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 1.dp,
        animationSpec = tween(200),
        label = "elevation"
    )

    val scaledHorizontalPadding = (18.dp * contentScale)
    val scaledVerticalPadding = (18.dp * contentScale)
    val scaledWordFontSize = (28.sp * contentScale)
    val scaledDictFontSize = (12.sp * contentScale)
    val scaledDefFontSize = (14.sp * contentScale)
    val scaledCornerRadius = (24.dp * contentScale).coerceIn(16.dp, 24.dp)
    val scaledDragIconSize = (20.dp * contentScale).coerceIn(14.dp, 28.dp)
    val scaledSpacing = (10.dp * contentScale)
    val scaledWordLineHeight = (34.sp * contentScale)
    val scaledDictLineHeight = (16.sp * contentScale)
    val scaledDefLineHeight = (21.sp * contentScale)
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val glassBorder = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .staggerEnterAnimation(index)
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .shadow(animatedElevation, RoundedCornerShape(scaledCornerRadius))
            .clip(RoundedCornerShape(scaledCornerRadius))
            .border(0.5.dp, glassBorder, RoundedCornerShape(scaledCornerRadius))
            .background(glassBg)
            .graphicsLayer {
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .pressScale(isPressed)
            .clickable(
                interactionSource = pressInteractionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = scaledHorizontalPadding, end = (scaledHorizontalPadding / 3)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = scaledVerticalPadding)
            ) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = scaledWordFontSize,
                        lineHeight = scaledWordLineHeight
                    ),
                    fontWeight = FontWeight.Bold,
                    color = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
                )
                if (dictionaryName.isNotEmpty()) {
                    Text(
                        text = dictionaryName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = scaledDictFontSize,
                            lineHeight = scaledDictLineHeight
                        ),
                        color = subtitleColor,
                        modifier = Modifier.padding(top = (3.dp * contentScale))
                    )
                }
                if (definition.isNotEmpty()) {
                    val previewText = remember(definition) { HtmlUtils.stripHtmlForPreview(definition) }
                    if (previewText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(scaledSpacing))
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = scaledDefFontSize,
                                lineHeight = scaledDefLineHeight
                            ),
                            color = textColor.copy(alpha = 0.95f),
                            maxLines = (3 * contentScale).roundToInt().coerceIn(2, 6),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = (4.dp * contentScale), end = 2.dp)
                    .size(38.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, glassBorder, RoundedCornerShape(12.dp))
                    .background(glassBg)
                    .pointerInput(index, totalItems) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffset = 0f
                            },
                            onDragEnd = {
                                val targetIndex = calculateTargetIndex(index, dragOffset, this.size.height)
                                if (targetIndex != index) {
                                    onReorder(index, targetIndex)
                                }
                                isDragging = false
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = if (isDragging) GdictColors.Primary else GdictColors.OnSurfaceVariant,
                    modifier = Modifier.size(scaledDragIconSize)
                )
            }
        }
    }
}

private fun calculateTargetIndex(currentIndex: Int, dragOffset: Float, itemHeight: Int): Int {
    if (itemHeight <= 0) return currentIndex
    val offsetItems = (dragOffset / itemHeight).roundToInt()
    return (currentIndex + offsetItems).coerceAtLeast(0)
}

@Composable
private fun EmptySearchResult(
    query: String,
    suggestions: List<String>,
    textColor: Color,
    subtitleColor: Color,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Did you mean:",
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            suggestions.take(5).forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GdictColors.PrimarySoft,
                    modifier = Modifier
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentSearchSection(
    history: List<HistoryItem>,
    textColor: Color,
    subtitleColor: Color,
    onWordClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.recent_searches),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        history.take(5).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWordClick(item.word) }
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
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
    textColor: Color,
    subtitleColor: Color,
    darkMode: Boolean,
    onWordClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            stringResource(R.string.word_of_the_day),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (words.isEmpty()) {
            val welcomeWord = stringResource(R.string.word_of_the_day_welcome)
            val welcomeDesc = stringResource(R.string.word_of_the_day_welcome_desc)
            val dictWord = stringResource(R.string.word_of_the_day_dictionary)
            val dictDesc = stringResource(R.string.word_of_the_day_dictionary_desc)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listOf(
                    Pair(welcomeWord, welcomeDesc),
                    Pair(dictWord, dictDesc)
                )) { (word, meaning) ->
                    WordOfDayCard(
                        word = word,
                        meaning = meaning,
                        darkMode = darkMode,
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
                        darkMode = darkMode,
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
    darkMode: Boolean,
    onClick: () -> Unit
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(130.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = glassBg),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GdictColors.Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
