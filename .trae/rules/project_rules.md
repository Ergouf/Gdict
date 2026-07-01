---
alwaysApply: false
description: Gdict 项目全局规则，AI 助手在编辑代码时必须遵循
---
# Gdict 项目规则

## 项目概述

Gdict 是一款跨平台词典应用（Android + Desktop），支持 MDX/MDD 词典格式，内置 FSRS 间隔重复算法。包名 `io.github.gdict`。

### 设计系统（双端统一 Fluent 风格）

- **Android 端与 Desktop 端均采用 Fluent Design 风格**（通过 MD3 `colorScheme` 承载 Fluent 色板），双端视效保持一致
- Fluent 规范统一遵循：8dp 圆角、1px `GdictColors.CardStroke` 描边、0 elevation（靠描边分层）、`SubtleHover` 悬停态、`indication = null` 去 ripple
- 两端共享同一套 `GdictColors` 色值定义（品牌绿色 accent + Fluent 中性色）
- 平台差异仅限"背景透明度"：Desktop 端背景半透明以透出 Windows 11 Mica/Acrylic 系统材料；Android 端无系统材料概念，背景不透明
- 底栏（如复习评分栏）采用 Liquid Glass 风格：半透明背景 + 顶部边缘高光渐变 + CardStroke 边界线
- 品牌主色（accent）固定为绿色 `#5D8A6B`，不得切换为 Fluent 默认蓝

## 架构

- **app 模块** — UI + ViewModel 层（Jetpack Compose + MVVM）
- **core 模块** — 纯逻辑层（MDX/MDD 解析、搜索、FSRS），无 Android UI 依赖

### MVVM 分层

```
Screen (Compose) → ViewModel (StateFlow) → Repository → core 模块
```

- 每个 Screen 对应一个或多个专用 ViewModel
- ViewModel 之间不互相引用，通过 Repository 共享数据
- Repository 按职责拆分为独立单例，由 `GdictApplication` 持有

### Repository 职责划分

| Repository | 职责 |
|------------|------|
| DictionaryRepository | 词典管理、搜索、音频资源获取 |
| HistoryRepository | 搜索历史管理 |
| BookmarkRepository | 收藏管理、FSRS 复习调度 |
| SettingsRepository | 深色模式、扫描弹窗开关等设置项 |

### ViewModel 职责划分

| ViewModel | 职责 |
|-----------|------|
| SettingsViewModel | 设置项读写（委托 SettingsRepository） |
| SearchViewModel | 搜索、搜索历史、Word of the Day |
| BookmarkViewModel | 收藏管理 |
| FlashcardViewModel | FSRS 闪卡复习会话 |
| DictionaryViewModel | 词典导入/管理/诊断 |

### Screen 依赖关系

- SearchScreen → SearchViewModel + SettingsViewModel
- WordDetailScreen → DictionaryRepository + SettingsViewModel
- BookmarksScreen → BookmarkViewModel + SettingsViewModel
- FlashcardScreen → FlashcardViewModel + SettingsViewModel + BookmarkViewModel
- DictionariesScreen → DictionaryViewModel
- SettingsScreen → SettingsViewModel

## 代码风格

### 语言与框架

- 100% Kotlin，UI 使用 Jetpack Compose
- Android 端与 Desktop 端均使用 Fluent Design 风格（通过 MD3 `colorScheme` token 承载 Fluent 色值，不引入第三方 Fluent 库）
- 禁止使用 XML 布局，所有 UI 必须用 Compose 编写
- 状态管理使用 ViewModel + StateFlow，禁止 LiveData
- 导航使用 Navigation Compose

### 命名约定

- Composable 函数使用大驼峰：`SearchScreen`、`WordDetailScreen`
- StateFlow 命名：`uiState`（公开）、`_uiState`（私有 backing）
- 事件处理函数：`onXxxClick`、`onXxxChange`
- 包名全小写：`io.github.gdict.ui.screens`

### 代码规范

- 不使用通配符 import
- 不添加多余注释，代码应自解释
- Compose 函数按 `@Composable` 注解标识
- 色值使用 `GdictColors` 色板（定义在 `theme/Color.kt`），不硬编码颜色
- 字体使用 `GdictTypography`（定义在 `theme/Type.kt`）

## 构建与测试

### 构建命令

```bash
cd android_project

# Debug 构建
./gradlew assembleDebug

# Release 构建（需要签名配置）
./gradlew assembleRelease

# 清理
./gradlew clean
```

### 测试命令

```bash
# core 模块单元测试（纯 JVM，无需模拟器）
./gradlew :core:testDebugUnitTest --rerun-tasks

# 指定 MDX 文件路径
./gradlew :core:testDebugUnitTest -Dmdx.file.path=/path/to/dict.mdx
```

### 构建环境

- JDK 17+（`JAVA_HOME` 必须指向 JDK 17）
- Android SDK API 34（`compileSdk`/`targetSdk`）
- `minSdk` = 26
- AGP 8.2.2 / Kotlin 1.9.24
- Gradle 8.5（通过 wrapper）
- `buildFeatures.buildConfig = true` 必须启用（用于 `BuildConfig.VERSION_NAME`）

## 版本管理

