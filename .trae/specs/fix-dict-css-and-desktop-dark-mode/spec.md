# Fix Dictionary CSS and Desktop Dark Mode Spec

## Why
1. **词典自带 CSS 失效（牛津高阶等所有含 CSS 的词典）**：`MdxParser.transformHtmlStatic` 将 MDict 自定义元素（`<hw>`、`<inf>`、`<ex>` 等）转换为标准 HTML 标签 + class（`<b class='hw'>`、`<i class='inf'>`），导致词典 CSS 中的**元素选择器**（`hw { font-size:24px; }`）因目标元素已不存在而全部失效。上一版 spec 注入的 `BASE_CSS` 反而用 Gdict 默认样式掩盖了问题——词典作者定义的样式从未生效。
2. **桌面端深色模式不完整**：所有顶层容器硬编码 `Color.Transparent`，依赖 Mica 背景透出，但 AWT 窗口内容面板是不透明的，Mica 被完全遮挡。`AmbientBackground` 在深色模式下直接 `return this` 不绘制任何内容。侧栏背景也是硬编码透明，定义好的 `DarkSidebarBackground`/`DarkSidebarSelected` 颜色 token 从未被引用。

## What Changes
- **条件跳过标签转换**：当词典自带 CSS 非空时，跳过 `transformHtmlStatic`，让词典 CSS 直接样式化原始 MDict 元素（Chromium 支持渲染未知元素并应用元素选择器）。仅当词典无 CSS 时才执行转换 + 注入 `BASE_CSS` 作为兜底。
- **条件注入 BASE_CSS**：`DefaultRenderer` 分支仅在没有词典 CSS 时注入 `BASE_CSS`，避免默认样式覆盖词典自带 CSS。
- **扩展 `RE_CSS_URL` 支持 `.css` 扩展名**：使 `@import url("theme.css")` 等引用能被正确重写为 `mdxres://` 前缀。
- **深色模式容器背景**：顶层容器在深色模式下使用 `MaterialTheme.colorScheme.background`（即 `DarkBackground`）作为实色背景，不再依赖 Mica 透出。
- **侧栏深色背景**：`CollapsibleSidebar` 在深色模式下使用 `DarkSidebarBackground`，不再硬编码透明。
- **AmbientBackground 深色渐变**：深色模式下绘制深色渐变背景而非直接返回。

## Impact
- Affected specs: WebView 渲染、词典 CSS 加载、桌面主题
- Affected code:
  - `shared/shared-ui/src/main/kotlin/io/github/gdict/ui/webview/HtmlContentBuilder.kt`（条件转换 + 条件 BASE_CSS + RE_CSS_URL）
  - `shared/core/src/main/kotlin/io/github/gdict/core/MdxParser.kt`（`transformHtmlStatic` 调用点）
  - `desktop/app/src/main/kotlin/io/github/gdict/ui/DesktopApp.kt`（容器背景）
  - `desktop/app/src/main/kotlin/io/github/gdict/ui/components/CollapsibleSidebar.kt`（侧栏背景）
  - `desktop/app/src/main/kotlin/io/github/gdict/ui/components/AmbientBackground.kt`（深色渐变）

## MODIFIED Requirements
### Requirement: 词典 CSS 渲染
系统 SHALL 在词典自带 CSS 存在时保留原始 MDict 元素（`<hw>`、`<inf>`、`<ex>` 等），不做标签转换，使词典 CSS 的元素选择器能直接生效。仅当词典无 CSS 时才执行标签转换并注入 `BASE_CSS` 兜底。

#### Scenario: 牛津词典使用自带 CSS
- **WHEN** 用户查询含自带 CSS 的词典（如牛津高阶第7版双解）
- **THEN** 词典 HTML 中的 MDict 元素保持原样（`<hw>`、`<inf>` 等不被转换），词典 CSS 的元素选择器直接生效，词条显示词典作者定义的完整样式

#### Scenario: 无 CSS 词典使用兜底样式
- **WHEN** 用户查询不含 CSS 的词典
- **THEN** 执行 `transformHtmlStatic` 标签转换 + 注入 `BASE_CSS`，词条显示 Gdict 默认样式

### Requirement: 桌面深色模式背景
系统 SHALL 在深色模式下为顶层容器、侧栏、环境背景提供深色实色背景，不依赖 Mica 透出。

#### Scenario: 深色模式界面
- **WHEN** 用户开启深色模式
- **THEN** 侧栏使用 `DarkSidebarBackground`，主内容区使用 `DarkBackground`，环境背景绘制深色渐变，界面整体呈现深色主题

### Requirement: CSS @import 引用解析
系统 SHALL 将词典 CSS 中的 `@import url("*.css")` 引用重写为 `mdxres://` 前缀，使子 CSS 文件能通过 MDD 资源协议加载。

## REMOVED Requirements
### Requirement: 无条件标签转换
**Reason**: 无条件执行 `transformHtmlStatic` 会破坏词典 CSS 的元素选择器，导致词典自带样式失效。
**Migration**: 改为仅在没有词典 CSS 时执行转换；有 CSS 时保留原始元素让 Chromium 直接渲染。
