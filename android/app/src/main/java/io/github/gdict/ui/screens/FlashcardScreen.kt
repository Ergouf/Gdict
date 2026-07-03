package io.github.gdict.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import io.github.gdict.R
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.core.model.ReviewStats
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.FlashcardViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import io.github.gdict.util.HtmlUtils

private fun simplifyDictionaryName(raw: String): String {
    if (raw.isBlank()) return ""
    val cleaned = raw
        .replace(Regex("\\.mdx$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\.mdd$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("[^\\w\\s\\u4e00-\\u9fff-]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    return cleaned
}

private data class ParsedDefinition(
    val wordForms: List<String> = emptyList(),
    val posTags: List<String> = emptyList(),
    val definitions: List<String> = emptyList()
)

private fun parseDefinition(raw: String): ParsedDefinition {
    val text = HtmlUtils.stripHtml(raw)
    if (text.isBlank()) return ParsedDefinition()

    val posPattern = Regex(
        "^\\s*(n\\.?|v\\.?|vt\\.?|vi\\.?|adj\\.?|adv\\.?|prep\\.?" +
        "|conj\\.?|pron\\.?|art\\.?|int\\.?|num\\.?|aux\\.?" +
        "|abbr\\.?|phr\\.?|pl\\.?|sing\\.?|def\\.?|indef\\.?" +
        "|[A-Z]{1,4}\\.)\\s*$",
        RegexOption.IGNORE_CASE
    )
    val wordFormPattern = Regex(
        "^\\s*([a-zA-Z]+(?:es|s|ing|ed|er|est|ly|tion|sion|ment|ness|ity|al|ful|less|ous|ive|able|ible|ous|y))\\s*$",
        RegexOption.IGNORE_CASE
    )

    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val wordForms = mutableListOf<String>()
    val posTags = mutableListOf<String>()
    val definitions = mutableListOf<String>()

    for (line in lines) {
        if (posPattern.matches(line)) {
            posTags.add(line.trim().removeSuffix(".").uppercase())
        } else if (wordFormPattern.matches(line) && line.length < 30) {
            wordForms.add(line)
        } else if (line.length > 1) {
            definitions.add(line)
        }
    }

    return ParsedDefinition(wordForms, posTags, definitions)
}

@Composable
fun FlashcardScreen(
    flashcardViewModel: FlashcardViewModel,
    settingsViewModel: SettingsViewModel,
    bookmarkViewModel: BookmarkViewModel
) {
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val reviewStats by flashcardViewModel.reviewStats.collectAsStateWithLifecycle()
    val dueBookmarks by flashcardViewModel.dueBookmarks.collectAsStateWithLifecycle()
    val currentCardIndex by flashcardViewModel.currentCardIndex.collectAsStateWithLifecycle()
    val currentScheduling by flashcardViewModel.currentScheduling.collectAsStateWithLifecycle()
    val sessionReviewed by flashcardViewModel.sessionReviewed.collectAsStateWithLifecycle()
    val bookmarks by bookmarkViewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())

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
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val glassBorder = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val titleColor = if (darkMode) GdictColors.DarkOnBackground else GdictColors.HeadingDark

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(bookmarks) {
        flashcardViewModel.refreshReviewStats()
    }

    val isSessionActive = dueBookmarks.isNotEmpty()
    val isSessionComplete = currentCardIndex >= dueBookmarks.size && isSessionActive

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                stringResource(R.string.flashcard),
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        }

        when {
            !isSessionActive -> FlashcardStartView(
                reviewStats = reviewStats,
                glassBg = glassBg,
                glassBorder = glassBorder,
                textColor = textColor,
                subtitleColor = subtitleColor,
                darkMode = darkMode,
                onStart = { flashcardViewModel.startReviewSession() }
            )
            isSessionComplete -> FlashcardCompleteView(
                reviewed = sessionReviewed,
                total = dueBookmarks.size,
                glassBg = glassBg,
                glassBorder = glassBorder,
                textColor = textColor,
                subtitleColor = subtitleColor,
                darkMode = darkMode,
                onRestart = { flashcardViewModel.startReviewSession() }
            )
            else -> FlashcardReviewView(
                item = dueBookmarks[currentCardIndex],
                scheduling = currentScheduling,
                currentIndex = currentCardIndex,
                totalCount = dueBookmarks.size,
                glassBg = glassBg,
                glassBorder = glassBorder,
                textColor = textColor,
                subtitleColor = subtitleColor,
                darkMode = darkMode,
                onRate = { flashcardViewModel.rateCurrentCard(it) },
                onSkip = { flashcardViewModel.skipCurrentCard() }
            )
        }
    }
    }
}

