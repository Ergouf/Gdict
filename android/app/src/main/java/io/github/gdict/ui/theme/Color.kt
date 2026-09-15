package io.github.gdict.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic color system for the Android UI.
 *
 * The palette follows Apple HIG principles rather than copying UIKit values or
 * pretending that Android renders Apple's system Liquid Glass. Content uses
 * quiet neutral surfaces; the Gdict brand blue is reserved for selection,
 * links, focus and primary actions.
 */
object GdictColors {
    // Brand / interaction accent
    val Primary = Color(0xFF1E8CFF)
    val PrimarySoft = Color(0xFF4DA3FF)
    val PrimaryLight = Color(0xFF64B0FF)
    val OnPrimary = Color.White
    val PrimaryContainer = Color(0xFFE8F3FF)
    val OnPrimaryContainer = Color(0xFF0B3A66)

    // Neutral secondary roles
    val Secondary = Color(0xFF6E6E73)
    val SecondaryLight = Color(0xFFAEAEB2)
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFEFEFF4)
    val OnSecondaryContainer = Color(0xFF3A3A3C)
    val Tertiary = Color(0xFF8E8E93)
    val OnTertiary = Color.White

    // Semantic state accents
    val Accent = Color(0xFF8E8E93)
    val TealAccent = Color(0xFF32ADE6)
    val CoralAccent = Color(0xFFFF453A)
    val AmberAccent = Color(0xFFFF9F0A)
    val MintGreen = Color(0xFF30D158)

    // Content surfaces — intentionally neutral
    val Background = Color(0xFFF2F2F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF2F2F7)
    val OnBackground = Color(0xFF1C1C1E)
    val OnSurface = Color(0xFF1C1C1E)
    val OnSurfaceVariant = Color(0xFF6E6E73)

    // Separators and low-emphasis fills
    val Outline = Color(0xFFC6C6C8)
    val OutlineVariant = Color(0xFFE5E5EA)
    val CardStroke = OutlineVariant
    val SubtleHover = Color(0xFFF2F2F7)
    val SubtleSelected = Color(0xFFE5E5EA)

    // Android chrome surfaces are deliberately opaque. Earlier faux-glass
    // alpha fills caused visible rectangular compositing bands around text and
    // navigation content on some Android GPU/driver combinations. Keep the
    // rounded material, border and elevation, but avoid translucent parent
    // surfaces until real backdrop material is available.
    val GlassSurface = Color(0xFFFFFFFF)
    val GlassSurfaceStrong = Color(0xFFFFFFFF)
    val GlassBorder = Color.White.copy(alpha = 0.72f)
    val GlassSeparator = Color.Black.copy(alpha = 0.08f)

    val DarkGlassSurface = Color(0xFF2C2C2E)
    val DarkGlassSurfaceStrong = Color(0xFF3A3A3C)
    val DarkGlassBorder = Color.White.copy(alpha = 0.14f)
    val DarkGlassSeparator = Color.White.copy(alpha = 0.12f)

    // Compatibility aliases during the phased migration. New code should use
    // the semantic Glass* / Background / Surface names above.
    val BlueSurfaceGlass = GlassSurface
    val BlueSurfaceGlassDark = DarkGlassSurface
    val BlueHighlightBorder = GlassBorder
    val BlueCardBorder = GlassSeparator
    val BlueBackgroundTop = Background
    val BlueBackgroundBottom = Background
    val AmbientLight = Color.Transparent
    val BluePrimaryLight = PrimaryContainer
    val BluePlaceholder = OnSurfaceVariant
    val HeadingDark = OnBackground

    // Dark semantic surfaces
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF1C1C1E)
    val DarkSurfaceVariant = Color(0xFF2C2C2E)
    val DarkOnBackground = Color(0xFFF2F2F7)
    val DarkOnSurface = Color(0xFFF2F2F7)
    val DarkOnSurfaceVariant = Color(0xFFAEAEB2)
    val DarkOutline = Color(0xFF636366)
    val DarkOutlineVariant = Color(0xFF38383A)
    val DarkCardStroke = DarkOutlineVariant
    val DarkSubtleHover = Color(0xFF2C2C2E)
    val DarkSubtleSelected = Color(0xFF3A3A3C)
    val DarkPrimaryContainer = Color(0xFF0A3B69)
    val DarkOnPrimaryContainer = Color(0xFFD8ECFF)
    val DarkSecondaryContainer = Color(0xFF2C2C2E)
    val DarkOnSecondaryContainer = Color(0xFFD1D1D6)

    val Scrim = Color.Black

    // Legacy snapshot aliases. Kept only so old Paparazzi fixtures remain
    // compilable until Phase 5 replaces them with production-composable goldens.
    val NavyBlue = Color(0xFF2C4A6E)
    val NavyBlueLight = Color(0xFF3D5F85)
    val NavyBlueDark = Color(0xFF1E3A5F)
    val LightGray = Color(0xFFF5F5F5)
    val MediumGray = Color(0xFF9E9E9E)
    val DarkGray = Color(0xFF424242)
}
