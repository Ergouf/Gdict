# 修复牛津词典 CSS 加载与桌面端深色模式（根本原因修复）

## 摘要

两个长期未修复的 bug 均已定位到真正的根本原因：

1. **CSS 失效**：牛津大词典（>10MB MDD）进入流式资源模式（`isResourceMode=true`）但 `wordCount` 保持 0。`DictionaryManager` 和 `DictSearchEngine` 两处都用 `wordCount > 0` 作为守卫，导致流式 MDD 被关闭丢弃，CSS 永远无法加载。`MdxParser` 内部的流式检索方法实现正确但从未被调用。

2. **深色模式不完整**：`Main.kt` 从未配置 Swing 窗口透明（`contentPane.isOpaque`、`window.background`、`rootPane.isOpaque`）。DWM Mica 已通过 `DwmSetWindowAttribute` 应用，但被不透明的 AWT 内容面板完全遮挡。之前的修复用不透明深色 Compose 层"覆盖"问题，而非修复 Swing 透明层。JCEF 面板还硬编码了浅灰色 `Color(0xF5, 0xF5, 0xF5)` 且从不更新。

---

## 当前状态分析

### CSS 加载流程（完整追踪）

1. 用户搜索 → `SearchViewModel.performSearch()` → `DictionaryRepository.searchWord()` → `DictionaryManager.searchWord()` → `DictSearchEngine.searchWord()`
2. `DictSearchEngine.searchWithParser()` 调用 `buildCss(parser, dictId, cssCache, loadedMdds, cssKeysCache)`
3. `buildCss()` 从两个来源获取 CSS：
   - 来源 A：`parser.companionCss`（MDX 同目录的 .css 文件，始终工作）
   - 来源 B：MDD 内部的 CSS（通过 `mddParser.findResourceKeys(".css")` + `readResourceBytesByKey()`）
4. **来源 B 被两道守卫阻断**：
   - `DictionaryManager.kt:201`：`if (mddParser.wordCount > 0)` → 流式 MDD 被关闭丢弃，从不存入 `loadedMdds`
   - `DictSearchEngine.kt:109`：`if (mddParser != null && mddParser.wordCount > 0)` → 即使 MDD 存在也跳过 CSS 构建

### 流式模式触发条件

`MdxParser.kt:736-739`：
```kotlin
if (wordCount == 0 && mdxFile.length() > 10 * 1024 * 1024) {
    log().w(TAG, "大文件MDD关键词区为空，启用资源流式模式")
    isResourceMode = true
}
```

`wordCount` 保持 0 的原因（`parseKeywordSection()` 早期返回）：
- 行 817-821：解压后关键词索引 > 200MB（内存保护）— 牛津 MDD 最可能命中此条件
- 行 801-805：`numKeyBlocks` 无效
- 行 811-815：`keyIndexCompLen` 无效

这是**设计内的降级路径**，不是解析 bug。`MdxParser` 的流式方法（`readResourceBytesStream`、`buildStreamKeywordIndex`、`findResourceKeysStream`）实现正确，能懒加载索引并按路径检索资源。

### 深色模式层级分析（Mica 透出条件）

| 层级 | 浅色模式 | 深色模式 | Mica 可见？ |
|------|---------|---------|------------|
| DWM Mica 背景 | 已应用 | 已应用（dark） | —（基础层） |
| **AWT JFrame/内容面板** | **不透明（默认）** | **不透明（默认）** | **完全遮挡（根本原因 #1）** |
| Compose 根容器 | `Color.Transparent` | `DarkBackground`(0xE6) | 浅色：是；深色：最多 10% 透出 |
| 侧栏 | `Transparent` | `DarkSidebarBackground`(0xFF) | 深色：完全遮挡 |
| 屏幕渐变 | 浅色渐变(不透明) | `DarkBackground→DarkSurfaceVariant` | 深色：大部分遮挡 |
| `acrylicAmbientBackground` | 浅色径向(低 alpha) | `0xFF1F1F1F→0xFF141414` | 深色：完全遮挡 |
| **JCEF Swing 面板** | `Color(0xF5,0xF5,0xF5)` | `Color(0xF5,0xF5,0xF5)`（从不更新） | **始终遮挡；显示浅灰（根本原因 #2）** |

**结论**：即使修复 Swing 透明，深色 Compose 层的不透明颜色仍会遮挡 Mica。需同时修复三个根本原因。

### 之前修复失败的原因

1. **CSS**：之前修复只处理了 HTML 注入顺序和路由条件，从未触及 `DictionaryManager`/`DictSearchEngine` 的 `wordCount > 0` 守卫。`css` 参数始终为空，所有 `css.isNotEmpty()` 条件分支从未触发。
2. **深色模式**：之前修复正确诊断了"AWT 内容面板不透明，Mica 被遮挡"，但补救措施是用不透明深色 Compose 层覆盖，而非让 AWT 透明。JCEF 面板的硬编码浅灰从未处理。

---

## 提议变更

### 变更 1：允许流式 MDD 被加载

