# 构建 Gdict Android

## 前置条件

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 34

## 构建步骤

1. 克隆或下载本仓库
2. 在 Android Studio 中打开 `android_project` 目录
3. 等待 Gradle 同步（可能需要几分钟）
4. 连接 Android 设备或启动模拟器
5. 点击 Run 或使用 `./gradlew installDebug`

## 命令行构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建（需要签名配置）
./gradlew assembleRelease

# 运行测试
./gradlew test
```

## 当前功能

- Jetpack Compose MD3 UI
- MDX/MDD 词典解析（V1.2 + V2.0）
- 单词搜索与释义展示
- 生词本与历史记录
- 深色模式
