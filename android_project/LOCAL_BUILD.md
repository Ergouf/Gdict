# 本地构建 APK

## 方法 1：使用 Android Studio（推荐）

1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 在 Android Studio 中打开 `android_project` 目录
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 "Run" 按钮或使用菜单 `Build -> Build Bundle(s) / APK(s) -> Build APK(s)`
6. APK 将位于：`app/build/outputs/apk/debug/app-debug.apk`

## 方法 2：命令行构建

### 前置条件
- JDK 17+
- Android SDK（通过 Android Studio 安装）
- 环境变量设置：
  - `ANDROID_HOME` 指向 Android SDK 目录
  - `JAVA_HOME` 指向 JDK 目录

### 构建步骤

```bash
# 进入项目目录
cd android_project

# 构建 Debug APK
./gradlew assembleDebug

# APK 输出位置
ls -la app/build/outputs/apk/debug/
```

## 方法 3：使用本地构建工具

项目在 `D:\workspace\build-tools` 提供了离线构建工具：

```
build-tools/
├── android-14/          # Android SDK Build Tools
│   ├── aapt2.exe
│   ├── d8.bat
│   ├── apksigner.bat
│   └── ...
├── cmake/               # CMake 4.3.2
└── gradle-8.5/          # Gradle 发行版
```

## 方法 4：GitHub Actions 自动化构建

创建 `.github/workflows/build.yml`：
```yaml
name: Build APK
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v3
      - run: cd android_project && ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: android_project/app/build/outputs/apk/debug/*.apk
```
