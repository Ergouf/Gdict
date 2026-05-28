# 本地构建配置 (Local Build Setup)

## 概述

本文档详细说明在本地开发环境中构建 Gdict Android 应用所需的步骤和配置。

> **已集成开发环境**：项目 `android_sdk/` 目录已捆绑 JDK 17 (`jdk-17.0.12+7`) 和 Android SDK (API 34)，开箱即用，无需额外安装。

## 前提条件

### 1. JDK 17+

项目需要 JDK 17 或更高版本。

> **注意**：`shared/core` 模块使用 `jvmToolchain(21)`，需要 JDK 21。Android 和 Desktop 模块使用 JDK 17。

**使用项目捆绑的 JDK（推荐）**：

项目 `android_sdk/jdk-17.0.12+7/` 已包含 JDK 17，构建时设置 `JAVA_HOME` 指向该目录即可：

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
```

**或自行安装**：

- 下载 [Eclipse Temurin JDK 17](https://adoptium.net/)
- 或通过 Android Studio 自带的 JDK（位于 `Android Studio安装目录/jbr/`）

**配置 JAVA_HOME**：

在系统环境变量中添加：

```
JAVA_HOME=C:\path\to\jdk-17
```

或在终端中临时设置：

```powershell
# PowerShell
$env:JAVA_HOME = "D:\path\to\jdk-17"
```

验证安装：

```bash
java -version
# 期望输出: openjdk version "17.0.x" ...
```

### 2. Android SDK

项目需要 Android SDK API 34。

**使用项目捆绑的 SDK（推荐）**：

项目 `android_sdk/` 已包含完整 SDK，包含 `platforms/android-34` 和 `build-tools/34.0.0`。

**或手动安装**：

1. 下载 [Android SDK Commandline Tools](https://developer.android.com/studio#command-line-tools-only)
2. 解压到目标目录（如 `D:\workspace\Gdict\android_sdk`）
3. 安装必要的 SDK 组件：

```bash
cmdline-tools\latest\bin\sdkmanager.bat "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

## 项目配置

### local.properties

在 `android/` 目录下创建 `local.properties` 文件（该文件已在 `.gitignore` 中排除）：

```properties
# Android SDK 路径（注意 Windows 路径中的反斜杠需要转义）
sdk.dir=D\\:\\workspace\\Gdict\\android_sdk

# Release 签名配置（可选，仅 Release 构建需要）
storeFile=release.keystore
storePassword=<你的 keystore 密码>
keyAlias=gdict
keyPassword=<你的 key 密码>
```

> **注意**：如果使用项目捆绑的 SDK，路径为 `D\\:\\workspace\\Gdict\\android_sdk`。

### Release 密钥生成

如果需要构建 Release APK，需要先生成签名密钥：

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias gdict \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

> **注意**：`release.keystore` 文件已在 `.gitignore` 中排除，不会提交到版本控制。请妥善保管。

## 构建命令

### PowerShell 环境变量

> **重要**：在 PowerShell 中，批处理文件不能直接通过管道传递。构建时请设置环境变量后直接运行：

```powershell
# 设置环境变量并构建
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
$env:ANDROID_HOME = "D:\workspace\Gdict\android_sdk"
& D:\workspace\Gdict\android\gradlew.bat -p D:\workspace\Gdict\android assembleDebug
```

### 清理构建

```bash
cd android

# 清理所有构建产物
./gradlew clean
```

### Debug 构建

```bash
cd android

# 构建 Debug APK（不需要签名）
./gradlew assembleDebug

# APK 输出位置
# app\build\outputs\apk\debug\app-debug.apk
```

### Release 构建

```bash
cd android

# 构建 Release APK（需要签名配置）
./gradlew assembleRelease

# APK 输出位置
# app\build\outputs\apk\release\app-release.apk
```

### 运行测试

```bash
cd android

# 运行 core 模块单元测试（纯 JVM 测试，无需模拟器）
./gradlew :core:testDebugUnitTest --rerun-tasks

# 指定 MDX 文件路径运行测试
./gradlew :core:testDebugUnitTest -Dmdx.file.path=D:\path\to\dict.mdx
```

## Desktop 构建

### 前提条件

- JDK 17+
- Gradle 8.5+（项目自带 wrapper）

### 构建 Desktop 应用

