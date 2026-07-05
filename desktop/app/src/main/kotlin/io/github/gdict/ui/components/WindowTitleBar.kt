package io.github.gdict.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import io.github.gdict.platform.WindowsBackdrop
import java.awt.Frame

/**
 * Windows 11 style custom title bar for the desktop app.
 *
 * Replaces the system native title bar so the app can fully control its
 * appearance in both light and dark modes.
 */
@Composable
fun WindowScope.WindowTitleBar(
    window: ComposeWindow,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val isMaximized = window.extendedState == Frame.MAXIMIZED_BOTH

    WindowDraggableArea(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(backgroundColor)
            .doubleClickToMaximize(window)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App icon + title
            Row(
                modifier = Modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource("icon.png"),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(onBackgroundColor)
                )
                Text(
                    text = "Gdict",
                    color = onBackgroundColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Window controls
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TitleBarButton(
                    contentDescription = "Minimize",
                    onClick = { WindowsBackdrop.minimize(window) }
                ) { iconColor ->
                    Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                TitleBarButton(
                    contentDescription = if (isMaximized) "Restore" else "Maximize",
                    onClick = { WindowsBackdrop.toggleMaximize(window) }
                ) { iconColor ->
                    if (isMaximized) {
                        RestoreIcon(iconColor)
                    } else {
                        MaximizeIcon(iconColor)
                    }
                }
                TitleBarButton(
                    contentDescription = "Close",
                    onClick = { WindowsBackdrop.close(window) },
                    hoverBackground = MaterialTheme.colorScheme.error,
                    hoverIconColor = Color.White
                ) { iconColor ->
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBarButton(
    contentDescription: String,
    onClick: () -> Unit,
    hoverBackground: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
    hoverIconColor: Color = MaterialTheme.colorScheme.onBackground,
    icon: @Composable (iconColor: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val iconColor = if (isHovered) hoverIconColor else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(46.dp)
            .background(if (isHovered) hoverBackground else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon(iconColor)
    }
}

@Composable
private fun MaximizeIcon(color: Color) {
    Box(
        modifier = Modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .drawBehind {
                    drawRect(
                        color = color,
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                        style = Stroke(width = 1.5f)
                    )
                }
        )
    }
}

@Composable
private fun RestoreIcon(color: Color) {
    Box(modifier = Modifier.size(12.dp)) {
        // Back rectangle (offset)
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(x = 3.dp, y = 0.dp)
                .drawBehind {
                    drawRect(
                        color = color,
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                        style = Stroke(width = 1.5f)
                    )
                }
        )
        // Front rectangle
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(x = 0.dp, y = 3.dp)
                .drawBehind {
                    drawRect(
                        color = color,
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                        style = Stroke(width = 1.5f)
                    )
                }
        )
    }
}

private fun Modifier.doubleClickToMaximize(window: ComposeWindow): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onDoubleTap = { WindowsBackdrop.toggleMaximize(window) }
    )
}
