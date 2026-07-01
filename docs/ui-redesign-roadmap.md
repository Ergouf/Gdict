# Gdict UI 替换路线图

> 目标：将 Android 端从「Fluent 绿 + 灰阶」切换为「Fluent Design 2 + 浅蓝主题 + Acrylic Glass」，与 Logo 品牌蓝 `#1E8CFF` 保持一致，打造轻盈、通透、现代的 Windows 11 风格视觉体验。

---

## 一、设计总览

- **设计语言**：Microsoft Fluent Design 2 / Windows 11
- **风格关键词**：Acrylic、Glassmorphism、Floating Card、Ambient Light、Soft Shadow、Blue Gradient、Minimal、Premium、Clean、Translucent
- **品牌主色**：`#1E8CFF`
- **全局要求**：
  - 去除所有高饱和绿色
  - 当前选中态统一改为蓝色
  - 背景使用蓝白渐变 + 柔和光斑
  - 卡片、搜索框、底部导航栏统一 Acrylic 毛玻璃材质
  - 圆角统一、轻阴影、避免厚重投影

---

## 二、颜色 Token

在 `android/app/src/main/java/io/github/gdict/ui/theme/Color.kt` 中新增以下蓝主题 token：

| Token | 值 | 用途 |
|---|---|---|
| `BluePrimary` | `#1E8CFF` | 品牌主色：图标、选中文字、强调 |
| `BluePrimarySoft` | `#4DA3FF` | hover/pressed 反馈色 |
| `BluePrimaryLight` | `#B3D8FF` | 浅蓝玻璃胶囊背景、选中态背景 |
| `BlueSurfaceGlass` | `Color.White.copy(alpha = 0.72f)` | Acrylic 卡片/导航栏填充 |
| `BlueSurfaceGlassDark` | `Color(0xCC1A2A3A)` | 深色模式 Acrylic 填充 |
| `BlueHighlightBorder` | `Color.White.copy(alpha = 0.60f)` | 1px 高光白边 |
| `BlueCardBorder` | `Color(0x331E8CFF)` | 浅蓝微描边（可选） |
| `BlueBackgroundTop` | `#F5FAFF` | 页面渐变顶部 |
| `BlueBackgroundBottom` | `#FFFFFF` | 页面渐变底部 |
| `AmbientLight` | `Color(0xFF1E8CFF).copy(alpha = 0.08f)` | 环境光斑 |

> 注意：先新增蓝主题 token，不直接删除原绿色 token，避免其他页面立即崩溃。

---

## 三、通用组件建议

为避免各页面重复实现 Acrylic 样式，建议新增两个可复用组件：

### 3.1 `AcrylicCard`

```kotlin
@Composable
fun AcrylicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .border(1.dp, BlueHighlightBorder, shape)
            .background(BlueSurfaceGlass)
    ) {
        content()
    }
}
```

### 3.2 `AcrylicCapsule`

```kotlin
@Composable
fun AcrylicCapsule(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, BlueHighlightBorder, RoundedCornerShape(28.dp))
            .background(BlueSurfaceGlass)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
```

### 3.3 模糊降级策略

- Android 12+（API 31）：使用 `Modifier.blur(radius, BlurredEdgeTreatment.Unbounded)`
- Android 12 以下：仅使用半透明背景，不调用 blur，保证兼容

---

## 四、搜索页设计规范

### 4.1 页面背景

- 蓝白垂直渐变：`#F5FAFF` → `#FFFFFF`
- 叠加 2~3 个径向环境光斑
- 光斑位置：左上、右下分散
- 光斑尺寸：200dp–300dp

### 4.2 标题

- 文字：「搜索」
- 字号：`headlineSmall` / 24sp
- 字重：`FontWeight.Bold`
- 颜色：`OnBackground`
- 边距：水平 20dp，顶部 `statusBarsPadding + 16dp`

### 4.3 搜索框

- 高度：52dp
- 圆角：28dp（胶囊形）
- 背景：Acrylic 毛玻璃（`BlueSurfaceGlass`）
- 背景模糊：8dp
- 1px 高光白边
- 轻阴影：2dp
- 搜索图标颜色：`BluePrimary`
- 占位文字：`BluePrimary.copy(alpha = 0.5f)`

### 4.4 历史记录

- 标题「最近搜索」：深色、Bold
- 历史图标 `History`：改为 `BluePrimary`
- 历史项：Acrylic 胶囊行，28dp 圆角，轻阴影
- 行背景 hover/press：`BluePrimaryLight.copy(alpha = 0.3f)`

### 4.5 每日一词卡片

- 尺寸：180dp × 110dp
- 圆角：20dp
- 背景：Acrylic 毛玻璃
- 1px 高光白边 + 2dp 轻阴影
- 标题颜色：`BluePrimary`
- 词典名：`subtitleColor`

### 4.6 搜索结果卡片

- 圆角：16dp（不再随缩放超过 16dp）
- 标题颜色：`BluePrimary`
- 背景：Acrylic 毛玻璃
- 拖拽阴影降低为 4dp

### 4.7 空结果/建议词

- 建议词改为蓝色胶囊标签
- Acrylic 背景 + 蓝色文字

### 4.8 错误提示卡

- 保持错误语义，但改为 Acrylic 材质
- 圆角 16dp 或 20dp

---

## 五、收藏页（我的词汇）设计规范

### 5.1 页面背景

与搜索页一致：
- 蓝白垂直渐变
- 叠加环境光斑

### 5.2 标题

