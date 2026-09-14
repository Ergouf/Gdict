---
alwaysApply: true
---
# Gdict 项目规则

## 项目概述

Gdict 是一款跨平台词典应用（Android + Desktop），支持 MDX/MDD 词典格式，内置 FSRS 间隔重复算法。包名 `io.github.gdict`。

### 设计系统（双端差异）

- **Android 端**：Jetpack Compose + Material Design 3 作为平台组件基础，视觉与交互遵循 `docs/apple-hig-ui-migration.md` 中的 Apple HIG-inspired 原则：内容优先、层级清晰、颜色克制、标准交互、可访问性优先。
- **Desktop 端**：暂时保持 Fluent Design 2 风格（通过 MD3 `colorScheme` 承载 Fluent 色板）；除非另开 Desktop parity phase，不要把 Android 的页面实现直接复制到 Desktop。
- Android 使用语义化 `GdictColors`：中性 Background/Surface/Glass/Separator/Label token；品牌蓝 `#1E8CFF` 只用于主要动作、链接、选择和有意义的状态。
- “Glass” 只作为导航/交互 chrome 的轻量材质表达。Jetpack Compose 的普通 `Modifier.blur()` 不是 backdrop blur，禁止把它包装成伪 Liquid Glass。
- 禁止页面自定义蓝白渐变、全局蓝色环境光斑、无业务含义的 stagger/page-enter/pulse 动画。
- 搜索、滚动、返回、收藏、分享、设置等优先采用 Android 用户熟悉的标准交互；不得用 pinch、drag、swipe-only 等隐藏手势替代唯一可发现的显式控制。
- 允许保留能解释状态的短动画，例如闪卡翻面、播放状态、明确的展开/收起。
- Android UI 变更必须通过 `scripts/check-apple-hig-ui.sh`；详细阶段、例外规则和 Apple 官方参考见 `docs/apple-hig-ui-migration.md`。

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
- DictionariesScreen → DictionaryViewModel + SettingsViewModel
- SettingsScreen → SettingsViewModel

## 代码风格

### 语言与框架

- 100% Kotlin，UI 使用 Jetpack Compose
- Android 端使用 Material Design 3 组件基础 + HIG-inspired 语义 UI 系统；Desktop 端暂时使用 Fluent Design 风格
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
- 色值使用 `GdictColors` 色板（定义在 `theme/Color.kt`），页面禁止硬编码 RGB/ARGB 色值
- 字体使用 `GdictTypography`（定义在 `theme/Type.kt`）
- 交互控件默认保证至少约 48dp 的可点击区域
- 用户可见字符串优先放入 string resource；诊断/开发信息除外
- 动效必须能说明状态变化或任务结果；纯装饰性循环、入场编舞不得进入生产页面

## 构建与测试

### 必过门禁

```bash
./scripts/check-apple-hig-ui.sh
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
```

CI 还必须运行 shared core tests 与 Desktop compile，确保 Android UI 改造不破坏跨平台核心模块。

### 构建环境

- JDK 17+
- Android SDK API 34（`compileSdk`/`targetSdk`）
- `minSdk` = 26
- AGP 8.2.2 / Kotlin 1.9.24
- Gradle 8.5（通过 wrapper）
- `buildFeatures.buildConfig = true` 必须启用（用于 `BuildConfig.VERSION_NAME`）

## 版本管理

版本号由 Git 自动生成，**不要手动修改** `versionCode` / `versionName`：

- `versionCode` = Git 提交总数（`git rev-list --count HEAD`）
- `versionName` = 最近 Git 标签（`git describe --tags --always`，去除 `v` 前缀）

发布新版本时打 Git tag：`git tag v1.2.0 && git push --tags`

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

- core 模块使用 `GdictLogger` 接口，不直接使用 `android.util.Log`
- app 模块在 `GdictApplication.onCreate()` 中注入 `AndroidLogger` 实现

### 搜索

- 二分查找 O(log n) 精确匹配 + 前缀预测搜索
- 搜索跨所有启用词典并行执行，结果按词典分组
- 输入防抖 300ms（使用 `Flow.debounce`，在 ViewModel 层实现）
- 搜索建议从词典前缀匹配生成（`DictionaryManager.searchSuggestions`）

### WebView 资源拦截

- 详情页使用 `MdxWebView` 组件（`ui/webview/MdxWebView.kt`）封装 WebView 逻辑
- HTML 内容由 `HtmlContentBuilder`（`ui/webview/HtmlContentBuilder.kt`）构建，支持 CSS 注入和主题切换
- 不同词典的定制化渲染通过 `DictionaryRenderer` 接口实现
- 音频播放由 `AudioPlayer` 单例处理
- `sound://` 自定义协议用于音频播放
- `entry://` 自定义协议用于交叉引用跳转
- MDD 资源路径匹配需兼容反斜杠、双反斜杠、文件名、正斜杠与 URL 解码
- 支持 CSS、JS、图片、字体、音频等资源拦截
- WebView 使用内容去重、CSS 内联和 `blockNetworkLoads = true` 等现有优化，不因 UI 重构而移除

### 发音

- 优先尝试词典 MDD 音频资源
- 可回退微软 Edge TTS 云端 API和 Android 本地 TTS
- `entry://` 交叉引用仍通过 `SearchViewModel.searchWordForResult` 搜索并导航
- Cambridge/Collins 的原生详情视觉可以重构，但不得破坏音频路径、交叉引用或 WebView fallback

### FSRS 间隔重复

- 实现 FSRS 算法（非 SM-2）
- 核心参数：Difficulty（1-10）、Stability、Retrievability
- meanReversion 使用独立权重 w=0.4
- UI 改造不得改变调度算法、评分语义或持久化格式

## 数据存储

- 使用 SharedPreferences 存储 JSON 数据（收藏、历史、词典配置、FSRS 状态）
- 每个导入的词典存储在 `filesDir/dictionaries/$id/` 独立目录
- 词典导入时复制文件，不修改原始文件

## 禁止事项

- 不要在 core 模块引入 Android UI 依赖
- 不要使用 LiveData，统一用 StateFlow
- 不要在迁移后的 Android 页面硬编码颜色值
- 不要恢复全页蓝白渐变、Acrylic 环境光斑或伪 backdrop blur
- 不要用隐藏手势作为唯一控制路径
- 不要手动修改 `versionCode` / `versionName`
- 不要将 `local.properties` 或 `*.keystore` 提交到 Git
- 不要在 Compose 中使用 XML 布局
