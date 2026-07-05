package io.github.gdict.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.components.pageEnterAnimation
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.webview.MdxWebView

// region 柯林斯词典数据模型

data class CollinsEntry(
    val word: String,
    val pronunciations: List<CollinsPronunciation>,
    val definitions: List<CollinsDefinition>,
    val wordForms: String,
    val frequency: Int, // 词频星级 0-5（◆ 实心数）
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

// endregion

/**
 * 柯林斯3rd词典详情页 —— Fluent Design 2 / Acrylic Glass 原生渲染。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CollinsDetailContent(
    word: String,
    definition: String,
    css: String,
    dictionaryName: String,
    isBookmarked: Boolean,
    darkMode: Boolean,
    dictionaryRepository: DictionaryRepository,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEntryClick: (String) -> Unit,
    webViewVisible: Boolean,
    playAudio: (String?, String) -> Unit
) {
    val data = remember(definition, word) { parseCollinsEntry(definition, word) }
    var contentScale by remember { mutableStateOf(1f) }

    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else Color.White.copy(alpha = 0.88f)
    val glassBorder = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val primaryTint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = with(density) { windowInfo.containerSize.width.toFloat() }
    val screenHeightPx = with(density) { windowInfo.containerSize.height.toFloat() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pronunciationBgGradient(darkMode))
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Floating Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircleButton(onClick = onBack, glassBg = glassBg, glassBorder = glassBorder) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryTint, modifier = Modifier.size(22.dp))
                }
                Text(
                    dictionaryName.ifEmpty { "Collins" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                GlassCircleButton(onClick = { }, glassBg = glassBg, glassBorder = glassBorder) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = primaryTint, modifier = Modifier.size(20.dp))
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
                            text = if (isBookmarked) "已收藏" else "添加收藏",
                            glassBg = glassBg,
                            glassBorder = glassBorder,
                            darkMode = darkMode,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                            onClick = onToggleBookmark
                        )
                        PronActionButton(
                            icon = Icons.Default.Share,
                            text = "分享",
                            glassBg = glassBg,
                            glassBorder = glassBorder,
                            darkMode = darkMode,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                            onClick = { }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        ) {
                            val displayWord = data.word.ifEmpty { word }
                            val audioPath = data.pronunciations.firstOrNull()?.audioPath

                            // 单词标题 + 发音按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    displayWord,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    lineHeight = 46.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                SpeakerButton(
                                    onPlay = { playAudio(audioPath, displayWord) },
                                    size = 44.dp
                                )
                            }

                            // 词频棱形
                            if (data.frequency > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                FrequencyDiamondsBlue(
                                    frequency = data.frequency,
                                    primaryTint = primaryTint
                                )
                            }

                            // 词形变化
                            if (data.wordForms.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    data.wordForms,
                                    fontSize = 15.sp,
                                    color = subtitleColor,
                                    fontStyle = FontStyle.Italic
                                )
                            }

                            // 释义区域
                            if (data.definitions.isNotEmpty()) {
                                CollinsSensesList(
                                    definitions = data.definitions,
                                    headword = displayWord,
                                    textColor = textColor,
                                    subtitleColor = subtitleColor,
                                    primaryTint = primaryTint
                                )
                            } else {
                                // 回退：WebView 渲染全部内容
                                Spacer(modifier = Modifier.height(12.dp))
                                MdxWebView(
                                    definition = definition,
                                    css = css,
                                    darkMode = darkMode,
                                    dictionaryRepository = dictionaryRepository,
                                    onEntryClick = onEntryClick,
                                    webViewVisible = webViewVisible
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

// region 释义文本高亮（词头加粗着色）

private fun buildAnnotatedDef(
    text: String,
    headword: String,
    baseColor: Color,
    highlightColor: Color
): AnnotatedString {
    if (headword.isEmpty()) {
        return buildAnnotatedString { append(text) }
    }
    return buildAnnotatedString {
        var idx = 0
        val lowerText = text.lowercase()
        val lowerHead = headword.lowercase()
        while (idx <= text.length - headword.length) {
            val found = lowerText.indexOf(lowerHead, idx)
            if (found < 0) {
                append(text.substring(idx))
                break
            }
            // 整词边界检查
            val before = if (found > 0) text[found - 1] else ' '
            val afterIdx = found + headword.length
            val after = if (afterIdx < text.length) text[afterIdx] else ' '
            val isWordBoundary = !before.isLetter() && !after.isLetter()
            if (isWordBoundary) {
                if (found > idx) {
                    withStyle(SpanStyle(color = baseColor)) {
                        append(text.substring(idx, found))
                    }
                }
                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(text.substring(found, afterIdx))
                }
                idx = afterIdx
            } else {
                idx = found + 1
            }
        }
        if (idx < text.length) {
            withStyle(SpanStyle(color = baseColor)) {
                append(text.substring(idx))
            }
        }
    }
}

// endregion

// region 柯林斯词典 HTML 解析

/**
 * 柯林斯3rd HTML 结构：
 *   <b>read reads reading read </b>          ← 单词 + 词形变化
 *   <font color=#669900">[VB]</font>         ← 词性（绿色方括号）
 *   <br> 释义文本 <br>                         ← 释义
 *   <img src="bullet.png"><font color="#004080"><i>例句</i></font>  ← 例句（蓝色斜体）
 *   +<b>read </b><font color="#669900">[N-SING...]</font> ...       ← 下一释义由 +<b> 分隔
 */
internal fun parseCollinsEntry(definition: String, fallbackWord: String): CollinsEntry {
    return when {
        definition.contains("class=\"hom\"") ||
                definition.contains("class='hom'") ||
                definition.contains("class=\"sensenum\"") ||
                definition.contains("id=\"collins_english_dictionary\"") ->
            parseCollinsAdvancedEntry(definition, fallbackWord)

        else -> parseCollins3rdEntry(definition, fallbackWord)
    }
}

private fun parseCollins3rdEntry(definition: String, fallbackWord: String): CollinsEntry {
    // 0. 词频棱形
    val freqMatch = Regex("""^[◆◇]+""").find(definition)
    val frequency = freqMatch?.value?.count { it == '◆' } ?: 0

    // 1. 第一个 <b>...</b> = 单词 + 词形变化
    val firstBoldMatch = Regex("""<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL).find(definition)
    val firstBoldRaw = firstBoldMatch?.groupValues?.get(1) ?: ""
    val cleanedBold = cleanCollinsText(firstBoldRaw)
    val tokens = cleanedBold.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val word = tokens.firstOrNull()?.ifEmpty { fallbackWord } ?: fallbackWord
    val wordForms = cleanedBold

    // 2. 以绿色词性标签为释义锚点
    val posFontPattern = Regex("""<font[^>]*669900[^>]*>.*?</font>""", RegexOption.DOT_MATCHES_ALL)
    val posFonts = posFontPattern.findAll(definition).toList()

    val definitions: List<CollinsDefinition> = if (posFonts.isEmpty()) {
        // 无绿色词性：整体作为单个释义（回退）
        val firstB = definition.indexOf("<b>")
        val senseHtml = if (firstB >= 0) definition.substring(firstB) else definition
        listOfNotNull(parseCollinsSense(senseHtml))
    } else {
        // 计算每个释义的起点（绿色标签前最近的 <b>）
        val senseStarts = posFonts.map { pf ->
            val bIdx = definition.lastIndexOf("<b>", pf.range.first)
            if (bIdx >= 0) bIdx else pf.range.first
        }
        posFonts.mapIndexed { i, _ ->
            val start = senseStarts[i]
            val end = if (i + 1 < senseStarts.size) senseStarts[i + 1] else definition.length
            val senseHtml = if (start < end) definition.substring(start, end) else ""
            parseCollinsSense(senseHtml)
        }.filterNotNull()
    }

    // 3. 音频
    val audioPath = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(definition)?.groupValues?.get(1)
    val pronunciations = listOfNotNull(
        if (audioPath != null) CollinsPronunciation("", "", audioPath) else null
    )

    val parsedOk = word.isNotEmpty() && definitions.isNotEmpty()
    return CollinsEntry(word, pronunciations, definitions, wordForms, frequency, parsedOk)
}

private fun parseCollinsAdvancedEntry(definition: String, fallbackWord: String): CollinsEntry {
    // 1. 单词
    val word = Regex("""<h2[^>]*class=["']h2_entry["'][^>]*>.*?<span[^>]*class=["']orth["'][^>]*>(.*?)</span>.*?</h2>""",
        RegexOption.DOT_MATCHES_ALL)
        .find(definition)?.groupValues?.get(1)?.let { cleanCollinsAdvancedText(it) }
        ?: Regex("""<span[^>]*class=["']orth["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
            .find(definition)?.groupValues?.get(1)?.let { cleanCollinsAdvancedText(it) }
        ?: fallbackWord

    // 2. 词频 data-band 1-5
    val frequency = Regex("""<span[^>]*class=["']word-frequency-img["'][^>]*\\bdata-band=["'](\\d)["']""",
        RegexOption.IGNORE_CASE)
        .find(definition)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 5) ?: 0

    // 3. 词形变化
    val wordForms = Regex("""<span[^>]*class=["']form inflected_forms[^"']*["'][^>]*>(.*?)</span>""",
        RegexOption.DOT_MATCHES_ALL)
        .find(definition)?.groupValues?.get(1)?.let { cleanCollinsAdvancedText(it) }
        ?: ""

    // 4. 发音（取第一个 .pron）
    val ipa = Regex("""<span[^>]*class=["']pron type-["'][^>]*>(.*?)</span>""",
        RegexOption.DOT_MATCHES_ALL)
        .find(definition)?.groupValues?.get(1)?.let { cleanCollinsAdvancedText(it) } ?: ""
    val audioPath = Regex("""<a[^>]*class=["'][^"']*hwd_sound[^"']*["'][^>]*href=["']sound://([^"']+)["']""",
        RegexOption.IGNORE_CASE)
        .find(definition)?.groupValues?.get(1)
        ?: Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(definition)?.groupValues?.get(1)
    val pronunciations = listOfNotNull(
        if (ipa.isNotEmpty() || !audioPath.isNullOrBlank())
            CollinsPronunciation(region = "", ipa = ipa, audioPath = audioPath) else null
    )

    // 5. 释义列表
    val homBlocks = Regex("""<div[^>]*class=["']hom["'][^>]*>(.*?)</div>\s*(?=<div[^>]*class=["']hom["']|<div[^>]*class=["']copyright|</div></div></div></div></div>)""",
        RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).toList()

    val definitions = if (homBlocks.isEmpty()) {
        // 回退：按 sensenum 分段
        parseCollinsAdvancedBySensenum(definition)
    } else {
        homBlocks.mapNotNull { parseCollinsAdvancedSense(it.groupValues[1]) }
    }

    val parsedOk = word.isNotEmpty() && definitions.isNotEmpty()
    return CollinsEntry(word, pronunciations, definitions, wordForms, frequency, parsedOk)
}

private fun parseCollinsAdvancedSense(homHtml: String): CollinsDefinition? {
    val pos = Regex("""<span[^>]*class=["']gramGrp["'][^>]*>.*?<span[^>]*class=["']pos["'][^>]*>(.*?)</span>.*?</span>""",
        RegexOption.DOT_MATCHES_ALL)
        .find(homHtml)?.groupValues?.get(1)?.let { cleanCollinsAdvancedText(it) } ?: ""

    val senseNum = Regex("""<span[^>]*class=["']sensenum["'][^>]*>(.*?)</span>""",
        RegexOption.DOT_MATCHES_ALL)
        .find(homHtml)?.groupValues?.get(1)?.let { cleanCollinsAdvancedText(it) } ?: ""

    val def = Regex("""<div[^>]*class=["']def["'][^>]*>(.*?)</div>""",
        RegexOption.DOT_MATCHES_ALL)
        .find(homHtml)?.groupValues?.get(1)?.let { cleanCollinsAdvancedText(it) } ?: ""

    val examples = Regex("""<div[^>]*class=["']cit type-example["'][^>]*>.*?<span[^>]*class=["']quote["'][^>]*>(.*?)</span>.*?</div>""",
        RegexOption.DOT_MATCHES_ALL)
        .findAll(homHtml)
        .map { cleanCollinsAdvancedText(it.groupValues[1]) }
        .filter { it.isNotEmpty() }
        .toList()

    if (pos.isEmpty() && def.isEmpty() && examples.isEmpty()) return null
    return CollinsDefinition(pos, "$senseNum$def", examples)
}

private fun parseCollinsAdvancedBySensenum(definition: String): List<CollinsDefinition> {
    val sensenumMatches = Regex("""<span[^>]*class=["']sensenum["'][^>]*>.*?</span>""",
        RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).toList()
    if (sensenumMatches.isEmpty()) return emptyList()

    val starts = sensenumMatches.map { it.range.first }
    return sensenumMatches.mapIndexed { i, match ->
        val start = match.range.first
        val end = if (i + 1 < starts.size) starts[i + 1] else definition.length
        val html = definition.substring(start, end)
        parseCollinsAdvancedSense(html)
    }.filterNotNull()
}

private fun cleanCollinsAdvancedText(html: String): String {
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

private fun parseCollinsSense(html: String): CollinsDefinition? {
    val pos = extractGreenPos(html)
    val examples = extractBlueExamples(html)
    val defText = extractSenseDefinition(html, pos)

    if (defText.isEmpty() && examples.isEmpty() && pos.isEmpty()) return null
    return CollinsDefinition(pos, defText, examples)
}

/** 提取绿色词性标签 */
private fun extractGreenPos(html: String): String {
    val match = Regex("""<font[^>]*669900[^>]*>(.*?)</font>""", RegexOption.DOT_MATCHES_ALL)
        .find(html) ?: return ""
    val raw = cleanCollinsText(match.groupValues[1])
        .removePrefix("[").removeSuffix("]").trim()
    return raw.substringBefore(":").trim()
}

/** 提取蓝色斜体例句 */
private fun extractBlueExamples(html: String): List<String> {
    return Regex("""<font[^>]*004080[^>]*>\s*<i>(.*?)</i>\s*</font>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(html)
        .map { cleanCollinsText(it.groupValues[1]) }
        .filter { it.isNotEmpty() }
        .toList()
}

/** 提取释义文本 */
private fun extractSenseDefinition(html: String, pos: String): String {
    val startPos = if (pos.isNotEmpty()) {
        Regex("""<font[^>]*669900[^>]*>.*?</font>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.range?.last?.plus(1) ?: 0
    } else {
        Regex("""<b>.*?</b>""", RegexOption.DOT_MATCHES_ALL).find(html)?.range?.last?.plus(1) ?: 0
    }
    val afterPos = if (startPos < html.length) html.substring(startPos) else ""

    val imgIdx = afterPos.indexOf("<img")
    val plusIdx = afterPos.indexOf("+<b>")
    val eqIdx = afterPos.indexOf("=<b>")
    // 独立同义词条：<br><b>单词</b><br>（单词不含空格）
    val synonymIdx = Regex("""<br><b>(\w+)</b><br>""").find(afterPos)?.range?.first ?: -1

    val endIdx = listOf(imgIdx, plusIdx, eqIdx, synonymIdx).filter { it >= 0 }.minOrNull() ?: afterPos.length
    val defRegion = afterPos.substring(0, endIdx.coerceIn(0, afterPos.length))
    return cleanCollinsText(defRegion).trim()
}

private fun cleanCollinsText(html: String): String {
    return html.replace(Regex("<[^>]+>"), "")
        // Collins 数据中部分内联加粗/斜体被损坏为 ^bp...^/by / ^ip...^/iy 形式
        .replace(Regex("""\^/?[biu][a-z]?"""), "")
        // 词频棱形字符
        .replace("◆", "")
        .replace("◇", "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace("^", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

// endregion

// region UI 组件

@Composable
fun CollinsSensesList(
    definitions: List<CollinsDefinition>,
    headword: String,
    textColor: Color,
    subtitleColor: Color,
    primaryTint: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        definitions.forEachIndexed { index, def ->
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 编号 + 词性徽标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTint
                        )
                    }
                    if (def.pos.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, primaryTint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                def.pos,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 释义文本（词头加粗着色）
                if (def.definition.isNotEmpty()) {
                    Text(
                        buildAnnotatedDef(def.definition, headword, textColor, primaryTint),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }

                // 例句
                if (def.examples.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    def.examples.forEach { example ->
                        Text(
                            example,
                            fontSize = 14.sp,
                            color = subtitleColor,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrequencyDiamondsBlue(
    frequency: Int,
    primaryTint: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            Text(
                if (index < frequency) "◆" else "◇",
                fontSize = 14.sp,
                color = if (index < frequency) primaryTint else primaryTint.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun PronActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    glassBg: Color,
    glassBorder: Color,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = tint
            )
        }
    }
}

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    glassBg: Color,
    glassBorder: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(20.dp))
            .background(glassBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun pronunciationBgGradient(darkMode: Boolean): androidx.compose.ui.graphics.Brush {
    return if (darkMode) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            0.0f to GdictColors.DarkBackground,
            1.0f to GdictColors.DarkSurfaceVariant
        )
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            0.0f to Color(0xFFDCEBFF),
            0.6f to Color(0xFFEDF4FF),
            1.0f to Color(0xFFFFFFFF)
        )
    }
}

// endregion
