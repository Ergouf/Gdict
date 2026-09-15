# Gdict

[English](./README.md)

Gdict 是一款使用 Kotlin 编写的 Android 与桌面端词典应用。它读取 MDX 词典、渲染词典 HTML 及其资源，并提供搜索、收藏和间隔重复复习功能。

## 功能特性

- **MDX/MDD 词典** — 解析 MDX V1.2/V2.0，支持 LZO/zlib 压缩和 RipeMD-128 加密头。
- **词典管理** — 可导入单个 MDX 文件或扫描文件夹；同名 MDD 与 CSS 资源可以随词典一起导入。支持启用、停用、诊断和删除词典。
- **快速查词** — 支持精确查词和前缀联想，搜索逻辑位于共享词典引擎中。
- **完整释义** — 渲染词典原始 HTML，以及 CSS、图片、字体和音频等资源。
- **发音** — 优先使用词典内置音频，其次使用在线发音服务；Android 还会回退到系统 TTS。
- **学习工具** — 搜索历史、每日单词、收藏和基于 FSRS 的闪卡复习，评分包括 Again / Hard / Good / Easy。
- **本地化与主题** — 提供中英文界面，以及浅色/深色主题。
- **平台界面** — Android 使用底部导航，桌面端使用可折叠侧边栏；桌面端通过 JCEF 渲染词典内容。

## 应用截图

### Android

<div align="center">
  <img src="screenshots/android-home.png" width="180" alt="搜索">
  <img src="screenshots/android-favorites.png" width="180" alt="收藏">
  <img src="screenshots/android-learning.png" width="180" alt="闪卡复习">
  <img src="screenshots/android-profile.png" width="180" alt="个人中心">
</div>

### 桌面端

<div align="center">
  <img src="screenshots/desktop-home.png" width="600" alt="搜索">
  <br><br>
  <img src="screenshots/desktop-favorites.png" width="600" alt="收藏">
  <br><br>
  <img src="screenshots/desktop-learning.png" width="600" alt="学习">
  <br><br>
  <img src="screenshots/desktop-dictionary.png" width="600" alt="词典管理">
  <br><br>
  <img src="screenshots/desktop-profile.png" width="600" alt="个人中心">
</div>

## 技术栈

| 类别 | 实现 |
|------|------|
| 开发语言 | Kotlin 2.1 |
| Android UI | Jetpack Compose + Material 3 |
| 桌面端 UI | Compose Multiplatform 1.7.3 |
| 共享逻辑 | `shared/` 下的 Kotlin/JVM 模块 |
| 状态管理 | ViewModel + StateFlow |
| Android 存储 | 应用私有目录 + JSON 仓储 |
| 桌面端存储 | `~/.gdict/` 下的 JSON 文件 |
| 词典 HTML | Android WebView / 桌面端 JCEF |
| 构建系统 | Gradle Kotlin DSL |

## 项目结构

```text
Gdict/
├── shared/
│   ├── core/          # MDX/MDD 解析、导入、搜索、资源、FSRS
│   └── shared-ui/     # 共享仓储、ViewModel 和 TTS 抽象
├── android/           # Android 应用及平台适配
├── desktop/           # Compose Desktop 应用及 JCEF 集成
├── screenshots/       # README 截图
├── BUILD.md           # 更完整的本地构建说明
└── scripts/           # CI 与 UI 检查脚本
```

## 构建与测试

环境要求：

- JDK 17
- Android SDK 34（构建 Android 应用时需要）
- Android 最低 API 26，目标/编译 API 34
- 在对应模块目录使用该模块自带的 Gradle Wrapper

### Android

```bash
cd android

# 构建 Debug APK
./gradlew assembleDebug

# 运行单元测试和 Paparazzi 截图测试
./gradlew testDebugUnitTest

# 构建 Release APK；需要配置 android/local.properties 签名信息
./gradlew assembleRelease
```

Debug APK 输出到 `android/app/build/outputs/apk/debug/`。SDK 和签名配置请参考 [BUILD.md](./BUILD.md)。

### 共享核心测试

```bash
cd shared
./gradlew :core:test
```

如需使用本地词典文件运行解析器测试，可追加 `-Dmdx.file.path=/path/to/dict.mdx`。

### 桌面端

```bash
cd desktop

# 本地运行
./gradlew run

# 构建 Linux AppImage 或 Windows 可执行文件
./gradlew packageAppImage
./gradlew packageExe

# 构建仓库提供的 MSIX 包装任务
./gradlew packageMsix

# 仅编译 Kotlin
./gradlew :app:compileKotlin
```

桌面端产物位于 `desktop/app/build/compose/binaries/`。打包 Windows 版本需要桌面端使用的 JCEF bundle，发布工作流会在打包前下载它。

Windows 下请将 `./gradlew` 替换为 `gradlew.bat`。

## 使用说明

1. 在个人中心/词典管理页面导入 `.mdx` 词典。同名 `.mdd` 和 CSS 资源可以自动加入；选择文件夹时可批量扫描导入。
2. 输入单词即可查看前缀联想、搜索历史，以及所有已启用词典中的匹配词条。
3. 打开词条查看原始 HTML 释义，并在可用时播放词典音频或合成发音。
4. 在释义页收藏词条，在「收藏」或「学习」页面使用 FSRS 闪卡复习保存的单词。
5. 在个人中心/设置中管理词典、切换语言或主题、启用扫描弹窗、清除本地历史和收藏，并查看诊断信息。

导入词典时，文件会复制到应用管理的存储目录，源文件不会被修改。

## 架构说明

- `shared/core` 包含 MDX 解析器、流式资源查找、词典管理/导入、搜索引擎和 FSRS 调度器。
- `shared/shared-ui` 包含跨平台仓储接口、ViewModel 和共享 TTS 接口。
- `android` 提供 Android 存储、WebView、音频、语言和持久化适配。
- `desktop` 提供 JSON 文件持久化、JCEF、桌面音频、原生打包和侧边栏界面。

共享解析器当前要求以 MDX 作为主词典文件；MDD 用于 CSS、图片、字体和音频等配套资源。

## 许可证

GPL-3.0
