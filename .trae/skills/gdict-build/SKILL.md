---
name: "gdict-build"
description: "Builds Gdict project: Android debug/release APK, Desktop AppImage, MSIX. Invoke when user wants to build, package, compile, or generate any distributable for Gdict."
---

# Gdict Local Build Skill

Complete local build automation for the Gdict project covering all platforms and build types.

## When to Use

- User wants to build debug APK, release APK, desktop exe, or MSIX package
- User says "打包", "build", "compile", "生成apk", "打包exe", "构建"
- Any request to produce a distributable artifact from the Gdict codebase

## Prerequisites

| Resource | Path | Notes |
|----------|------|-------|
| JDK 17 | `D:\workspace\Gdict\android_sdk\jdk-17.0.12+7` | Bundled, required for all builds |
| Android SDK | `D:\workspace\Gdict\android_sdk` | API 34, build-tools 34.0.0 |
| local.properties | `D:\workspace\Gdict\android\local.properties` | SDK path + signing config |
| release.keystore | `D:\workspace\Gdict\android\release.keystore` | For release APK signing |

> **Critical**: Desktop project also requires JDK 17 (NOT 21) because `shared/core` module uses `jvmToolchain(21)` but the desktop `app/build.gradle.kts` uses `jvmToolchain(17)`. Use the bundled JDK 17 for all builds.

## Environment Setup

Every build command MUST set `JAVA_HOME` before invoking Gradle. Use `Start-Process` with Java directly (NOT .bat files) to avoid PowerShell pipeline issues.

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
```

### Why Start-Process with Java?

PowerShell has issues with .bat files:
1. `& .\gradlew.bat` output gets swallowed (no visible output)
2. `.\gradlew.bat | ForEach-Object` causes `CantActivateDocumentInPipeline` error
3. `Start-Process -FilePath ".\gradlew.bat"` causes "终止批处理操作吗(Y/N)?" prompt

**Correct approach**: Invoke Java directly with the Gradle wrapper jar:

```powershell
$javaExe = "$env:JAVA_HOME\bin\java.exe"
$wrapperJar = "<project>\gradle\wrapper\gradle-wrapper.jar"
$proc = Start-Process -FilePath $javaExe `
    -ArgumentList "-classpath",$wrapperJar,"org.gradle.wrapper.GradleWrapperMain","<tasks>","--no-daemon" `
    -WorkingDirectory "<project>" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
Write-Output "Exit code: $($proc.ExitCode)"
```

After completion, read results from the log files:

```powershell
Get-Content "D:\workspace\Gdict\build_stdout.txt" -Tail 20
Get-Content "D:\workspace\Gdict\build_stderr.txt"
```

## Build Types

### 1. Android Debug APK

**Output**: `D:\workspace\Gdict\android\app\build\outputs\apk\debug\app-debug.apk`

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\android\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain","assembleDebug","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\android" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
Write-Output "Exit code: $($proc.ExitCode)"
# Verify:
Get-Item "D:\workspace\Gdict\android\app\build\outputs\apk\debug\app-debug.apk" | Select-Object Name, Length
```

### 2. Android Release APK

**Output**: `D:\workspace\Gdict\android\app\build\outputs\apk\release\app-release.apk`

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\android\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain","assembleRelease","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\android" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
Write-Output "Exit code: $($proc.ExitCode)"
# Verify:
Get-Item "D:\workspace\Gdict\android\app\build\outputs\apk\release\app-release.apk" | Select-Object Name, Length
```

**Prerequisites for release build**: `local.properties` must contain signing config:

```properties
sdk.dir=D:/workspace/Gdict/android_sdk
storeFile=D:/workspace/Gdict/android/release.keystore
storePassword=gdict123
keyAlias=gdict
keyPassword=gdict123
```

### 3. Desktop AppImage (Portable Exe Directory)

**Output**: `D:\workspace\Gdict\desktop\app\build\compose\binaries\main\app\Gdict\Gdict.exe`

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\desktop\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain",":app:packageAppImage","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\desktop" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
Write-Output "Exit code: $($proc.ExitCode)"
# Verify:
Get-ChildItem "D:\workspace\Gdict\desktop\app\build\compose\binaries\main\app\Gdict" -Filter "*.exe" | Select-Object Name, Length
```

