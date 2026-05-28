package io.github.gdict.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GdictColors.Primary,
    onPrimary = GdictColors.OnPrimary,
    primaryContainer = Color(0xFFEDF0F4),
    onPrimaryContainer = Color(0xFF3F4E5C),
    secondary = GdictColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8ECF1),
    onSecondaryContainer = Color(0xFF505D6C),
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
    primaryContainer = Color(0xFF2B3542),
    onPrimaryContainer = Color(0xFFBCC6D1),
    secondary = GdictColors.SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E3845),
    onSecondaryContainer = Color(0xFFA9B5C3),
    tertiary = Color(0xFF95A4B5),
    onTertiary = Color.Black,
    background = GdictColors.DarkBackground,
    onBackground = GdictColors.DarkOnBackground,
    surface = GdictColors.DarkSurface,
    onSurface = GdictColors.DarkOnSurface,
    surfaceVariant = GdictColors.DarkSurfaceVariant,
    onSurfaceVariant = GdictColors.DarkOnSurfaceVariant,
    outline = GdictColors.DarkOutline,
    outlineVariant = Color(0xFF3A424D),
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
