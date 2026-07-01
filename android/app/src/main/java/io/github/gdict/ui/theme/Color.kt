package io.github.gdict.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fluent Design 色板（通过 MD3 colorScheme token 承载）。
 *
 * 与桌面端 GdictColors 共享同一套 Fluent 色值（品牌绿 accent + Fluent 中性灰阶）。
 * 差异：Android 端无 Mica 系统材料，Background 不透明（alpha=FF）。
 * 设计依据：Fluent 2 Color Tokens（https://fluent2.microsoft.design/color-tokens/）。
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

    // Fluent 中性背景（纯灰阶，Android 不透明）
    val Background = Color(0xFFFAFAFA) // colorNeutralBackground2
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
    val DarkBackground = Color(0xFF1F1F1F) // colorNeutralBackground1 dark
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
}