**文件**：`d:\workspace\Gdict\shared\core\src\main\kotlin\io\github\gdict\core\DictionaryManager.kt`
**行**：201
**原因**：流式模式 MDD 的 `wordCount=0` 但 `isResourceMode=true`，应被接受而非丢弃。
**当前代码**：
```kotlin
if (mddParser.wordCount > 0) {
```
**修改为**：
```kotlin
if (mddParser.wordCount > 0 || mddParser.isResourceMode) {
```

### 变更 2：允许流式 MDD 贡献 CSS

**文件**：`d:\workspace\Gdict\shared\core\src\main\kotlin\io\github\gdict\core\DictSearchEngine.kt`
**行**：109
**原因**：第二道守卫同样用 `wordCount > 0` 跳过 CSS 构建。即使变更 1 让 MDD 被加载，此守卫仍会阻止 CSS 检索。
**当前代码**：
```kotlin
if (mddParser != null && mddParser.wordCount > 0) {
```
**修改为**：
```kotlin
if (mddParser != null && (mddParser.wordCount > 0 || mddParser.isResourceMode)) {
```

### 变更 3：始终注入 BASE_CSS（DefaultRenderer 分支）

**文件**：`d:\workspace\Gdict\shared\shared-ui\src\main\kotlin\io\github\gdict\ui\webview\HtmlContentBuilder.kt`
**行**：164
**原因**：当词典 CSS 非空时，BASE_CSS 被丢弃。但 BASE_CSS 提供了 CSS 变量定义（`--bg`、`--text`、`--header` 等）和 MDict 标签默认样式。词典 CSS 可能引用这些变量。注入顺序必须遵循项目规则：BASE_CSS → 词典CSS → 透明背景。
**当前代码**：
```kotlin
${if (css.isEmpty()) BASE_CSS else ""}
```
**修改为**：
```kotlin
$BASE_CSS
```

### 变更 4：配置 Swing 窗口透明（深色模式根本修复）

**文件**：`d:\workspace\Gdict\desktop\app\src\main\kotlin\io\github\gdict\Main.kt`
**行**：158 之后（`window.minimumSize = Dimension(800, 600)` 之后，`LaunchedEffect(Unit)` 之前）
**原因**：DWM Mica 已通过 `DwmSetWindowAttribute` 应用，但 AWT `JFrame` 的内容面板默认不透明，绘制一个不透明灰色矩形覆盖 Mica。必须将 Swing 窗口设为透明，Mica 才能透出。
**新增代码**：
```kotlin
// 使 Swing 窗口透明，让 DWM Mica 背景透出
window.contentPane.isOpaque = false
window.rootPane.isOpaque = false
window.background = java.awt.Color(0, 0, 0, 0)
```

### 变更 5：JCEF 面板支持深色模式

**文件**：`d:\workspace\Gdict\desktop\app\src\main\kotlin\io\github\gdict\ui\webview\MdxWebView.kt`
**位置**：`GlobalBrowserManager` object 内（约行 397-545）和 `MdxWebView` composable 的 `LaunchedEffect(darkMode)`（约行 687）
**原因**：JCEF 浏览器面板的 AWT 背景硬编码为 `Color(0xF5, 0xF5, 0xF5)`（浅灰），在 `getOrCreatePanel()` 时设置一次且从不更新。`HtmlContentBuilder` 强制 `body { background: transparent !important }`，所以浅灰 AWT 背景会透过 HTML 透明区域显示。

**修改 5a**：在 `GlobalBrowserManager` 中新增 `updatePanelTheme(dark: Boolean)` 方法：
```kotlin
fun updatePanelTheme(dark: Boolean) {
    val bgColor = if (dark) java.awt.Color(0x1F, 0x1F, 0x1F) else java.awt.Color(0xF5, 0xF5, 0xF5)
    SwingUtilities.invokeLater {
        cachedPanel?.background = bgColor
        cachedErrorPanel?.background = bgColor
        cachedBrowserComponent?.background = bgColor
    }
}
```

需要将 `errorPanel`、`p`（主面板）、`browserComponent` 的引用缓存到 `GlobalBrowserManager` 的字段中（当前它们是 `getOrCreatePanel()` 内的局部变量）。

**修改 5b**：在 `MdxWebView` composable 的 `LaunchedEffect(darkMode)` 中调用：
```kotlin
LaunchedEffect(darkMode) {
    GlobalBrowserManager.setThemeInBrowser(darkMode)
    GlobalBrowserManager.updatePanelTheme(darkMode)
}
```

### 变更 6：降低深色 Compose 层 alpha 让 Mica 透出

**文件**：`d:\workspace\Gdict\desktop\app\src\main\kotlin\io\github\gdict\ui\theme\GdictColors.kt`
**行**：73
**原因**：`DarkBackground` 当前是 `0xE6`（90% 不透明），只有 10% Mica 能透出。降低到 `0xCC`（80% 不透明）让 Mica 更明显。`DarkSidebarBackground` 保持 `0xFF`（侧栏采用 Fluent NavigationView 实心风格）。
**当前代码**：
```kotlin
val DarkBackground = Color(0xE61F1F1F)
```
**修改为**：
```kotlin
val DarkBackground = Color(0xCC1F1F1F)
```

