# Gdict

一款遵循 Material Design 3 设计规范的现代 Android 词典应用。

## 项目概览

Gdict 是一款极简、通透、原生的移动端词典 App，支持 MDX/MDD 词典格式（V1.2 & V2.0），采用低饱和度绿色品牌色，强调呼吸感与留白设计。

## 技术栈

- **Kotlin** — 主开发语言
- **Jetpack Compose** — 声明式 UI 框架
- **Material Design 3** — 设计系统
- **Navigation Compose** — 屏幕导航
- **ViewModel + StateFlow** — 状态管理
- **SharedPreferences** — 词典元数据持久化
- **Gradle Kotlin DSL** — 构建配置

## 功能特性

- MDX/MDD 词典文件解析（支持 V1.2 和 V2.0，含 LZO/zlib 解压）
- 单词搜索（二分查找，O(log n)）
- 前缀预测搜索（模糊建议，最多 20 条）
- 搜索历史记录
- 生词本（书签收藏）
- 词典管理（添加/删除/启停，重启后持久化）
- 深色模式
- 文件夹批量扫描导入词典
- MD3 药丸形底部导航栏

## 项目结构

```
android_project/
├── app/                                    # 应用模块
│   ├── src/main/java/io/github/gdict/
│   │   ├── MainActivity.kt                 # 入口 Activity
│   │   ├── GdictApplication.kt             # Application
│   │   ├── data/
│   │   │   └── AppRepository.kt            # 数据仓库
│   │   ├── viewmodel/
│   │   │   └── AppViewModel.kt             # ViewModel
│   │   └── ui/
│   │       ├── GoldenDictNgApp.kt          # 主 UI + 导航
│   │       ├── screens/                    # 各页面
│   │       │   ├── SearchScreen.kt         # 搜索页
│   │       │   ├── WordDetailScreen.kt     # 词条详情页
│   │       │   ├── BookmarksScreen.kt      # 生词本页
│   │       │   ├── HistoryScreen.kt        # 历史记录页
│   │       │   ├── DictionariesScreen.kt   # 词典管理页
│   │       │   └── SettingsScreen.kt       # 设置页
│   │       └── theme/                      # 主题
│   │           ├── Color.kt                # GdictColors 色板
│   │           ├── Theme.kt                # GdictTheme
│   │           └── Type.kt                 # GdictTypography
│   └── build.gradle.kts
├── core/                                   # 核心库模块
│   ├── src/main/java/io/github/gdict/core/
│   │   ├── MdxParser.kt                    # MDX/MDD 解析器（二分搜索、Adler32 校验）
│   │   ├── Lzo1xDecompressor.kt            # LZO1X 解压（基于 Linux 内核实现）
│   │   └── DictionaryManager.kt            # 词典管理（SharedPreferences 持久化）
│   └── src/test/java/io/github/gdict/core/
│       └── MdxParserTest.kt                # MDX 解析器单元测试（8 个用例）
├── build_and_verify.ps1                    # 自动化测试 + 构建脚本
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+（推荐 Temurin 21）
- Android SDK API 34
- Gradle 8.5（项目自带 wrapper）

### 运行测试

```bash
cd android_project

# 运行 MDX 解析器单元测试
.\gradlew.bat :core:testDebugUnitTest --rerun-tasks
```

### 构建

```bash
cd android_project

# Debug 构建
.\gradlew.bat assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

### 使用构建验证脚本

```powershell
.\build_and_verify.ps1
```

脚本会先运行单元测试，通过后再构建 APK。

### 使用 Android Studio

1. 打开 `android_project` 目录
2. 等待 Gradle 同步
3. 连接设备或启动模拟器
4. 点击 Run

## 本地构建工具

项目在 `D:\workspace\build-tools` 目录下提供了离线构建工具：

- `android-sdk/` — Android SDK（build-tools 34.0.0、platforms android-34）
- `jdk17/` — JDK 17
- `gradle-8.5/` — Gradle 发行版

如需在没有 Android Studio 的环境中构建，可将这些工具配置到环境变量中。

## 致谢

本项目的开发参考和借鉴了以下开源项目，在此致以诚挚感谢：

| 项目 | 说明 | 链接 |
|------|------|------|
| **mdict4j** | MDX/MDD 词典格式的 Java 解析库，本项目的 MdxParser 在设计上参考了其解析思路 | [GitHub: eb4j/mdict4j](https://github.com/eb4j/mdict4j) / [Codeberg: miurahr/mdict4j](https://codeberg.org/miurahr/mdict4j) |
| **Linux Kernel LZO** | LZO1X 解压缩算法，本项目的 Lzo1xDecompressor 基于内核 `lzo1x_decompress_safe.c` 重写 | `lib/lzo/lzo1x_decompress_safe.c` |
| **GoldenDict-ng** | 桌面端词典应用，本项目的灵感来源和命名参考 | [GitHub: xiaoyifang/goldendict-ng](https://github.com/xiaoyifang/goldendict-ng) |
| **Material Design 3** | Google 设计系统，本项目 UI 严格遵循其规范 | [Material Design 3](https://m3.material.io/) |
| **Jetpack Compose** | Android 声明式 UI 工具包 | [Android Developers](https://developer.android.com/jetpack/compose) |

## 许可证

GPL-3.0
