package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.R
import io.github.gdict.data.AndroidDictionaryRepository
import io.github.gdict.ui.components.pageEnterAnimation
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.webview.MdxWebView

// region 柯林斯词典数据模型

data class CollinsEntry(
    val word: String,
    val partOfSpeech: String,
    val pronunciations: List<CollinsPronunciation>,
    val definitions: List<CollinsDefinition>,
    val wordForms: String,
    val parsedOk: Boolean
)

data class CollinsPronunciation(
    val region: String, // "UK" or "US"
    val ipa: String,
    val audioPath: String?
)

data class CollinsDefinition(
    val definition: String,
    val examples: List<String>,
    val grammarNote: String
)

// endregion

/**
 * 柯林斯3rd词典详情页 —— Fluent Design 2 / Acrylic Glass 原生渲染。
 * 诊断模式：显示HTML结构帮助定位解析逻辑。
 */
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
    var contentScale by remember { mutableStateOf(1f) }

    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else Color.White.copy(alpha = 0.88f)
    val glassBorder = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val primaryTint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val cdBack = stringResource(R.string.cd_back)
    val cdShare = stringResource(R.string.cd_share)
    val savedText = stringResource(R.string.saved)
    val favText = stringResource(R.string.add_to_favorites)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pronunciationBgGradient(darkMode))
            .pronunciationAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Floating Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircleButton(onClick = onBack, glassBg = glassBg, glassBorder = glassBorder) {
                    Icon(Icons.Default.ArrowBack, contentDescription = cdBack, tint = primaryTint, modifier = Modifier.size(22.dp))
                }
                Text(
                    dictionaryName.ifEmpty { "Collins" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                GlassCircleButton(onClick = onShare, glassBg = glassBg, glassBorder = glassBorder) {
                    Icon(Icons.Default.Share, contentDescription = cdShare, tint = primaryTint, modifier = Modifier.size(20.dp))
                }
            }

            // 可缩放 + 滚动的主体
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
                        .padding(horizontal = 24.dp)
                ) {
                    // 功能按钮区
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PronActionButton(
                            icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            text = if (isBookmarked) savedText else favText,
                            glassBg = glassBg,
                            glassBorder = glassBorder,
                            darkMode = darkMode,
                            modifier = Modifier.weight(1f),
                            onClick = onToggleBookmark
                        )
                        PronActionButton(
                            icon = Icons.Default.Share,
                            text = cdShare,
                            glassBg = glassBg,
                            glassBorder = glassBorder,
                            darkMode = darkMode,
                            modifier = Modifier.weight(1f),
                            onClick = onShare
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Floating Acrylic Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(28.dp), ambientColor = GdictColors.Primary.copy(alpha = 0.10f), spotColor = GdictColors.Primary.copy(alpha = 0.06f))
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
                            .background(glassBg)
                            .pageEnterAnimation()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            val displayWord = data.word.ifEmpty { word }

                            // 单词标题
                            Text(
                                displayWord,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                lineHeight = 46.sp
                            )

                            // 词性
                            if (data.partOfSpeech.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    data.partOfSpeech,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryTint,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            // 发音
                            if (data.pronunciations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CollinsPronunciationRows(
                                    pronunciations = data.pronunciations,
                                    darkMode = darkMode
                                ) { entry ->
                                    playAudio(entry.audioPath, displayWord)
                                }
                            }

                            // 词形变化
                            if (data.wordForms.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    data.wordForms,
                                    fontSize = 14.sp,
                                    color = subtitleColor
                                )
                            }

                            // 诊断面板
                            Spacer(modifier = Modifier.height(16.dp))
                            val diagColor = if (darkMode) Color(0xFFFFA500) else Color(0xFFCC6600)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "[DIAG] parsedOk=${data.parsedOk} prons=${data.pronunciations.size} defs=${data.definitions.size} pos='${data.partOfSpeech}' forms='${data.wordForms.take(30)}'",
                                    fontSize = 10.sp,
                                    color = diagColor,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "[CSS] ${css.take(200)}",
                                    fontSize = 9.sp,
                                    color = diagColor.copy(alpha = 0.7f),
                                    lineHeight = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "[HTML] ${definition.take(1000)}",
                                    fontSize = 9.sp,
                                    color = diagColor.copy(alpha = 0.7f),
                                    lineHeight = 12.sp
                                )
                            }

                            // 释义区域（如果有原生解析的释义）
                            if (data.definitions.isNotEmpty()) {
                                data.definitions.forEachIndexed { index, def ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    // Accent Bar + 释义
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(GdictColors.Primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${index + 1}. ${def.definition}",
                                            fontSize = 15.sp,
                                            color = textColor,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    def.examples.forEach { ex ->
                                        Row(modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, top = 4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(GdictColors.Primary.copy(alpha = 0.5f))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                ex,
                                                fontSize = 14.sp,
                                                color = subtitleColor,
                                                lineHeight = 20.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 回退：WebView 渲染全部内容
                                Spacer(modifier = Modifier.height(12.dp))
                                MdxWebView(
                                    definition = definition,
                                    css = css,
                                    darkMode = darkMode,
                                    contentScale = contentScale,
                                    dictionaryRepository = dictionaryRepository,
                                    onEntryClick = onEntryClick,
                                    onPlayAudio = { audioPath ->
                                        playAudio(audioPath, displayWord)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

// region 柯林斯发音行

@Composable
private fun CollinsPronunciationRows(
    pronunciations: List<CollinsPronunciation>,
    darkMode: Boolean,
    onPlay: (CollinsPronunciation) -> Unit
) {
    val ukProns = pronunciations.filter { it.region.equals("UK", true) }
    val usProns = pronunciations.filter { it.region.equals("US", true) }
    val allProns = if (ukProns.isEmpty() && usProns.isEmpty()) pronunciations else ukProns + usProns

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        allProns.forEach { pron ->
            val ipaColor = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(0.5.dp, GdictColors.BlueHighlightBorder, RoundedCornerShape(14.dp))
                    .background(
                        if (darkMode) GdictColors.DarkSubtleHover.copy(alpha = 0.4f)
                        else GdictColors.BluePrimaryLight.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pron.region.isNotEmpty()) {
                    Text(
                        pron.region,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ipaColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GdictColors.Primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (pron.ipa.isNotEmpty()) {
                    Text(
                        pron.ipa,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = ipaColor,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                SpeakerButton(onPlay = { onPlay(pron) }, size = 32.dp)
            }
        }
    }
}

// endregion

// region 柯林斯 HTML 解析

private fun parseCollinsEntry(definition: String, fallbackWord: String): CollinsEntry {
    val word = extractCollinsWord(definition).ifEmpty { fallbackWord }
    val partOfSpeech = extractCollinsPos(definition)
    val pronunciations = extractCollinsPronunciations(definition)
    val definitions = extractCollinsDefinitions(definition)
    val wordForms = extractCollinsWordForms(definition)
    val parsedOk = word.isNotEmpty() && (pronunciations.isNotEmpty() || definitions.isNotEmpty() || partOfSpeech.isNotEmpty())
    return CollinsEntry(word, partOfSpeech, pronunciations, definitions, wordForms, parsedOk)
}

private fun extractCollinsWord(definition: String): String {
    // 尝试多种柯林斯HTML格式
    // 1. <span class="HWD">word</span> (Collins 大写类名)
    extractSpanClass(definition, "HWD").takeIf { it.isNotEmpty() }?.let { return it }
    // 2. <span class="hw">word</span>
    extractSpanClass(definition, "hw").takeIf { it.isNotEmpty() }?.let { return it }
    // 3. <hw>word</hw>
    Regex("""<hw[^>]*>(.*?)</hw>""", RegexOption.DOT_MATCHES_ALL).find(definition)?.let {
        return cleanCollinsText(it.groupValues[1]).replace("|", "")
    }
    // 4. <span class="headword">word</span>
    extractSpanClass(definition, "headword").takeIf { it.isNotEmpty() }?.let { return it }
    // 5. <h2>word</h2> 或 <h3>word</h3>
    Regex("""<h[1-4][^>]*>(.*?)</h[1-4]>""", RegexOption.DOT_MATCHES_ALL).find(definition)?.let {
        val w = cleanCollinsText(it.groupValues[1])
        if (w.length < 50) return w
    }
    return ""
}

private fun extractCollinsPos(definition: String): String {
    // <span class="POS">noun</span>
    extractSpanClass(definition, "POS").takeIf { it.isNotEmpty() }?.let { return it }
    // <span class="pos">noun</span>
    extractSpanClass(definition, "pos").takeIf { it.isNotEmpty() }?.let { return it }
    // <pos>noun</pos>
    Regex("""<pos[^>]*>(.*?)</pos>""", RegexOption.DOT_MATCHES_ALL).find(definition)?.let {
        return cleanCollinsText(it.groupValues[1])
    }
    // <span class="wordclass">verb</span>
    extractSpanClass(definition, "wordclass").takeIf { it.isNotEmpty() }?.let { return it }
    // <gram>verb</gram>
    Regex("""<gram[^>]*>(.*?)</gram>""", RegexOption.DOT_MATCHES_ALL).find(definition)?.let {
        return cleanCollinsText(it.groupValues[1])
    }
    return ""
}

private fun extractCollinsPronunciations(definition: String): List<CollinsPronunciation> {
    val result = mutableListOf<CollinsPronunciation>()

    // 搜索国旗图片
    val flagPattern = Regex(
        """<img[^>]*src=["'][^"']*(uk_sound|us_sound|uk|us|gb|american)\.png[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    val flags = flagPattern.findAll(definition).toList()

    // 搜索所有 IPA
    val ipas = mutableListOf<Pair<String, String>>() // (region, ipa)
    // <span class="PRON">...</span>
    Regex("""<span[^>]*class=["'][^"']*\bPRON\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add("UK" to cleanCollinsText(it.groupValues[1])) }
    // <span class="pron">...</span>
    Regex("""<span[^>]*class=["'][^"']*\bpron\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add("UK" to cleanCollinsText(it.groupValues[1])) }
    // <span class="pho">...</span>
    Regex("""<span[^>]*class=["'][^"']*\bpho\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add("UK" to cleanCollinsText(it.groupValues[1])) }
    // <phon>...</phon>
    Regex("""<phon[^>]*>(.*?)</phon>""", RegexOption.DOT_MATCHES_ALL).find(definition)?.let {
        ipas.add("UK" to cleanCollinsText(it.groupValues[1]))
    }
    // 文本中的 /IPA/ 模式
    Regex("""/([a-zA-Zˈˌːɪiːæɑːʌʊuːeɛəɜːɔːɒːθðʃʒŋəˈˌː]+)/""").findAll(definition).forEach {
        ipas.add("UK" to "/${it.groupValues[1]}/")
    }

    // 音频链接
    val audios = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .findAll(definition).map { it.groupValues[1] }.toList()

    if (flags.isNotEmpty()) {
        flags.forEachIndexed { i, flag ->
            val region = if (flag.groupValues[1].lowercase().let { it.contains("uk") || it.contains("gb") }) "UK" else "US"
            val segStart = flag.range.last + 1
            val segEnd = if (i + 1 < flags.size) flags[i + 1].range.first else definition.length
            val segment = definition.substring(segStart, segEnd)
            var ipa = ""
            Regex("""/([a-zA-Zˈˌːɪiːæɑːʌʊuːeɛəɜːɔːɒːθðʃʒŋəˈˌː]+)/""").find(segment)?.let {
                ipa = "/${it.groupValues[1]}/"
            }
            if (ipa.isEmpty() && i < ipas.size) ipa = ipas[i].second
            val audio = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE).find(segment)?.groupValues?.get(1)
                ?: audios.getOrNull(i)
            result.add(CollinsPronunciation(region, ipa, audio))
        }
    } else if (ipas.isNotEmpty()) {
        ipas.forEachIndexed { i, (region, ipa) ->
            result.add(CollinsPronunciation(region, ipa, audios.getOrNull(i)))
        }
    }

    return result.distinctBy { it.region + it.ipa }
}

private fun extractCollinsDefinitions(definition: String): List<CollinsDefinition> {
    val result = mutableListOf<CollinsDefinition>()
    // <span class="DEF">definition</span>
    val defPattern = Regex("""<span[^>]*class=["'][^"']*\bDEF\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    defPattern.findAll(definition).forEach { match ->
        val defText = cleanCollinsText(match.groupValues[1])
        if (defText.isNotEmpty()) {
            result.add(CollinsDefinition(defText, emptyList(), ""))
        }
    }
    // <def>definition</def>
    if (result.isEmpty()) {
        Regex("""<def[^>]*>(.*?)</def>""", RegexOption.DOT_MATCHES_ALL).findAll(definition).forEach { match ->
            val defText = cleanCollinsText(match.groupValues[1])
            if (defText.isNotEmpty()) {
                result.add(CollinsDefinition(defText, emptyList(), ""))
            }
        }
    }
    // <span class="definition">definition</span>
    if (result.isEmpty()) {
        extractSpanClass(definition, "definition").takeIf { it.isNotEmpty() }?.let {
            result.add(CollinsDefinition(it, emptyList(), ""))
        }
    }
    return result
}

private fun extractCollinsWordForms(definition: String): String {
    // <span class="INFLX">...</span> 或 <span class="inflections">...</span>
    val inflx = extractSpanClass(definition, "INFLX").ifEmpty { extractSpanClass(definition, "inflections") }
    if (inflx.isNotEmpty()) return inflx
    // <span class="forms">...</span>
    extractSpanClass(definition, "forms").takeIf { it.isNotEmpty() }?.let { return it }
    // 搜索 word+s, word+ed, word+ing 模式
    return ""
}

private fun extractSpanClass(content: String, className: String): String {
    val pattern = Regex(
        """<span[^>]*class=["'][^"']*\b$className\b[^"']*["'][^>]*>(.*?)</span>""",
        RegexOption.DOT_MATCHES_ALL
    )
    return pattern.find(content)?.groupValues?.get(1)?.let { cleanCollinsText(it) } ?: ""
}

private fun cleanCollinsText(html: String): String {
    return html.replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace(Regex("\\s+"), " ")
        .trim()
}

// endregion