- 文字：「我的词汇」
- 字号：28sp / `headlineMedium`
- 字重：`FontWeight.Bold`
- 对齐：左对齐
- 边距：水平 24dp，顶部 `statusBarsPadding + 24dp`

### 5.3 收藏列表卡片

- 材质：Floating Acrylic Card
- 背景：`BlueSurfaceGlass`
- 背景模糊：18–24dp
- 1px 白色高光描边
- 阴影：4dp 柔和阴影
- 圆角：24dp
- 内边距：24dp
- 卡片间距：16dp

#### 卡片内部布局

| 区域 | 规范 |
|---|---|
| 左侧图标容器 | 圆形，56dp，浅蓝玻璃背景 `#1E8CFF` 12% alpha，蓝色 Bookmark 图标 |
| 中间文字 | 单词名称：Bold、大字号、深色；词典名：浅灰、单行省略 |
| 右侧删除按钮 | 圆形 Icon Button，默认透明深灰图标，点击浅蓝 ripple + 玻璃高亮 |

### 5.4 闪卡入口功能卡片

- 宽度：填满（水平 24dp padding）
- 高度：约 120dp
- 圆角：24dp
- 材质：与收藏卡片一致 Acrylic
- 左侧：蓝色书签图标 + 圆形玻璃容器
- 中间标题：「闪卡」，Bold，深色
- 描述：浅灰英文 "To practice and learn your word lists."
- 右侧：Chevron Right 箭头

### 5.5 删除确认对话框

- 圆角：24dp
- 背景：Acrylic 毛玻璃
- 按钮颜色：蓝色品牌色
- 取消默认实色背景

---

## 六、底部导航栏（全局）

### 6.1 形态

- Floating Navigation Bar，悬浮于页面底部
- 不贴边：水平边距 24dp，底部边距 20dp
- 圆角：32dp
- 高度：72dp

### 6.2 材质

- 背景：`BlueSurfaceGlass`
- 背景模糊：18dp
- 1px 高光白边
- 柔和阴影：6dp

### 6.3 选中态

- 图标颜色：`BluePrimary`
- 文字颜色：`BluePrimary`
- 背景：浅蓝色玻璃胶囊 `RoundedCornerShape(20dp)`
- 胶囊背景：`BluePrimaryLight.copy(alpha = 0.35f)`
- 外发光：可选蓝色 glow shadow

### 6.4 未选中态

- 图标：深灰色线性图标
- 文字：深灰色
- 无背景胶囊

### 6.5 Tab 顺序

1. 搜索
2. 收藏
3. 学习
4. 我的

---

## 七、完整实施路线图

| 阶段 | 模块 | 核心任务 | 影响文件 |
|---|---|---|---|
| 1 | 色板基建 | 新增蓝主题 token，保留旧绿 token | `Color.kt` |
| 2 | 通用组件 | 新建 `AcrylicCard`、`AcrylicCapsule` | 新增组件文件 |
| 3 | 搜索页背景 | 蓝白渐变 + 环境光斑 | `SearchScreen.kt` |
| 4 | 搜索框 | 搜索框 Acrylic 化 + 蓝色图标 | `SearchScreen.kt` |
| 5 | 搜索页历史 | 历史项 Acrylic 化 + 蓝色历史图标 | `SearchScreen.kt` |
| 6 | 搜索页每日一词 | 卡片 Acrylic 化 + 蓝色标题 | `SearchScreen.kt` |
| 7 | 搜索结果/空态 | 结果卡片蓝色标题 + Acrylic；建议词蓝色胶囊 | `SearchScreen.kt` |
| 8 | 全局底部导航 | Floating Acrylic Navigation Bar + 蓝色选中态 | `MainActivity.kt` |
| 9 | 收藏页背景 | 蓝白渐变 + 环境光斑 | `BookmarksScreen.kt` |
| 10 | 收藏页标题 | 大字号 Bold 标题 | `BookmarksScreen.kt` |
| 11 | 收藏卡片 | Floating Acrylic Card + 蓝色书签 + 删除按钮 Fluent 化 | `BookmarksScreen.kt` |
| 12 | 闪卡入口 | 大 Acrylic 功能卡片 + Chevron | `BookmarksScreen.kt` |
| 13 | 删除对话框 | Acrylic 圆角对话框 + 蓝色按钮 | `BookmarksScreen.kt` |
| 14 | 学习/我的页 | 按同样蓝主题 + Acrylic 规范改造 | `FlashcardScreen.kt`, `SettingsScreen.kt` 等 |
| 15 | 验证发布 | CI 编译 + 真机截图验证 + release tag | CI / GitHub |

---

## 八、技术约束

1. **背景模糊兼容性**：`Modifier.blur()` 需要 API 31+，低版本需降级为纯半透明。
2. **性能**：避免在同一屏叠加过多模糊层，环境光斑使用简单 radial gradient。
3. **深色模式**：同步定义 `BlueSurfaceGlassDark` 等深色 token。
4. **逐步替换**：先聚焦搜索页和收藏页，不一次性全项目去绿，避免风格撕裂。
5. **截图测试**：当前项目有 screenshot tests，改造后需同步更新快照图片。

---

## 九、下一步决策

1. 是否确认以上搜索页 + 收藏页设计规范？
2. 是否现在开始执行 **阶段 1（色板基建）+ 阶段 2（通用 Acrylic 组件）**？
3. 底部导航栏是否统一为 Floating Navigation Bar（悬浮胶囊）？
4. 深色模式是否同步适配蓝主题 Acrylic？
