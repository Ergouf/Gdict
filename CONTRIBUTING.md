# Contributing to Gdict

Thanks for your interest in contributing! This guide outlines the process.

## How to Report a Bug

Please include the following information:

- **Android version** (e.g. Android 14)
- **Device model** (e.g. Pixel 7 / Samsung S24)
- **Which dictionary file** you're using (e.g. Oxford.mdx, no need to share the file itself)
- **Steps to reproduce** — what you did, what you expected, what actually happened
- **Screenshots or logs** if available

## How to Suggest a Feature

1. **Search existing Issues** first — your idea may already be discussed
2. If no similar issue exists, open a new one with a clear title and description
3. The maintainer will review and tag it

## Development Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 21 (shared/core) / JDK 17+ (Android & Desktop) (project bundles JDK in `android_sdk/jdk-17.0.12+7/`)
- Android SDK API 34 (project bundles SDK in `android_sdk/`)

### First Steps After Cloning

1. Clone the repository:

   ```bash
   git clone https://github.com/Ergouf/Gdict.git
   cd Gdict/android
   ```

2. Create `local.properties`:

   ```properties
   sdk.dir=<your Android SDK path>
   # e.g. sdk.dir=C\\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```

3. Sync Gradle and build:

   ```bash
   ./gradlew assembleDebug
   ```

4. See [BUILD.md](./BUILD.md) for detailed instructions.

### Project Structure

- `shared/core/` — Core engine (MDX parser, FSRS, search, logging abstraction) — pure JVM, no platform dependency
- `shared/shared-ui/` — Shared UI logic (ViewModels, Repository interfaces, TTS) — used by Desktop
- `android/app/` — Android UI & platform-specific implementations (Jetpack Compose)
- `desktop/app/` — Desktop UI & platform-specific implementations (Compose Multiplatform)

## Code Style

### General Rules

- Language: **Kotlin**
- UI framework: **Jetpack Compose** with Material Design 3
- Architecture: MVVM with **ViewModel + StateFlow**
- No wildcard imports
- No unnecessary comments — code should be self-documenting

### Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <description>

feat: add user login
fix: correct search result ordering
docs: update API usage guide
refactor: split AppViewModel into domain ViewModels
test: add unit tests for MdxParser
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `style`, `perf`, `ci`, `build`

## Pull Request Process

1. **Discuss first** — Open an Issue to discuss your idea before writing code. This avoids wasted effort if the change doesn't fit the project direction.

2. **Branch off master** — Use a descriptive branch name:

   ```bash
   git checkout -b feat/flashcard-import
   git checkout -b fix/search-crash
   ```

3. **Write your code** — Follow the code style rules above.

4. **Run tests** to make sure nothing is broken:

   ```bash
   ./gradlew :core:testDebugUnitTest
   ./gradlew assembleDebug
   ```

5. **Push your branch** and open a Pull Request:

   ```bash
   git push origin feat/flashcard-import
   ```

6. **Describe your PR** — Include:
   - What problem it solves (link to Issue)
   - What changes were made
   - Screenshots if UI is affected
   - Any breaking changes

7. **Wait for review** — The maintainer will review and provide feedback.

Thanks for contributing!
