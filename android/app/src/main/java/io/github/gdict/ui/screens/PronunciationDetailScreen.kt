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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.R
import io.github.gdict.data.AndroidDictionaryRepository
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
 *
 * 原生渲染：Blue Frost 渐变背景 + 弥散光斑、Floating 搜索栏、Acrylic Card、
 * 单词视觉中心、Pronunciation Chip（英/美水平排列）、Word Form Chip。
 * 释义/例句等阅读内容由 MdxWebView 承载。
 * 当发音数据解析失败时，回退为 WebView 渲染全部内容（仍保留原生 Acrylic 框架）。
 */
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
    var contentScale by remember { mutableStateOf(1f) }
    var searchQuery by remember { mutableStateOf("") }

    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
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
    val cdPron = stringResource(R.string.cd_pronunciation)
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
                GlassCircleButton(onClick = onBack, darkMode = darkMode) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = cdBack,
                        tint = primaryTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    cdPron,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                GlassCircleButton(onClick = onShare, darkMode = darkMode) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = cdShare,
                        tint = primaryTint,
                        modifier = Modifier.size(20.dp)
                    )
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // 大型 Floating Acrylic Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(32.dp), ambientColor = GdictColors.Primary.copy(alpha = 0.12f), spotColor = GdictColors.Primary.copy(alpha = 0.08f))
                            .clip(RoundedCornerShape(32.dp))
                            .border(1.dp, glassBorder, RoundedCornerShape(32.dp))
                            .background(glassBg)
                            .pageEnterAnimation()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            val displayWord = data.word.ifEmpty { word }

                            if (data.parsedOk && data.pronunciations.isNotEmpty()) {
                                // —— 原生渲染：单词 + 发音 ——
                                Text(
                                    displayWord,
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    lineHeight = 62.sp
                                )

                                Spacer(modifier = Modifier.height(28.dp))
                                SectionHeader(cdPron, darkMode)
                                Spacer(modifier = Modifier.height(16.dp))
                                PronunciationChipsRow(
                                    pronunciations = data.pronunciations,
                                    darkMode = darkMode,
                                    glassBorder = glassBorder
                                ) { entry ->
                                    playAudio(entry.audioPath, displayWord)
                                }
                            } else {
                                // —— 回退：WebView 渲染全部内容 ——
                                Text(
                                    displayWord,
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    lineHeight = 62.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                PronunciationWebView(
                                    definition = definition,
                                    css = css,
                                    darkMode = darkMode,
                                    contentScale = contentScale,
                                    dictionaryRepository = dictionaryRepository,
                                    onEntryClick = onEntryClick,
                                    playAudio = playAudio,
                                    displayWord = displayWord
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
private fun PronunciationWebView(
    definition: String,
    css: String,
    darkMode: Boolean,
    contentScale: Float,
    dictionaryRepository: AndroidDictionaryRepository,
    onEntryClick: (String) -> Unit,
    playAudio: (String?, String) -> Unit,
    displayWord: String
) {
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
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .shadow(3.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
            .background(glassBg)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = GdictColors.Primary.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            androidx.compose.material3.TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("搜索单词", color = placeholderColor, fontSize = 16.sp)
                },
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = GdictColors.Primary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
                )
            )
        }
    }
}

