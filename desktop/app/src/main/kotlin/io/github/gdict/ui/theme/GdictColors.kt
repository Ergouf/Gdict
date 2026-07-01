package io.github.gdict.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fluent Design 色板（通过 MD3 colorScheme token 承载）。
 *
 * 设计依据：Fluent 2 Color Tokens（https://fluent2.microsoft.design/color-tokens/）。
 * - 品牌主色（accent）固定为绿色 #5D8A6B，保留项目视觉身份。
 * - 中性色采用 Fluent 官方纯灰阶（colorNeutralBackground/Foreground/Stroke）。
 * - 桌面端 Background 使用半透明 alpha，让 Windows 11 Mica 材料透出；
 *   Android 端 Background 不透明（无 Mica 概念），RGB 与桌面保持一致。
 */
object GdictColors {
    // 品牌主色（绿色 accent）—— Fluent accent 语义
    val Primary = Color(0xFF5D8A6B)
    val PrimarySoft = Color(0xFF63BD04) // 高亮绿，用于开关/选中态强反馈
    val PrimaryLight = Color(0xFF7DA88B) // 深色主题下的主色（提亮以保证对比度）
    val OnPrimary = Color.White

    val PrimaryContainer = Color(0xFFE8F0EB) // 浅绿容器
    val OnPrimaryContainer = Color(0xFF1F3A28)

    // 次级色（中性石板灰，由原 Primary 降级而来）—— 用于次要交互
    val Secondary = Color(0xFF5B6B7C)
    val SecondaryLight = Color(0xFF8495A8)
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFEFEFEF) // colorSubtleBackground hover
    val OnSecondaryContainer = Color(0xFF3F4E5C)

    val Accent = Color(0xFF7D8E9F)
    val TealAccent = Color(0xFF5D8A6B) // 别名，与 Primary 一致，保留以兼容旧引用
    val CoralAccent = Color(0xFFE08B82) // error
    val AmberAccent = Color(0xFFE5A84B)
    val MintGreen = Color(0xFF8FBC8F)

    // Fluent 中性背景（纯灰阶）
    val Background = Color(0xE6FAFAFA) // 桌面端 90% 不透明，透出 Mica
    val Surface = Color(0xFFFAFAFA) // colorNeutralCardBackground
    val SurfaceVariant = Color(0xFFF4F4F4) // colorNeutralBackground3

    // Fluent 中性前景
    val OnBackground = Color(0xFF242424) // colorNeutralForeground1
    val OnSurface = Color(0xFF242424)
    val OnSurfaceVariant = Color(0xFF383838) // colorNeutralForeground2

    // Fluent 描边
    val Outline = Color(0xFFD1D1D1) // colorNeutralStroke1
    val OutlineVariant = Color(0xFFE0E0E0) // colorNeutralStroke2
    val CardStroke = Color(0xFFE0E0E0) // CardStrokeColorDefault

    // Fluent 微妙填充（hover / selected 态）
    val SubtleHover = Color(0xFFF4F4F4) // colorSubtleBackground hover
    val SubtleSelected = Color(0xFFEAEAEA) // colorSubtleBackground selected

    // 深色主题
    val DarkBackground = Color(0xE61F1F1F) // 桌面端 90% 不透明，透出深色 Mica
    val DarkSurface = Color(0xFF292929) // colorNeutralCardBackground dark
    val DarkSurfaceVariant = Color(0xFF141414)
    val DarkOnBackground = Color(0xFFFFFFFF)
    val DarkOnSurface = Color(0xFFFFFFFF)
    val DarkOnSurfaceVariant = Color(0xFFD4D4D4)
    val DarkOutline = Color(0xFF595959)
    val DarkOutlineVariant = Color(0xFF404040)
    val DarkCardStroke = Color(0xFF404040)
    val DarkSubtleHover = Color(0xFF2B2B2B)
    val DarkSubtleSelected = Color(0xFF333333)
    val DarkPrimaryContainer = Color(0xFF2A3D31)
    val DarkOnPrimaryContainer = Color(0xFFB5D9C1)
    val DarkSecondaryContainer = Color(0xFF2B2B2B)
    val DarkOnSecondaryContainer = Color(0xFFD4D4D4)

    val Scrim = Color(0xFF000000)

    // 桌面侧边栏（Fluent NavigationView 语义，桌面端专用）
    val SidebarBackground = Color(0x00FFFFFF) // 透明，叠在 Mica 之上
    val SidebarSelected = Color(0xFFE8F0EB) // 选中态浅绿填充
    val SidebarIconActive = Color(0xFF5D8A6B) // 品牌绿
    val SidebarIconInactive = Color(0xFF565656) // colorNeutralForeground3
    val DarkSidebarBackground = Color(0x001C1F24)
    val DarkSidebarSelected = Color(0xFF1E3020)
}
