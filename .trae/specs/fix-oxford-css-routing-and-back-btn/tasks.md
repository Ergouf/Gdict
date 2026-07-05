# Tasks
- [x] Task 1: 修复桌面 `GlassCircleButton` 点击失效
  - [x] SubTask 1.1: 在 `desktop/app/src/main/kotlin/io/github/gdict/ui/screens/CollinsDetailScreen.kt` 的 `GlassCircleButton`（第 710-728 行）`Box` 修饰符链末尾添加 `.clickable(onClick = onClick)`，与 Android 端实现对齐
  - [x] SubTask 1.2: 验证 `PronunciationDetailScreen.kt:136`（返回按钮）和 `CollinsDetailScreen.kt:130`（返回按钮）的 `onClick` 能被正确触发
- [x] Task 2: 收紧 `isPronunciationDict` 路由判定，避免牛津词典被误路由
  - [x] SubTask 2.1: 修改桌面 `WordDetailScreen.kt`（第 137-140 行）的 `isPronunciationDict` 判定逻辑：仅当包含 `cepd18.css`，或同时出现 Cambridge 专属信号（`uk_sound.png`/`us_sound.png` 与 `<prongrp>`）时才为 true
  - [x] SubTask 2.2: 同步修改 Android `WordDetailScreen.kt`（第 86-89 行）的相同判定逻辑
  - [x] SubTask 2.3: 验证 Cambridge EPD 词典仍能正确路由到 `PronunciationDetailContent`
- [x] Task 3: 为 `DefaultRenderer` 分支注入 `BASE_CSS` 兜底样式
  - [x] SubTask 3.1: 修改 `shared/shared-ui/src/main/kotlin/io/github/gdict/ui/webview/HtmlContentBuilder.kt` 的 `isDefaultRenderer` 分支（第 148-170 行），在词典 CSS 之前注入 `BASE_CSS`（或精简子集）
  - [x] SubTask 3.2: 验证注入 `BASE_CSS` 后，词典自带 CSS 仍能正确覆盖默认样式（CSS 顺序：`BASE_CSS` → 词典 CSS → 透明背景）
  - [x] SubTask 3.3: 验证透明背景在 WebView 上仍保持生效
- [x] Task 4: 端到端验证
  - [x] SubTask 4.1: 在 Windows 桌面端查询牛津高阶词典词条，确认 CSS 正确加载、排版正常
  - [x] SubTask 4.2: 在 Windows 桌面端进入 `PronunciationDetailScreen`，点击返回按钮确认可返回
  - [x] SubTask 4.3: 确认未引入其他词典（如 Collins、Cambridge EPD）的回归

# Task Dependencies
- Task 1、Task 2、Task 3 相互独立，可并行实施
- Task 4 依赖 Task 1、Task 2、Task 3 全部完成
