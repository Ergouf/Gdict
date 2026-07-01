package io.github.gdict.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GdictColors.Primary,
    onPrimary = GdictColors.OnPrimary,
    primaryContainer = GdictColors.PrimaryContainer,
    onPrimaryContainer = GdictColors.OnPrimaryContainer,
    secondary = GdictColors.Secondary,
    onSecondary = GdictColors.OnSecondary,
    secondaryContainer = GdictColors.SecondaryContainer,
    onSecondaryContainer = GdictColors.OnSecondaryContainer,
    tertiary = GdictColors.Accent,
    onTertiary = Color.White,
    background = GdictColors.Background,
    onBackground = GdictColors.OnBackground,
    surface = GdictColors.Surface,
    onSurface = GdictColors.OnSurface,
    surfaceVariant = GdictColors.SurfaceVariant,
    onSurfaceVariant = GdictColors.OnSurfaceVariant,
    outline = GdictColors.Outline,
    outlineVariant = GdictColors.OutlineVariant,
    error = GdictColors.CoralAccent,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = GdictColors.PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = GdictColors.DarkPrimaryContainer,
    onPrimaryContainer = GdictColors.DarkOnPrimaryContainer,
    secondary = GdictColors.SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = GdictColors.DarkSecondaryContainer,
    onSecondaryContainer = GdictColors.DarkOnSecondaryContainer,
    tertiary = Color(0xFF95A4B5),
    onTertiary = Color.Black,
    background = GdictColors.DarkBackground,
    onBackground = GdictColors.DarkOnBackground,
    surface = GdictColors.DarkSurface,
    onSurface = GdictColors.DarkOnSurface,
    surfaceVariant = GdictColors.DarkSurfaceVariant,
    onSurfaceVariant = GdictColors.DarkOnSurfaceVariant,
    outline = GdictColors.DarkOutline,
    outlineVariant = GdictColors.DarkOutlineVariant,
    error = Color(0xFFEF9A9A),
    onError = Color.Black,
)

@Composable
fun GdictTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
