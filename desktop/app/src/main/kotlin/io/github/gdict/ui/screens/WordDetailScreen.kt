@file:OptIn(ExperimentalComposeUiApi::class)

package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.core.GdictLogger
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.tts.EdgeTtsClient
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.components.pageEnterAnimation
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.webview.DesktopAudioPlayer
import io.github.gdict.ui.webview.MdxWebView
import io.github.gdict.viewmodel.BookmarkViewModel
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
    onBack: () -> Unit,
    onEntryClick: (String) -> Unit = {},
    dictionaryRepository: DictionaryRepository,
    settingsViewModel: SettingsViewModel,
    bookmarkViewModel: BookmarkViewModel,
    webViewVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val darkMode by settingsViewModel.darkMode.collectAsState()
    val detailZoom by settingsViewModel.detailZoom.collectAsState()
    val bookmarksByWord by bookmarkViewModel.bookmarksByWord.collectAsState()
    val isBookmarked by remember(word) {
        derivedStateOf { bookmarksByWord.containsKey(word) }
    }

    val coroutineScope = rememberCoroutineScope()

    val playPronunciationAudio: (String?, String) -> Unit = { audioPath, fallbackWord ->
        coroutineScope.launch {
            try {
                var played = false
                if (!audioPath.isNullOrBlank()) {
                    val mddAudio = withContext(Dispatchers.IO) {
                        dictionaryRepository.getAudioResourceByPath(audioPath)
                    }
                    if (mddAudio != null && mddAudio.isNotEmpty()) {
                        withContext(Dispatchers.IO) { DesktopAudioPlayer.play(mddAudio) }
                        played = true
                    }
                }
                if (!played) {
                    val mddAudio = withContext(Dispatchers.IO) {
                        dictionaryRepository.getAudioResource(fallbackWord)
                    }
                    if (mddAudio != null && mddAudio.isNotEmpty()) {
                        withContext(Dispatchers.IO) { DesktopAudioPlayer.play(mddAudio) }
                        played = true
                    }
                }
                if (!played) {
                    val edgeTtsData = withContext(Dispatchers.IO) {
                        EdgeTtsClient.synthesize(fallbackWord)
                    }
                    if (edgeTtsData != null) {
                        withContext(Dispatchers.IO) { DesktopAudioPlayer.play(edgeTtsData) }
                    }
                }
            } catch (e: Throwable) {
                log.e("WordDetailScreen", "playPronunciationAudio failed: ${e.message}")
            }
        }
    }

    val isPronunciationDict = definition.contains("cepd18.css", ignoreCase = true) ||
            (definition.contains("<prongrp", ignoreCase = true) &&
                    (definition.contains("uk_sound.png", ignoreCase = true) ||
                            definition.contains("us_sound.png", ignoreCase = true)))

    val isCollinsDict = (dictionaryName.contains("collins", ignoreCase = true) ||
            dictionaryName.contains("柯林斯", ignoreCase = true)) &&
            isCollinsEntry(definition)

    log.i(
        "WordDetailScreen",
        "word='$word', defLength=${definition.length}, dictName='$dictionaryName', " +
                "cssLength=${css.length}, isPronunciation=$isPronunciationDict, isCollins=$isCollinsDict"
    )

    LaunchedEffect(detailZoom) {
        val zoomLevel = (detailZoom - 1.0f) * 3.0
        io.github.gdict.ui.webview.setBrowserZoom(zoomLevel)
    }

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

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = with(density) { windowInfo.containerSize.width.toFloat() }
    val screenHeightPx = with(density) { windowInfo.containerSize.height.toFloat() }

    val onToggleBookmark: () -> Unit = {
        val existing = bookmarksByWord[word]
        if (existing != null) {
            bookmarkViewModel.removeBookmark(existing)
        } else {
            bookmarkViewModel.addBookmark(word, definition, dictionaryName)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
        when {
            isPronunciationDict -> PronunciationDetailContent(
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
                webViewVisible = webViewVisible,
                playAudio = playPronunciationAudio
            )

            isCollinsDict -> CollinsDetailContent(
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
                webViewVisible = webViewVisible,
                playAudio = playPronunciationAudio
            )

            else -> RegularDetailContent(
                word = word,
                definition = definition,
                css = css,
                dictionaryName = dictionaryName,
                isBookmarked = isBookmarked,
                darkMode = darkMode,
                detailZoom = detailZoom,
                dictionaryRepository = dictionaryRepository,
                settingsViewModel = settingsViewModel,
                onBack = onBack,
                onToggleBookmark = onToggleBookmark,
                onEntryClick = onEntryClick,
                webViewVisible = webViewVisible
            )
        }
    }
}

