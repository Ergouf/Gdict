---
name: "build-release-apk"
description: "Builds signed release APK for Gdict Android app locally. Invoke when user wants to package, build, or generate a release APK locally."
---

# Build Local Release APK

This skill automates the complete process of building a signed release APK for the Gdict Android project, including environment checks, dependency resolution, and error recovery.

## When to Use

- User wants to build/package a release APK locally
- User asks to generate a signed APK
- User mentions "打包 release", "build release APK", "生成正式包"

## Prerequisites

- JDK 17+ installed and `JAVA_HOME` configured
- Android SDK with API 34 platform and build-tools installed
- `release.keystore` file exists in `android/` directory
- Keystore password known (default alias: `gdict`, password: `gdict123`)

## Complete Build Workflow

### Step 1: Environment Check

Before building, verify the environment is properly configured:

```powershell
# Check JDK
java -version
# Should show JDK 17+

# Check Android SDK exists
Test-Path "D:\workspace\Gdict\android_sdk"
# Should return True

# Check keystore file
Test-Path "D:\workspace\Gdict\android\release.keystore"
# Should return True
```

If any check fails, resolve before proceeding.

### Step 2: Configure local.properties

The `android/local.properties` file MUST exist with correct paths. Use forward slashes for paths in properties files:

```properties
sdk.dir=D:/workspace/Gdict/android_sdk
storeFile=D:/workspace/Gdict/android/release.keystore
storePassword=gdict123
keyAlias=gdict
keyPassword=gdict123
```

**Important**:
- Use forward slashes `/` in paths, NOT backslashes `\\`
- `local.properties` is in `.gitignore` and should NOT be committed
- If the user's SDK is at a different path, ask them for the correct location

To create/update `local.properties`:

```powershell
$content = @"
sdk.dir=D:/workspace/Gdict/android_sdk
storeFile=D:/workspace/Gdict/android/release.keystore
storePassword=gdict123
keyAlias=gdict
keyPassword=gdict123
"@
Set-Content -Path "D:\workspace\Gdict\android\local.properties" -Value $content -NoNewline
```

### Step 3: Configure Gradle Wrapper (Network Fix)

If the Gradle distribution cannot be downloaded from `services.gradle.org` (common in China), switch to a mirror:

Edit `android/gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip
networkTimeout=120000
validateDistributionUrl=false
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Key changes from defaults**:
- `distributionUrl`: Changed from `services.gradle.org` to `mirrors.cloud.tencent.com/gradle`
- `networkTimeout`: Increased from `10000` to `120000` (120 seconds)
- `validateDistributionUrl`: Set to `false` to allow mirror URLs

**If the mirror also fails**, try these alternatives:
- Aliyun: `https://mirrors.aliyun.com/macports/distfiles/gradle/gradle-8.7-bin.zip`
- Huawei: `https://repo.huaweicloud.com/gradle/gradle-8.7-bin.zip`

**After changing the mirror**, clean the incomplete download cache:

```powershell
Remove-Item -Path "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.7-bin" -Recurse -Force -ErrorAction SilentlyContinue
```

### Step 4: Clean Build Cache

Before building, clean any stale build artifacts that may cause errors:

```powershell
Remove-Item -Path "D:\workspace\Gdict\shared\core\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "D:\workspace\Gdict\shared\shared-ui\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "D:\workspace\Gdict\android\app\build" -Recurse -Force -ErrorAction SilentlyContinue
```

This prevents the common error:
```
Cannot access output property 'destinationDirectory' of task ':shared:core:compileKotlin'.
Failed to create MD5 hash for file '...Lzo1xDecompressor.class' as it does not exist.
```

### Step 5: Execute Build

Run the Gradle assembleRelease task. **IMPORTANT**: In PowerShell, use `Start-Process` with output redirection because:
1. `.bat` files cannot be piped in PowerShell (`|` causes "CantActivateDocumentInPipeline" error)
2. Direct `& .\gradlew.bat` output is often truncated in the terminal

```powershell
Start-Process -FilePath ".\gradlew.bat" -ArgumentList "assembleRelease" -Wait -PassThru -NoNewWindow -RedirectStandardOutput "D:\workspace\Gdict\build_stdout.txt" -RedirectStandardError "D:\workspace\Gdict\build_stderr.txt"
```

After the process completes, check both output files:

```powershell
# Check for errors
Get-Content "D:\workspace\Gdict\build_stderr.txt"

# Check build progress
Get-Content "D:\workspace\Gdict\build_stdout.txt" -Tail 30
```

### Step 6: Verify Build Result

Check if the APK was generated:

```powershell
Test-Path "D:\workspace\Gdict\android\app\build\outputs\apk\release\app-release.apk"
```

If successful, get APK details:

```powershell
Get-Item "D:\workspace\Gdict\android\app\build\outputs\apk\release\app-release.apk" | Select-Object Name, Length, LastWriteTime
```

Expected output location: `D:\workspace\Gdict\android\app\build\outputs\apk\release\app-release.apk`

## Error Recovery Guide

### Error 1: Gradle Download Timeout

**Symptom**:
```
Exception in thread "main" java.io.IOException: Downloading from https://services.gradle.org/distributions/gradle-8.7-bin.zip failed: timeout
```

**Fix**:
1. Switch to mirror in `gradle-wrapper.properties` (see Step 3)
2. Clean incomplete download cache: `Remove-Item "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.7-bin" -Recurse -Force`
3. Retry build

### Error 2: Stale Build Cache (MD5 hash error)

**Symptom**:
```
Cannot access output property 'destinationDirectory' of task ':shared:core:compileKotlin'.
Failed to create MD5 hash for file '...class' as it does not exist.
```

**Fix**:
1. Clean build directories (see Step 4)
2. Retry build

### Error 3: SDK Location Not Found

**Symptom**:
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file
```

**Fix**:
1. Ask user for Android SDK path
2. Add `sdk.dir=<path>` to `local.properties` (use forward slashes)
3. Retry build

### Error 4: Keystore Not Found

**Symptom**:
```
Keystore file '...\release.keystore' not found for signing config 'release'.
```

**Fix**:
1. Verify `release.keystore` exists in `android/` directory
2. Ensure `local.properties` has correct `storeFile` path with forward slashes
3. If path contains `android_project` instead of `android`, the `rootProject` resolution is wrong - use explicit path in `local.properties`

### Error 5: Wrong Keystore Password

**Symptom**:
```
com.android.ide.common.signing.KeystoreSigningException: Failed to read key gdict from store
```

**Fix**:
1. Ask user for correct keystore password
2. Update `storePassword` and `keyPassword` in `local.properties`
3. Retry build

### Error 6: Gradle Daemon Issues

**Symptom**: Build hangs or uses stale configuration

**Fix**:
```powershell
& .\gradlew.bat --stop
```
Then retry the build.

## Build Output Cleanup

After a successful build, clean up temporary output files:

```powershell
Remove-Item -Path "D:\workspace\Gdict\build_stdout.txt" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "D:\workspace\Gdict\build_stderr.txt" -Force -ErrorAction SilentlyContinue
```

## Important Notes

- **Do NOT commit** `local.properties` or keystore passwords to Git
- The `gradle-wrapper.properties` mirror change is a local modification - if the original URL works, prefer it
- Build time is typically 3-10 minutes depending on machine and cache state
- The release APK is minified with R8 and signed with the release key
- Version info is auto-generated from Git tags (`versionCode` = commit count, `versionName` = latest tag)
