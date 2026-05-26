package io.github.gdict.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.theme.GdictTheme
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false
    )

    @Test
    fun captureSearchScreen() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = false) {
                SearchScreenSnapshot()
            }
        }
    }

    @Test
    fun captureSearchScreenDark() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = true) {
                SearchScreenSnapshot()
            }
        }
    }

    @Test
    fun captureDetailScreen() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = false) {
                WordDetailScreenSnapshot()
            }
        }
    }

    @Test
    fun captureBookmarksScreen() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = false) {
                BookmarksScreenSnapshot()
            }
        }
    }

    @Test
    fun captureFlashcardScreen() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = false) {
                FlashcardScreenSnapshot()
            }
        }
    }

    @Test
    fun captureFlashcardFront() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = false) {
                FlashcardFrontSnapshot()
            }
        }
    }

    @Test
    fun captureFlashcardBack() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = true) {
                FlashcardBackSnapshot()
            }
        }
    }

    @Test
    fun captureDictionariesScreen() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = false) {
                DictionariesScreenSnapshot()
            }
        }
    }

    @Test
    fun captureSettingsScreen() {
        paparazzi.snapshot {
            GdictTheme(darkTheme = false) {
                SettingsScreenSnapshot()
            }
        }
    }
}

// ── Snapshot Composables (纯 UI 渲染，无 ViewModel 依赖) ──

