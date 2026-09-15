package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.R
import androidx.compose.ui.res.stringResource
import io.github.gdict.data.AndroidDictionaryRepository
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.webview.MdxWebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class PronunciationEntry(
    val region: String,
    val ipa: String,
    val audioPath: String?
)

private data class PronunciationData(
    val word: String,
    val pronunciations: List<PronunciationEntry>,
    val parsedOk: Boolean
)

@Composable
fun PronunciationDetailContent(
    word: String,
    definition: String,
    css: String,
    isBookmarked: Boolean,
    darkMode: Boolean,
    dictionaryRepository: AndroidDictionaryRepository,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEntryClick: (String) -> Unit,
    onShare: () -> Unit,
    playAudio: (audioPath: String?, fallbackWord: String) -> Unit
) {
    val data = remember(definition, word) { parsePronunciationData(definition, word) }
    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val displayWord = data.word.ifBlank { word }

    Column(modifier = Modifier.fillMaxSize().background(background)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircleButton(onClick = onBack, glassBg = surface, glassBorder = outline) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = textColor, modifier = Modifier.size(22.dp))
            }
            Text(
                text = stringResource(R.string.cd_pronunciation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            GlassCircleButton(onClick = onShare, glassBg = surface, glassBorder = outline) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share), tint = secondary, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PronActionButton(
                    icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    text = if (isBookmarked) stringResource(R.string.saved) else stringResource(R.string.add_to_favorites),
                    glassBg = surface,
                    glassBorder = outline,
                    darkMode = darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleBookmark
                )
                PronActionButton(
                    icon = Icons.Default.Share,
                    text = stringResource(R.string.cd_share),
                    glassBg = surface,
                    glassBorder = outline,
                    darkMode = darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = displayWord,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = textColor
                    )
                    if (data.pronunciations.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        data.pronunciations.forEach { pronunciation ->
                            PronunciationRow(pronunciation, darkMode) {
                                playAudio(pronunciation.audioPath, word)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.tab_origin),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    MdxWebView(
                        definition = definition,
                        css = css + HIDE_PRON_CSS,
                        darkMode = darkMode,
                        contentScale = 1f,
                        dictionaryRepository = dictionaryRepository,
                        fallbackWord = word,
                        onEntryClick = onEntryClick,
                        onPlayAudio = { path -> playAudio(path, word) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PronunciationRow(pronunciation: PronunciationEntry, darkMode: Boolean, onPlay: () -> Unit) {
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val fill = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().background(fill, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pronunciation.region.ifBlank { "PRON" },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = secondary,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = pronunciation.ipa,
            style = MaterialTheme.typography.bodyLarge,
            color = if (pronunciation.ipa.isBlank()) secondary else GdictColors.Primary,
            modifier = Modifier.weight(1f)
        )
        SpeakerButton(onPlay, size = 40.dp)
    }
}

@Composable
internal fun GlassCircleButton(
    onClick: () -> Unit,
    glassBg: Color,
    glassBorder: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = glassBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, glassBorder)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
internal fun SpeakerButton(onPlay: () -> Unit, size: Dp = 40.dp) {
    var playing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fill = if (playing) GdictColors.Primary else GdictColors.PrimaryContainer
    val tint = if (playing) GdictColors.OnPrimary else GdictColors.Primary
    Surface(
        modifier = Modifier.size(size).clickable {
            if (!playing) {
                playing = true
                onPlay()
                scope.launch { delay(900); playing = false }
            }
        },
        shape = CircleShape,
        color = fill
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.VolumeUp, contentDescription = stringResource(R.string.cd_pronunciation), tint = tint, modifier = Modifier.size((size.value * 0.48f).dp))
        }
    }
}

@Composable
internal fun PronActionButton(
    icon: ImageVector,
    text: String,
    glassBg: Color,
    glassBorder: Color,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = tint)
    }
}

/** Compatibility for Collins while Phase 4 converges all detail screens. */
internal fun pronunciationBgGradient(darkMode: Boolean): Brush {
    val color = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    return Brush.verticalGradient(listOf(color, color))
}

@Composable
internal fun Modifier.pronunciationAmbientBackground(
    darkMode: Boolean,
    screenWidthPx: Float,
    screenHeightPx: Float
): Modifier = this