### 4. Desktop MSIX Package

**Output**: `D:\workspace\Gdict\desktop\app\build\compose\binaries\main\msix\`

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\desktop\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain",":app:packageMsix","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\desktop" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
Write-Output "Exit code: $($proc.ExitCode)"
```

> **Note**: MSIX build requires WiX Toolset. The `packageMsix` task depends on `packageAppImage` and runs a PowerShell script from `desktop/msix/package.ps1`.

### 5. Clean Build

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
# Android clean
$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\android\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain","clean","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\android" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
# Desktop clean
$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\desktop\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain","clean","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\desktop" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
```

### 6. Run Unit Tests

```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\android\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain",":shared:core:testDebugUnitTest","--rerun-tasks","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\android" `
    -Wait -NoNewWindow -PassThru `
    -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" `
    -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
```

## Output Locations Summary

| Build Type | Output Path |
|------------|-------------|
| Debug APK | `android\app\build\outputs\apk\debug\app-debug.apk` |
| Release APK | `android\app\build\outputs\apk\release\app-release.apk` |
| Desktop AppImage | `desktop\app\build\compose\binaries\main\app\Gdict\Gdict.exe` (self-contained directory) |
| Desktop MSIX | `desktop\app\build\compose\binaries\main\msix\` |

## Error Recovery

### Error: "Cannot find a Java installation matching: {languageVersion=17}"

**Cause**: `JAVA_HOME` points to wrong JDK version (e.g., JDK 21 instead of 17).

**Fix**: Always use the bundled JDK 17:
```powershell
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
```

### Error: "SDK location not found"

**Fix**: Ensure `android/local.properties` exists with:
```properties
sdk.dir=D:/workspace/Gdict/android_sdk
```

### Error: "Keystore file not found"

**Fix**: Ensure `release.keystore` exists at `D:\workspace\Gdict\android\release.keystore` and `local.properties` has correct `storeFile` path.

### Error: Gradle download timeout

**Fix**: Switch to mirror in `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip
```

### Error: Stale build cache (MD5 hash error)

**Fix**: Clean build directories:
```powershell
Remove-Item "D:\workspace\Gdict\shared\core\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "D:\workspace\Gdict\shared\shared-ui\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "D:\workspace\Gdict\android\app\build" -Recurse -Force -ErrorAction SilentlyContinue
```

### Error: Gradle daemon stuck

**Fix**:
```powershell
& "$env:JAVA_HOME\bin\java.exe" "-classpath" "D:\workspace\Gdict\android\gradle\wrapper\gradle-wrapper.jar" "org.gradle.wrapper.GradleWrapperMain" "--stop" "-p" "D:\workspace\Gdict\android"
```

## Cleanup

After build, clean up temporary log files:

```powershell
Remove-Item "D:\workspace\Gdict\build_stdout.txt" -Force -ErrorAction SilentlyContinue
Remove-Item "D:\workspace\Gdict\build_stderr.txt" -Force -ErrorAction SilentlyContinue
```

## Quick Reference: Parallel Builds

To build Android and Desktop simultaneously, run both in separate terminals:

```powershell
# Terminal 1: Android
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\android\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain","assembleDebug","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\android" -Wait -NoNewWindow `
    -RedirectStandardOutput "D:\workspace\Gdict\build_android.log" `
    -RedirectStandardError "D:\workspace\Gdict\build_android_err.log"

# Terminal 2: Desktop
$env:JAVA_HOME = "D:\workspace\Gdict\android_sdk\jdk-17.0.12+7"
Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-classpath","D:\workspace\Gdict\desktop\gradle\wrapper\gradle-wrapper.jar","org.gradle.wrapper.GradleWrapperMain",":app:packageAppImage","--no-daemon" `
    -WorkingDirectory "D:\workspace\Gdict\desktop" -Wait -NoNewWindow `
    -RedirectStandardOutput "D:\workspace\Gdict\build_desktop.log" `
    -RedirectStandardError "D:\workspace\Gdict\build_desktop_err.log"
```
