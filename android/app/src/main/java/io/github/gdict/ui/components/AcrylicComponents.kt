package io.github.gdict.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gdict.ui.theme.GdictColors

// API 31+ 才支持 Modifier.blur()，低版本降级为纯半透明
private fun Modifier.acrylicBlur(blurRadius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(blurRadius)
    } else {
        this
    }

@Composable
fun AcrylicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    darkMode: Boolean = false,
    blurRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    Box(
        modifier = modifier
            .shadow(2.dp, shape)
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
            .background(glassBg)
    ) {
        content()
    }
}

@Composable
fun AcrylicCapsule(
    modifier: Modifier = Modifier,
    darkMode: Boolean = false,
    blurRadius: Dp = 18.dp,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    Row(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(28.dp))
            .background(glassBg)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
