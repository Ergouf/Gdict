package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.R
import io.github.gdict.data.AndroidDictionaryRepository
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.webview.MdxWebView

data class CollinsEntry(
    val word: String,
    val pronunciations: List<CollinsPronunciation>,
    val definitions: List<CollinsDefinition>,
    val wordForms: String,
    val frequency: Int,
    val parsedOk: Boolean
)

data class CollinsPronunciation(
    val region: String,
    val ipa: String,
    val audioPath: String?
)

data class CollinsDefinition(
    val pos: String,
    val definition: String,
    val examples: List<String>
)

@Composable
fun CollinsDetailContent(
    word: String,
    definition: String,
    css: String,
    dictionaryName: String,
    isBookmarked: Boolean,
    darkMode: Boolean,
    dictionaryRepository: AndroidDictionaryRepository,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEntryClick: (String) -> Unit,
    onShare: () -> Unit,
    playAudio: (audioPath: String?, fallbackWord: String) -> Unit
) {
    val data = remember(definition, word) { parseCollinsEntry(definition, word) }
    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val primaryTint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
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
                text = dictionaryName.ifBlank { "Collins" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayWord,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        SpeakerButton(
                            onPlay = { playAudio(data.pronunciations.firstOrNull()?.audioPath, displayWord) },
                            size = 44.dp
                        )
                    }
                    if (data.frequency > 0) {
                        Spacer(Modifier.height(8.dp))
                        FrequencyDiamondsBlue(data.frequency, primaryTint)
                    }
                    if (data.wordForms.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(data.wordForms, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = secondary)
                    }
                    if (data.definitions.isNotEmpty()) {
                        CollinsSensesList(data.definitions, displayWord, textColor, secondary, primaryTint)
                    } else {
                        Spacer(Modifier.height(12.dp))
                        MdxWebView(
                            definition = definition,
                            css = css,
                            darkMode = darkMode,
                            contentScale = 1f,
                            dictionaryRepository = dictionaryRepository,
                            onEntryClick = onEntryClick,
                            onPlayAudio = { path -> playAudio(path, displayWord) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun buildAnnotatedDef(
    text: String,
    headword: String,
    baseColor: Color,
    highlightColor: Color
): AnnotatedString {
    if (headword.isBlank()) return buildAnnotatedString { append(text) }
    return buildAnnotatedString {
        var index = 0
        val source = text.lowercase()
        val needle = headword.lowercase()
        while (index < text.length) {
            val found = source.indexOf(needle, index)
            if (found < 0) {
                withStyle(SpanStyle(color = baseColor)) { append(text.substring(index)) }
                break
            }
            val end = found + headword.length
            val before = text.getOrNull(found - 1)
            val after = text.getOrNull(end)
            val boundary = before?.isLetter() != true && after?.isLetter() != true
            if (!boundary) {
                withStyle(SpanStyle(color = baseColor)) { append(text.substring(index, found + 1)) }
                index = found + 1
                continue
            }
            if (found > index) withStyle(SpanStyle(color = baseColor)) { append(text.substring(index, found)) }
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) { append(text.substring(found, end)) }
            index = end
        }
    }
}

@Composable
internal fun FrequencyDiamondsBlue(
    frequency: Int,
    primaryTint: Color,
    total: Int = 5
) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(total) { index ->
            Text(
                text = if (index < frequency) "◆" else "◇",
                fontSize = 14.sp,
                color = if (index < frequency) primaryTint else primaryTint.copy(alpha = 0.25f)
            )
        }
    }
}

fun isCollinsEntry(definition: String): Boolean =
    definition.contains("◆") || definition.contains("◇") ||
        definition.contains("669900", ignoreCase = true) ||
        definition.contains("class=\"hom\"") || definition.contains("class='hom'") ||
        definition.contains("class=\"sensenum\"") ||
        definition.contains("id=\"collins_english_dictionary\"")

@Deprecated("Use isCollinsEntry instead", ReplaceWith("isCollinsEntry(definition)"))
fun isCollins3rdEntry(definition: String): Boolean = isCollinsEntry(definition)

@Composable
fun CollinsSensesList(
    definitions: List<CollinsDefinition>,
    headword: String,
    textColor: Color,
    subtitleColor: Color,
    primaryTint: Color
) {
    definitions.forEachIndexed { index, sense ->
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = RoundedCornerShape(13.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryTint)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = primaryTint)
                }
            }
            if (sense.pos.isNotBlank()) {
                Text(
                    text = sense.pos,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTint
                )
            }
        }
        if (sense.definition.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildAnnotatedDef(sense.definition, headword, textColor, primaryTint),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 23.sp,
                modifier = Modifier.padding(start = 36.dp)
            )
        }
        sense.examples.forEach { example ->
            Row(modifier = Modifier.padding(start = 36.dp, top = 7.dp), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.padding(top = 8.dp).size(4.dp).background(primaryTint, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedDef(example, headword, subtitleColor, primaryTint),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

internal fun parseCollinsEntry(definition: String, fallbackWord: String): CollinsEntry =
    if (
        definition.contains("class=\"hom\"") || definition.contains("class='hom'") ||
        definition.contains("class=\"sensenum\"") || definition.contains("id=\"collins_english_dictionary\"")
    ) parseCollinsAdvancedEntry(definition, fallbackWord) else parseCollins3rdEntry(definition, fallbackWord)

private fun parseCollins3rdEntry(definition: String, fallbackWord: String): CollinsEntry {
    val frequency = Regex("""^[◆◇]+""").find(definition)?.value?.count { it == '◆' } ?: 0
    val firstBold = Regex("""<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL).find(definition)?.groupValues?.get(1).orEmpty()
    val wordForms = cleanCollinsText(firstBold)
    val word = wordForms.split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { fallbackWord }

    val posFonts = Regex("""<font[^>]*669900[^>]*>.*?</font>""", RegexOption.DOT_MATCHES_ALL).findAll(definition).toList()
    val senses = if (posFonts.isEmpty()) {
        listOfNotNull(parseCollinsSense(definition))
    } else {
        val starts = posFonts.map { match -> definition.lastIndexOf("<b>", match.range.first).takeIf { it >= 0 } ?: match.range.first }
        starts.mapIndexedNotNull { index, start ->
            val end = starts.getOrNull(index + 1) ?: definition.length
            parseCollinsSense(definition.substring(start, end))
        }
    }

    val audio = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(definition)?.groupValues?.get(1)
    return CollinsEntry(
        word = word,
        pronunciations = listOfNotNull(audio?.let { CollinsPronunciation("", "", it) }),
        definitions = senses,
        wordForms = wordForms,
        frequency = frequency,
        parsedOk = senses.isNotEmpty()
    )
}

private fun parseCollinsSense(html: String): CollinsDefinition? {
    if (html.isBlank()) return null
    val pos = Regex("""<font[^>]*669900[^>]*>(.*?)</font>""", RegexOption.DOT_MATCHES_ALL)
        .find(html)?.groupValues?.get(1)?.let(::cleanCollinsText)?.removePrefix("[")?.removeSuffix("]").orEmpty()
    val examples = Regex("""<font[^>]*(?:004080|4f81bd)[^>]*>\s*(?:<i>)?(.*?)(?:</i>)?\s*</font>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        .findAll(html).map { cleanCollinsText(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()

    var definition = html
        .replace(Regex("""<b>.*?</b>""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""<font[^>]*669900[^>]*>.*?</font>""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""<font[^>]*(?:004080|4f81bd)[^>]*>.*?</font>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), " ")
    definition = cleanCollinsText(definition)
        .replace(Regex("^[+\\s]+"), "")
        .trim()
    return if (definition.isBlank() && examples.isEmpty()) null else CollinsDefinition(pos, definition, examples)
}

private fun parseCollinsAdvancedEntry(definition: String, fallbackWord: String): CollinsEntry {
    val word = Regex("""<span[^>]*class=["'][^"']*\borth\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .find(definition)?.groupValues?.get(1)?.let(::cleanCollinsText).orEmpty().ifBlank { fallbackWord }
    val frequency = Regex("""data-band=["'](\d)["']""", RegexOption.IGNORE_CASE)
        .find(definition)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 5) ?: 0
    val wordForms = Regex("""<span[^>]*class=["'][^"']*inflected_forms[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .find(definition)?.groupValues?.get(1)?.let(::cleanCollinsText).orEmpty()
    val ipa = Regex("""<span[^>]*class=["'][^"']*\bpron\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .find(definition)?.groupValues?.get(1)?.let(::cleanCollinsText).orEmpty()
    val audio = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(definition)?.groupValues?.get(1)

    val sensePattern = Regex("""<(?:div|li)[^>]*class=["'][^"']*\bsense\b[^"']*["'][^>]*>(.*?)</(?:div|li)>""", RegexOption.DOT_MATCHES_ALL)
    var senses = sensePattern.findAll(definition).mapNotNull { match -> parseAdvancedSense(match.groupValues[1]) }.toList()
    if (senses.isEmpty()) {
        val blocks = Regex("""<span[^>]*class=["'][^"']*\bdef\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(definition).map { cleanCollinsText(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
        senses = blocks.map { CollinsDefinition("", it, emptyList()) }
    }

    return CollinsEntry(
        word = word,
        pronunciations = if (ipa.isNotBlank() || audio != null) listOf(CollinsPronunciation("", ipa, audio)) else emptyList(),
        definitions = senses,
        wordForms = wordForms,
        frequency = frequency,
        parsedOk = senses.isNotEmpty()
    )
}

private fun parseAdvancedSense(html: String): CollinsDefinition? {
    val pos = Regex("""<span[^>]*class=["'][^"']*\bpos\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .find(html)?.groupValues?.get(1)?.let(::cleanCollinsText).orEmpty()
    val definition = Regex("""<span[^>]*class=["'][^"']*\bdef\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .find(html)?.groupValues?.get(1)?.let(::cleanCollinsText).orEmpty()
    val examples = Regex("""<(?:span|div)[^>]*class=["'][^"']*(?:quote|example)[^"']*["'][^>]*>(.*?)</(?:span|div)>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(html).map { cleanCollinsText(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
    return if (definition.isBlank() && examples.isEmpty()) null else CollinsDefinition(pos, definition, examples)
}

private fun cleanCollinsText(html: String): String = html
    .replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&#39;", "'")
    .replace("&quot;", "\"")
    .replace(Regex("\\s+"), " ")
    .trim()
