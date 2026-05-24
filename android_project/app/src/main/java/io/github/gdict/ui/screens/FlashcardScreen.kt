package io.github.gdict.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import io.github.gdict.data.BookmarkItem
import io.github.gdict.data.ReviewStats
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.AppViewModel

@Composable
fun FlashcardScreen(
    viewModel: AppViewModel
) {
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val reviewStats by viewModel.reviewStats.collectAsStateWithLifecycle()
    val dueBookmarks by viewModel.dueBookmarks.collectAsStateWithLifecycle()
    val currentCardIndex by viewModel.currentCardIndex.collectAsStateWithLifecycle()
    val currentScheduling by viewModel.currentScheduling.collectAsStateWithLifecycle()
    val sessionReviewed by viewModel.sessionReviewed.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())

    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.LightGray
    val cardColor = if (darkMode) GdictColors.DarkSurface else Color.White
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.DarkGray

    LaunchedEffect(bookmarks) {
        viewModel.refreshReviewStats()
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
                onStart = { viewModel.startReviewSession() }
            )
            isSessionComplete -> FlashcardCompleteView(
                reviewed = sessionReviewed,
                total = dueBookmarks.size,
                cardColor = cardColor,
                textColor = textColor,
                darkMode = darkMode,
                onRestart = { viewModel.startReviewSession() }
            )
            else -> FlashcardReviewView(
                item = dueBookmarks[currentCardIndex],
                scheduling = currentScheduling,
                currentIndex = currentCardIndex,
                totalCount = dueBookmarks.size,
                cardColor = cardColor,
                textColor = textColor,
                darkMode = darkMode,
                onRate = { viewModel.rateCurrentCard(it) },
                onSkip = { viewModel.skipCurrentCard() }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (showFront) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = item.word,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            if (item.dictionaryName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.dictionaryName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GdictColors.MediumGray
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Tap to reveal",
                                style = MaterialTheme.typography.bodySmall,
                                color = GdictColors.MediumGray.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer { scaleX = -1f }
                        ) {
                            Text(
                                text = item.word,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = item.definition.ifBlank { "(No definition)" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 10,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 24.sp
                            )
                        }
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
private fun RatingButtonsRow(
    scheduling: Map<Rating, SchedulingCard>,
    onRate: (Rating) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
            days == 0 -> "<1d"
            days == 1 -> "1d"
            days < 30 -> "${days}d"
            else -> "${days / 30}mo"
        }
    } ?: ""

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable { onClick(rating) }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Text(
            text = rating.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = daysText,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
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
