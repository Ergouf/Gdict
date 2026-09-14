package io.github.gdict.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gdict.R
import io.github.gdict.core.Rating
import io.github.gdict.core.SchedulingCard
import io.github.gdict.core.model.BookmarkItem
import io.github.gdict.core.model.ReviewStats
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.util.HtmlUtils
import io.github.gdict.viewmodel.BookmarkViewModel
import io.github.gdict.viewmodel.FlashcardViewModel
import io.github.gdict.viewmodel.SettingsViewModel

private fun simplifyDictionaryName(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace(Regex("\\.mdx$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\.mdd$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("[^\\w\\s\\u4e00-\\u9fff-]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
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
        "^\\s*([a-zA-Z]+(?:es|s|ing|ed|er|est|ly|tion|sion|ment|ness|ity|al|ful|less|ous|ive|able|ible|y))\\s*$",
        RegexOption.IGNORE_CASE
    )

    val wordForms = mutableListOf<String>()
    val posTags = mutableListOf<String>()
    val definitions = mutableListOf<String>()
    text.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
        when {
            posPattern.matches(line) -> posTags += line.removeSuffix(".").uppercase()
            wordFormPattern.matches(line) && line.length < 30 -> wordForms += line
            line.length > 1 -> definitions += line
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

    LaunchedEffect(bookmarks) { flashcardViewModel.refreshReviewStats() }

    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val onBackground = if (darkMode) GdictColors.DarkOnBackground else GdictColors.OnBackground
    val isSessionActive = dueBookmarks.isNotEmpty()
    val isSessionComplete = isSessionActive && currentCardIndex >= dueBookmarks.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.flashcard),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        when {
            !isSessionActive -> FlashcardStartView(
                reviewStats = reviewStats,
                darkMode = darkMode,
                onStart = flashcardViewModel::startReviewSession
            )

            isSessionComplete -> FlashcardCompleteView(
                reviewed = sessionReviewed,
                darkMode = darkMode,
                onRestart = flashcardViewModel::startReviewSession
            )

            else -> FlashcardReviewView(
                item = dueBookmarks[currentCardIndex],
                scheduling = currentScheduling,
                currentIndex = currentCardIndex,
                totalCount = dueBookmarks.size,
                darkMode = darkMode,
                onRate = flashcardViewModel::rateCurrentCard,
                onSkip = flashcardViewModel::skipCurrentCard
            )
        }
    }
}

@Composable
private fun FlashcardStartView(
    reviewStats: ReviewStats,
    darkMode: Boolean,
    onStart: () -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val hasItems = reviewStats.total > 0
    val hasDue = reviewStats.new + reviewStats.due > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = when {
                !hasItems -> "No vocabulary yet"
                !hasDue -> "All caught up"
                else -> "Ready to review?"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasItems) "${reviewStats.total} saved words" else stringResource(R.string.add_words_to_favorites_first),
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
            textAlign = TextAlign.Center
        )

        if (hasItems) {
            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(stringResource(R.string.stat_new), reviewStats.new, textColor, secondary)
                    StatItem(stringResource(R.string.stat_due), reviewStats.due, textColor, secondary)
                    StatItem(stringResource(R.string.stat_learned), reviewStats.learned, textColor, secondary)
                }
            }
        }

        if (hasDue) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GdictColors.Primary)
            ) {
                Text("Start review (${reviewStats.new + reviewStats.due})", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int, textColor: Color, secondary: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = secondary)
    }
}

