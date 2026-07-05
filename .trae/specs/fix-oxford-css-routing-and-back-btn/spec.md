# Fix Oxford CSS Routing and Desktop Back Button Spec

## Why
1. **牛津高阶英语词典（第7版双解版）CSS 失效**：上一版 spec (`fix-dict-css-and-back-button`) 只调整了透明背景的注入顺序，未触及真正的根因——`isPronunciationDict` 路由判定过于宽松，将牛津词条误判为"发音词典"并路由到 `PronunciationDetailContent`，在那里回退到 `MdxWebView` + `DefaultRenderer` 渲染管线，而该分支不注入 `BASE_CSS`，导致 MDict 自定义标签（`<hw>`、`<inf>`、`<ex>` 等）被转换成 `<b class='hw'>` 后无默认样式，词典自带 CSS 也可能因使用元素选择器而失效。
2. **Windows 桌面返回按钮点击无效**：桌面端 `GlassCircleButton`（`CollinsDetailScreen.kt:710-728`）接受 `onClick` 参数但从未在 `Box` 修饰符链中调用 `.clickable(onClick = onClick)`，导致按钮完全无法响应点击。Android 版本（`PronunciationDetailScreen.kt:380-399`）正确包含 `.clickable()`，证明这是桌面端独有遗漏。

## What Changes
- **修复桌面 `GlassCircleButton`**：在 `Box` 修饰符链末尾添加 `.clickable(onClick = onClick)`，与 Android 实现对齐。
- **收紧 `isPronunciationDict` 路由判定**：仅当检测到 Cambridge EPD 专属信号（`cepd18.css`）或多个 Cambridge 专属模式（如 `uk_sound.png` / `us_sound.png` 与 `<prongrp>` 同时出现）时才路由到发音词典，避免因通用 MDict 标签（`<arl>`、`<soundfile>`）误判。
- **为 `DefaultRenderer` 分支注入 `BASE_CSS`**：在 `HtmlContentBuilder.build` 的 `isDefaultRenderer` 分支中加入 `BASE_CSS`（或其精简子集），确保即使词典自带 CSS 仅用元素选择器，转换后的标签也能获得默认样式。

## Impact
- Affected specs: WebView 渲染、词典路由、桌面导航
- Affected code:
  - `desktop/app/src/main/kotlin/io/github/gdict/ui/screens/CollinsDetailScreen.kt`（`GlassCircleButton`）
  - `desktop/app/src/main/kotlin/io/github/gdict/ui/screens/WordDetailScreen.kt`（`isPronunciationDict`，第 137-140 行）
  - `android/app/src/main/java/io/github/gdict/ui/screens/WordDetailScreen.kt`（`isPronunciationDict`，第 86-89 行）
  - `shared/shared-ui/src/main/kotlin/io/github/gdict/ui/webview/HtmlContentBuilder.kt`（`DefaultRenderer` 分支，第 148-170 行）

## MODIFIED Requirements
### Requirement: 词典路由判定
系统 SHALL 仅在词条确认为 Cambridge EPD 发音词典内容（包含 `cepd18.css` 或多个 Cambridge 专属信号）时路由到 `PronunciationDetailContent`。包含通用 MDict 标签（`<arl>`、`<soundfile>`、`<prongrp>`）但不包含 Cambridge 专属信号的词典（如牛津高阶）SHALL 走默认 `WordDetailContent` 渲染路径。

### Requirement: 桌面 GlassCircleButton 点击行为
桌面端 `GlassCircleButton` SHALL 通过 `.clickable(onClick = onClick)` 将点击事件绑定到 `Box` 修饰符，确保返回/分享按钮可被点击。

### Requirement: DefaultRenderer 基础样式
`DefaultRenderer` 分支 SHALL 在词典 CSS 之前注入 `BASE_CSS`（或精简子集），为转换后的 MDict 标签提供默认样式，避免在词典 CSS 使用元素选择器或缺失规则时出现无样式文本。

## ADDED Requirements
### Requirement: 跨平台 GlassCircleButton 一致性
桌面端 `GlassCircleButton` 的修饰符链 SHALL 与 Android 端保持功能对齐，必须包含 `.clickable(onClick = onClick)`。

#### Scenario: 桌面返回按钮点击
- **WHEN** 用户在 Windows 桌面端 `PronunciationDetailScreen` 或 `CollinsDetailScreen` 点击返回按钮
- **THEN** `onBack` 回调被触发，导航返回上一屏

#### Scenario: 牛津词典正常渲染
- **WHEN** 用户查询 `[英-汉]牛津高阶英语词典（第7版双解版）` 中的词条
- **THEN** 词条走默认 `WordDetailContent` 路径，词典自带 CSS 被正确加载，`BASE_CSS` 提供默认样式兜底，词条显示完整的字体、颜色与排版

#### Scenario: Cambridge EPD 仍走发音词典路径
- **WHEN** 词条包含 `cepd18.css` 或多个 Cambridge 专属信号（`uk_sound.png`/`us_sound.png` 配合 `<prongrp>`）
- **THEN** 仍路由到 `PronunciationDetailContent`，原有发音解析逻辑不受影响
