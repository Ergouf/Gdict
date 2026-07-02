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
import io.github.gdict.BuildConfig
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

            // 版本标记（调试用，确认实际运行版本）
            Text(
                "v${BuildConfig.VERSION_NAME} · pron-native",
                fontSize = 10.sp,
                color = subtitleColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp)
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
                            .shadow(6.dp, RoundedCornerShape(32.dp))
                            .clip(RoundedCornerShape(32.dp))
                            .border(1.dp, glassBorder, RoundedCornerShape(32.dp))
                            .background(glassBg)
                            .pageEnterAnimation()
                    ) {
                        Column(modifier = Modifier.padding(28.dp)) {
                            val displayWord = data.word.ifEmpty { word }

                            // 始终原生渲染单词标题（即使解析失败）
                            Text(
                                displayWord,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                lineHeight = 58.sp
                            )

                            if (data.parsedOk && data.pronunciations.isNotEmpty()) {
                                // —— 原生渲染：Pronunciation Chip + Word Forms + 阅读区 ——
                                if (data.pronunciations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    SectionHeader(cdPron, darkMode)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    PronunciationChipsRow(
                                        pronunciations = data.pronunciations,
                                        darkMode = darkMode,
                                        glassBorder = glassBorder
                                    ) { entry ->
                                        playAudio(entry.audioPath, displayWord)
                                    }
                                }

                                if (data.wordForms.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    SectionHeader("Word Forms", darkMode)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        data.wordForms.forEach { form ->
                                            WordFormChip(
                                                form = form,
                                                darkMode = darkMode,
                                                glassBorder = glassBorder,
                                                onEntryClick = onEntryClick
                                            ) {
                                                playAudio(form.audioPath, form.word.ifEmpty { displayWord })
                                            }
                                        }
                                    }
                                }

                                if (data.hasReadingContent) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    SectionHeader("Definitions", darkMode)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PronunciationWebView(
                                        definition = definition,
                                        css = css + HIDE_PRON_CSS,
                                        darkMode = darkMode,
                                        contentScale = contentScale,
                                        dictionaryRepository = dictionaryRepository,
                                        onEntryClick = onEntryClick,
                                        playAudio = playAudio,
                                        displayWord = displayWord
                                    )
                                }
                            } else {
                                // —— 回退：WebView 渲染全部内容（保留原生单词标题 + Acrylic 卡片框架）——
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pronunciations.forEach { entry ->
            PronunciationChip(entry, darkMode, glassBorder) { onPlay(entry) }
        }
    }
}

