package io.github.gdict.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.core.model.HistoryItem
import io.github.gdict.core.model.SearchResultItem
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.SearchViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import io.github.gdict.util.HtmlUtils
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    onWordClick: (word: String, definition: String, dictionaryName: String, css: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by searchViewModel.searchResults.collectAsState()
    val history by searchViewModel.history.collectAsState()
    val errorMessage by searchViewModel.errorMessage.collectAsState()
    val wordOfTheDay by searchViewModel.wordOfTheDay.collectAsState()
    val suggestions by searchViewModel.suggestions.collectAsState()

    var reorderedResults by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    val cardScale by settingsViewModel.cardScale.collectAsState()

    LaunchedEffect(searchResults) {
        reorderedResults = searchResults
    }

    LaunchedEffect(Unit) {
        if (wordOfTheDay.isEmpty()) {
            searchViewModel.loadWordOfTheDay()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WideSearchBar(
            query = searchQuery,
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

        errorMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { searchViewModel.clearError() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (reorderedResults.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${reorderedResults.size} results",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { settingsViewModel.setCardScale((cardScale - 0.2f).coerceIn(0.6f, 1.6f)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    String.format("%.0f%%", cardScale * 100),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { settingsViewModel.setCardScale((cardScale + 0.2f).coerceIn(0.6f, 1.6f)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val minCardSize = (320f * cardScale).coerceIn(180f, 520f).dp

            DraggableGrid(
                items = reorderedResults,
                onReorder = { from, to ->
                    val mutable = reorderedResults.toMutableList()
                    val item = mutable.removeAt(from)
                    mutable.add(to, item)
                    reorderedResults = mutable
                },
                minCardSize = minCardSize,
                cardScale = cardScale,
                onWordClick = onWordClick
            )
        } else if (searchQuery.isNotEmpty()) {
            EmptySearchResult(
                query = searchQuery,
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    searchQuery = suggestion
                    searchViewModel.searchWord(suggestion)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (history.isNotEmpty()) {
                    RecentSearchSection(
                        history = history,
                        onWordClick = { word ->
                            searchQuery = word
                            searchViewModel.searchWord(word)
                        }
                    )
                }
                WordOfTheDaySection(
                    words = wordOfTheDay,
                    onWordClick = { word ->
                        searchQuery = word
                        searchViewModel.searchWord(word)
                    }
                )
            }
        }
    }
}

private fun findNearestSlot(
    dragIndex: Int,
    dragOffset: Offset,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    itemCount: Int
): Int {
    if (dragIndex < 0 || dragIndex >= itemCount) return dragIndex

    val layoutInfo = gridState.layoutInfo
    val draggedInfo = layoutInfo.visibleItemsInfo.find { it.index == dragIndex }
        ?: return dragIndex

    val centerX = draggedInfo.offset.x.toFloat() + dragOffset.x + draggedInfo.size.width / 2f
    val centerY = draggedInfo.offset.y.toFloat() + dragOffset.y + draggedInfo.size.height / 2f

    var closestIndex = dragIndex
    var closestDistance = Float.MAX_VALUE

    for (itemInfo in layoutInfo.visibleItemsInfo) {
        if (itemInfo.index == dragIndex || itemInfo.index >= itemCount) continue
        val itemCenterX = itemInfo.offset.x + itemInfo.size.width / 2f
        val itemCenterY = itemInfo.offset.y + itemInfo.size.height / 2f
        val dx = centerX - itemCenterX
        val dy = centerY - itemCenterY
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < closestDistance) {
            closestDistance = distance
            closestIndex = itemInfo.index
        }
    }

    return closestIndex.coerceIn(0, itemCount - 1)
}

@Composable
private fun DraggableGrid(
    items: List<SearchResultItem>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    minCardSize: androidx.compose.ui.unit.Dp,
    cardScale: Float,
    onWordClick: (word: String, definition: String, dictionaryName: String, css: String) -> Unit
) {
    val gridState = rememberLazyGridState()
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // Only run the staggered entrance animation on the very first non-empty render
    // of the grid, not on every recomposition or list mutation.
    var hasAnimatedIn by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // After the first non-empty items render, mark the entrance as done so
        // subsequent item insertions / reorderings don't re-run the stagger.
        androidx.compose.runtime.LaunchedEffect(items.size) {
            if (items.isNotEmpty()) hasAnimatedIn = true
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = minCardSize),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 16.dp
            )
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> "${item.word}_${item.dictionaryName}" }
            ) { index, result ->
                val isDragging = index == dragIndex
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "cardElevation"
                )

                // Staggered entrance animation only on the first render
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 380,
                            delayMillis = if (hasAnimatedIn) 0 else (index * 30).coerceAtMost(300),
                            easing = FastOutSlowInEasing
                        )
                    ) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 380,
                            delayMillis = if (hasAnimatedIn) 0 else (index * 30).coerceAtMost(300),
                            easing = FastOutSlowInEasing
                        )
                    ) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 280f)
                    ),
                    exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (isDragging) {
                                Modifier
                                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                                    .shadow(elevation, RoundedCornerShape(12.dp))
                                    .graphicsLayer { alpha = 0.9f }
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(items.toList()) {
                            var isDraggingNow = false
                            var totalDragOffset = Offset.Zero
                            detectDragGestures(
                                onDragStart = {
                                    isDraggingNow = false
                                    totalDragOffset = Offset.Zero
                                },
                                onDragEnd = {
                                    if (isDraggingNow) {
                                        val fromIdx = dragIndex
                                        val targetIdx = findNearestSlot(
                                            dragIndex, dragOffset, gridState, items.size
                                        )
                                        dragIndex = -1
                                        dragOffset = Offset.Zero
                                        if (fromIdx != targetIdx && fromIdx >= 0 && targetIdx >= 0) {
                                            onReorder(fromIdx, targetIdx)
                                        }
                                    } else {
                                        onWordClick(
                                            result.word,
                                            result.definition,
                                            result.dictionaryName,
                                            result.css
                                        )
                                    }
                                },
                                onDragCancel = {
                                    dragIndex = -1
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragOffset += dragAmount
                                    if (!isDraggingNow && totalDragOffset.getDistance() > 8f) {
                                        isDraggingNow = true
                                        dragIndex = index
                                        dragOffset = Offset.Zero
                                    }
                                    if (isDraggingNow) {
                                        dragOffset += dragAmount
                                    }
                                }
                            )
                        }
                ) {
                    WordTranslationCard(
                        word = result.word,
                        definition = result.definition,
                        dictionaryName = result.dictionaryName,
                        cardScale = cardScale,
                        onClick = {
                            onWordClick(
                                result.word,
                                result.definition,
                                result.dictionaryName,
                                result.css
                            )
                        }
                    )
                }
                } // AnimatedVisibility
            }
        }

        ThinGridScrollbar(
            gridState = gridState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
                .padding(end = 6.dp)
        )
    }
}

