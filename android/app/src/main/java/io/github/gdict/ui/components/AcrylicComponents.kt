package io.github.gdict.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.gdict.ui.theme.GdictColors

/**
 * Restrained translucent surface for navigation and interactive chrome.
 *
 * Compose's Modifier.blur() blurs the composable itself, not the content behind
 * it, so this component deliberately does not expose a fake backdrop-blur API.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val background = if (darkMode) GdictColors.DarkGlassSurface else GdictColors.GlassSurface
    val border = if (darkMode) GdictColors.DarkGlassBorder else GdictColors.GlassBorder

    Box(
        modifier = modifier
            .shadow(1.dp, shape)
            .clip(shape)
            .border(0.5.dp, border, shape)
            .background(background)
    ) {
        content()
    }
}

@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    darkMode: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val background = if (darkMode) GdictColors.DarkGlassSurface else GdictColors.GlassSurface
    val border = if (darkMode) GdictColors.DarkGlassBorder else GdictColors.GlassBorder

    Row(
        modifier = modifier
            .shadow(1.dp, shape)
            .clip(shape)
            .border(0.5.dp, border, shape)
            .background(background)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Compatibility wrappers for code that has not reached its migration phase. */
@Composable
fun AcrylicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) = GlassCard(modifier, shape, darkMode, content)

@Composable
fun AcrylicCapsule(
    modifier: Modifier = Modifier,
    darkMode: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) = GlassCapsule(modifier, darkMode, onClick, content)