@Composable
private fun FlashcardReviewView(
    item: BookmarkItem,
    scheduling: Map<Rating, SchedulingCard>,
    currentIndex: Int,
    totalCount: Int,
    darkMode: Boolean,
    onRate: (Rating) -> Unit,
    onSkip: () -> Unit
) {
    var isFlipped by remember(item.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(320),
        label = "flashcardFlip"
    )
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val parsed = remember(item.definition) { parseDefinition(item.definition) }
    val collinsEntry = remember(item.definition, item.word) {
        if (isCollinsEntry(item.definition)) parseCollinsEntry(item.definition, item.word) else null
    }
    val progress = ((currentIndex + 1).toFloat() / totalCount.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(4.dp)
                .background(outline, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(GdictColors.Primary, RoundedCornerShape(2.dp))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${currentIndex + 1} / $totalCount",
                style = MaterialTheme.typography.labelLarge,
                color = secondary
            )
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Skip")
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 16f * density
                    }
                    .clickable { isFlipped = !isFlipped },
                shape = RoundedCornerShape(24.dp),
                color = surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, outline),
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                if (rotation < 90f) {
                    FlashcardFront(
                        word = item.word,
                        dictionaryName = simplifyDictionaryName(item.dictionaryName),
                        textColor = textColor,
                        secondary = secondary
                    )
                } else {
                    Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                        FlashcardBack(
                            word = item.word,
                            dictionaryName = simplifyDictionaryName(item.dictionaryName),
                            parsed = parsed,
                            collinsEntry = collinsEntry,
                            darkMode = darkMode,
                            textColor = textColor,
                            secondary = secondary
                        )
                    }
                }
            }
        }

        if (isFlipped && scheduling.isNotEmpty()) {
            RatingButtonsRow(scheduling = scheduling, darkMode = darkMode) { rating ->
                onRate(rating)
                isFlipped = false
            }
        } else {
            Text(
                text = stringResource(R.string.tap_to_reveal),
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun FlashcardFront(
    word: String,
    dictionaryName: String,
    textColor: Color,
    secondary: Color
) {
    Box(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        if (dictionaryName.isNotBlank()) {
            Text(
                text = dictionaryName,
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        Text(
            text = word,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun FlashcardBack(
    word: String,
    dictionaryName: String,
    parsed: ParsedDefinition,
    collinsEntry: CollinsEntry?,
    darkMode: Boolean,
    textColor: Color,
    secondary: Color
) {
    val primaryTint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
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
            if (dictionaryName.isNotBlank()) {
                Text(
                    text = dictionaryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 16.dp).width(120.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        if (collinsEntry != null && collinsEntry.parsedOk) {
            if (collinsEntry.frequency > 0) {
                FrequencyDiamondsBlue(collinsEntry.frequency, primaryTint)
                Spacer(Modifier.height(10.dp))
            }
            if (collinsEntry.wordForms.isNotBlank()) {
                Text(collinsEntry.wordForms, style = MaterialTheme.typography.bodyMedium, color = secondary)
                Spacer(Modifier.height(12.dp))
            }
            CollinsSensesList(
                definitions = collinsEntry.definitions,
                headword = collinsEntry.word.ifEmpty { word },
                textColor = textColor,
                subtitleColor = secondary,
                primaryTint = primaryTint
            )
        } else {
            if (parsed.wordForms.isNotEmpty()) {
                Text(
                    text = parsed.wordForms.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondary
                )
                Spacer(Modifier.height(10.dp))
            }
            if (parsed.posTags.isNotEmpty()) {
                Text(
                    text = parsed.posTags.joinToString("  "),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTint
                )
                Spacer(Modifier.height(14.dp))
            }
            val definitions = parsed.definitions.ifEmpty { listOf("No definition") }
            definitions.take(8).forEachIndexed { index, definition ->
                Text(
                    text = definition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (index == 0) textColor else secondary,
                    lineHeight = 25.sp
                )
                if (index != definitions.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun RatingButtonsRow(
    scheduling: Map<Rating, SchedulingCard>,
    darkMode: Boolean,
    onRate: (Rating) -> Unit
) {
    val outline = if (darkMode) GdictColors.DarkOutline else GdictColors.Outline
    val labels = listOf(
        Rating.Again to ("Again" to GdictColors.CoralAccent),
        Rating.Hard to ("Hard" to GdictColors.AmberAccent),
        Rating.Good to ("Good" to GdictColors.Primary),
        Rating.Easy to ("Easy" to GdictColors.MintGreen)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { (rating, labelAndColor) ->
            val schedule = scheduling[rating]
            val (label, tint) = labelAndColor
            OutlinedButton(
                onClick = { onRate(rating) },
                enabled = schedule != null,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (schedule != null) tint.copy(alpha = 0.5f) else outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tint)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (schedule != null) {
                        Text(formatInterval(schedule.scheduledDays), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun formatInterval(days: Int): String = when {
    days <= 0 -> "<1d"
    days == 1 -> "1d"
    days < 30 -> "${days}d"
    days < 365 -> "${days / 30}mo"
    else -> "${days / 365}y"
}

@Composable
private fun FlashcardCompleteView(
    reviewed: Int,
    darkMode: Boolean,
    onRestart: () -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = GdictColors.MintGreen.copy(alpha = 0.12f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = GdictColors.MintGreen, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Review complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = textColor)
        Spacer(Modifier.height(8.dp))
        Text("$reviewed cards reviewed", style = MaterialTheme.typography.bodyMedium, color = secondary)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Review again")
        }
    }
}
