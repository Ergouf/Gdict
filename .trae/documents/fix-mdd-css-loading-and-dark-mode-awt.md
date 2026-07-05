# Fix MDD CSS Loading and Dark Mode AWT Backgrounds

## Summary

两个前序修复均失败，因为未触及真正根因：
1. **词典 CSS 失效**：牛津 MDD 文件较大（>10MB）时进入"流式资源模式"（`isResourceMode = true`），但 `wordCount` 保持为 0。`DictionaryManager` 和 `DictSearchEngine` 均以 `wordCount > 0` 作为 guard 拒绝/跳过该 MDD，导致 CSS 永远不会从 MDD 中加载。`css` 参数对牛津始终为空字符串，之前所有 `HtmlContentBuilder` 的条件分支修改都不起作用。
2. **深色模式不完整**：Compose 层的修复（容器背景、侧栏、AmbientBackground）已正确应用，但 AWT/Swing 层从未处理——JCEF 浏览器面板硬编码浅灰背景（`0xF5,0xF5,0xF5`），Swing JFrame 内容面板不透明且为默认浅色。WordDetailScreen 中嵌入的 JCEF 面板直接显示浅灰，覆盖了 Compose 深色层。

## Current State Analysis

### CSS 管线（已验证）

| 步骤 | 文件 | 行号 | 当前行为 | 问题 |
|------|------|------|----------|------|
| MDD 解析 | `MdxParser.kt` | 736-738 | 大文件 MDD `wordCount=0` 时设 `isResourceMode=true` | `wordCount` 未更新 |
| MDD 加载 | `DictionaryManager.kt` | 201 | `if (mddParser.wordCount > 0)` | 拒绝 `wordCount=0` 的 MDD |
| CSS 构建 | `DictSearchEngine.kt` | 109 | `if (mddParser != null && mddParser.wordCount > 0)` | 二次 guard 也跳过 |
| HTML 构建 | `HtmlContentBuilder.kt` | 117,164 | 条件跳过 transform / 条件 BASE_CSS | 因 `css=""` 全部走空分支 |

MdxParser 内部的流式方法（`readResourceBytesStream`、`findResourceKeysStream`、`buildStreamKeywordIndex`）已正确实现并能正常工作——只要 MDD 不被拒绝。

### 深色模式管线（已验证）

| 层级 | 文件 | 行号 | 当前状态 |
|------|------|------|----------|
| Compose 容器 | `DesktopApp.kt` | 131,136,166,211 | ✅ 已修复：`if (darkMode) MaterialTheme.colorScheme.background` |
| Compose 侧栏 | `CollapsibleSidebar.kt` | 82 | ✅ 已修复：`if (darkMode) DarkSidebarBackground` |
| Compose 环境 | `AmbientBackground.kt` | 22-29 | ✅ 已修复：深色渐变 |
| AWT JCEF 面板 | `MdxWebView.kt` | 416,528,530 | ❌ 硬编码 `Color(0xF5,0xF5,0xF5)` |
| Swing JFrame | `Main.kt` | 151-157 | ❌ 未设 `isOpaque=false`/深色背景 |
| DarkBackground alpha | `GdictColors.kt` | 73 | ⚠️ `0xE6`（90%不透明），10%透出浅色 Swing |

## Proposed Changes

### Change 1: 修复 MDD 流式模式被拒绝（CSS 根因）

**文件**: `d:\workspace\Gdict\shared\core\src\main\kotlin\io\github\gdict\core\DictionaryManager.kt`

**行号**: 201

**当前**:
```kotlin
if (mddParser.wordCount > 0) {
```

**改为**:
```kotlin
if (mddParser.wordCount > 0 || mddParser.isResourceMode) {
```

**原因**: 流式模式下 `wordCount` 为 0 但 MDD 仍可正常提供资源（`findResourceKeysStream` / `readResourceBytesStream` 可用）。

### Change 2: 修复 CSS 构建跳过流式 MDD（CSS 根因）

**文件**: `d:\workspace\Gdict\shared\core\src\main\kotlin\io\github\gdict\core\DictSearchEngine.kt`

**行号**: 109

**当前**:
```kotlin
if (mddParser != null && mddParser.wordCount > 0) {
```

**改为**:
```kotlin
if (mddParser != null && (mddParser.wordCount > 0 || mddParser.isResourceMode)) {
```

**原因**: 双重 guard，即使 DictionaryManager 加载了 MDD，这里也会因 `wordCount=0` 跳过 CSS 加载。

### Change 3: JCEF 面板背景适配深色模式

**文件**: `d:\workspace\Gdict\desktop\app\src\main\kotlin\io\github\gdict\ui\webview\MdxWebView.kt`