版本号由 Git 自动生成，**不要手动修改** `versionCode` / `versionName`：

- `versionCode` = Git 提交总数（`git rev-list --count HEAD`）
- `versionName` = 最近 Git 标签（`git describe --tags --always`，去除 `v` 前缀）

发布新版本时打 Git tag 即可：`git tag v1.2.0 && git push --tags`

## 签名配置

- Release keystore 位于 `android_project/release.keystore`（根目录，非 app/ 下）
- 密码通过 `local.properties` 或环境变量 `STORE_PASSWORD` / `KEY_PASSWORD` 传入
- `local.properties` 已在 `.gitignore` 中排除，不提交到版本控制
- keyAlias 固定为 `gdict`

## 核心模块注意事项

### MDX/MDD 解析

- 支持 V1.2（4 字节偏移）和 V2.0（8 字节偏移 + 压缩索引）
- 压缩类型：0=无压缩、1=LZO、2=zlib
- V2.0 加密使用 RipeMD-128
- MDD 与 MDX 共享格式，但存储资源文件（CSS/图片/音频）

### 日志

- core 模块使用 `GdictLogger` 接口（`core/src/.../GdictLogger.kt`），不直接使用 `android.util.Log`
- app 模块在 `GdictApplication.onCreate()` 中注入 `AndroidLogger` 实现

### 搜索

- 二分查找 O(log n) 精确匹配 + 前缀预测搜索
- 搜索跨所有启用词典并行执行，结果按词典分组
- 输入防抖 300ms（使用 `Flow.debounce`，在 ViewModel 层实现）
- 搜索建议从词典前缀匹配生成（`DictionaryManager.searchSuggestions`）

### WebView 资源拦截

- 详情页使用 `MdxWebView` 组件（`ui/webview/MdxWebView.kt`）封装 WebView 逻辑
- HTML 内容由 `HtmlContentBuilder`（`ui/webview/HtmlContentBuilder.kt`）构建，支持 CSS 注入和主题切换
- 不同词典的定制化渲染通过 `DictionaryRenderer` 接口（`ui/webview/DictionaryRenderer.kt`）实现
  - `DefaultRenderer`：默认渲染，直接透传 HTML
  - `CambridgeEpdRenderer`：Cambridge EPD 专用，替换发音图片为 CSS 图标
- 音频播放由 `AudioPlayer` 单例（`ui/webview/AudioPlayer.kt`）处理
- 详情页通过 `WebViewClient.shouldInterceptRequest` 拦截资源请求
- 从 MDD 同步读取 CSS/图片/音频/字体资源
- `sound://` 自定义协议用于音频播放
- `entry://` 自定义协议用于交叉引用跳转（`shouldOverrideUrlLoading` 拦截，提取目标词条名后异步搜索并导航）
- 资源路径匹配：尝试多种格式（反斜杠、双反斜杠、仅文件名、正斜杠），URL 解码处理 `%20` 等编码字符
- 支持拦截的文件类型：CSS、JS、图片（png/jpg/gif/svg/webp）、字体（ttf/woff/woff2）、音频（mp3/wav/ogg/spx）
- `DictionaryManager` 维护 `resourceCache`（按路径缓存资源数据）和 `cssKeysCache`（按词典 ID 缓存 CSS 关键词列表）
- `SearchViewModel` 维护按词典名缓存的 CSS，避免导航到详情页时重复从 MDD 读取
- 卸载词典时清空 `resourceCache`，避免缓存残留
- WebView 加载优化：`setTag/getTag` 内容去重避免重复 `loadDataWithBaseURL`；CSS 内联注入后移除原始 `<link>` 标签；`blockNetworkLoads = true`

### 发音

- 优先使用微软 Edge TTS 云端 API（`tts/EdgeTtsClient.kt`）
- 回退从 MDD 提取音频资源
- 最终回退到 Android 本地 TTS
- 需要 `INTERNET` 权限（已在 AndroidManifest.xml 声明）
- 发音图标使用 CSS `::before` 伪元素渲染 Unicode ▶（U+25B6），不使用 emoji
- Cambridge EPD 等词典的发音图片（speaker/play/sound/volume 等）替换为 `.speaker-icon` 元素
- Cambridge 专用 CSS 始终注入，不受 MDD CSS 是否为空影响
- `entry://` 交叉引用跳转通过 `SearchViewModel.searchWordForResult` 异步搜索后导航

### FSRS 间隔重复

- 实现了 FSRS 算法（非 SM-2）
- 核心参数：Difficulty（1-10）、Stability、Retrievability
- meanReversion 使用独立权重 w=0.4

## 数据存储

- 使用 SharedPreferences 存储 JSON 数据（收藏、历史、词典配置、FSRS 状态）
- 每个导入的词典存储在 `filesDir/dictionaries/$id/` 独立目录
- 词典导入时复制文件，不修改原始文件

## 禁止事项

- 不要在 core 模块引入 Android UI 依赖
- 不要使用 LiveData，统一用 StateFlow
- 不要硬编码颜色值，使用 `GdictColors` 色板
- 不要手动修改 `versionCode` / `versionName`
- 不要将 `local.properties` 或 `*.keystore` 提交到 Git
- 不要在 Compose 中使用 XML 布局