### 变更 7：降低 AmbientBackground 深色渐变 alpha

**文件**：`d:\workspace\Gdict\desktop\app\src\main\kotlin\io\github\gdict\ui\components\AmbientBackground.kt`
**行**：22-29
**原因**：深色渐变当前完全不透明（0xFF），完全遮挡 Mica。降低到 0xCC 让 Mica 透出。
**当前代码**：
```kotlin
if (darkMode) {
    return this.background(
        Brush.verticalGradient(
            0.0f to Color(0xFF1F1F1F),
            1.0f to Color(0xFF141414)
        )
    )
}
```
**修改为**：
```kotlin
if (darkMode) {
    return this.background(
        Brush.verticalGradient(
            0.0f to Color(0xCC1F1F1F),
            1.0f to Color(0xCC141414)
        )
    )
}
```

---

## 假设与决策

### 假设
1. 牛津 MDD 确实进入流式模式（`isResourceMode=true`，`wordCount=0`）— 基于大文件（>10MB）和日志 "大文件MDD关键词区为空，启用资源流式模式"
2. `MdxParser` 的流式检索方法实现正确 — 已通过代码审查确认
3. 用户希望 Mica 在深色模式下可见（而非纯实心深色）— 基于用户反馈"mica没有深色"
4. JCEF 面板的浅灰背景是单词详情页深色模式不完整的主要原因

### 决策
1. **保留前序 HtmlContentBuilder 修复**：条件跳过 `transformHtmlStatic`、条件 BASE_CSS（变更 3 会将其改为始终注入）、扩展的 `RE_CSS_URL` — 这些在 CSS 实际加载后会生效
2. **不修改 `MdxParser.kt`**：流式方法实现正确，无需改动
3. **不修改 `WindowsBackdrop.kt`**：DWM 属性设置正确，问题在 Swing 层未配置透明
4. **侧栏保持不透明深色**：采用 Fluent NavigationView 风格，侧栏是实心面板，不需要 Mica 透出
5. **`DarkBackground` 降到 0xCC**：让 Mica 明显透出但保持可读性。如果太透可回调到 0xE6
6. **`AmbientBackground` 降到 0xCC**：与 `DarkBackground` 保持一致
7. **不添加 DWMWA_USE_IMMERSIVE_DARK_MODE 属性 19 回退**：Windows 10 < 2004 已停止支持，且用户使用的是 Windows 11

---

## 验证步骤

### CSS 修复验证
1. 编译 `shared/core` 模块：`./gradlew.bat :shared:core:compileKotlin`
2. 运行现有单元测试：`./gradlew.bat :shared:core:testDebugUnitTest`
3. 启动桌面应用，导入牛津词典，搜索单词，确认：
   - 词典内置 CSS 样式生效（字体、颜色、布局等）
   - BASE_CSS 默认样式存在（链接颜色、表格边框等）
   - 透明背景生效（Mica 或深色背景透出）
4. 检查日志：应看到 "MDD loaded" 而非 "MDD '...' has wordCount=0, treating as empty"

### 深色模式修复验证
1. 编译桌面应用：`./gradlew.bat :app:packageAppImage`
2. 启动应用，切换深色模式，确认：
   - 标题栏深色（DWMWA_USE_IMMERSIVE_DARK_MODE 生效）
   - Mica 背景可见（深色云纹效果透出）
   - 侧栏深色（实心深色面板）
   - 内容区深色且有 Mica 透出
   - 单词详情页（JCEF）深色，无浅灰背景透出
3. 切换回浅色模式，确认无回归：
   - Mica 浅色背景可见
   - 所有元素浅色

### 回归测试
1. Cambridge EPD 词典路由无回归（仍走 `CambridgeEpdRenderer`）
2. Collins 词典路由无回归
3. 其他词典 CSS 加载无回归
4. Android 端编译成功（变更 1-3 在 shared 模块，影响 Android）

---

## 关键文件路径汇总

| 变更 | 文件 | 行号 | 类型 |
|------|------|------|------|
| 1 | `shared\core\src\main\kotlin\io\github\gdict\core\DictionaryManager.kt` | 201 | CSS 修复 |
| 2 | `shared\core\src\main\kotlin\io\github\gdict\core\DictSearchEngine.kt` | 109 | CSS 修复 |
| 3 | `shared\shared-ui\src\main\kotlin\io\github\gdict\ui\webview\HtmlContentBuilder.kt` | 164 | CSS 修复 |
| 4 | `desktop\app\src\main\kotlin\io\github\gdict\Main.kt` | 158 后 | 深色模式修复 |
| 5 | `desktop\app\src\main\kotlin\io\github\gdict\ui\webview\MdxWebView.kt` | 397-545, 687 | 深色模式修复 |
| 6 | `desktop\app\src\main\kotlin\io\github\gdict\ui\theme\GdictColors.kt` | 73 | 深色模式修复 |
| 7 | `desktop\app\src\main\kotlin\io\github\gdict\ui\components\AmbientBackground.kt` | 22-29 | 深色模式修复 |
