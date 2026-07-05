# Tasks
- [x] Task 1: 条件跳过 `transformHtmlStatic` + 条件注入 `BASE_CSS`
  - [x] SubTask 1.1: 修改 `HtmlContentBuilder.build()`，当 `css` 非空时跳过 `MdxParser.transformHtmlStatic`（保留原始 MDict 元素），仅当 `css` 为空时执行转换
  - [x] SubTask 1.2: 修改 `DefaultRenderer` 分支，仅当 `css` 为空时注入 `BASE_CSS`，有词典 CSS 时不注入（避免默认样式覆盖词典 CSS）
  - [x] SubTask 1.3: 验证 CSS 注入顺序：无 CSS 时为 `BASE_CSS`（class 选择器匹配转换后标签）；有 CSS 时仅词典 CSS（元素选择器匹配原始元素）
- [x] Task 2: 扩展 `RE_CSS_URL` 支持 `.css` 扩展名
  - [x] SubTask 2.1: 修改 `HtmlContentBuilder.kt` 的 `RE_CSS_URL` 正则，在扩展名列表中添加 `.css`，使 `@import url("theme.css")` 能被重写为 `mdxres://` 前缀
  - [x] SubTask 2.2: 验证含 `@import` 的词典 CSS 能正确加载子 CSS 文件
- [x] Task 3: 修复桌面深色模式容器背景
  - [x] SubTask 3.1: 修改 `DesktopApp.kt` 的顶层容器（`Column`/`Row`/`Box`，约第 130、135、164、209 行），在深色模式下使用 `MaterialTheme.colorScheme.background` 替代 `Color.Transparent`
  - [x] SubTask 3.2: 修改 `CollapsibleSidebar.kt`（约第 81 行），在深色模式下使用 `GdictColors.DarkSidebarBackground` 替代 `Color.Transparent`
  - [x] SubTask 3.3: 修改 `AmbientBackground.kt`（约第 22 行），深色模式下绘制深色渐变（如 `DarkBackground` → `DarkSurfaceVariant`）而非 `return this`
- [x] Task 4: 端到端验证
  - [x] SubTask 4.1: 查询牛津高阶词典词条，确认词典自带 CSS 完整生效（字体、颜色、排版均为词典定义）
  - [x] SubTask 4.2: 查询无 CSS 词典词条，确认兜底 `BASE_CSS` 样式正常
  - [x] SubTask 4.3: 切换深色模式，确认侧栏、主内容区、环境背景均为深色
  - [x] SubTask 4.4: 确认亮色模式未受影响

# Task Dependencies
- Task 1、Task 2、Task 3 相互独立，可并行实施
- Task 4 依赖 Task 1、Task 2、Task 3 全部完成
