# Gdict

一款遵循 Material Design 3 设计规范的现代 Android 词典应用，支持 MDX/MDD 词典格式。

## 功能特性

- **MDX/MDD 词典解析** — 支持 V1.2 和 V2.0 规范，含 LZO/zlib 解压、RipeMD128 加密解密
- **多词典管理** — 添加、删除、启停词典，重启后持久化，文件夹批量扫描导入
- **单词搜索** — 二分查找 O(log n) 精确匹配 + 前缀预测搜索
- **HTML 释义渲染** — WebView 渲染词典原始 HTML 内容，支持从 MDD 提取 CSS 样式
- **MDD 音频播放** — 从 MDD 资源包提取发音音频，回退到 TTS 语音合成
- **Word of the Day** — 从已加载词典中动态生成每日推荐单词
- **生词本** — 收藏单词，支持删除和二次确认
- **搜索历史** — 自动记录搜索历史
- **深色模式** — 支持系统深色模式和应用内切换
- **全面屏适配** — Edge-to-Edge 沉浸式状态栏和导航栏
- **MD3 底部导航栏** — Search / Favorites / Learning / Profile 四页导航

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose |
| 设计系统 | Material Design 3 |
| 导航 | Navigation Compose |
| 状态管理 | ViewModel + StateFlow |
| 数据持久化 | SharedPreferences |
| 音频 | MediaPlayer + TextToSpeech |
| 构建配置 | Gradle Kotlin DSL |

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
│   │       ├── GoldenDictNgApp.kt          # 主 UI + 底部导航
│   │       ├── screens/
│   │       │   ├── SearchScreen.kt         # 搜索页 + Word of the Day
│   │       │   ├── WordDetailScreen.kt     # 词条详情页 + 发音
│   │       │   ├── BookmarksScreen.kt      # 生词本页
│   │       │   ├── HistoryScreen.kt        # 历史记录页
│   │       │   ├── DictionariesScreen.kt   # 词典管理页
│   │       │   └── SettingsScreen.kt       # 设置页
│   │       └── theme/
│   │           ├── Color.kt                # GdictColors 色板
│   │           ├── Theme.kt                # GdictTheme + Edge-to-Edge
│   │           └── Type.kt                 # GdictTypography
│   └── build.gradle.kts
├── core/                                   # 核心库模块
│   ├── src/main/java/io/github/gdict/core/
│   │   ├── MdxParser.kt                    # MDX/MDD 解析器
│   │   ├── Lzo1xDecompressor.kt            # LZO1X 解压
│   │   ├── RipeMD128.kt                    # RipeMD-128 哈希
│   │   └── DictionaryManager.kt            # 词典管理
│   └── src/test/java/io/github/gdict/core/
│       └── MdxParserTest.kt                # 解析器单元测试
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK API 34
- Gradle 8.5（项目自带 wrapper）

### 构建

```bash
cd android_project

# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

### 使用 Android Studio

1. 打开 `android_project` 目录
2. 等待 Gradle 同步
3. 连接设备或启动模拟器
4. 点击 Run

### 运行测试

```bash
cd android_project

# 运行 MDX 解析器单元测试
./gradlew :core:testDebugUnitTest --rerun-tasks
```

## 使用说明

### 导入词典

1. 在设置页点击「Dictionary Management」
2. 点击 + 按钮选择词典文件
3. 支持 `.mdx` 文件，自动查找同目录下的 `.mdd` 资源包和 `.css` 样式文件
4. 支持选择文件夹批量导入目录下所有词典

### 搜索单词

- 在搜索页输入单词，实时显示搜索建议
- 点击搜索结果进入详情页
- 详情页展示词典原始 HTML 释义内容

### 发音

- 详情页点击发音按钮播放单词发音
- 优先从 MDD 资源包提取音频文件播放
- 若无 MDD 音频，回退到系统 TTS 语音合成

### 生词本

- 在详情页点击书签图标收藏单词
- 在 Favorites 页查看和管理收藏的单词
- 支持删除收藏，有二次确认对话框

## 核心架构

### MDX/MDD 文件格式

MDX 文件结构：
```
┌──────────────────────────────────────────────┐
│ 1. Header Section   - 词典元信息（XML 格式）  │
│ 2. Keyword Section  - 关键词索引 + 关键词块    │
│ 3. Record Section   - 释义记录索引 + 记录数据块 │
└──────────────────────────────────────────────┘
```

- V1.2: 整数字段为 4 字节 Big-Endian，关键词索引不压缩
- V2.0: 整数字段为 8 字节 Big-Endian（64 位），关键词索引经过压缩

压缩块的前 8 字节为压缩头：
```
[0..3] 压缩类型（小端序）：0=不压缩, 1=LZO, 2=zlib
[4..7] Adler32 校验和（大端序）
[8..]  实际压缩数据
```

MDD 文件与 MDX 共享相同的格式规范，但存储的是资源文件（CSS、图片、音频等）。

### 词典数据隔离

每个词典导入后复制到独立目录 `filesDir/dictionaries/$id/`，通过唯一 ID 和路径区分，确保多词典数据互不干扰。

### 搜索流程

1. 用户输入查询词
2. ViewModel 分发到所有启用的词典
3. 每个 MdxParser 实例独立执行二分查找
4. 结果汇总后按词典分组展示

## 致谢

| 项目 | 说明 | 链接 |
|------|------|------|
| **mdict4j** | MDX/MDD 词典格式的 Java 解析库 | [GitHub: eb4j/mdict4j](https://github.com/eb4j/mdict4j) |
| **Linux Kernel LZO** | LZO1X 解压缩算法 | `lib/lzo/lzo1x_decompress_safe.c` |
| **GoldenDict-ng** | 桌面端词典应用，灵感来源 | [GitHub: xiaoyifang/goldendict-ng](https://github.com/xiaoyifang/goldendict-ng) |
| **Material Design 3** | Google 设计系统 | [Material Design 3](https://m3.material.io/) |

## 许可证

GPL-3.0
