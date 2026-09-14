package io.github.gdict.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val isPronunciationDict = definition.contains("cepd18.css", ignoreCase = true) ||
        (definition.contains("<prongrp", ignoreCase = true) &&
            (definition.contains("uk_sound.png", ignoreCase = true) || definition.contains("us_sound.png", ignoreCase = true)))
    val isCollinsDict = (dictionaryName.contains("collins", ignoreCase = true) || dictionaryName.contains("柯林斯", ignoreCase = true)) &&
        isCollinsEntry(definition)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.setLanguage(Locale.US)
                ttsReady = true
            }
        }
        tts = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    val playAudio: (String?, String) -> Unit = { path, fallbackWord ->
        scope.launch {
            try {
                var played = false
                if (path != null) {
                    val bytes = withContext(Dispatchers.IO) { dictionaryRepository.getAudioResourceByPath(path) }
                    if (bytes != null) played = withContext(Dispatchers.IO) { AudioPlayer.play(context, bytes) }
                }
                if (!played) {
                    val bytes = withContext(Dispatchers.IO) { dictionaryRepository.getAudioResource(fallbackWord) }
                    if (bytes != null) played = withContext(Dispatchers.IO) { AudioPlayer.play(context, bytes) }
                }
                if (!played) {
                    val bytes = withContext(Dispatchers.IO) { EdgeTtsClient.synthesize(fallbackWord) }
                    if (bytes != null) played = withContext(Dispatchers.IO) { AudioPlayer.play(context, bytes) }
                }
                if (!played && ttsReady) tts?.speak(fallbackWord, TextToSpeech.QUEUE_FLUSH, null, "gdict_${System.currentTimeMillis()}")
            } catch (_: Exception) {
                if (ttsReady) tts?.speak(fallbackWord, TextToSpeech.QUEUE_FLUSH, null, "gdict_${System.currentTimeMillis()}")
            }
        }
    }

    if (isPronunciationDict) {
        PronunciationDetailContent(
            word, definition, css, isBookmarked, darkMode, dictionaryRepository,
            onBack, onToggleBookmark, onEntryClick, {}, playAudio
        )
        return
    }
    if (isCollinsDict) {
        CollinsDetailContent(
            word, definition, css, dictionaryName, isBookmarked, darkMode, dictionaryRepository,
            onBack, onToggleBookmark, onEntryClick, {}, playAudio
        )
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_origin), stringResource(R.string.tab_examples), stringResource(R.string.tab_synonyms))
    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Column(modifier = Modifier.fillMaxSize().background(background)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = textColor)
            }
            Text(
                text = dictionaryName.ifBlank { stringResource(R.string.tab_origin) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share), tint = secondary)
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = textColor)
                            val pos = remember(definition) { extractPartOfSpeech(definition) }
                            if (pos.isNotBlank()) Text(pos, style = MaterialTheme.typography.bodyMedium, color = secondary)
                        }
                        Surface(
                            modifier = Modifier.size(48.dp).clickable(enabled = !isPlaying) {
                                isPlaying = true
                                playAudio(null, word)
                                scope.launch { delay(500); isPlaying = false }
                            },
                            shape = CircleShape,
                            color = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VolumeUp, contentDescription = stringResource(R.string.cd_pronunciation), tint = GdictColors.Primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEachIndexed { index, tab ->
                            TextButton(
                                onClick = { selectedTab = index },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = if (selectedTab == index) GdictColors.Primary else secondary
                                )
                            ) {
                                Text(tab, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBookmarked) stringResource(R.string.saved) else stringResource(R.string.add_to_favorites))
                }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.cd_share))
                }
            }
            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> ContentSurface(darkMode) {
                    Text("Definitions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = secondary)
                    Spacer(Modifier.height(10.dp))
                    MdxWebView(
                        definition = definition,
                        css = css,
                        darkMode = darkMode,
                        contentScale = 1f,
                        dictionaryRepository = dictionaryRepository,
                        onEntryClick = onEntryClick,
                        onPlayAudio = { path ->
                            val fallback = path.substringAfterLast('/').substringAfterLast('\\')
                                .removeSuffix(".mp3").removeSuffix(".wav").removeSuffix(".ogg").removeSuffix(".spx")
                            playAudio(path, fallback)
                        }
                    )
                }
                1 -> ContentSurface(darkMode) {
                    val examples = remember(definition) { dictionaryRepository.extractExamples(definition) }
                    Text(stringResource(R.string.tab_examples), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = secondary)
                    Spacer(Modifier.height(10.dp))
                    if (examples.isEmpty()) Text("暂无例句", color = secondary) else examples.forEach { example ->
                        Text("• $example", style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.padding(vertical = 5.dp))
                    }
                }
                2 -> ContentSurface(darkMode) {
                    val synonyms = remember(definition) { dictionaryRepository.extractSynonyms(definition) }
                    Text(stringResource(R.string.tab_synonyms), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = secondary)
                    Spacer(Modifier.height(10.dp))
                    if (synonyms.isEmpty()) Text("暂无同义词", color = secondary) else SynonymFlow(synonyms, darkMode, onEntryClick)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContentSurface(darkMode: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynonymFlow(synonyms: List<String>, darkMode: Boolean, onEntryClick: (String) -> Unit) {
    val fill = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        synonyms.forEach { synonym ->
            Surface(shape = RoundedCornerShape(12.dp), color = fill, modifier = Modifier.clickable { onEntryClick(synonym) }) {
                Text(synonym, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = GdictColors.Primary)
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
        val match = pattern.find(definition) ?: continue
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
    return ""
}