@Composable
private fun ThinGridScrollbar(
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val thumbAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.6f else 0.25f,
        animationSpec = tween(durationMillis = 150),
        label = "scrollbarAlpha"
    )

    Box(
        modifier = modifier
            .width(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isHovered) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .hoverable(interactionSource = interactionSource)
    ) {
        val layoutInfo = gridState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo

        if (totalItems > 0 && visibleItems.isNotEmpty()) {
            val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: 0
            val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: firstVisibleIndex
            val visibleCount = lastVisibleIndex - firstVisibleIndex + 1

            val scrollFraction = firstVisibleIndex.toFloat() / (totalItems - visibleCount).coerceAtLeast(1)
            val thumbFraction = visibleCount.toFloat() / totalItems.coerceAtLeast(1).coerceAtLeast(visibleCount)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset(y = ((layoutInfo.viewportSize.height * scrollFraction).coerceIn(0f, layoutInfo.viewportSize.height.toFloat())).toInt().dp)
                    .height((layoutInfo.viewportSize.height * thumbFraction).coerceIn(20f, layoutInfo.viewportSize.height.toFloat()).toInt().dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = thumbAlpha))
            )
        }
    }
}

@Composable
private fun WideSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    // Fluent AutoSuggestBox 风格：
    // - 容器透明，hover/聚焦时 Subtle 填充
    // - 圆角 8dp（Fluent 标配）
    // - 聚焦时底部 1px accent 下划线
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val background = when {
        isFocused -> GdictColors.SubtleSelected
        isHovered -> GdictColors.SubtleHover
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val bottomBarColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(background)
                    .hoverable(interactionSource)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusable(interactionSource = interactionSource),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                "Search English Dictionary... Enter word or phrase",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            // 聚焦时底部 1px accent 下划线（AutoSuggestBox 聚焦反馈）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(bottomBarColor)
            )
        }
    }
}

@Composable
private fun WordTranslationCard(
    word: String,
    definition: String,
    dictionaryName: String,
    cardScale: Float = 1.0f,
    onClick: () -> Unit = {}
) {
    // Fluent 卡片：去阴影，改用 1px 描边 + hover/press 时 Subtle 填充反馈
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isPressed by cardInteractionSource.collectIsPressedAsState()
    val isHovered by cardInteractionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered -> 1.012f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "cardPressScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> GdictColors.SubtleSelected
            isHovered -> GdictColors.SubtleHover
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "cardBackground"
    )

    val titleFontSize = (16f * cardScale).coerceIn(12f, 24f).sp
    val bodyFontSize = (14f * cardScale).coerceIn(10f, 20f).sp
    val labelFontSize = (11f * cardScale).coerceIn(8f, 16f).sp
    val contentPadding = (16f * cardScale).coerceIn(8f, 24f).dp
    val spacerHeight = (8f * cardScale).coerceIn(4f, 16f).dp
    val maxLines = if (cardScale >= 1.0f) 4 else if (cardScale >= 0.8f) 3 else 2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp)) // Fluent 标配 8dp
            .border(1.dp, GdictColors.CardStroke, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = word,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dictionaryName.isNotEmpty()) {
                    Text(
                        text = dictionaryName,
                        fontSize = labelFontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (definition.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacerHeight))
                Text(
                    text = HtmlUtils.stripHtmlForPreview(definition),
                    fontSize = bodyFontSize,
                    lineHeight = (14f * cardScale * 1.45f).coerceIn(10f, 28f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentSearchSection(
    history: List<HistoryItem>,
    onWordClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            "Recent Searches",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        history.take(5).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onWordClick(item.word) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun WordOfTheDaySection(
    words: List<Pair<String, String>>,
    onWordClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            "Word of the Day",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (words.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf(
                    Pair("Welcome", "Start by adding a dictionary"),
                    Pair("Explore", "Discover new words every day")
                )) { (word, meaning) ->
                    WordOfDayCard(word = word, meaning = meaning, onClick = { onWordClick(word) })
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(words) { (word, dictName) ->
                    WordOfDayCard(word = word, meaning = dictName, onClick = { onWordClick(word) })
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Did you mean?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSuggestionClick(suggestion) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
