package io.github.gdict.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fluent Design 2 色板（品牌蓝主题）。
 *
 * 设计依据：Fluent 2 Color Tokens + Windows 11 Acrylic Glass。
 * 品牌主色为 Logo 同款蓝 #1E8CFF，全面移除绿色。
 * 桌面端 Background 使用半透明 alpha，让 Windows 11 Mica 材料透出。
 */
object GdictColors {
    // 品牌主色（蓝色 accent）—— Fluent accent 语义
    val Primary = Color(0xFF1E8CFF)
    val PrimarySoft = Color(0xFF4DA3FF) // hover/pressed 反馈色
    val PrimaryLight = Color(0xFF7BB8FF) // 深色主题下的主色
    val OnPrimary = Color.White

    val PrimaryContainer = Color(0xFFE3F0FF) // 浅蓝容器
    val OnPrimaryContainer = Color(0xFF0D3B6B)

    // 次级色（中性石板灰）—— 用于次要交互
    val Secondary = Color(0xFF5B6B7C)
    val SecondaryLight = Color(0xFF8495A8)
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFEFEFEF)
    val OnSecondaryContainer = Color(0xFF3F4E5C)

    // 第三色
    val Tertiary = Color(0xFF6B7A90)
    val OnTertiary = Color.White

    val Accent = Color(0xFF6B7A90)
    val TealAccent = Color(0xFF1E8CFF) // 别名，与 Primary 一致（蓝色）
    val CoralAccent = Color(0xFFE08B82) // error / Again 评分
    val AmberAccent = Color(0xFFE5A84B) // Hard 评分
    val MintGreen = Color(0xFF5BC0EB) // Easy 评分（改为青蓝）

    // Fluent 中性背景（桌面端半透明，透出 Mica）
    val Background = Color(0xE6F5FAFF) // 桌面端 90% 不透明，蓝白渐变顶部
    val Surface = Color(0xFFFAFAFA) // colorNeutralCardBackground
    val SurfaceVariant = Color(0xFFF4F4F4) // colorNeutralBackground3

    // Fluent 中性前景
    val OnBackground = Color(0xFF102A56) // 深蓝黑色
    val OnSurface = Color(0xFF242424)
    val OnSurfaceVariant = Color(0xFF6B7A90) // 浅灰蓝辅助文字

    // Fluent 描边
    val Outline = Color(0xFFD1D1D1)
    val OutlineVariant = Color(0xFFE0E0E0)
    val CardStroke = Color(0xFFE0E0E0)

    // Fluent 微妙填充（hover / selected 态）
    val SubtleHover = Color(0xFFF4F4F4)
    val SubtleSelected = Color(0xFFEAEAEA)

    // Acrylic Glass 玻璃材质 token（与 Android 端统一）
    val BlueSurfaceGlass = Color(0xFFF7FAFE)
    val BlueSurfaceGlassDark = Color(0xCC1A2A3A)
    val BlueHighlightBorder = Color.White.copy(alpha = 0.60f)
    val BlueCardBorder = Color(0x221E8CFF)
    val BlueBackgroundTop = Color(0xFFDCEBFF)
    val BlueBackgroundBottom = Color(0xFFFFFFFF)
    val AmbientLight = Color(0xFF1E8CFF).copy(alpha = 0.06f)

    // Android 玻璃拟态扩展 token
    val BluePrimaryLight = Color(0xFFB3D8FF)
    val BluePlaceholder = Color(0xFF1E8CFF).copy(alpha = 0.5f)
    val HeadingDark = Color(0xFF102A56)

    // 深色主题 — 统一为纯黑，确保标题栏、侧边栏、内容区颜色一致
    val DarkBackground = Color(0xFF141414)
    val DarkSurface = Color(0xFF292929)
    val DarkSurfaceVariant = Color(0xFF141414)
    val DarkOnBackground = Color(0xFFFFFFFF)
    val DarkOnSurface = Color(0xFFFFFFFF)
    val DarkOnSurfaceVariant = Color(0xFFD4D4D4)
    val DarkOutline = Color(0xFF595959)
    val DarkOutlineVariant = Color(0xFF404040)
    val DarkCardStroke = Color(0xFF404040)
    val DarkSubtleHover = Color(0xFF2B2B2B)
    val DarkSubtleSelected = Color(0xFF333333)
    val DarkPrimaryContainer = Color(0xFF0D3B6B)
    val DarkOnPrimaryContainer = Color(0xFFB3D8FF)
    val DarkSecondaryContainer = Color(0xFF2B2B2B)
    val DarkOnSecondaryContainer = Color(0xFFD4D4D4)

    val Scrim = Color(0xFF000000)

    // 桌面侧边栏（Fluent NavigationView 语义，桌面端专用）
    val SidebarBackground = Color(0x00FFFFFF) // 透明，叠在 Mica 之上
    val SidebarSelected = Color(0xFFE3F0FF) // 选中态浅蓝填充
    val SidebarIconActive = Color(0xFF1E8CFF) // 品牌蓝
    val SidebarIconInactive = Color(0xFF565656)
    val DarkSidebarBackground = Color(0xFF141414) // 与 DarkBackground 统一
    val DarkSidebarSelected = Color(0xFF0D3B6B)
}