@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    darkMode: Boolean,
    content: @Composable () -> Unit
) {
    val bg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val border = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .border(0.5.dp, border, CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(title: String, darkMode: Boolean) {
    val accent = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(GdictColors.PrimarySoft, GdictColors.Primary))
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PronunciationChipsRow(
    pronunciations: List<PronunciationEntry>,
    darkMode: Boolean,
    glassBorder: Color,
    onPlay: (PronunciationEntry) -> Unit
) {
    // 按 region 分组：UK 一列，US 一列，整齐排列
    val ukProns = pronunciations.filter { it.region.equals("UK", true) }
    val usProns = pronunciations.filter { it.region.equals("US", true) }
    val maxRows = maxOf(ukProns.size, usProns.size)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0 until maxRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // UK 列
                if (i < ukProns.size) {
                    PronunciationChip(ukProns[i], darkMode, glassBorder, Modifier.weight(1f)) { onPlay(ukProns[i]) }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                // US 列
                if (i < usProns.size) {
                    PronunciationChip(usProns[i], darkMode, glassBorder, Modifier.weight(1f)) { onPlay(usProns[i]) }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PronunciationChip(
    pron: PronunciationEntry,
    darkMode: Boolean,
    glassBorder: Color,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit
) {
    // 亚克力质感：半透明渐变背景 + 高光边框 + 柔和阴影
    val chipBg = if (darkMode) {
        Brush.linearGradient(
            colors = listOf(GdictColors.DarkSubtleHover.copy(alpha = 0.7f), GdictColors.BlueSurfaceGlassDark.copy(alpha = 0.5f))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.6f), GdictColors.BluePrimaryLight.copy(alpha = 0.22f))
        )
    }
    val highlightBorder = if (darkMode) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.7f)
    }
    val ipaColor = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, highlightBorder, RoundedCornerShape(20.dp))
            .background(chipBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlagBadge(pron.region, size = 28.dp)
            if (pron.ipa.isNotEmpty()) {
                Text(
                    pron.ipa,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = ipaColor,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            SpeakerButton(onPlay)
        }
    }
}

@Composable
private fun WordFormChip(
    form: WordFormEntry,
    darkMode: Boolean,
    glassBorder: Color,
    onEntryClick: (String) -> Unit,
    onPlay: () -> Unit
) {
    val chipBg = if (darkMode) {
        GdictColors.DarkSubtleHover.copy(alpha = 0.6f)
    } else {
        GdictColors.BluePrimaryLight.copy(alpha = 0.15f)
    }
    val wordColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val ipaColor = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(18.dp))
            .background(chipBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                form.word,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = wordColor,
                modifier = Modifier.clickable { onEntryClick(form.word) }
            )
            if (form.ipa.isNotEmpty()) {
                Text(
                    form.ipa,
                    fontSize = 15.sp,
                    color = ipaColor
                )
            }
        }
        SpeakerButton(onPlay)
    }
}

@Composable
private fun SpeakerButton(onPlay: () -> Unit) {
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
            .size(40.dp)
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
            Icons.Default.VolumeUp,
            contentDescription = null,
            tint = GdictColors.OnPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PronActionButton(
    icon: ImageVector,
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
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(24.dp))
            .background(glassBg)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = tint)
        }
    }
}

/**
 * 简化国旗徽标：UK 为蓝底十字旗，US 为星条旗，均裁切为圆形。
 */
