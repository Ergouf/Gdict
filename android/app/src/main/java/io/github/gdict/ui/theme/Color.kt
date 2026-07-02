package io.github.gdict.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fluent Design 2 色板（品牌蓝主题）。
 *
 * 通过 MD3 colorScheme token 承载，与桌面端共享同一套色值。
 * 品牌主色为 Logo 同款蓝 #1E8CFF，全面移除绿色。
 * 设计依据：Fluent 2 Color Tokens + Windows 11 Acrylic Glass。
 */
object GdictColors {
    // 品牌主色（蓝色 accent）—— Fluent accent 语义
    val Primary = Color(0xFF1E8CFF)
    val PrimarySoft = Color(0xFF4DA3FF) // hover/pressed 反馈色
    val PrimaryLight = Color(0xFF7BB8FF) // 深色主题下的主色（提亮以保证对比度）
    val OnPrimary = Color.White

    val PrimaryContainer = Color(0xFFE3F0FF) // 浅蓝容器
    val OnPrimaryContainer = Color(0xFF0D3B6B)

    // 次级色（中性石板灰）—— 用于次要交互
    val Secondary = Color(0xFF5B6B7C)
    val SecondaryLight = Color(0xFF8495A8)
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFEFEFEF)
    val OnSecondaryContainer = Color(0xFF3F4E5C)

    // 第三色（用于 MD3 tertiary token）
    val Tertiary = Color(0xFF6B7A90)
    val OnTertiary = Color.White

    val Accent = Color(0xFF6B7A90)
    val TealAccent = Color(0xFF1E8CFF) // 别名，与 Primary 一致（蓝色）
    val CoralAccent = Color(0xFFE08B82) // error / Again 评分
    val AmberAccent = Color(0xFFE5A84B) // Hard 评分
    val MintGreen = Color(0xFF5BC0EB) // Easy 评分（改为青蓝）

    // Fluent 中性背景（纯灰阶，Android 不透明）
    val Background = Color(0xFFF5FAFF) // 蓝白渐变顶部色
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

    // Acrylic Glass 玻璃材质 token — 参考设计稿：淡蓝白、足够不透明以遮挡页面背景
    val BlueSurfaceGlass = Color(0xFFEFF4FB) // Acrylic 卡片/导航栏填充（不透明，避免透白）
    val BlueSurfaceGlassDark = Color(0xCC1A2A3A) // 深色模式 Acrylic 填充
    val BlueHighlightBorder = Color.White.copy(alpha = 0.45f) // 1px 高光白边
    val BlueCardBorder = Color(0x221E8CFF) // 浅蓝微描边
    val BlueBackgroundTop = Color(0xFFDCEBFF) // 页面渐变顶部（更饱和蓝）
    val BlueBackgroundBottom = Color(0xFFFFFFFF) // 页面渐变底部
    val AmbientLight = Color(0xFF1E8CFF).copy(alpha = 0.06f) // 环境光斑

    // 路线图补充 token — 选中态胶囊背景 / 占位文字 / 标题色
    val BluePrimaryLight = Color(0xFFB3D8FF) // 浅蓝玻璃胶囊背景、选中态背景
    val BluePlaceholder = Color(0xFF1E8CFF).copy(alpha = 0.5f) // 搜索框占位文字（品牌蓝半透明）
    val HeadingDark = Color(0xFF102A56) // 深蓝黑色标题

    // 深色主题
    val DarkBackground = Color(0xFF1F1F1F)
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
}
