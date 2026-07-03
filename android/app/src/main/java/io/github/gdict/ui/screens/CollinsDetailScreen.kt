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
import androidx.compose.ui.text.font.FontStyle
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
    val pronunciations: List<CollinsPronunciation>,
    val definitions: List<CollinsDefinition>,
    val wordForms: String,
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
 * HTML 结构： <b>word forms</b><font color=#669900>[POS]</font> 释义 <img><font color=#004080><i>例句</i></font>
 * 多释义由 +<b> 分隔。
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

                            // 诊断面板
                            Spacer(modifier = Modifier.height(16.dp))
                            val diagColor = if (darkMode) Color(0xFFFFA500) else Color(0xFFCC6600)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "[DIAG] parsedOk=${data.parsedOk} defs=${data.definitions.size} forms='${data.wordForms.take(40)}' audio=${audioPath != null}",
                                    fontSize = 10.sp,
                                    color = diagColor,
                                    lineHeight = 14.sp
                                )
                                data.definitions.forEachIndexed { i, d ->
                                    Text(
                                        "[S$i] pos='${d.pos}' def='${d.definition.take(60)}' ex=${d.examples.size}",
                                        fontSize = 9.sp,
                                        color = diagColor.copy(alpha = 0.85f),
                                        lineHeight = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "[HTML] ${definition.take(800)}",
                                    fontSize = 9.sp,
                                    color = diagColor.copy(alpha = 0.7f),
                                    lineHeight = 12.sp
                                )
                            }

                            // 释义区域
                            if (data.definitions.isNotEmpty()) {
                                data.definitions.forEach { def ->
                                    Spacer(modifier = Modifier.height(18.dp))
                                    // [POS] 徽标
                                    if (def.pos.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GdictColors.Primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                def.pos,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryTint
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    // 释义文本
                                    if (def.definition.isNotEmpty()) {
                                        Text(
                                            def.definition,
                                            fontSize = 15.sp,
                                            color = textColor,
                                            lineHeight = 22.sp
                                        )
                                    }
                                    // 例句
                                    def.examples.forEach { ex ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 4.dp, top = 6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("•", fontSize = 14.sp, color = primaryTint)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                ex,
                                                fontSize = 14.sp,
                                                color = subtitleColor,
                                                fontStyle = FontStyle.Italic,
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

// region 柯林斯 HTML 解析

/**
 * 柯林斯3rd HTML 结构：
 *   C <br><img src="audio.png">              ← 节字母 + 音频图标
 *   <b>read reads reading read </b>          ← 单词 + 词形变化
 *   <font color=#669900">[VB]</font>         ← 词性（绿色方括号）
 *   <br> 释义文本 <br>                         ← 释义
 *   <img src="bullet.png"><font color="#004080"><i>例句</i></font>  ← 例句（蓝色斜体）
 *   +<b>read </b><font color="#669900">[N-SING...]</font> ...       ← 下一释义由 +<b> 分隔
 */
private fun parseCollinsEntry(definition: String, fallbackWord: String): CollinsEntry {
    // 1. 第一个 <b>...</b> = 单词 + 词形变化
    val firstBoldMatch = Regex("""<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL).find(definition)
    val firstBoldRaw = firstBoldMatch?.groupValues?.get(1) ?: ""
    val cleanedBold = cleanCollinsText(firstBoldRaw)
    val tokens = cleanedBold.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val word = tokens.firstOrNull()?.ifEmpty { fallbackWord } ?: fallbackWord
    val wordForms = cleanedBold

    // 2. 以绿色词性标签 <font...669900...>...</font> 为释义锚点（鲁棒：只要 font 标签内
    //    出现 669900 即认定，避免 color="#669900 / color=#669900" 等引号缺失变体导致漏匹配）。
    //    每个释义的起点 = 该绿色标签之前最近的 <b>（词头）；终点 = 下一个释义的 <b> 起点（或末尾）。
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

    // 3. 音频：sound:// 链接（若有）
    val audioPath = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(definition)?.groupValues?.get(1)
    val pronunciations = listOfNotNull(
        if (audioPath != null) CollinsPronunciation("", "", audioPath) else null
    )

    val parsedOk = word.isNotEmpty() && definitions.isNotEmpty()
    return CollinsEntry(word, pronunciations, definitions, wordForms, parsedOk)
}

private fun parseCollinsSense(html: String): CollinsDefinition? {
    val pos = extractGreenPos(html)
    val examples = extractBlueExamples(html)
    val defText = extractSenseDefinition(html, pos)

    if (defText.isEmpty() && examples.isEmpty() && pos.isEmpty()) return null
    return CollinsDefinition(pos, defText, examples)
}

/** 提取绿色词性标签 <font...669900...>[VB]</font>，取冒号前的主词性。鲁棒匹配 669900 色值。 */
private fun extractGreenPos(html: String): String {
    val match = Regex("""<font[^>]*669900[^>]*>(.*?)</font>""", RegexOption.DOT_MATCHES_ALL)
        .find(html) ?: return ""
    val raw = cleanCollinsText(match.groupValues[1])
        .removePrefix("[").removeSuffix("]").trim()
    return raw.substringBefore(":").trim()
}

/** 提取蓝色斜体例句 <font...004080...><i>...</i></font>。鲁棒匹配 004080 色值。 */
private fun extractBlueExamples(html: String): List<String> {
    return Regex("""<font[^>]*004080[^>]*>\s*<i>(.*?)</i>\s*</font>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(html)
        .map { cleanCollinsText(it.groupValues[1]) }
        .filter { it.isNotEmpty() }
        .toList()
}

/**
 * 提取释义文本：POS 标签之后、首个例句图标 <img / 下一释义 +<b> / 同义词 =<b> /
 * 独立成行的同义词条 <br><b>word</b><br> 之前。
 * 独立同义词判断：单独一行、内容为单个无空格词、紧接 <br> ——
 * 这样可保留句中内联的 <b>scrum</b>，只截断 <br><b>scrummage</b><br> 这类交叉引用。
 */
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
        // Collins 数据中部分内联加粗/斜体被损坏为 ^bp...^/by / ^ip...^/iy 形式，按伪标签清除
        .replace(Regex("""\^/?[biu][a-z]?"""), "")
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