@Composable
private fun FlagBadge(region: String, size: Dp = 26.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            if (region.equals("UK", ignoreCase = true)) {
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

// region 背景

private fun pronunciationBgGradient(darkMode: Boolean): Brush = if (darkMode) {
    Brush.verticalGradient(
        0.0f to GdictColors.DarkBackground,
        0.5f to GdictColors.DarkSurface,
        1.0f to GdictColors.DarkSurfaceVariant
    )
} else {
    Brush.verticalGradient(
        0.0f to Color(0xFFDCEBFF),
        0.4f to Color(0xFFEDF4FF),
        0.75f to Color(0xFFF7FAFF),
        1.0f to Color(0xFFFFFFFF)
    )
}

/**
 * 发音页专属弥散背景：比通用版更明显，模拟 Windows 11 Acrylic 漫射光。
 * 多个不规则径向光斑叠加，浓度足以在浅色背景上可见。
 */
@Composable
private fun Modifier.pronunciationAmbientBackground(
    darkMode: Boolean,
    screenWidthPx: Float,
    screenHeightPx: Float
): Modifier {
    if (darkMode) {
        val d1 = Brush.radialGradient(
            colors = listOf(Color(0xFF1E8CFF).copy(alpha = 0.10f), Color.Transparent),
            center = Offset(screenWidthPx * 0.15f, screenHeightPx * 0.1f),
            radius = screenHeightPx * 0.55f
        )
        val d2 = Brush.radialGradient(
            colors = listOf(Color(0xFF4DA3FF).copy(alpha = 0.08f), Color.Transparent),
            center = Offset(screenWidthPx * 0.85f, screenHeightPx * 0.85f),
            radius = screenHeightPx * 0.5f
        )
        return this.background(d1).background(d2)
    }

    val spot1 = Brush.radialGradient(
        colors = listOf(Color(0xFF1E8CFF).copy(alpha = 0.14f), Color.Transparent),
        center = Offset(screenWidthPx * 0.12f, screenHeightPx * 0.08f),
        radius = screenHeightPx * 0.55f
    )
    val spot2 = Brush.radialGradient(
        colors = listOf(Color(0xFF7B9CFF).copy(alpha = 0.11f), Color.Transparent),
        center = Offset(screenWidthPx * 0.88f, screenHeightPx * 0.22f),
        radius = screenHeightPx * 0.5f
    )
    val spot3 = Brush.radialGradient(
        colors = listOf(Color(0xFF5BA8E8).copy(alpha = 0.09f), Color.Transparent),
        center = Offset(screenWidthPx * 0.3f, screenHeightPx * 0.65f),
        radius = screenHeightPx * 0.45f
    )
    val spot4 = Brush.radialGradient(
        colors = listOf(Color(0xFF1E8CFF).copy(alpha = 0.10f), Color.Transparent),
        center = Offset(screenWidthPx * 0.82f, screenHeightPx * 0.92f),
        radius = screenHeightPx * 0.5f
    )
    val spot5 = Brush.radialGradient(
        colors = listOf(Color(0xFFA8C8FF).copy(alpha = 0.08f), Color.Transparent),
        center = Offset(screenWidthPx * 0.5f, screenHeightPx * 0.4f),
        radius = screenHeightPx * 0.6f
    )
    return this
        .background(spot1)
        .background(spot2)
        .background(spot3)
        .background(spot4)
        .background(spot5)
}

// endregion

// region 发音词典 HTML 解析

private const val HIDE_PRON_CSS = "\n.cpepd .main-headword,.cpepd .main-ipa,.cpepd .main-pronunciation,.cpepd .main-audio-btns,.cpepd .cepd-forms-section{display:none !important;}"

private fun parsePronunciationData(definition: String, fallbackWord: String): PronunciationData {
    // 实际 HTML 结构：<span class="arl"> 包裹 <hit targettype="hw"> 和 <hit targettype="inflection">
    val word = extractHeadword(definition).ifEmpty { fallbackWord }
    val pronunciations = parsePronunciations(definition)
    val wordForms = parseHitInflections(definition)
    val parsedOk = pronunciations.isNotEmpty() || wordForms.isNotEmpty()
    return PronunciationData(word, pronunciations, wordForms, hasReadingContent(definition), parsedOk)
}

/**
 * 从 <hit targettype="inflection"> 提取词形变化。
 * 每个 hit 包含 <span class="results"> > <span class="base"> > <span class="inf">词形</span>
 * 以及 <span class="comment">标签</span>（如 present tense）
 */
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

/**
 * 解析发音：在整个 definition 中搜索国旗图片、IPA、音频链接。
 * 支持多种格式：<img src="uk_sound.png">、<span class="phon">、<span class="ipa">、
 * sound:// 链接、以及 /IPA/ 文本模式。
 */
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

    // 3. 搜索所有 IPA（多种来源）
    val ipas = mutableListOf<String>()
    // <span class="phon">...</span>
    Regex("""<span[^>]*class=["'][^"']*phon[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add(cleanText(it.groupValues[1])) }
    // <span class="ipa">...</span>
    Regex("""<span[^>]*class=["'][^"']*\bipa\b[^"']*["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add(cleanText(it.groupValues[1])) }
    // <ipa>...</ipa>
    Regex("""<ipa[^>]*>(.*?)</ipa>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(definition).forEach { ipas.add(cleanText(it.groupValues[1])) }
    // 文本中的 /IPA/ 模式（如 /riːd/）
    Regex("""/([a-zA-Zˈˌːˈˌɪiːæɑːʌʊuːeɛəɜːɔːɒːθðʃʒŋɲʎɽɾʀʁʋʍʜʢʡɕʑʝʎɣχʁβɸθðszfvbdgkptmnɲŋlrjwhæɑɒʌʊeɛəɪiːæɑːɔːuːɜːəˈˌːˈ]+)/""")
        .findAll(definition).forEach { ipas.add("/${it.groupValues[1]}/") }
    // 去重并过滤空
    val cleanIpas = ipas.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    if (flags.isNotEmpty()) {
        // 有国旗：按国旗分段提取
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
    // <span class="phon">...</span>
    extractSpanClass(content, "phon").takeIf { it.isNotEmpty() }?.let { return it }
    // <span class="ipa">...</span>
    extractSpanClass(content, "ipa").takeIf { it.isNotEmpty() }?.let { return it }
    // <ipa>...</ipa>
    Regex("""<ipa[^>]*>(.*?)</ipa>""", RegexOption.DOT_MATCHES_ALL).find(content)?.let {
        return cleanText(it.groupValues[1])
    }
    // 文本中的 /IPA/ 模式
    Regex("""/([^\s/<]+[^\s/<]*[^\s/<]*)/""").find(content)?.let {
        val candidate = cleanText(it.groupValues[1])
        // 确保看起来像 IPA（包含音标字符或纯字母）
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
            definition.contains("<comment", ignoreCase = true) ||
            definition.contains("definition", ignoreCase = true) ||
            definition.contains("class=\"def\"", ignoreCase = true) ||
            definition.contains("class='def'", ignoreCase = true)
}

// endregion
