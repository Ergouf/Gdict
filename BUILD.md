# 本地构建配置 (Local Build Setup)

## 概述

本文档详细说明在本地开发环境中构建 Gdict Android 应用所需的步骤和配置。

## 前提条件

### 1. JDK 17+

项目需要 JDK 17 或更高版本。推荐使用 OpenJDK。

**Windows 安装方式**：

- 下载 [Eclipse Temurin JDK 17](https://adoptium.net/)
- 或通过 Android Studio 自带的 JDK（位于 `Android Studio安装目录/jbr/`）
- 或下载 Android SDK Commandline Tools 附带的 JDK

**配置 JAVA_HOME**：

在系统环境变量中添加：

```
JAVA_HOME=C:\path\to\jdk-17
```

或在终端中临时设置：

```powershell
# PowerShell
$env:JAVA_HOME = "D:\path\to\jdk-17"

# CMD
set JAVA_HOME=D:\path\to\jdk-17
```

验证安装：

```bash
java -version
# 期望输出: openjdk version "17.0.x" ...
```

### 2. Android SDK

项目需要 Android SDK API 34。

**手动安装**：

1. 下载 [Android SDK Commandline Tools](https://developer.android.com/studio#command-line-tools-only)
2. 解压到目标目录（如 `D:\workspace\Gdict\android_sdk`）
3. 安装必要的 SDK 组件：

```bash
# 进入 SDK 目录
cd D:\workspace\Gdict\android_sdk

# 安装平台和构建工具
cmdline-tools\latest\bin\sdkmanager.bat "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

## 项目配置

### local.properties

在 `android_project/` 目录下创建 `local.properties` 文件（该文件已在 `.gitignore` 中排除）：

```properties
# Android SDK 路径（注意 Windows 路径中的反斜杠需要转义）
sdk.dir=D\\:\\workspace\\Gdict\\android_sdk

# Release 签名配置（以下仅为示例，请替换为实际值）
storeFile=release.keystore
storePassword=<你的 keystore 密码>
keyAlias=gdict
keyPassword=<你的 key 密码>
```

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

### 清理构建

```bash
cd android_project

# 清理所有构建产物
./gradlew clean
```

### Debug 构建

```bash
cd android_project

# 构建 Debug APK（不需要签名）
./gradlew assembleDebug

# APK 输出位置
# app\build\outputs\apk\debug\app-debug.apk
```

### Release 构建

```bash
cd android_project

# 构建 Release APK（需要签名配置）
./gradlew assembleRelease

# APK 输出位置
# app\build\outputs\apk\release\app-release.apk
```

### 运行测试

```bash
cd android_project

# 运行 core 模块单元测试（纯 JVM 测试，无需模拟器）
./gradlew :core:testDebugUnitTest --rerun-tasks

# 指定 MDX 文件路径运行测试
./gradlew :core:testDebugUnitTest -Dmdx.file.path=D:\path\to\dict.mdx
```

## 项目依赖

项目使用 Gradle 管理依赖，主要依赖项：

| 依赖 | 版本 | 用途 |
|------|------|------|
| Kotlin | 1.9.22 | 编程语言 |
| AGP | 8.2.2 | Android Gradle 插件 |
| Jetpack Compose | BOM 2024.01.00 | UI 框架 |
| Navigation Compose | 2.7.6 | 页面导航 |
| Woodstox | - | XML 解析（MDX Header） |

首次构建时 Gradle 会自动下载所有依赖。

## 常见问题

### Q: `JAVA_HOME is not set and no 'java' command could be found`

**原因**：系统未配置 Java 环境变量。

**解决**：
1. 确认已安装 JDK 17+
2. 设置 `JAVA_HOME` 环境变量指向 JDK 安装目录
3. 或将 `java.exe` 所在目录添加到 `PATH`

### Q: `SDK location not found`

**原因**：Gradle 找不到 Android SDK。

**解决**：
1. 确认 `android_project/local.properties` 文件存在
2. 确认 `sdk.dir` 路径正确
3. Windows 路径中反斜杠需要双写：`sdk.dir=D\\:\\path\\to\\sdk`

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
| `local.properties` | 本地 SDK 路径和签名配置 | ❌ (已在 .gitignore) |
| `release.keystore` | 签名密钥文件 | ❌ (已在 .gitignore) |
| `gradle.properties` | Gradle 全局配置 | ✅ |
| `settings.gradle.kts` | 模块配置 | ✅ |
| `build.gradle.kts` | 构建脚本 | ✅ |
