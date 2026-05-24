# Gdict

[中文版 (Chinese)](./README.zh-CN.md)

A modern Android dictionary app following Material Design 3, supporting MDX/MDD dictionary formats.

## Features

- **MDX/MDD Parsing** — Supports V1.2 & V2.0 specs, LZO/zlib decompression, RipeMD128 encryption
- **Multi-Dictionary** — Add, remove, enable/disable dictionaries, persistent across restarts, batch folder import
- **Word Search** — Binary search O(log n) exact match + prefix predictive search
- **HTML Rendering** — WebView renders original dictionary HTML, MDD CSS/img/audio resource extraction
- **MDD Audio Playback** — Extract pronunciation audio from MDD, fallback to TTS
- **Word of the Day** — Dynamic daily word generation from loaded dictionaries
- **Bookmarks** — Save words, delete with confirmation dialog
- **Search History** — Auto-recorded search history
- **Dark Mode** — System dark mode + in-app toggle
- **Edge-to-Edge** — Immersive status bar and navigation bar
- **MD3 Bottom Nav** — Search / Favorites / Learning / Profile

## Tech Stack

| Category | Technology |
|------|------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material Design 3 |
| Navigation | Navigation Compose |
| State Management | ViewModel + StateFlow |
| Data Persistence | SharedPreferences (JSON) |
| Audio | MediaPlayer + TextToSpeech |
| Build | Gradle Kotlin DSL |

## Project Structure

```
Gdict/
├── android_project/                        # Android 应用（主项目）
│   ├── app/                                # 应用模块
│   │   ├── src/main/java/io/github/gdict/
│   │   │   ├── MainActivity.kt             # 入口 Activity
│   │   │   ├── GdictApplication.kt         # Application
│   │   │   ├── data/
│   │   │   │   └── AppRepository.kt        # 数据仓库
│   │   │   ├── viewmodel/
│   │   │   │   ├── SettingsViewModel.kt     # Global settings (dark mode, etc.)
│   │   │   │   ├── SearchViewModel.kt       # Search + history + WotD
│   │   │   │   ├── BookmarkViewModel.kt     # Bookmark management
│   │   │   │   ├── FlashcardViewModel.kt    # FSRS flashcard session
│   │   │   │   └── DictionaryViewModel.kt   # Dict import/management
│   │   │   └── ui/
│   │   │       ├── GdictApp.kt            # 主 UI + 底部导航
│   │   │       ├── screens/
│   │   │       │   ├── SearchScreen.kt     # 搜索页 + Word of the Day
│   │   │       │   ├── WordDetailScreen.kt # 词条详情页 + 发音
│   │   │       │   ├── BookmarksScreen.kt  # 生词本页
│   │   │       │   ├── FlashcardScreen.kt  # 间隔重复复习页 (FSRS)
│   │   │       │   ├── HistoryScreen.kt    # 历史记录页
│   │   │       │   ├── DictionariesScreen.kt # 词典管理页
│   │   │       │   └── SettingsScreen.kt   # 设置页
│   │   │       └── theme/
│   │   │           ├── Color.kt            # GdictColors 色板
│   │   │           ├── Theme.kt            # GdictTheme + Edge-to-Edge
│   │   │           └── Type.kt             # GdictTypography
│   │   ├── build.gradle.kts
│   │   └── proguard-rules.pro
│   ├── core/                               # 核心库模块
│   │   ├── src/main/java/io/github/gdict/core/
│   │   │   ├── MdxParser.kt                # MDX/MDD 解析器（含流式查找）
│   │   │   ├── Lzo1xDecompressor.kt        # LZO1X 解压
│   │   │   ├── RipeMD128.kt                # RipeMD-128 哈希
│   │   │   ├── DictionaryManager.kt        # 词典管理（协调器）
│   │   │   ├── DictPersistence.kt          # 词典持久化
│   │   │   ├── DictFileImporter.kt         # 词典文件导入
│   │   │   ├── DictSearchEngine.kt         # 词典搜索引擎
│   │   │   └── FsrsAlgorithm.kt            # FSRS 间隔重复算法
│   │   ├── src/test/java/io/github/gdict/core/
│   │   │   └── MdxParserTest.kt            # 解析器单元测试
│   │   ├── build.gradle.kts
│   │   └── proguard-rules.pro
│   ├── gradle/wrapper/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── export.build.gradle.kts             # 导出任务构建配置
│   └── play_store_512.png                  # Play Store 图标
├── export_project/                         # 独立导出工具项目
│   └── build.gradle.kts
├── export_words.kt                         # 独立 MDX 导出脚本（含 LZO/RipeMD128）
├── .gitignore
└── README.md
```

## Quick Start

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK API 34
- Gradle 8.5 (included via wrapper)

### Build

