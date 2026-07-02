package io.github.gdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.gdict.ui.theme.GdictColors

/**
 * 亚克力弥散背景：多个不规则径向光斑叠加，模拟自然浓淡过渡。
 * 替代纯圆形 radialGradient，营造更接近真实亚克力材质的弥散效果。
 *
 * 光斑参数使用屏幕比例，适配不同尺寸。
 */
@Composable
fun Modifier.acrylicAmbientBackground(
    darkMode: Boolean = false,
    screenWidthPx: Float,
    screenHeightPx: Float
): Modifier {
    if (darkMode) return this

    // 多个弥散光斑：模拟亚克力材质的自然浓淡过渡，参考设计稿的柔和漫射光
    val spot1 = Brush.radialGradient(
        colors = listOf(Color(0xFF1E8CFF).copy(alpha = 0.07f), Color.Transparent),
        center = Offset(screenWidthPx * 0.1f, screenHeightPx * 0.08f),
        radius = screenHeightPx * 0.5f
    )
    val spot2 = Brush.radialGradient(
        colors = listOf(Color(0xFF7B9CFF).copy(alpha = 0.05f), Color.Transparent),
        center = Offset(screenWidthPx * 0.9f, screenHeightPx * 0.25f),
        radius = screenHeightPx * 0.45f
    )
    val spot3 = Brush.radialGradient(
        colors = listOf(Color(0xFF5BA8E8).copy(alpha = 0.04f), Color.Transparent),
        center = Offset(screenWidthPx * 0.3f, screenHeightPx * 0.6f),
        radius = screenHeightPx * 0.4f
    )
    val spot4 = Brush.radialGradient(
        colors = listOf(Color(0xFF1E8CFF).copy(alpha = 0.05f), Color.Transparent),
        center = Offset(screenWidthPx * 0.85f, screenHeightPx * 0.9f),
        radius = screenHeightPx * 0.5f
    )

    return this
        .background(spot1)
        .background(spot2)
        .background(spot3)
        .background(spot4)
}