```bash
cd desktop

# 构建 Desktop 应用
./gradlew run

# 打包分发
./gradlew packageDistributable
```

### Desktop 构建说明

Desktop 应用基于 Compose Multiplatform，使用 JCEF（Java Chromium Embedded Framework）渲染词典 HTML 内容。

- 构建产物位于 `app/build/compose/binaries/`
- 首次构建会自动下载 JCEF 运行时
- 数据存储在用户主目录 `~/.gdict/` 下

## 项目依赖

项目使用 Gradle 管理依赖，主要依赖项：

### Android 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.1.0 | 编程语言 |
| AGP | 8.2.2 | Android Gradle 插件 |
| Jetpack Compose | BOM 2024.02.02 | UI 框架 |
| Navigation Compose | 2.7.7 | 页面导航 |
| DataStore | 1.0.0 | 本地键值存储 |
| Material Icons Extended | 1.6.3 | Material Design 图标库 |

### Desktop 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.1.0 | 编程语言 |
| Compose Multiplatform | 1.7.3 | 桌面 UI 框架 |
| JCEF | 126.2.0 | HTML 渲染引擎 |

### 共享模块依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| kotlinx-coroutines-core | 1.8.1 | 协程支持 |
| org.json | 20231013 | JSON 解析 |
| jcefmaven | 126.2.0 | JCEF 浏览器引擎 (Desktop) |
| jlayer | 1.0.1 | MP3 音频播放 (Desktop) |

首本次构建时 Gradle 会自动下载所有依赖。

## 常见问题

### Q: `JAVA_HOME is not set and no 'java' command could be found`

**原因**：系统未配置 Java 环境变量。

**解决**：
1. 确认已安装 JDK 17+，或使用项目捆绑的 JDK
2. 在运行 gradlew 前设置 `$env:JAVA_HOME`
3. 或将 `java.exe` 所在目录添加到 `PATH`

### Q: `SDK location not found`

**原因**：Gradle 找不到 Android SDK。

**解决**：
1. 确认 `android/local.properties` 文件存在
2. 确认 `sdk.dir` 路径正确（Windows 路径反斜杠需双写：`sdk.dir=D\\:\\path\\to\\sdk`）
3. 或设置 `$env:ANDROID_HOME` 环境变量

### Q: PowerShell中运行gradlew.bat报 `CantActivateDocumentInPipeline`

**原因**：PowerShell 不允许在管道中运行 `.bat` 文件。

**解决**：不使用管道，直接调用：

```powershell
& D:\workspace\Gdict\android\gradlew.bat -p D:\workspace\Gdict\android assembleDebug
```

### Q: `Unresolved reference: BuildConfig`

**原因**：AGP 8.x 默认关闭 `BuildConfig` 生成。

**解决**：已在 `app/build.gradle.kts` 中启用：
```kotlin
buildFeatures {
    buildConfig = true
}
```

### Q: `Compilation error`

**原因**：Kotlin 编译错误。

**解决**：
1. 检查 JDK 版本是否为 17+
2. 尝试 `./gradlew clean` 后重新构建
3. 检查 IDE 中的 Kotlin 插件版本

### Q: `BUILD FAILED` 但无详细错误信息

**解决**：使用 `--info` 或 `--stacktrace` 获取详细信息：

```bash
./gradlew assembleDebug --info
./gradlew assembleDebug --stacktrace
```

### Q: 构建过程中 Gradle 下载速度慢

**解决**：配置 Gradle 镜像（中国大陆用户）：

在 `~/.gradle/init.gradle` 中添加：

```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public/' }
        maven { url 'https://maven.aliyun.com/repository/google/' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin/' }
        mavenCentral()
        google()
    }
}
```

## 文件说明

| 文件 | 说明 | 是否提交 Git |
|------|------|-------------|
| `android_sdk/` | 捆绑的 Android SDK + JDK 17 | ❌ (已在 .gitignore) |
| `local.properties` | 本地 SDK 路径和签名配置 | ❌ (已在 .gitignore) |
| `release.keystore` | 签名密钥文件 | ❌ (已在 .gitignore) |
| `gradle.properties` | Gradle 全局配置 | ✅ |
| `settings.gradle.kts` | 模块配置 | ✅ |
| `build.gradle.kts` | 构建脚本 | ✅ |