```bash
cd android_project

# Debug build
./gradlew assembleDebug

# Release build (requires local.properties configuration)
./gradlew assembleRelease

# APK output
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

### Release Signing

Create `local.properties` under `android_project/`:

```properties
sdk.dir=D\\:\\path\\to\\android_sdk
storeFile=release.keystore
storePassword=<your_store_password>
keyAlias=gdict
keyPassword=<your_key_password>
```

Note: See [LOCAL_BUILD.md](./LOCAL_BUILD.md) for detailed local build setup instructions.

Generate a keystore:

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias gdict \
  -keyalg RSA -keysize 2048 -validity 10000
```

### Open in Android Studio

1. Open the `android_project` directory
2. Wait for Gradle sync
3. Connect a device or start an emulator
4. Click Run

### Run Tests

```bash
cd android_project

# Run MDX parser unit tests
./gradlew :core:testDebugUnitTest --rerun-tasks

# Run tests with MDX file path
./gradlew :core:testDebugUnitTest -Dmdx.file.path=/path/to/dict.mdx
```

## Usage

### Import Dictionaries

1. Navigate to Settings → Dictionary Management
2. Tap + to select dictionary files
3. Supports `.mdx` files; auto-discovers `.mdd` resources and `.css` styles in the same directory
4. Supports folder-based batch import

### Search

- Type a word in the search bar for real-time suggestions
- Tap a result to view the full definition
- Detail page renders the dictionary's original HTML content

### Pronunciation

- Tap the speaker button on the detail page
- Prefers MDD audio extraction when available
- Falls back to system TTS

### Bookmarks

- Tap the bookmark icon on the detail page to save words
- Manage bookmarks under the Favorites tab
- Delete bookmarks with confirmation dialog

### Flashcard Review (FSRS)

- Uses the FSRS (Free Spaced Repetition Scheduler) algorithm
- Start a review session from the Learning tab or Bookmarks page
- Rate cards as Again / Hard / Good / Easy

### Export Entries (Dev Tool)

`export_words.kt` is a standalone Kotlin script for exporting MDX entries to HTML files:

```bash
kotlinc -script export_words.kt -- /path/to/dict.mdx /path/to/output

# Or via Gradle
cd android_project
./gradlew export -PmdxPath=/path/to/dict.mdx -PoutputDir=/path/to/output
```

## Core Architecture

### MDX/MDD File Format

MDX file structure:
```
┌──────────────────────────────────────────────┐
│ 1. Header Section   - 词典元信息（XML 格式）  │
│ 2. Keyword Section  - 关键词索引 + 关键词块    │
│ 3. Record Section   - 释义记录索引 + 记录数据块 │
└──────────────────────────────────────────────┘
```

- V1.2: Integer fields are 4-byte Big-Endian, keyword index is uncompressed
- V2.0: Integer fields are 8-byte Big-Endian (64-bit), keyword index is compressed

Compression block header (8 bytes):
```
[0..3] Compression type (little-endian): 0=none, 1=LZO, 2=zlib
[4..7] Adler32 checksum (big-endian)
[8..]  Actual compressed data
```

MDD files share the same format spec but store resource files (CSS, images, audio, etc.).

### Dictionary Data Isolation

Each imported dictionary is copied to a dedicated directory `filesDir/dictionaries/$id/`, identified by a unique ID and path, ensuring no cross-contamination between dictionaries.

### Search Flow

1. User enters a query
2. ViewModel dispatches to all enabled dictionaries
3. Each MdxParser instance performs an independent binary search
4. Results are aggregated and displayed grouped by dictionary

### Streaming Resource Lookup

For MDD resource files (CSS, images, audio, etc.), a streaming lookup approach is used:

1. Read keyword index blocks directly from file
2. Decompress and search for target resource keys block by block
3. Upon match, read resource data via record offset
4. File pointer is restored after lookup to not interfere with subsequent operations

### WebView Resource Interception

The detail page intercepts resource requests via `WebViewClient.shouldInterceptRequest`:

1. WebView loads dictionary HTML content and requests CSS/images/audio
2. Intercepted requests are served by synchronously reading from the MDD file
3. Returns a `WebResourceResponse` containing the resource data

## Acknowledgments

| Project | Description | Link |
|------|------|------|
| **Linux Kernel** | LZO1X decompression algorithm, ported from `lib/lzo/lzo1x_decompress_safe.c` | [GitHub: torvalds/linux](https://github.com/torvalds/linux) |
| **Woodstox** | XML StAX parser for MDX Header parsing | [GitHub: FasterXML/woodstox](https://github.com/FasterXML/woodstox) |
| **Jetpack Compose** | Android declarative UI framework | [Android Developers](https://developer.android.com/compose) |
| **Material Design 3** | Google design system | [Material Design 3](https://m3.material.io/) |

## License

GPL-3.0
