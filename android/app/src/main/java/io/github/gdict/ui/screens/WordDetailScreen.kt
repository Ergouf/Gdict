package io.github.gdict.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import io.github.gdict.R
import io.github.gdict.data.AndroidDictionaryRepository
import io.github.gdict.tts.EdgeTtsClient
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.webview.AudioPlayer
import io.github.gdict.ui.webview.MdxWebView
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    dictionaryRepository: AndroidDictionaryRepository,
    settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_origin), stringResource(R.string.tab_examples), stringResource(R.string.tab_synonyms))

    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val cardColor = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var contentScale by remember { mutableStateOf(1f) }

    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.setLanguage(Locale.US)
                ttsReady = true
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = GdictColors.OnSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    stringResource(R.string.tab_origin),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.cd_share),
                        tint = GdictColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                word,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            val partOfSpeech = remember(definition) { extractPartOfSpeech(definition) }
                            if (partOfSpeech.isNotEmpty()) {
                                Text(
                                    partOfSpeech,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GdictColors.OnSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GdictColors.PrimarySoft.copy(alpha = 0.1f))
                                .clickable {
                                    if (isPlaying) return@clickable
                                    isPlaying = true
                                    coroutineScope.launch {
                                        try {
                                            val edgeTtsData = withContext(Dispatchers.IO) {
                                                EdgeTtsClient.synthesize(word)
                                            }
                                            if (edgeTtsData != null) {
                                                withContext(Dispatchers.IO) {
                                                    AudioPlayer.play(context, edgeTtsData)
                                                }
                                            } else {
                                                val audioData = dictionaryRepository.getAudioResource(word)
                                                if (audioData != null) {
                                                    withContext(Dispatchers.IO) {
                                                        AudioPlayer.play(context, audioData)
                                                    }
                                                } else {
                                                    val engine = tts
                                                    if (engine != null && ttsReady) {
                                                        engine.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {
                                            val engine = tts
                                            if (engine != null && ttsReady) {
                                                engine.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
                                            }
                                        } finally {
                                            delay(500)
                                            isPlaying = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = stringResource(R.string.cd_pronunciation),
                                tint = GdictColors.PrimarySoft,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            TabButton(
                                text = tab,
                                isSelected = selectedTab == index,
                                darkMode = darkMode,
                                onClick = { selectedTab = index }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ActionButtonsRow(
                    isBookmarked = isBookmarked,
                    cardColor = cardColor,
                    darkMode = darkMode,
                    onToggleBookmark = onToggleBookmark
                )

                Spacer(modifier = Modifier.height(16.dp))

                DefinitionCard(
                    definition = definition,
                    css = css,
                    cardColor = cardColor,
                    textColor = textColor,
                    darkMode = darkMode,
                    contentScale = contentScale,
                    dictionaryRepository = dictionaryRepository,
                    onEntryClick = onEntryClick,
                    onPlayAudio = { audioPath ->
                        val fallbackWord = audioPath.removeSuffix(".mp3")
                            .removeSuffix(".wav")
                            .removeSuffix(".ogg")
                            .removeSuffix(".spx")
                            .substringAfterLast("/")
                            .substringAfterLast("\\")
                        coroutineScope.launch {
                            try {
                                val edgeTtsData = withContext(Dispatchers.IO) {
                                    EdgeTtsClient.synthesize(fallbackWord)
                                }
                                if (edgeTtsData != null) {
                                    withContext(Dispatchers.IO) {
                                        AudioPlayer.play(context, edgeTtsData)
                                    }
                                } else {
                                    val engine = tts
                                    if (engine != null && ttsReady) {
                                        engine.speak(fallbackWord, TextToSpeech.QUEUE_FLUSH, null, "audio_${System.currentTimeMillis()}")
                                    }
                                }
                            } catch (_: Exception) {
                                val engine = tts
                                if (engine != null && ttsReady) {
                                    engine.speak(fallbackWord, TextToSpeech.QUEUE_FLUSH, null, "audio_${System.currentTimeMillis()}")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    darkMode: Boolean = false,
    onClick: () -> Unit
) {
    val selectedBg = if (darkMode) GdictColors.PrimarySoft.copy(alpha = 0.2f) else GdictColors.PrimarySoft.copy(alpha = 0.1f)
    val selectedColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.PrimarySoft
    val unselectedColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) selectedBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) selectedColor else unselectedColor
        )
    }
}

@Composable
private fun ActionButtonsRow(
    isBookmarked: Boolean,
    cardColor: Color,
    darkMode: Boolean = false,
    onToggleBookmark: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            text = if (isBookmarked) stringResource(R.string.saved) else stringResource(R.string.add_to_favorites),
            cardColor = cardColor,
            darkMode = darkMode,
            modifier = Modifier.weight(1f),
            onClick = onToggleBookmark
        )
        ActionButton(
            icon = Icons.Default.Share,
            text = "Share",
            cardColor = cardColor,
            darkMode = darkMode,
            modifier = Modifier.weight(1f),
            onClick = { }
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    cardColor: Color,
    darkMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val iconTint = if (darkMode) GdictColors.DarkOnSurface else GdictColors.PrimarySoft
    val textTint = if (darkMode) GdictColors.DarkOnSurface else GdictColors.PrimarySoft

    Card(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
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
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = textTint
            )
        }
    }
}

@Composable
private fun DefinitionCard(
    definition: String,
    css: String,
    cardColor: Color,
    textColor: Color,
    darkMode: Boolean,
    contentScale: Float = 1f,
    dictionaryRepository: AndroidDictionaryRepository,
    onEntryClick: (String) -> Unit = {},
    onPlayAudio: (String) -> Unit = {}
) {
    val scaledPadding = (20.dp * contentScale)
    val scaledTitleFontSize = (14.sp * contentScale)
    val scaledTitleLineHeight = (20.sp * contentScale)
    val scaledSpacerHeight = (12.dp * contentScale)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape((16.dp * contentScale).coerceIn(8.dp, 24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(scaledPadding)
        ) {
            Text(
                "Definitions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = scaledTitleFontSize,
                    lineHeight = scaledTitleLineHeight
                ),
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(scaledSpacerHeight))
            MdxWebView(
                definition = definition,
                css = css,
                darkMode = darkMode,
                contentScale = contentScale,
                dictionaryRepository = dictionaryRepository,
                onEntryClick = onEntryClick,
                onPlayAudio = onPlayAudio
            )
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