@Composable
private fun FlashcardStartView(
    reviewStats: ReviewStats,
    glassBg: Color,
    glassBorder: Color,
    textColor: Color,
    subtitleColor: Color,
    darkMode: Boolean,
    onStart: () -> Unit
) {
    val hasItems = reviewStats.total > 0
    val hasDue = reviewStats.due > 0 || reviewStats.new > 0

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(GdictColors.PrimarySoft.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = GdictColors.PrimarySoft.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                if (!hasItems) "No vocabulary yet" else if (!hasDue) "All caught up!" else "Ready to review?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            if (hasItems) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatChip(label = stringResource(R.string.stat_new), count = reviewStats.new, color = GdictColors.TealAccent)
                    StatChip(label = stringResource(R.string.stat_due), count = reviewStats.due, color = GdictColors.CoralAccent)
                    StatChip(label = stringResource(R.string.stat_learned), count = reviewStats.learned, color = GdictColors.MintGreen)
                }
            } else {
                Text(
                    stringResource(R.string.add_words_to_favorites_first),
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }

            if (hasDue) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
                        .background(glassBg)
                        .clickable(onClick = onStart)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = GdictColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start Review (${reviewStats.new + reviewStats.due})",
                            color = GdictColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun FlashcardReviewView(
    item: BookmarkItem,
    scheduling: Map<Rating, SchedulingCard>,
    currentIndex: Int,
    totalCount: Int,
    glassBg: Color,
    glassBorder: Color,
    textColor: Color,
    subtitleColor: Color,
    darkMode: Boolean,
    onRate: (Rating) -> Unit,
    onSkip: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )
    val showFront = rotation < 90f

    val parsed = remember(item.definition) { parseDefinition(item.definition) }
    val dictName = remember(item.dictionaryName) { simplifyDictionaryName(item.dictionaryName) }
    // 柯林斯3rd：用原生解析 + 共享释义渲染（与详情页一致）
    val collinsEntry = remember(item.definition, item.word) {
        if (isCollins3rdEntry(item.definition)) parseCollinsEntry(item.definition, item.word) else null
    }

    val progress = (currentIndex + 1).toFloat() / totalCount

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 圆角胶囊进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                .background(glassBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            0.0f to GdictColors.Primary,
                            1.0f to GdictColors.PrimarySoft
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${currentIndex + 1} / $totalCount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = GdictColors.OnBackground
            )
            TextButton(onClick = onSkip) {
                Text("Skip", color = GdictColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Acrylic 玻璃大圆角卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 16f * density
                    }
                    .shadow(16.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.5.dp, glassBorder, RoundedCornerShape(32.dp))
                    .background(glassBg)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -100f && !isFlipped) isFlipped = true
                            else if (dragAmount > 100f && isFlipped) isFlipped = false
                        }
                    }
                    .clickable { isFlipped = !isFlipped },
                contentAlignment = Alignment.Center
            ) {
                if (showFront) {
                    FlashcardFront(
                        word = item.word,
                        dictName = dictName,
                        darkMode = darkMode,
                        textColor = textColor,
                        subtitleColor = subtitleColor,
                        glassBg = glassBg,
                        glassBorder = glassBorder
                    )
                } else {
                    FlashcardBack(
                        word = item.word,
                        dictName = dictName,
                        parsed = parsed,
                        collinsEntry = collinsEntry,
                        darkMode = darkMode,
                        textColor = textColor,
                        subtitleColor = subtitleColor
                    )
                }
            }
        }

        if (isFlipped && scheduling.isNotEmpty()) {
            RatingButtonsRow(
                scheduling = scheduling,
                darkMode = darkMode,
                onRate = { rating ->
                    onRate(rating)
                    isFlipped = false
                }
            )
        } else {
            Spacer(modifier = Modifier.height(80.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FlashcardFront(
    word: String,
    dictName: String,
    darkMode: Boolean,
    textColor: Color,
    subtitleColor: Color,
    glassBg: Color,
    glassBorder: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        if (dictName.isNotBlank()) {
            Text(
                text = dictName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = subtitleColor.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopCenter),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 56.sp,
                    lineHeight = 62.sp
                ),
                fontWeight = FontWeight.Black,
                color = GdictColors.Primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            // 蓝色胶囊翻转按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
                    .background(glassBg)
                    .padding(horizontal = 18.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(R.string.tap_to_reveal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GdictColors.Primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FlashcardBack(
    word: String,
    dictName: String,
    parsed: ParsedDefinition,
    collinsEntry: CollinsEntry?,
    darkMode: Boolean,
    textColor: Color,
    subtitleColor: Color
) {
    val primaryTint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .graphicsLayer { scaleX = -1f }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (dictName.isNotBlank()) {
                    Text(
                        text = dictName,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (collinsEntry != null && collinsEntry.parsedOk) {
                // —— 柯林斯3rd：原生渲染（与详情页一致）——
                // 词频棱形
                if (collinsEntry.frequency > 0) {
                    FrequencyDiamondsBlue(frequency = collinsEntry.frequency, primaryTint = primaryTint)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // 词形变化
                if (collinsEntry.wordForms.isNotEmpty()) {
                    Text(
                        text = collinsEntry.wordForms,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // 释义列表（编号圆形 + POS 徽标 + 词头高亮 + 例句）
                if (collinsEntry.definitions.isNotEmpty()) {
                    CollinsSensesList(
                        definitions = collinsEntry.definitions,
                        headword = collinsEntry.word.ifEmpty { word },
                        textColor = textColor,
                        subtitleColor = subtitleColor,
                        primaryTint = primaryTint
                    )
                }
            } else {
                // —— 通用渲染（原有逻辑）——
                if (parsed.wordForms.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GdictColors.PrimarySoft.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = parsed.wordForms.joinToString(" "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (parsed.posTags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        parsed.posTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = GdictColors.TealAccent.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "[$tag]",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GdictColors.TealAccent
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (parsed.definitions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parsed.definitions.forEachIndexed { index, def ->
                            Text(
                                text = def,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (index == 0) textColor else subtitleColor,
                                lineHeight = 26.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                } else {
                    Text(
                        text = "(No definition)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = subtitleColor,
                        lineHeight = 26.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .graphicsLayer { scaleX = -1f },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = subtitleColor.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = subtitleColor.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RatingButtonsRow(
    scheduling: Map<Rating, SchedulingCard>,
    darkMode: Boolean,
    onRate: (Rating) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RatingButton(
                    rating = Rating.Again,
                    scheduling = scheduling[Rating.Again],
                    color = GdictColors.CoralAccent,
                    darkMode = darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                )
                RatingButton(
                    rating = Rating.Hard,
                    scheduling = scheduling[Rating.Hard],
                    color = GdictColors.AmberAccent,
                    darkMode = darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                )
                RatingButton(
                    rating = Rating.Good,
                    scheduling = scheduling[Rating.Good],
                    color = GdictColors.TealAccent,
                    darkMode = darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                )
                RatingButton(
                    rating = Rating.Easy,
                    scheduling = scheduling[Rating.Easy],
                    color = GdictColors.MintGreen,
                    darkMode = darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                )
            }
        }
    }
}

@Composable
private fun RatingButton(
    rating: Rating,
    scheduling: SchedulingCard?,
    color: Color,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Rating) -> Unit
) {
    val daysText = scheduling?.scheduledDays?.let { days ->
        when {
            days == 1 -> "1d"
            days < 30 -> "${days}d"
            else -> "${days / 30}mo"
        }
    } ?: ""

    val btnBg = if (darkMode) GdictColors.DarkSurfaceVariant else color.copy(alpha = 0.1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(btnBg)
            .clickable { onClick(rating) }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Text(
            text = rating.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = daysText,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun FlashcardCompleteView(
    reviewed: Int,
    total: Int,
    glassBg: Color,
    glassBorder: Color,
    textColor: Color,
    subtitleColor: Color,
    darkMode: Boolean,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(GdictColors.MintGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = GdictColors.MintGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                "Session Complete!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                "Reviewed $reviewed of $total cards",
                style = MaterialTheme.typography.bodyLarge,
                color = subtitleColor
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
                    .background(glassBg)
                    .clickable(onClick = onRestart)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = GdictColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Review Again",
                        color = GdictColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}