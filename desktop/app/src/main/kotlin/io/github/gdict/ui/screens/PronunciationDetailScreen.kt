package io.github.gdict.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.components.pageEnterAnimation
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.ui.webview.MdxWebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PronunciationEntry(
    val region: String,
    val ipa: String,
    val audioPath: String?
)

private data class WordFormEntry(
    val word: String,
    val ipa: String,
    val audioPath: String?
)

private data class PronunciationData(
    val word: String,
    val pronunciations: List<PronunciationEntry>,
    val wordForms: List<WordFormEntry>,
    val hasReadingContent: Boolean,
    val parsedOk: Boolean
)

/**
 * 发音词典详情页（Cambridge EPD）—— Fluent Design 2 / Acrylic Glass。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PronunciationDetailContent(
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
    val data = remember(definition, word) { parsePronunciationData(definition, word) }
    var contentScale by remember { mutableStateOf(1f) }
    var searchQuery by remember { mutableStateOf("") }

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
                    "发音",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                GlassCircleButton(onClick = { }, glassBg = glassBg, glassBorder = glassBorder) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = primaryTint, modifier = Modifier.size(20.dp))
                }
            }

            // Floating Search Field
            FloatingSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                darkMode = darkMode,
                glassBg = glassBg,
                glassBorder = glassBorder
            )

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
                    // 顶部间距
                    Spacer(modifier = Modifier.height(6.dp))

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

                            if (data.parsedOk && data.pronunciations.isNotEmpty()) {
                                // —— 原生渲染：单词 + 紧凑发音行 ——
                                Text(
                                    displayWord,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    lineHeight = 46.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 紧凑发音行：每行展示国旗 + IPA + 喇叭按钮
                                PronunciationRows(
                                    pronunciations = data.pronunciations,
                                    darkMode = darkMode
                                ) { entry ->
                                    playAudio(entry.audioPath, displayWord)
                                }
                            } else {
                                // —— 回退：WebView 渲染全部内容 ——
                                Text(
                                    displayWord,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    lineHeight = 46.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                MdxWebView(
                                    definition = definition,
                                    css = css,
                                    darkMode = darkMode,
                                    dictionaryRepository = dictionaryRepository,
                                    onEntryClick = onEntryClick,
                                    onPlayAudio = { playAudio(it, displayWord) },
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

@Composable
private fun FloatingSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    darkMode: Boolean,
    glassBg: Color,
    glassBorder: Color
) {
    val placeholderColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .shadow(3.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(28.dp))
            .background(glassBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = placeholderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (query.isEmpty()) "搜索发音..." else query,
                fontSize = 16.sp,
                color = if (query.isEmpty()) placeholderColor else if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
            )
        }
    }
}

@Composable
private fun PronunciationRows(
    pronunciations: List<PronunciationEntry>,
    darkMode: Boolean,
    onPlay: (PronunciationEntry) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pronunciations.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlagBadge(region = entry.region, size = 26.dp)
                Text(
                    text = entry.ipa,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface,
                    modifier = Modifier.weight(1f)
                )
                SpeakerButton(onPlay = { onPlay(entry) }, size = 36.dp)
            }
        }
    }
}

/**
 * 圆形喇叭按钮，点击后播放发音并进入短暂播放态动画。
 */
@Composable
internal fun SpeakerButton(onPlay: () -> Unit, size: Dp = 40.dp) {
    var playing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(GdictColors.Primary.copy(alpha = if (playing) pulseAlpha else 0.14f))
            .border(0.5.dp, GdictColors.BlueHighlightBorder, CircleShape)
            .clickable {
                if (playing) return@clickable
                playing = true
                onPlay()
                scope.launch {
                    delay(1200)
                    playing = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Play pronunciation",
            tint = GdictColors.OnPrimary,
            modifier = Modifier.size((size.value * 0.5f).dp)
        )
    }
}

/**
 * 简化圆形国旗徽标：支持 UK / British / GB 与 US / American / USA，
 * 其他 region 默认按 US 星条旗显示。
 */
@Composable
private fun FlagBadge(region: String, size: Dp = 26.dp) {
    val isUk = region.equals("UK", ignoreCase = true) ||
            region.equals("British", ignoreCase = true) ||
            region.equals("GB", ignoreCase = true)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            if (isUk) {
                drawRect(Color(0xFF012169))
                val whiteW = w * 0.16f
                drawRect(
                    Color.White,
                    topLeft = Offset(0f, (h - whiteW) / 2f),
                    size = Size(w, whiteW)
                )
                drawRect(
                    Color.White,
                    topLeft = Offset((w - whiteW) / 2f, 0f),
                    size = Size(whiteW, h)
                )
                val redW = w * 0.07f
                drawRect(
                    Color(0xFFC8102E),
                    topLeft = Offset(0f, (h - redW) / 2f),
                    size = Size(w, redW)
                )
                drawRect(
                    Color(0xFFC8102E),
                    topLeft = Offset((w - redW) / 2f, 0f),
                    size = Size(redW, h)
                )
            } else {
                val stripes = 7
                val stripeH = h / stripes
                for (i in 0 until stripes) {
                    drawRect(
                        color = if (i % 2 == 0) Color(0xFFB22234) else Color.White,
                        topLeft = Offset(0f, i * stripeH),
                        size = Size(w, stripeH)
                    )
                }
                drawRect(
                    Color(0xFF3C3B6E),
                    topLeft = Offset(0f, 0f),
                    size = Size(w * 0.42f, h * 0.57f)
                )
            }
        }
    }
}

// region 发音词典 HTML 解析

