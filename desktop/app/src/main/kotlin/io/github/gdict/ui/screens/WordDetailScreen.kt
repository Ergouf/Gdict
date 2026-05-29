package io.github.gdict.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.core.GdictLogger
import io.github.gdict.tts.EdgeTtsClient
import io.github.gdict.ui.webview.DesktopAudioPlayer
import io.github.gdict.ui.webview.MdxWebView
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val log = GdictLogger.get()

@Composable
fun WordDetailScreen(
    word: String,
    definition: String,
    dictionaryName: String,
    css: String = "",
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEntryClick: (String) -> Unit = {},
    dictionaryRepository: DictionaryRepository,
    settingsViewModel: SettingsViewModel,
    webViewVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    val darkMode by settingsViewModel.darkMode.collectAsState()
    val detailZoom by settingsViewModel.detailZoom.collectAsState()
    val isPronunciationDict = definition.contains("cepd18.css")

    log.i("WordDetailScreen", "Rendering: word='$word', defLength=${definition.length}, dictName='$dictionaryName', cssLength=${css.length}")

    LaunchedEffect(detailZoom) {
        val zoomLevel = (detailZoom - 1.0f) * 3.0
        io.github.gdict.ui.webview.setBrowserZoom(zoomLevel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                if (isPronunciationDict) "发音" else word,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            if (!isPronunciationDict) {
                ActionButtonsRow(
                    isBookmarked = isBookmarked,
                    onToggleBookmark = onToggleBookmark,
                    onPronounce = {
                        if (isPlaying) return@ActionButtonsRow
                        isPlaying = true
                        coroutineScope.launch {
                            try {
                                var played = false
                                val audioData = withContext(Dispatchers.IO) {
                                    dictionaryRepository.getAudioResource(word)
                                }
                                if (audioData != null && audioData.isNotEmpty()) {
                                    withContext(Dispatchers.IO) {
                                        DesktopAudioPlayer.play(audioData)
                                    }
                                    played = true
                                }
                                if (!played) {
                                    val edgeTtsData = withContext(Dispatchers.IO) {
                                        EdgeTtsClient.synthesize(word)
                                    }
                                    if (edgeTtsData != null) {
                                        withContext(Dispatchers.IO) {
                                            DesktopAudioPlayer.play(edgeTtsData)
                                        }
                                    } else {
                                        log.w("WordDetailScreen", "No audio available for '$word'")
                                    }
                                }
                            } catch (e: Throwable) {
                                log.e("WordDetailScreen", "Pronunciation failed: ${e.message}")
                            } finally {
                                delay(500)
                                isPlaying = false
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            DefinitionCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                definition = definition,
                css = css,
                darkMode = darkMode,
                dictionaryRepository = dictionaryRepository,
                detailZoom = detailZoom,
                onZoomChange = { settingsViewModel.setDetailZoom(it) },
                onEntryClick = onEntryClick,
                webViewVisible = webViewVisible,
                onPlayAudio = { audioPath ->
                    log.i("WordDetailScreen", "onPlayAudio called with path: $audioPath")
                    val cleanPath = audioPath.replace(Regex("[\\x00-\\x1f\\x7f]"), "")
                    val fallbackWord = cleanPath.removeSuffix(".mp3")
                        .removeSuffix(".wav")
                        .removeSuffix(".ogg")
                        .removeSuffix(".spx")
                        .substringAfterLast("/")
                        .substringAfterLast("\\")
                    coroutineScope.launch {
                        try {
                            var played = false
                            val audioData = withContext(Dispatchers.IO) {
                                dictionaryRepository.getAudioResourceByPath(cleanPath)
                            }
                            if (audioData != null && audioData.isNotEmpty()) {
                                log.i("WordDetailScreen", "Playing dict audio: ${audioData.size} bytes from '$cleanPath'")
                                withContext(Dispatchers.IO) {
                                    DesktopAudioPlayer.play(audioData)
                                }
                                played = true
                            }
                            if (!played) {
                                val audioData2 = withContext(Dispatchers.IO) {
                                    dictionaryRepository.getAudioResource(fallbackWord)
                                }
                                if (audioData2 != null && audioData2.isNotEmpty()) {
                                    log.i("WordDetailScreen", "Playing dict audio by word: ${audioData2.size} bytes for '$fallbackWord'")
                                    withContext(Dispatchers.IO) {
                                        DesktopAudioPlayer.play(audioData2)
                                    }
                                    played = true
                                }
                            }
                            if (!played) {
                                log.i("WordDetailScreen", "No dict audio, trying TTS for '$fallbackWord'")
                                val edgeTtsData = withContext(Dispatchers.IO) {
                                    EdgeTtsClient.synthesize(fallbackWord)
                                }
                                if (edgeTtsData != null) {
                                    withContext(Dispatchers.IO) {
                                        DesktopAudioPlayer.play(edgeTtsData)
                                    }
                                } else {
                                    log.w("WordDetailScreen", "No audio available for '$cleanPath'")
                                }
                            }
                        } catch (e: Throwable) {
                            log.e("WordDetailScreen", "Audio playback failed: ${e.javaClass.simpleName}: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onPronounce: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            text = if (isBookmarked) "Saved" else "Add to Favorites",
            modifier = Modifier.weight(1f),
            onClick = onToggleBookmark
        )
        ActionButton(
            icon = Icons.Default.VolumeUp,
            text = "Pronounce",
            modifier = Modifier.weight(1f),
            onClick = onPronounce
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DefinitionCard(
    definition: String,
    css: String,
    darkMode: Boolean,
    dictionaryRepository: DictionaryRepository,
    detailZoom: Float,
    onZoomChange: (Float) -> Unit = {},
    onEntryClick: (String) -> Unit = {},
    onPlayAudio: (String) -> Unit = {},
    webViewVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Definitions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onZoomChange((detailZoom - 0.2f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        java.lang.String.format("%.0f%%", detailZoom * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onZoomChange((detailZoom + 0.2f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                MdxWebView(
                    definition = definition,
                    css = css,
                    darkMode = darkMode,
                    dictionaryRepository = dictionaryRepository,
                    onEntryClick = onEntryClick,
                    onPlayAudio = onPlayAudio,
                    webViewVisible = webViewVisible
                )
            }
        }
    }
}

private fun extractPartOfSpeech(definition: String): String {
    if (definition.isBlank()) return ""
    val posPatterns = listOf(
        Regex("<pos>([^<]+)</pos>", RegexOption.IGNORE_CASE),
        Regex("<(?:span|font)[^>]*>(adj|adv|n|v|pron|prep|conj|interj|art|num|modal|det)[.;]?\\s*</(?:span|font)>", RegexOption.IGNORE_CASE),
        Regex("\\b(adj\\.|adv\\.|n\\.|v\\.|pron\\.|prep\\.|conj\\.|interj\\.|art\\.|num\\.|modal\\.|det\\.)\\s*", RegexOption.IGNORE_CASE),
        Regex("<(?:b|strong)[^>]*>([^<]{1,20})</(?:b|strong)>", RegexOption.IGNORE_CASE),
        Regex("(noun|verb|adjective|adverb|pronoun|preposition|conjunction|interjection|article|numeral|determiner|modal verb)[.,;]?\\s*", RegexOption.IGNORE_CASE)
    )
    for (pattern in posPatterns) {
        val match = pattern.find(definition)
        if (match != null) {
            val raw = match.groupValues[1].trim().lowercase()
            return when {
                raw.startsWith("adj") -> "adj."
                raw.startsWith("adv") -> "adv."
                raw.startsWith("n") && !raw.startsWith("num") -> "n."
                raw.startsWith("v") -> "v."
                raw.startsWith("pron") -> "pron."
                raw.startsWith("prep") -> "prep."
                raw.startsWith("conj") -> "conj."
                raw.startsWith("interj") -> "interj."
                raw.startsWith("art") -> "art."
                raw.startsWith("num") -> "num."
                raw.startsWith("modal") -> "modal."
                raw.startsWith("det") -> "det."
                raw == "noun" -> "n."
                raw == "verb" -> "v."
                raw == "adjective" -> "adj."
                raw == "adverb" -> "adv."
                raw == "pronoun" -> "pron."
                raw == "preposition" -> "prep."
                raw == "conjunction" -> "conj."
                raw == "interjection" -> "interj."
                raw == "article" -> "art."
                raw == "numeral" -> "num."
                raw == "determiner" -> "det."
                else -> raw
            }
        }
    }
    return ""
}