@Composable
private fun SearchScreenSnapshot() {
    val searchQuery = remember { mutableStateOf("") }
    val sampleResults = listOf(
        Triple("hello", "used as a greeting. Also an exclamation of surprise.", "Oxford Dictionary"),
        Triple("hesitate", "pause before saying or doing something, especially through uncertainty.", "Oxford Dictionary"),
        Triple("heritage", "property that is or may be inherited; an inheritance.", "Longman Dictionary")
    )
    val sampleHistory = listOf("hello", "world", "hesitate", "dictionary")
    val wordOfTheDay = listOf(
        "serendipity" to "the occurrence of events by chance in a happy way",
        "ephemeral" to "lasting for a very short time",
        "eloquent" to "fluent or persuasive in speaking or writing"
    )

    val bgColor = GdictColors.LightGray
    val cardColor = Color.White
    val textColor = GdictColors.DarkGray

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardColor)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = GdictColors.MediumGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search", color = GdictColors.MediumGray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Mic, null, tint = GdictColors.AmberAccent, modifier = Modifier.size(20.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Recent Searches",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(sampleHistory.take(3)) { word ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, null, tint = GdictColors.MediumGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(word, color = textColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text(
                    "Word of the Day",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(wordOfTheDay) { (word, def) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GdictColors.NavyBlue.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(word, fontWeight = FontWeight.Bold, color = GdictColors.NavyBlue, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(def, color = textColor, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordDetailScreenSnapshot() {
    val bgColor = GdictColors.LightGray
    val textColor = GdictColors.DarkGray

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Text("Word Definition", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Result", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        Text("serendipity", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Text("noun", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GdictColors.TealAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Origin", "Examples", "Synonyms").forEach { tab ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (tab == "Origin") Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(tab, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Bookmark" to Icons.Filled.Bookmark, "Learning" to Icons.Outlined.School).forEach { (label, icon) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GdictColors.NavyBlue.copy(alpha = 0.06f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = GdictColors.NavyBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = GdictColors.NavyBlue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Definitions", fontWeight = FontWeight.Bold, color = textColor, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "noun. The occurrence and development of events by chance in a happy or beneficial way.\n\n" +
                        "\"a fortunate stroke of serendipity\"\n\n" +
                        "synonyms: chance, happy chance, accident, happy accident, fluke",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarksScreenSnapshot() {
    val sampleBookmarks = listOf(
        Triple("serendipity", "Oxford Dictionary", "the occurrence of events by chance..."),
        Triple("ephemeral", "Longman Dictionary", "lasting for a very short time"),
        Triple("eloquent", "Oxford Dictionary", "fluent or persuasive in speaking"),
        Triple("luminous", "Cambridge Dictionary", "producing or reflecting bright light"),
        Triple("resilience", "Merriam-Webster", "the capacity to recover quickly"),
    )

    val bgColor = GdictColors.LightGray
    val cardColor = Color.White
    val textColor = GdictColors.DarkGray

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
            Text("My Vocabulary", fontWeight = FontWeight.Bold, color = textColor, style = MaterialTheme.typography.headlineSmall)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleBookmarks) { (word, dict, _) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GdictColors.NavyBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Bookmark, null, tint = GdictColors.NavyBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(word, fontWeight = FontWeight.SemiBold, color = textColor, style = MaterialTheme.typography.titleSmall)
                            Text(dict, color = GdictColors.MediumGray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardScreenSnapshot() {
    val bgColor = GdictColors.LightGray
    val textColor = GdictColors.DarkGray

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
            Text("Flashcard", fontWeight = FontWeight.Bold, color = textColor, style = MaterialTheme.typography.headlineSmall)
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    Icon(Icons.Outlined.School, null, tint = GdictColors.NavyBlue.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                }

                Text("Ready to review?", fontWeight = FontWeight.SemiBold, color = textColor, style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("New", 3, GdictColors.TealAccent)
                    StatChip("Due", 5, GdictColors.CoralAccent)
                    StatChip("Learned", 12, GdictColors.MintGreen)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GdictColors.NavyBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.School, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Review (8)", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardFrontSnapshot() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "blasphemy",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = GdictColors.DarkGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                "Collins Advanced",
                style = MaterialTheme.typography.bodySmall,
                color = GdictColors.MediumGray,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(
                    "Tap to reveal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GdictColors.MediumGray.copy(alpha = 0.7f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    null,
                    tint = GdictColors.MediumGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FlashcardBackSnapshot() {
    val cardBg = Color(0xFF1A2332)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GdictColors.LightGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Flashcard",
                fontWeight = FontWeight.Bold,
                color = GdictColors.DarkGray,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(4.dp)
                .background(GdictColors.NavyBlue.copy(alpha = 0.6f))
                .clip(RoundedCornerShape(2.dp))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("1 / 2", color = GdictColors.MediumGray, style = MaterialTheme.typography.bodyMedium)
            Text("Skip", color = GdictColors.MediumGray, style = MaterialTheme.typography.bodyMedium)
        }

        Box(modifier = Modifier.weight(1f)) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "blaspheme",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "柯林斯第三版",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "Collins 3rd",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.45f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "blasphemes blaspheming\nblasphemed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                "[VB]",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GdictColors.TealAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "If someone blasphemes, they say or do something that is considered to be disrespectful to God or other sacred people or things.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "如果某人对上帝或其他神圣的人或物说出或做出不敬的话或或事，那么他就犯了亵渎罪。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.65f),
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RatingButtonSnapshot("Again", "1d", GdictColors.CoralAccent)
            RatingButtonSnapshot("Hard", "1d", GdictColors.AmberAccent)
            RatingButtonSnapshot("Good", "1d", GdictColors.TealAccent)
            RatingButtonSnapshot("Easy", "1d", GdictColors.MintGreen)
        }
    }
}

@Composable
private fun RowScope.RatingButtonSnapshot(label: String, interval: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            interval,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DictionariesScreenSnapshot() {
    val sampleDictionaries = listOf(
        "Oxford Dictionary" to "42,318 words",
        "Longman Dictionary" to "38,921 words",
        "Cambridge Dictionary" to "35,604 words"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GdictColors.LightGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GdictColors.NavyBlue)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Dictionaries", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GdictColors.NavyBlue.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = GdictColors.NavyBlue.copy(alpha = 0.15f)) {
                        Icon(Icons.Filled.MenuBook, null, tint = GdictColors.NavyBlue, modifier = Modifier.padding(12.dp))
                    }
                    Text("${sampleDictionaries.size} dictionaries added", fontWeight = FontWeight.SemiBold, color = GdictColors.NavyBlue, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            sampleDictionaries.forEach { (name, count) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.InsertDriveFile, null, tint = GdictColors.NavyBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.SemiBold, color = GdictColors.DarkGray)
                                Text(count, color = GdictColors.MediumGray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Switch(checked = true, onCheckedChange = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreenSnapshot() {
    val bgColor = GdictColors.LightGray
    val cardColor = Color.White
    val textColor = GdictColors.DarkGray

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
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text("Profile", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text("Manage your settings", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            SettingsSectionCard("Dictionaries", cardColor, textColor) {
                SettingsRow(Icons.Outlined.MenuBook, "Dictionary Management", "Add, remove and manage dictionaries", textColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSectionCard("Appearance", cardColor, textColor) {
                SettingsRowSwitch(Icons.Outlined.DarkMode, "Dark Mode", "Toggle dark/light theme", checked = false, textColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSectionCard("About", cardColor, textColor) {
                SettingsRow(Icons.Outlined.Info, "Version Info", "Gdict v1.1.0", textColor)
                Spacer(modifier = Modifier.height(4.dp))
                SettingsRow(Icons.Outlined.DeleteOutline, "Clear Data", "Clear all history and bookmarks", textColor)
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(title: String, cardColor: Color, textColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = textColor, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GdictColors.MediumGray, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, color = textColor, style = MaterialTheme.typography.bodyLarge)
                Text(desc, color = GdictColors.MediumGray, style = MaterialTheme.typography.bodySmall)
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = GdictColors.MediumGray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsRowSwitch(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, checked: Boolean, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = if (checked) GdictColors.TealAccent else GdictColors.MediumGray, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, color = textColor, style = MaterialTheme.typography.bodyLarge)
                Text(desc, color = GdictColors.MediumGray, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = {})
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
        Text(count.toString(), fontWeight = FontWeight.Bold, color = color, fontSize = 20.sp)
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}