private fun parsePronunciationData(definition: String, fallbackWord: String): PronunciationData {
    val word = extractHeadword(definition).ifEmpty { fallbackWord }
    val pronunciations = parsePronunciations(definition)
    val wordForms = parseHitInflections(definition)
    val parsedOk = pronunciations.isNotEmpty() || wordForms.isNotEmpty()
    return PronunciationData(word, pronunciations, wordForms, hasReadingContent(definition), parsedOk)
}

/** 从 <hit targettype="inflection"> 提取词形变化 */
private fun parseHitInflections(definition: String): List<WordFormEntry> {
    val hitPattern = Regex("""<hit[^>]*targettype=["']inflection["'][^>]*>(.*?)</hit>""", RegexOption.DOT_MATCHES_ALL)
    return hitPattern.findAll(definition).map { hit ->
        val inner = hit.groupValues[1]
        val formWord = extractSpanClass(inner, "inf")
            .ifEmpty { extractSpanClass(inner, "base") }
            .ifEmpty { extractHeadword(inner) }
        val label = extractSpanClass(inner, "comment")
        val ipa = extractIpa(inner)
        val audio = extractSoundPath(inner)
        val displayWord = if (label.isNotEmpty()) "$formWord ($label)" else formWord
        WordFormEntry(displayWord, ipa, audio)
    }.filter { it.word.isNotEmpty() }.toList()
}

/** 解析发音 */
private fun parsePronunciations(definition: String): List<PronunciationEntry> {
    // 1. 搜索国旗图片
    val flagPattern = Regex(
        """<img[^>]*src=["'][^"']*(uk_sound|us_sound)\.png[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    val flags = flagPattern.findAll(definition).toList()

    // 2. 搜索所有 sound:// 音频链接
    val audioPattern = Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
    val audios = audioPattern.findAll(definition).map { it.groupValues[1] }.toList()

    // 3. 搜索所有 IPA
    val ipas = mutableListOf<String>()
    Regex("""<span[^>]*class=["'][^"']*phon[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add(cleanText(it.groupValues[1])) }
    Regex("""<span[^>]*class=["'][^"']*\bipa\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add(cleanText(it.groupValues[1])) }
    Regex("""<ipa[^>]*>(.*?)</ipa>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add(cleanText(it.groupValues[1])) }
    // 文本中的 /IPA/ 模式
    Regex("""/([a-zA-Zˈˌːˈˌɪiːæɑːʌʊuːeɛəɜːɔːɒːθðʃʒŋɲʎɽɾʀʁʋʍʜʢʡɕʑʝʎɣχʁβɸθðszfvbdgkptmnɲŋlrjwhæɑɒʌʊeɛəɪiːæɑːɔːuːɜːəˈˌːˈ]+)/""")
        .findAll(definition).forEach { ipas.add("/${it.groupValues[1]}/") }
    val cleanIpas = ipas.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    if (flags.isNotEmpty()) {
        return flags.mapIndexed { i, flag ->
            val region = if (flag.groupValues[1].equals("uk_sound", ignoreCase = true)) "UK" else "US"
            val segStart = flag.range.last + 1
            val segEnd = if (i + 1 < flags.size) flags[i + 1].range.first else definition.length
            val segment = definition.substring(segStart, segEnd)
            var ipa = extractIpa(segment)
            if (ipa.isEmpty() && i < cleanIpas.size) ipa = cleanIpas[i]
            val audio = extractSoundPath(segment) ?: audios.getOrNull(i)
            PronunciationEntry(region, ipa, audio)
        }
    }

    // 无国旗：尝试用音频链接和 IPA 组合
    if (cleanIpas.isNotEmpty() || audios.isNotEmpty()) {
        val ipa = cleanIpas.firstOrNull() ?: ""
        val audio = audios.firstOrNull()
        return listOf(PronunciationEntry("UK", ipa, audio))
    }

    return emptyList()
}

/** 提取 <span class="className">content</span> 的内容 */
private fun extractSpanClass(content: String, className: String): String {
    val pattern = Regex(
        """<span[^>]*class=["'][^"']*\b$className\b[^"']*["'][^>]*>(.*?)</span>""",
        RegexOption.DOT_MATCHES_ALL
    )
    return pattern.find(content)?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
}

private fun extractHeadword(content: String): String {
    return Regex("""<hw[^>]*>(.*?)</hw>""", RegexOption.DOT_MATCHES_ALL)
        .find(content)?.groupValues?.get(1)?.replace("|", "")?.let { cleanText(it) } ?: ""
}

private fun extractIpa(content: String): String {
    extractSpanClass(content, "phon").takeIf { it.isNotEmpty() }?.let { return it }
    extractSpanClass(content, "ipa").takeIf { it.isNotEmpty() }?.let { return it }
    Regex("""<ipa[^>]*>(.*?)</ipa>""", RegexOption.DOT_MATCHES_ALL).find(content)?.let {
        return cleanText(it.groupValues[1])
    }
    Regex("""/([^\s/<]+[^\s/<]*[^\s/<]*)/""").find(content)?.let {
        val candidate = cleanText(it.groupValues[1])
        if (candidate.isNotEmpty() && candidate.length < 50) return "/$candidate/"
    }
    return ""
}

private fun extractSoundPath(content: String): String? {
    return Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(content)?.groupValues?.get(1)
}

private fun cleanText(html: String): String {
    return html.replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun hasReadingContent(definition: String): Boolean {
    return definition.contains("sense-block", ignoreCase = true) ||
            definition.contains("sense-head", ignoreCase = true) ||
            definition.contains("<ex", ignoreCase = true) ||
            definition.contains("panel", ignoreCase = true) ||
            definition.contains("entry", ignoreCase = true)
}

// endregion