@Composable
private fun RegularDetailContent(
    word: String,
    definition: String,
    css: String,
    dictionaryName: String,
    isBookmarked: Boolean,
    darkMode: Boolean,
    detailZoom: Float,
    dictionaryRepository: DictionaryRepository,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEntryClick: (String) -> Unit,
    webViewVisible: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("词源", "例句", "同义词")
    var isPlaying by remember { mutableStateOf(false) }

    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val glassBorder = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    val playWordAudio: () -> Unit = {
        if (!isPlaying) {
        isPlaying = true
        coroutineScope.launch {
            try {
                var played = false
                val audioData = withContext(Dispatchers.IO) {
                    dictionaryRepository.getAudioResource(word)
                }
                if (audioData != null && audioData.isNotEmpty()) {
                    withContext(Dispatchers.IO) { DesktopAudioPlayer.play(audioData) }
                    played = true
                }
                if (!played) {
                    val edgeTtsData = withContext(Dispatchers.IO) { EdgeTtsClient.synthesize(word) }
                    if (edgeTtsData != null) {
                        withContext(Dispatchers.IO) { DesktopAudioPlayer.play(edgeTtsData) }
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
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "词源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    tint = subtitleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
                    .background(glassBg)
                    .pageEnterAnimation()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = GdictColors.Primary
                            )
                            val partOfSpeech = remember(definition) { extractPartOfSpeech(definition) }
                            if (partOfSpeech.isNotEmpty()) {
                                Text(
                                    text = partOfSpeech,
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
                                .clickable(onClick = playWordAudio),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Pronunciation",
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
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    definition = definition,
                    css = css,
                    glassBg = glassBg,
                    glassBorder = glassBorder,
                    darkMode = darkMode,
                    dictionaryRepository = dictionaryRepository,
                    detailZoom = detailZoom,
                    onZoomChange = { settingsViewModel.setDetailZoom(it) },
                    onEntryClick = onEntryClick,
                    webViewVisible = webViewVisible
                )

                1 -> {
                    val examples = remember(definition) { dictionaryRepository.extractExamples(definition) }
                    ExamplesCard(
                        examples = examples,
                        glassBg = glassBg,
                        glassBorder = glassBorder,
                        textColor = textColor,
                        subtitleColor = subtitleColor
                    )
                }

                2 -> {
                    val synonyms = remember(definition) { dictionaryRepository.extractSynonyms(definition) }
                    SynonymsCard(
                        synonyms = synonyms,
                        glassBg = glassBg,
                        glassBorder = glassBorder,
                        textColor = textColor,
                        subtitleColor = subtitleColor,
                        onEntryClick = onEntryClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
            color = if (isSelected) GdictColors.Primary else unselectedColor
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
            text = if (isBookmarked) "Saved" else "Add to Favorites",
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
    val iconTint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    val textTint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary

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
    darkMode: Boolean,
    dictionaryRepository: DictionaryRepository,
    detailZoom: Float,
    onZoomChange: (Float) -> Unit = {},
    onEntryClick: (String) -> Unit = {},
    onPlayAudio: (String) -> Unit = {},
    webViewVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
            .background(glassBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Definitions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GdictColors.Primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onZoomChange((detailZoom - 0.2f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ZoomOut,
                            contentDescription = null,
                            tint = GdictColors.OnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = java.lang.String.format("%.0f%%", detailZoom * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = GdictColors.OnSurfaceVariant
                    )
                    IconButton(
                        onClick = { onZoomChange((detailZoom + 0.2f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ZoomIn,
                            contentDescription = null,
                            tint = GdictColors.OnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
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

@Composable
private fun ExamplesCard(
    examples: List<String>,
    glassBg: Color,
    glassBorder: Color,
    textColor: Color,
    subtitleColor: Color
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
                text = "例句",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GdictColors.Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (examples.isEmpty()) {
                Text(
                    text = "暂无例句",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            } else {
                examples.forEach { example ->
                    Text(
                        text = "\u2022 $example",
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
                text = "同义词",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GdictColors.Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (synonyms.isEmpty()) {
                Text(
                    text = "暂无同义词",
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
                                text = synonym,
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

private fun isCollinsEntry(definition: String): Boolean {
    return definition.contains("◆") ||
            definition.contains("◇") ||
            definition.contains("#669900") ||
            definition.contains("color=\"#669900\"") ||
            definition.contains("color='#669900'") ||
            definition.contains("class=\"hom\"") ||
            definition.contains("class='hom'") ||
            definition.contains("class=\"sensenum\"") ||
            definition.contains("id=\"collins_english_dictionary\"")
}

@Deprecated("Use isCollinsEntry instead", ReplaceWith("isCollinsEntry(definition)"))
private fun isCollins3rdEntry(definition: String): Boolean = isCollinsEntry(definition)
