package io.github.gdict.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import io.github.gdict.ui.components.acrylicAmbientBackground
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
    val isPronunciationDict = definition.contains("cepd18.css", ignoreCase = true) ||
            definition.contains("<arl", ignoreCase = true) ||
            definition.contains("<prongrp", ignoreCase = true) ||
            definition.contains("<soundfile", ignoreCase = true)

    // 柯林斯词典检测：通过词典名、CSS引用、HTML特征识别
    val isCollinsDict = dictionaryName.contains("collins", ignoreCase = true) ||
            dictionaryName.contains("柯林斯", ignoreCase = true) ||
            dictionaryName.contains("cobuild", ignoreCase = true) ||
            css.contains("collins", ignoreCase = true) ||
            css.contains("ccald", ignoreCase = true) ||
            css.contains("cobuild", ignoreCase = true) ||
            definition.contains("ccald", ignoreCase = true) ||
            definition.contains("cobuild", ignoreCase = true)
    android.util.Log.d("WordDetail", "isPronunciationDict=$isPronunciationDict isCollinsDict=$isCollinsDict dict=$dictionaryName word=$word cssHead=${css.take(100)} defHead=${definition.take(300)}")

    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
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

    val playPronunciationAudio: (String?, String) -> Unit = { audioPath, fallbackWord ->
        coroutineScope.launch {
            try {
                var played = false
                if (audioPath != null) {
                    val mddAudio = withContext(Dispatchers.IO) {
                        dictionaryRepository.getAudioResourceByPath(audioPath)
                    }
                    if (mddAudio != null) {
                        played = withContext(Dispatchers.IO) { AudioPlayer.play(context, mddAudio) }
                    }
                }
                if (!played) {
                    val mddAudio = withContext(Dispatchers.IO) {
                        dictionaryRepository.getAudioResource(fallbackWord)
                    }
                    if (mddAudio != null) {
                        played = withContext(Dispatchers.IO) { AudioPlayer.play(context, mddAudio) }
                    }
                }
                if (!played) {
                    val edgeTtsData = withContext(Dispatchers.IO) { EdgeTtsClient.synthesize(fallbackWord) }
                    if (edgeTtsData != null) {
                        played = withContext(Dispatchers.IO) { AudioPlayer.play(context, edgeTtsData) }
                    }
                }
                if (!played) {
                    val engine = tts
                    if (engine != null && ttsReady) {
                        engine.speak(fallbackWord, TextToSpeech.QUEUE_FLUSH, null, "pron_${System.currentTimeMillis()}")
                    }
                }
            } catch (_: Exception) {
                val engine = tts
                if (engine != null && ttsReady) {
                    engine.speak(fallbackWord, TextToSpeech.QUEUE_FLUSH, null, "pron_${System.currentTimeMillis()}")
                }
            }
        }
    }

    if (isPronunciationDict) {
        PronunciationDetailContent(
            word = word,
            definition = definition,
            css = css,
            isBookmarked = isBookmarked,
            darkMode = darkMode,
            dictionaryRepository = dictionaryRepository,
            onBack = onBack,
            onToggleBookmark = onToggleBookmark,
            onEntryClick = onEntryClick,
            onShare = { },
            playAudio = playPronunciationAudio
        )
        return
    }

    if (isCollinsDict) {
        CollinsDetailContent(
            word = word,
            definition = definition,
            css = css,
            dictionaryName = dictionaryName,
            isBookmarked = isBookmarked,
            darkMode = darkMode,
            dictionaryRepository = dictionaryRepository,
            onBack = onBack,
            onToggleBookmark = onToggleBookmark,
            onEntryClick = onEntryClick,
            onShare = { },
            playAudio = playPronunciationAudio
        )
        return
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    if (isPronunciationDict) "发音" else stringResource(R.string.tab_origin),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
                    .background(glassBg)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isPronunciationDict) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                word,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = GdictColors.Primary
                            )
                            val partOfSpeech = remember(definition) { extractPartOfSpeech(definition) }
                            if (partOfSpeech.isNotEmpty()) {
                                Text(
                                    partOfSpeech,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = subtitleColor
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
                                            var played = false

                                            val mddAudio = withContext(Dispatchers.IO) {
                                                dictionaryRepository.getAudioResource(word)
                                            }
                                            if (mddAudio != null) {
                                                played = withContext(Dispatchers.IO) {
                                                    AudioPlayer.play(context, mddAudio)
                                                }
                                            }

                                            if (!played) {
                                                val edgeTtsData = withContext(Dispatchers.IO) {
                                                    EdgeTtsClient.synthesize(word)
                                                }
                                                if (edgeTtsData != null) {
                                                    played = withContext(Dispatchers.IO) {
                                                        AudioPlayer.play(context, edgeTtsData)
                                                    }
                                                }
                                            }

                                            if (!played) {
                                                val engine = tts
                                                if (engine != null && ttsReady) {
                                                    engine.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
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
                    }

                    if (!isPronunciationDict) {
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
                    glassBg = glassBg,
                    glassBorder = glassBorder,
                    darkMode = darkMode,
                    onToggleBookmark = onToggleBookmark
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> DefinitionCard(
                        definition = definition,
                        css = css,
                        glassBg = glassBg,
                        glassBorder = glassBorder,
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
                                var played = false

                                val mddAudio = withContext(Dispatchers.IO) {
                                    dictionaryRepository.getAudioResourceByPath(audioPath)
                                        ?: dictionaryRepository.getAudioResource(fallbackWord)
                                }
                                if (mddAudio != null) {
                                    played = withContext(Dispatchers.IO) {
                                        AudioPlayer.play(context, mddAudio)
                                    }
                                }

                                if (!played) {
                                    val edgeTtsData = withContext(Dispatchers.IO) {
                                        EdgeTtsClient.synthesize(fallbackWord)
                                    }
                                    if (edgeTtsData != null) {
                                        played = withContext(Dispatchers.IO) {
                                            AudioPlayer.play(context, edgeTtsData)
                                        }
                                    }
                                }

                                if (!played) {
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
                    1 -> {
                        val examples = remember(definition) { dictionaryRepository.extractExamples(definition) }
                        ExamplesCard(examples = examples, glassBg = glassBg, glassBorder = glassBorder, textColor = textColor, subtitleColor = subtitleColor, darkMode = darkMode)
                    }
                    2 -> {
                        val synonyms = remember(definition) { dictionaryRepository.extractSynonyms(definition) }
                        SynonymsCard(synonyms = synonyms, glassBg = glassBg, glassBorder = glassBorder, textColor = textColor, subtitleColor = subtitleColor, darkMode = darkMode, onEntryClick = onEntryClick)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ExamplesCard(
    examples: List<String>,
    glassBg: Color,
    glassBorder: Color,
    textColor: Color,
    subtitleColor: Color,
    darkMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
            .background(glassBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.tab_examples),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GdictColors.Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (examples.isEmpty()) {
                Text(
                    "暂无例句",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            } else {
                examples.forEach { example ->
                    Text(
                        "\u2022 $example",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynonymsCard(
    synonyms: List<String>,
    glassBg: Color,
    glassBorder: Color,
    textColor: Color,
    subtitleColor: Color,
    darkMode: Boolean,
    onEntryClick: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
            .background(glassBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.tab_synonyms),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GdictColors.Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (synonyms.isEmpty()) {
                Text(
                    "暂无同义词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    synonyms.forEach { synonym ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GdictColors.BluePrimaryLight.copy(alpha = 0.25f),
                            modifier = Modifier.clickable { onEntryClick(synonym) }
                        ) {
                            Text(
                                synonym,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = GdictColors.Primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
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
    val selectedBg = GdictColors.BluePrimaryLight.copy(alpha = 0.25f)
    val selectedColor = GdictColors.Primary
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
    glassBg: Color,
    glassBorder: Color,
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
            glassBg = glassBg,
            glassBorder = glassBorder,
            darkMode = darkMode,
            modifier = Modifier.weight(1f),
            onClick = onToggleBookmark
        )
        ActionButton(
            icon = Icons.Default.Share,
            text = "Share",
            glassBg = glassBg,
            glassBorder = glassBorder,
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
    glassBg: Color,
    glassBorder: Color,
    darkMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val iconTint = GdictColors.Primary
    val textTint = GdictColors.Primary

    Box(
        modifier = modifier
            .height(44.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(16.dp))
            .background(glassBg)
            .clickable(onClick = onClick)
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
    glassBg: Color,
    glassBorder: Color,
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
    val scaledCornerRadius = (20.dp * contentScale).coerceIn(8.dp, 24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(scaledCornerRadius))
            .clip(RoundedCornerShape(scaledCornerRadius))
            .border(0.5.dp, glassBorder, RoundedCornerShape(scaledCornerRadius))
            .background(glassBg)
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
                color = GdictColors.Primary
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
