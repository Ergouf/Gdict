# 参与贡献 Gdict

感谢你对 Gdict 的关注！本指南将说明如何参与项目。

## 如何提交 Bug

提交 Bug 时请包含以下信息：

- **Android 版本**（如 Android 14）
- **设备型号**（如 Pixel 7 / 小米 14 / 三星 S24）
- **使用的词典文件**（如 "牛津高阶双解.mdx"，无需分享文件本身）
- **复现步骤** — 你做了什么、期望的结果、实际的结果
- **截图或日志**（如果有的话）

## 如何提功能建议

1. **先搜索已有 Issues** — 你的想法可能已经有人在讨论
2. 如果没有人提过，提交一个新的 Issue，标题和描述尽量清晰
3. 项目维护者会进行评审并打上标签

## 开发环境搭建

### 前提条件

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 21（shared/core）/ JDK 17+（Android & Desktop）（项目 `android_sdk/` 已捆绑 JDK 17）
- Android SDK API 34（项目 `android_sdk/` 已包含）

### 克隆后的第一步

1. 克隆仓库：

   ```bash
   git clone https://github.com/Ergouf/Gdict.git
   cd Gdict/android
   ```

2. 创建 `local.properties`：

   ```properties
   sdk.dir=<你的 Android SDK 路径>
   # 例如 sdk.dir=C\\:\\Users\\<用户名>\\AppData\\Local\\Android\\Sdk
   ```

3. 同步 Gradle 并构建：

   ```bash
   ./gradlew assembleDebug
   ```

4. 详细步骤见 [BUILD.md](./BUILD.md)。

### 项目结构

- `shared/core/` — 核心引擎（MDX 解析器、FSRS 算法、搜索引擎、日志抽象）— 纯 JVM，无平台依赖
- `shared/shared-ui/` — 共享 UI 逻辑（ViewModel、Repository 接口、TTS）— Desktop 使用
- `android/app/` — Android 界面和平台特定实现（Jetpack Compose）
- `desktop/app/` — 桌面端界面和平台特定实现（Compose Multiplatform）

## 代码风格

### 基本规则

- 语言：**Kotlin**
- UI 框架：**Jetpack Compose** + Material Design 3
- 架构：MVVM 模式，使用 **ViewModel + StateFlow**
- 不使用通配符导入（`import package.*`）
- 不需要多余的注释 — 代码本身应自解释

### 提交信息

项目遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 规范：

```
<类型>: <描述>

feat: 新增用户登录功能
fix: 修复搜索结果显示顺序错误
docs: 更新 API 使用指南
refactor: 拆分 AppViewModel 为领域专用 ViewModel
test: 添加 MdxParser 单元测试
```

类型：`feat`（新功能）、`fix`（修复）、`docs`（文档）、`refactor`（重构）、`test`（测试）、`chore`（杂项）、`style`（格式）、`perf`（性能）、`ci`（持续集成）、`build`（构建）

## Pull Request 流程

1. **开工前先讨论** — 写代码前先开一个 Issue 讨论你的想法。这样可以避免你花时间写完后，发现改动不符合项目方向。

2. **从 master 分支** — 用有意义的分支名称：

   ```bash
   git checkout -b feat/flashcard-import
   git checkout -b fix/search-crash
   ```

3. **编写代码** — 遵循上述代码风格规则。

4. **运行测试** — 确保没有破坏已有功能：

   ```bash
   ./gradlew :core:testDebugUnitTest
   ./gradlew assembleDebug
   ```

5. **推送分支并提交 Pull Request**：

   ```bash
   git push origin feat/flashcard-import
   ```

6. **在 PR 描述中说明**：
   - 解决了什么问题（附上 Issue 链接）
   - 做了哪些改动
   - 如果 UI 有变化，附上截图
   - 是否有破坏性变更

7. **等待评审** — 维护者会进行代码评审并提供反馈。

感谢你的贡献！
