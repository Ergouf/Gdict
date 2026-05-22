package io.github.gdict.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GdictColors.Primary,
    onPrimary = GdictColors.OnPrimary,
    primaryContainer = GdictColors.PrimaryContainer,
    onPrimaryContainer = GdictColors.OnPrimaryContainer,
    secondary = GdictColors.Secondary,
    onSecondary = GdictColors.OnSecondary,
    secondaryContainer = GdictColors.SecondaryContainer,
    onSecondaryContainer = GdictColors.OnSecondaryContainer,
    tertiary = GdictColors.Tertiary,
    onTertiary = GdictColors.OnTertiary,
    tertiaryContainer = GdictColors.TertiaryContainer,
    onTertiaryContainer = GdictColors.OnTertiaryContainer,
    error = GdictColors.Error,
    onError = GdictColors.OnError,
    errorContainer = GdictColors.ErrorContainer,
    onErrorContainer = GdictColors.OnErrorContainer,
    background = GdictColors.Surface,
    onBackground = GdictColors.OnSurface,
    surface = GdictColors.Surface,
    onSurface = GdictColors.OnSurface,
    surfaceVariant = GdictColors.SurfaceVariant,
    onSurfaceVariant = GdictColors.OnSurfaceVariant,
    outline = GdictColors.Outline,
    outlineVariant = GdictColors.OutlineVariant,
    surfaceDim = GdictColors.SurfaceDim,
    surfaceBright = GdictColors.SurfaceBright,
    surfaceContainerLowest = GdictColors.SurfaceContainerLowest,
    surfaceContainerLow = GdictColors.SurfaceContainerLow,
    surfaceContainer = GdictColors.SurfaceContainer,
    surfaceContainerHigh = GdictColors.SurfaceContainerHigh,
    surfaceContainerHighest = GdictColors.SurfaceContainerHighest,
    inverseSurface = GdictColors.InverseSurface,
    inverseOnSurface = GdictColors.InverseOnSurface,
    inversePrimary = GdictColors.InversePrimary,
    scrim = GdictColors.Scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = GdictColors.DarkPrimary,
    onPrimary = GdictColors.DarkOnPrimary,
    primaryContainer = GdictColors.DarkPrimaryContainer,
    onPrimaryContainer = GdictColors.PrimaryContainer,
    secondary = Color(0xFFC1CCAD),
    onSecondary = Color(0xFF2B341F),
    secondaryContainer = Color(0xFF414B34),
    onSecondaryContainer = Color(0xFFDCE7C6),
    tertiary = Color(0xFFA0CFD0),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF1E4E4F),
    onTertiaryContainer = Color(0xFFBBEBEC),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = GdictColors.DarkBackground,
    onBackground = GdictColors.DarkOnSurface,
    surface = GdictColors.DarkSurface,
    onSurface = GdictColors.DarkOnSurface,
    surfaceVariant = GdictColors.DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC2C8BB),
    outline = Color(0xFF8C9387),
    outlineVariant = Color(0xFF43483F),
    surfaceDim = Color(0xFF121410),
    surfaceBright = Color(0xFF383A33),
    surfaceContainerLowest = Color(0xFF0D0F0B),
    surfaceContainerLow = Color(0xFF1A1C17),
    surfaceContainer = Color(0xFF1E201B),
    surfaceContainerHigh = Color(0xFF282B25),
    surfaceContainerHighest = Color(0xFF333530),
    inverseSurface = Color(0xFFE1E4DA),
    inverseOnSurface = Color(0xFF2F312A),
    inversePrimary = GdictColors.Primary,
    scrim = GdictColors.Scrim,
)

@Composable
fun GdictTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GdictTypography,
        content = content
    )
}