**改动点 1** — 添加 `updatePanelTheme` 方法到 `GlobalBrowserManager`（在 `setThemeInBrowser` 方法后面，约 623 行）:

```kotlin
fun updatePanelTheme(dark: Boolean) {
    synchronized(lock) {
        val bgColor = if (dark) java.awt.Color(0x1F, 0x1F, 0x1F) else java.awt.Color(0xF5, 0xF5, 0xF5)
        panel?.background = bgColor
        browser?.uiComponent?.background = bgColor
    }
}
```

**改动点 2** — 在 `MdxWebView` Composable 的 `LaunchedEffect(darkMode)` 中调用（约 687 行）:

当前:
```kotlin
LaunchedEffect(darkMode) {
    if (!GlobalBrowserManager.isBrowserReady()) {
        while (!GlobalBrowserManager.isBrowserReady()) {
            delay(100)
        }
    }
    GlobalBrowserManager.setThemeInBrowser(darkMode)
}
```

改为:
```kotlin
LaunchedEffect(darkMode) {
    GlobalBrowserManager.updatePanelTheme(darkMode)
    if (!GlobalBrowserManager.isBrowserReady()) {
        while (!GlobalBrowserManager.isBrowserReady()) {
            delay(100)
        }
    }
    GlobalBrowserManager.setThemeInBrowser(darkMode)
}
```

**改动点 3** — 在面板创建时使用默认浅色（保持不变），但将三处硬编码 `java.awt.Color(0xF5, 0xF5, 0xF5)` 保留（它们是初始创建时的默认值，`updatePanelTheme` 会在运行时更新）。

### Change 4: Swing 窗口深色背景

**文件**: `d:\workspace\Gdict\desktop\app\src\main\kotlin\io\github\gdict\Main.kt`

**行号**: 207-209（现有 `LaunchedEffect(darkMode)`）

当前:
```kotlin
LaunchedEffect(darkMode) {
    WindowsBackdrop.applyMica(window, darkMode = darkMode)
}
```

改为:
```kotlin
LaunchedEffect(darkMode) {
    // 设置 Swing 窗口背景为深色，避免不透明的内容面板透出浅色
    window.contentPane.isOpaque = false
    window.background = if (darkMode) java.awt.Color(0x1F, 0x1F, 0x1F) else java.awt.Color(0xF5, 0xFA, 0xFF)
    WindowsBackdrop.applyMica(window, darkMode = darkMode)
}
```

**原因**: Swing JFrame 默认不透明，内容面板为浅色。设置 `isOpaque = false` 让 Mica 可透出；同时设置深色背景作为兜底。

### Change 5: DarkBackground 改为完全不透明

**文件**: `d:\workspace\Gdict\desktop\app\src\main\kotlin\io\github\gdict\ui\theme\GdictColors.kt`

**行号**: 73

当前:
```kotlin
val DarkBackground = Color(0xE61F1F1F) // 桌面端 90% 不透明，透出深色 Mica
```

改为:
```kotlin
val DarkBackground = Color(0xFF1F1F1F) // 桌面端完全不透明
```

**原因**: Mica 无法可靠透出（AWT 窗口不透明），90% 不透明会让 10% 浅色 Swing 背景透出。改为完全不透明确保深色模式纯实色。

## Assumptions & Decisions

1. **保留前序 HtmlContentBuilder 修复**：条件跳过 `transformHtmlStatic` 和条件 `BASE_CSS` 注入是正确的——MDD 修复后 `css` 非空，这些逻辑将正确激活。
2. **不修改 MdxParser 内部流式方法**：`readResourceBytesStream`、`findResourceKeysStream`、`buildStreamKeywordIndex` 已正确实现，无需改动。
3. **JCEF 面板背景在创建时保持浅色默认值**：`updatePanelTheme` 会在 Compose 首次 recompose 时立即更新为正确主题。
4. **亮色模式 Swing 背景使用 `0xF5,0xFA,0xFF`**（与 `Background` token 一致），而非纯白。
5. **`isResourceMode` 是 public var**（MdxParser.kt:59），可直接从外部访问。

## Verification Steps

1. **编译验证**: `.\gradlew.bat :app:compileKotlin`（desktop）+ Android `:app:compileDebugKotlin`
2. **MDD 加载验证**: 检查日志确认牛津 MDD 不再被拒绝（搜索 `wordCount=0, treating as empty` 不应出现）
3. **CSS 加载验证**: 查询牛津词条，确认 `css` 参数非空，词条显示词典自带样式
4. **深色模式验证**: 切换深色模式，确认侧栏、主内容区、JCEF 面板区域均为深色
5. **亮色模式回归**: 确认亮色模式不受影响
