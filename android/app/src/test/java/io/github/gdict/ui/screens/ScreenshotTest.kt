package io.github.gdict.ui.screens

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    @Test fun captureSearchScreen() = snapshot(false) { SearchFixture() }
    @Test fun captureSearchScreenDark() = snapshot(true) { SearchFixture(dark = true) }
    @Test fun captureDetailScreen() = snapshot(false) { DetailFixture() }
    @Test fun captureBookmarksScreen() = snapshot(false) { FavoritesFixture() }
    @Test fun captureFlashcardScreen() = snapshot(false) { LearningFixture() }
    @Test fun captureDictionariesScreen() = snapshot(false) { DictionariesFixture() }
    @Test fun captureSettingsScreen() = snapshot(false) { SettingsFixture() }

    private fun snapshot(dark: Boolean, content: @Composable () -> Unit) {
        paparazzi.snapshot { GdictTheme(darkTheme = dark) { content() } }
    }
}

@Composable
private fun SearchFixture(dark: Boolean = false) {
    val background = if (dark) GdictColors.DarkBackground else GdictColors.Background
    val surface = if (dark) GdictColors.DarkSurface else GdictColors.Surface
    val text = if (dark) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (dark) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val outline = if (dark) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val history = listOf("serendipity", "resilient", "ephemeral")

    Column(Modifier.fillMaxSize().background(background).padding(20.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = text)
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = secondary)
                Spacer(Modifier.width(10.dp))
                Text("Search dictionaries", color = secondary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Recent searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = text)
        history.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, null, tint = secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(item, color = text)
            }
            HorizontalDivider(color = outline)
        }
        Spacer(Modifier.height(24.dp))
        Text("Word of the Day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = text)
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("serendipity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = text)
                Spacer(Modifier.height(4.dp))
                Text("the occurrence of events by chance in a happy way", color = secondary)
            }
        }
    }
}

@Composable
private fun DetailFixture() {
    Column(Modifier.fillMaxSize().background(GdictColors.Background).padding(20.dp)) {
        Text("Oxford Dictionary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = GdictColors.OnSurface)
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = GdictColors.Surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, GdictColors.OutlineVariant)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("serendipity", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GdictColors.OnSurface)
                        Text("noun", color = GdictColors.OnSurfaceVariant)
                    }
                    Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = GdictColors.PrimaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.VolumeUp, null, tint = GdictColors.Primary) }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Definitions", style = MaterialTheme.typography.labelLarge, color = GdictColors.OnSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("The occurrence and development of events by chance in a happy or beneficial way.", color = GdictColors.OnSurface)
            }
        }
    }
}

@Composable
private fun FavoritesFixture() {
    val words = listOf("serendipity", "ephemeral", "resilience", "eloquent")
    Column(Modifier.fillMaxSize().background(GdictColors.Background).padding(20.dp)) {
        Text("My Vocabulary", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GdictColors.OnBackground)
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(words) { word ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = GdictColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GdictColors.OutlineVariant)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, null, tint = GdictColors.Primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(word, fontWeight = FontWeight.SemiBold, color = GdictColors.OnSurface)
                            Text("Oxford Dictionary", style = MaterialTheme.typography.bodySmall, color = GdictColors.OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningFixture() {
    Column(
        Modifier.fillMaxSize().background(GdictColors.Background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Learning", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GdictColors.OnBackground, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(40.dp))
        Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = GdictColors.Surface) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = GdictColors.Primary, modifier = Modifier.size(30.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Text("Ready to review?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = GdictColors.OnSurface)
        Spacer(Modifier.height(6.dp))
        Text("24 saved words", color = GdictColors.OnSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = GdictColors.Surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, GdictColors.OutlineVariant)
        ) {
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatFixture("New", "4")
                StatFixture("Due", "7")
                StatFixture("Learned", "13")
            }
        }
    }
}

@Composable
private fun StatFixture(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = GdictColors.OnSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = GdictColors.OnSurfaceVariant)
    }
}

@Composable
private fun DictionariesFixture() {
    val dictionaries = listOf("Oxford Advanced Learner's Dictionary", "Collins English Dictionary", "Cambridge EPD")
    Column(Modifier.fillMaxSize().background(GdictColors.Background).padding(20.dp)) {
        Text("Dictionaries", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GdictColors.OnBackground)
        Spacer(Modifier.height(16.dp))
        dictionaries.forEach { name ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(18.dp),
                color = GdictColors.Surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GdictColors.OutlineVariant)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = GdictColors.SurfaceVariant) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Book, null, tint = GdictColors.Primary) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(name, modifier = Modifier.weight(1f), color = GdictColors.OnSurface, fontWeight = FontWeight.Medium)
                    Switch(checked = true, onCheckedChange = null)
                }
            }
        }
    }
}

@Composable
private fun SettingsFixture() {
    Column(Modifier.fillMaxSize().background(GdictColors.Background).padding(20.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GdictColors.OnBackground)
        Text("Manage your settings", color = GdictColors.OnSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text("APPEARANCE", style = MaterialTheme.typography.labelMedium, color = GdictColors.OnSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = GdictColors.Surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, GdictColors.OutlineVariant)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = GdictColors.SurfaceVariant) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.DarkMode, null, tint = GdictColors.Primary) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dark Mode", color = GdictColors.OnSurface, fontWeight = FontWeight.Medium)
                    Text("Toggle dark/light theme", style = MaterialTheme.typography.bodySmall, color = GdictColors.OnSurfaceVariant)
                }
                Switch(checked = false, onCheckedChange = null)
            }
        }
    }
}
