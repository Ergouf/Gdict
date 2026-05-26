package io.github.gdict.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.core.model.ReviewStats
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.FlashcardViewModel
import io.github.gdict.viewmodel.SettingsViewModel

private fun stripHtml(html: String): String {
    return html
        .replace(Regex("<img[^>]*>"), "")
        .replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("<p\\s*/?>|</p>|<div[^>]*>|</div>"), "\n")
        .replace(Regex("<li\\s*>"), "• ")
        .replace(Regex("</li>"), "\n")
        .replace(Regex("<a[^>]*href=\"[^\"]*\"[^>]*>(.*?)</a>"), "$1")
        .replace(Regex("<font[^>]*>|</font>"), "")
        .replace(Regex("<b\\s*>|<strong\\s*>|</b>|</strong>"), "")
        .replace(Regex("<i\\s*>|<em\\s*>|</i>|</em>"), "")
        .replace(Regex("<u\\s*>|</u>"), "")
        .replace(Regex("<span[^>]*>|</span>"), "")
        .replace(Regex("&nbsp;"), " ")
        .replace(Regex("&amp;"), "&")
        .replace(Regex("&lt;"), "<")
        .replace(Regex("&gt;"), ">")
        .replace(Regex("&quot;"), "\"")
        .replace(Regex("&#39;"), "'")
        .replace(Regex("<[^>]+>"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

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
    val text = stripHtml(raw)
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

    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.LightGray
    val cardColor = if (darkMode) GdictColors.DarkSurface else Color.White
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.DarkGray

    LaunchedEffect(bookmarks) {
        flashcardViewModel.refreshReviewStats()
    }

    val isSessionActive = dueBookmarks.isNotEmpty()
    val isSessionComplete = currentCardIndex >= dueBookmarks.size && isSessionActive

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Flashcard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        when {
            !isSessionActive -> FlashcardStartView(
                reviewStats = reviewStats,
                cardColor = cardColor,
                textColor = textColor,
                darkMode = darkMode,
                onStart = { flashcardViewModel.startReviewSession() }
            )
            isSessionComplete -> FlashcardCompleteView(
                reviewed = sessionReviewed,
                total = dueBookmarks.size,
                cardColor = cardColor,
                textColor = textColor,
                darkMode = darkMode,
                onRestart = { flashcardViewModel.startReviewSession() }
            )
            else -> FlashcardReviewView(
                item = dueBookmarks[currentCardIndex],
                scheduling = currentScheduling,
                currentIndex = currentCardIndex,
                totalCount = dueBookmarks.size,
                cardColor = cardColor,
                textColor = textColor,
                darkMode = darkMode,
                onRate = { flashcardViewModel.rateCurrentCard(it) },
                onSkip = { flashcardViewModel.skipCurrentCard() }
            )
        }
    }
}

@Composable
private fun FlashcardStartView(
    reviewStats: ReviewStats,
    cardColor: Color,
    textColor: Color,
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
                    .background(GdictColors.NavyBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = GdictColors.NavyBlue.copy(alpha = 0.6f),
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
                    StatChip(label = "New", count = reviewStats.new, color = GdictColors.TealAccent)
                    StatChip(label = "Due", count = reviewStats.due, color = GdictColors.CoralAccent)
                    StatChip(label = "Learned", count = reviewStats.learned, color = GdictColors.MintGreen)
                }
            } else {
                Text(
                    "Add words to favorites first",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GdictColors.MediumGray
                )
            }

            if (hasDue) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onStart),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GdictColors.NavyBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start Review (${reviewStats.new + reviewStats.due})",
                            color = Color.White,
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
    cardColor: Color,
    textColor: Color,
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / totalCount },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            color = GdictColors.NavyBlue,
            trackColor = if (darkMode) GdictColors.DarkSurface else GdictColors.SurfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${currentIndex + 1} / $totalCount",
                style = MaterialTheme.typography.labelMedium,
                color = GdictColors.MediumGray
            )
            TextButton(onClick = onSkip) {
                Text("Skip", color = GdictColors.MediumGray)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -100f && !isFlipped) isFlipped = true
                            else if (dragAmount > 100f && isFlipped) isFlipped = false
                        }
                    }
                    .clickable { isFlipped = !isFlipped },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (showFront) cardColor else GdictColors.NavyBlue
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showFront) {
                        FlashcardFront(
                            word = item.word,
                            dictName = dictName,
                            textColor = textColor
                        )
                    } else {
                        FlashcardBack(
                            word = item.word,
                            dictName = dictName,
                            parsed = parsed
                        )
                    }
                }
            }
        }

        if (isFlipped && scheduling.isNotEmpty()) {
            RatingButtonsRow(
                scheduling = scheduling,
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
    textColor: Color
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
                color = GdictColors.MediumGray,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Tap to reveal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GdictColors.MediumGray.copy(alpha = 0.7f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = GdictColors.MediumGray.copy(alpha = 0.5f),
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
    parsed: ParsedDefinition
) {
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
                    color = Color.White
                )
                if (dictName.isNotBlank()) {
                    Text(
                        text = dictName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (parsed.wordForms.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = parsed.wordForms.joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
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
                            color = if (index == 0) Color.White else Color.White.copy(alpha = 0.65f),
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            } else {
                Text(
                    text = "(No definition)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 26.sp
                )
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
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RatingButtonsRow(
    scheduling: Map<Rating, SchedulingCard>,
    onRate: (Rating) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = if (scheduling.isNotEmpty()) {
            GdictColors.NavyBlue.copy(alpha = 0.03f)
        } else Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GdictColors.MediumGray.copy(alpha = 0.15f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingButton(
                    rating = Rating.Again,
                    scheduling = scheduling[Rating.Again],
                    color = GdictColors.CoralAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                )
                RatingButton(
                    rating = Rating.Hard,
                    scheduling = scheduling[Rating.Hard],
                    color = GdictColors.AmberAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                )
                RatingButton(
                    rating = Rating.Good,
                    scheduling = scheduling[Rating.Good],
                    color = GdictColors.TealAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onRate
                )
                RatingButton(
                    rating = Rating.Easy,
                    scheduling = scheduling[Rating.Easy],
                    color = GdictColors.MintGreen,
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable { onClick(rating) }
            .padding(vertical = 10.dp, horizontal = 4.dp)
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
            color = color.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun FlashcardCompleteView(
    reviewed: Int,
    total: Int,
    cardColor: Color,
    textColor: Color,
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
                color = GdictColors.MediumGray
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clickable(onClick = onRestart),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GdictColors.NavyBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Review Again",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