@Composable
private fun PronunciationChip(
    pron: PronunciationEntry,
    darkMode: Boolean,
    glassBorder: Color,
    onPlay: () -> Unit
) {
    val chipBg = if (darkMode) {
        GdictColors.DarkSubtleHover.copy(alpha = 0.6f)
    } else {
        GdictColors.BluePrimaryLight.copy(alpha = 0.18f)
    }
    val ipaColor = if (darkMode) GdictColors.PrimaryLight else GdictColors.Primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .border(0.5.dp, glassBorder, RoundedCornerShape(18.dp))
            .background(chipBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlagBadge(pron.region)
        if (pron.ipa.isNotEmpty()) {
            Text(
                pron.ipa,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = ipaColor
            )
        }
        SpeakerButton(onPlay)
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
    val arlPattern = Regex("""<arl[^>]*>(.*?)</arl>""", RegexOption.DOT_MATCHES_ALL)
    val arlMatches = arlPattern.findAll(definition).toList()

    if (arlMatches.isEmpty()) {
        return PronunciationData(fallbackWord, emptyList(), emptyList(), hasReadingContent(definition), parsedOk = false)
    }

    val mainContent = arlMatches.last().groupValues[1]
    val formsContents = arlMatches.dropLast(1).map { it.groupValues[1] }

    val word = extractHeadword(mainContent).ifEmpty { fallbackWord }
    val pronunciations = parsePronunciations(mainContent)
    val wordForms = formsContents.map { parseWordForm(it) }.toMutableList()
    wordForms.addAll(parseInflections(mainContent))

    val parsedOk = pronunciations.isNotEmpty() || wordForms.isNotEmpty()
    return PronunciationData(word, pronunciations, wordForms, hasReadingContent(definition), parsedOk)
}

private fun parseInflections(content: String): List<WordFormEntry> {
    val inflPattern = Regex("""<inflection[^>]*>(.*?)</inflection>""", RegexOption.DOT_MATCHES_ALL)
    return inflPattern.findAll(content).map { infl ->
        val inner = infl.groupValues[1]
        val labelMatch = Regex("""<label[^>]*>(.*?)</label>""", RegexOption.DOT_MATCHES_ALL).find(inner)
        val label = cleanText(labelMatch?.groupValues?.get(1) ?: "")
        val inflWord = extractHeadword(inner).ifEmpty { cleanText(inner).substringBefore(":").trim() }
        val ipa = extractIpa(inner).ifEmpty {
            Regex("""<inf[^>]*>(.*?)</inf>""", RegexOption.DOT_MATCHES_ALL).find(inner)?.let { cleanText(it.groupValues[1]) } ?: ""
        }
        val audio = extractSoundPath(inner)
        val displayWord = if (label.isNotEmpty()) "$inflWord ($label)" else inflWord
        WordFormEntry(displayWord, ipa, audio)
    }.toList()
}

private fun parsePronunciations(content: String): List<PronunciationEntry> {
    val prongrpPattern = Regex("""<prongrp[^>]*>(.*?)</prongrp>""", RegexOption.DOT_MATCHES_ALL)
    val prongrpContent = prongrpPattern.findAll(content).joinToString("") { it.groupValues[1] }
    val source = if (prongrpContent.isNotEmpty()) prongrpContent else content

    val flagPattern = Regex(
        """<img[^>]*src=["'][^"']*(uk_sound|us_sound)\.png[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    val flags = flagPattern.findAll(source).toList()

    val allIpas = Regex("""<(?:inf|ipa)[^>]*>(.*?)</(?:inf|ipa)>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(content)
        .map { cleanText(it.groupValues[1]) }
        .filter { it.isNotEmpty() }
        .toList()

    if (flags.isEmpty()) {
        val ipa = allIpas.firstOrNull() ?: ""
        val audio = extractSoundPath(content)
        return if (ipa.isNotEmpty() || audio != null) {
            listOf(PronunciationEntry("UK", ipa, audio))
        } else emptyList()
    }

    return flags.mapIndexed { i, flag ->
        val region = if (flag.groupValues[1].equals("uk_sound", ignoreCase = true)) "UK" else "US"
        val segStart = flag.range.last + 1
        val segEnd = if (i + 1 < flags.size) flags[i + 1].range.first else source.length
        val segment = source.substring(segStart, segEnd)
        var ipa = extractIpa(segment)
        if (ipa.isEmpty() && i < allIpas.size) ipa = allIpas[i]
        val audio = extractSoundPath(segment)
        PronunciationEntry(region, ipa, audio)
    }
}

private fun parseWordForm(content: String): WordFormEntry {
    val word = extractHeadword(content)
    val ipa = extractIpa(content)
    val audio = extractSoundPath(content)
    return WordFormEntry(word, ipa, audio)
}

private fun extractHeadword(content: String): String {
    return Regex("""<hw[^>]*>(.*?)</hw>""", RegexOption.DOT_MATCHES_ALL)
        .find(content)?.groupValues?.get(1)?.replace("|", "")?.let { cleanText(it) } ?: ""
}

private fun extractIpa(content: String): String {
    Regex("""<inf[^>]*>(.*?)</inf>""", RegexOption.DOT_MATCHES_ALL).find(content)?.let {
        val t = cleanText(it.groupValues[1])
        if (t.isNotEmpty()) return t
    }
    Regex("""<ipa[^>]*>(.*?)</ipa>""", RegexOption.DOT_MATCHES_ALL).find(content)?.let {
        return cleanText(it.groupValues[1])
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
        .trim()
}

private fun hasReadingContent(definition: String): Boolean {
    return definition.contains("sense-block", ignoreCase = true) ||
            definition.contains("sense-head", ignoreCase = true) ||
            definition.contains("<ex", ignoreCase = true) ||
            definition.contains("panel", ignoreCase = true) ||
            definition.contains("<comment", ignoreCase = true) ||
            definition.contains("definition", ignoreCase = true) ||
            definition.contains("class=\"def\"", ignoreCase = true)
}

// endregion