private const val HIDE_PRON_CSS = "\n.cpepd .main-headword,.cpepd .main-ipa,.cpepd .main-pronunciation,.cpepd .main-audio-btns,.cpepd .cepd-forms-section{display:none !important;}"

private fun parsePronunciationData(definition: String, fallbackWord: String): PronunciationData {
    val primaryDefinition = extractPrimaryEpdEntry(definition)
    val word = extractHeadword(primaryDefinition).ifBlank { fallbackWord }
    val pronunciations = parsePronunciations(primaryDefinition)
    return PronunciationData(word, pronunciations, pronunciations.isNotEmpty())
}

private fun extractPrimaryEpdEntry(definition: String): String {
    val headPattern = Regex(
        """<(?:span|div)[^>]*class=["'][^"']*\bdi-head\b[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    val heads = headPattern.findAll(definition).toList()
    return if (heads.size > 1) definition.substring(0, heads[1].range.first) else definition
}

internal fun parsePronunciations(definition: String): List<PronunciationEntry> {
    val primaryDefinition = extractPrimaryEpdEntry(definition)
    val flagPattern = Regex(
        """<img[^>]*src=["'][^"']*(uk_sound|us_sound)\.png[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    val soundfileEntries = Regex(
        """<soundfile\b[^>]*>(.*?)</soundfile>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    ).findAll(primaryDefinition).mapNotNull { soundfile ->
        val content = soundfile.groupValues[1]
        val region = flagPattern.find(content)?.groupValues?.get(1) ?: return@mapNotNull null
        val audioPath = extractSoundPath(content) ?: return@mapNotNull null
        (if (region.equals("uk_sound", ignoreCase = true)) "UK" else "US") to audioPath
    }.toList()
    val flags = flagPattern.findAll(primaryDefinition).toList()
    val audios = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .findAll(primaryDefinition).map { it.groupValues[1] }.toList()

    // EPD puts the sound link and its region marker in the same soundfile
    // element. Do not derive a link from the text between two flags: the
    // next region's link can occur there because the link precedes its image.
    if (soundfileEntries.isNotEmpty()) {
        val ipa = extractIpa(primaryDefinition)
        return soundfileEntries.map { (region, audioPath) ->
            PronunciationEntry(region, ipa, audioPath)
        }
    }

    if (flags.isNotEmpty()) {
        return flags.mapIndexed { index, flag ->
            val start = flag.range.last + 1
            val end = flags.getOrNull(index + 1)?.range?.first ?: primaryDefinition.length
            val segment = primaryDefinition.substring(start, end)
            PronunciationEntry(
                region = if (flag.groupValues[1].equals("uk_sound", true)) "UK" else "US",
                ipa = extractIpa(segment),
                audioPath = extractSoundPath(segment) ?: audios.getOrNull(index)
            )
        }
    }

    val ipa = extractIpa(primaryDefinition)
    if (ipa.isNotBlank() || audios.isNotEmpty()) {
        return listOf(PronunciationEntry("", ipa, audios.firstOrNull()))
    }
    return emptyList()
}

private fun extractHeadword(content: String): String =
    Regex("""<hw[^>]*>(.*?)</hw>""", RegexOption.DOT_MATCHES_ALL)
        .find(content)?.groupValues?.get(1)?.replace("|", "")?.let(::cleanText) ?: ""

private fun extractIpa(content: String): String {
    val patterns = listOf(
        Regex("""<span[^>]*class=["'][^"']*phon[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL),
        Regex("""<span[^>]*class=["'][^"']*\bipa\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL),
        Regex("""<ipa[^>]*>(.*?)</ipa>""", RegexOption.DOT_MATCHES_ALL)
    )
    patterns.forEach { pattern ->
        val value = pattern.find(content)?.groupValues?.get(1)?.let(::cleanText).orEmpty()
        if (value.isNotBlank()) return value
    }
    return Regex("""/([^/<>]{2,40})/""").find(cleanText(content))?.groupValues?.get(1)?.let { "/$it/" }.orEmpty()
}

private fun extractSoundPath(content: String): String? =
    Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(content)?.groupValues?.get(1)

private fun cleanText(html: String): String = html
    .replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&#39;", "'")
    .replace("&quot;", "\"")
    .replace(Regex("\\s+"), " ")
    .trim()
