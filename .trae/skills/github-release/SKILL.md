---
name: "github-release"
description: "Automates GitHub code push, version tagging, release monitoring, and error recovery. Invoke when user wants to push code, create version tags, monitor CI/CD builds, or fix failed releases."
---

# GitHub Release Automation & Monitoring

This skill provides complete release lifecycle management including automated git operations, CI/CD build monitoring, error analysis, and automatic recovery.

## When to Use

- User wants to push code to GitHub and create a new release
- User needs to check if a GitHub Actions build succeeded or failed
- User requests to download and analyze failed build logs
- User asks to fix build errors and retry the release
- User wants to monitor the entire release process from push to deployment

## Prerequisites

- Git installed (`C:\Program Files\Git\cmd\git.exe` on Windows)
- GitHub repository with remote origin configured
- GitHub Personal Access Token (PAT) with `repo` and `actions` scopes for API access
- Project has `.github/workflows/release.yml` for CI/CD

## Complete Workflow

### Phase 1: Code Push & Tagging (Initial Release)

#### Step 1: Gather Release Information

Confirm with the user:
- **Version number**: Semantic version (e.g., 1.2.2, 2.0.0)
- **Changes summary**: What's included in this release
- **Commit message**: Follow Conventional Commits format

#### Step 2: Execute Git Operations

```powershell
# Use full path to avoid PATH issues
$git = "C:\Program Files\Git\cmd\git.exe"
$projectPath = "<project_directory>"

# Stage all changes
& $git -C $projectPath add -A

# Commit with descriptive message
& $git -C $projectPath commit -m "<type>: <description>"

# Create annotated tag
& $git -C $projectPath tag -a v<version> -m "Release v<version>: <summary>"

# Push code and tags to trigger CI/CD
& $git -C $projectPath push origin master --tags
```

**Success indicators**:
- All commands exit with code 0
- Output shows successful push and tag creation
- GitHub shows new commit and tag in repository

### Phase 2: Build Monitoring & Status Check

#### Step 3: Monitor GitHub Actions Workflow

After pushing, immediately start monitoring the build:

**Method A: Using GitHub CLI (if installed)**
```powershell
# List recent workflow runs
gh run list --limit 5

# Watch specific run status
gh run watch <run_id>
```

**Method B: Using GitHub REST API**
```powershell
$token = "<your_github_pat>"
$headers = @{
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
}

# Get latest workflow run
$response = Invoke-RestMethod -Uri "https://api.github.com/repos/<owner>/<repo>/actions/runs?per_page=1" -Headers $headers
$runId = $response.workflow_runs[0].id
$status = $response.workflow_runs[0].status
$conclusion = $response.workflow_runs[0].conclusion

Write-Host "Run ID: $runId"
Write-Host "Status: $status"
Write-Host "Conclusion: $conclusion"
```

**Status meanings**:
- `queued` → Waiting for runner
- `in_progress` → Building
- `success` ✅ → Build completed successfully
- `failure` ❌ → Build failed (proceed to Phase 3)
- `cancelled` → Build was cancelled

#### Step 4: Polling Strategy

Implement intelligent polling with exponential backoff:

```powershell
$maxAttempts = 30  # Max 30 minutes wait
$attempt = 0
$waitTime = 30  # Start with 30 seconds

do {
    $attempt++
    Write-Host "Checking build status (Attempt $attempt/$maxAttempts)..."

    # Check status using API or CLI
    $status = Get-BuildStatus -RunId $runId

    if ($status -eq "success") {
        Write-Host "✅ Build successful!" -ForegroundColor Green
        break
    }
    elseif ($status -eq "failure") {
        Write-Host "❌ Build failed!" -ForegroundColor Red
        # Proceed to error analysis
        break
    }
    elseif ($status -in @("queued", "in_progress")) {
        Write-Host "⏳ Build in progress... Waiting ${waitTime}s" -ForegroundColor Yellow
        Start-Sleep -Seconds $waitTime
        # Exponential backoff: max 120 seconds
        $waitTime = [Math]::Min($waitTime * 1.5, 120)
    }

} while ($attempt -lt $maxAttempts)
```

### Phase 3: Error Analysis & Log Retrieval (If Failed)

#### Step 5: Download Failed Build Logs

When build fails, automatically retrieve and analyze logs:

**Download all job logs:**
```powershell
$token = "<your_github_pat>"
$owner = "<repo_owner>"
$repo = "<repository_name>"
$runId = "<failed_run_id>"

# Get jobs for this workflow run
$jobsUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId/jobs"
$jobsResponse = Invoke-RestMethod -Uri $jobsUrl -Headers @{Authorization="token $token"}

foreach ($job in $jobsResponse.jobs) {
    Write-Host "`n=== Job: $($job.name) ==="
    Write-Host "Status: $($job.conclusion)"

    # Download logs for each step
    foreach ($step in $job.steps) {
        if ($step.conclusion -eq "failure") {
            Write-Host "`n❌ Failed Step: $($step.name)" -ForegroundColor Red

            # Get step logs (requires specific API endpoint)
            $logsUrl = "https://api.github.com/repos/$owner/$repo/actions/jobs/$($job.id)/logs"
            Invoke-RestMethod -Uri $logsUrl -Headers @{Authorization="token $token"} -OutFile "build_logs_$($job.name)_$($step.number).txt"

            Write-Host "Logs saved to: build_logs_$($job.name)_$($step.number).txt"
        }
    }
}
```

**Alternative: Download full workflow logs as ZIP**
```powershell
# Download complete logs archive
$logsZipUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId/logs"
Invoke-RestMethod -Uri $logsZipUrl -Headers @{Authorization="token $token"} -OutFile "workflow_logs_$runId.zip"

Write-Host "Full logs downloaded: workflow_logs_$runId.zip"
```

#### Step 6: Analyze Error Patterns

Automatically scan logs for common error categories:

**Common Build Failures & Detection Patterns:**

| Error Type | Pattern/Keyword | Typical Fix |
|------------|----------------|-------------|
| **Compilation Error** | `error:`, `cannot find symbol`, `unresolved dependency` | Fix syntax, add missing dependencies |
| **Test Failure** | `Tests failed:`, `AssertionError`, `expected:<X> but was:<Y>` | Fix failing tests |
| **Resource Not Found** | `404 Not Found`, `file not found`, `ENOENT` | Add missing files, fix paths |
| **Permission Denied** | `Permission denied`, `403 Forbidden`, `access denied` | Fix token permissions, update secrets |
| **Timeout** | `timeout`, `exceeded time limit`, `build timed out` | Optimize build, increase timeout |
| **Out of Memory** | `OutOfMemoryError`, `heap space`, `OOM` | Increase JVM memory, optimize code |
| **Dependency Conflict** | `dependency conflict`, `version conflict`, `duplicate class` | Resolve dependency versions |
| **Signing Error** | `signing failed`, `keystore`, `certificate` | Fix signing configuration |
| **Gradle/Kotlin Error** | `FAILURE: Build failed`, `Kotlin compiler error` | Fix Gradle config, Kotlin syntax |

**Log Analysis Script:**
```powershell
function Analyze-BuildLogs {
    param([string]$logFile)

    $content = Get-Content $logFile -Raw

    # Detect error patterns
    $errors = @()

    if ($content -match "error:\s*(.+)") { $errors += "Compilation: $($Matches[1])" }
    if ($content -match "Tests failed:\s*(\d+)") { $errors += "Test Failure: $($Matches[1]) tests failed" }
    if ($content -match "(?:OutOfMemoryError|heap space)") { $errors += "OOM: Out of memory" }
    if ($content -match "(?:timeout|timed out)") { $errors += "Timeout: Build exceeded time limit" }
    if ($content -match "(?:Permission denied|403 Forbidden)") { $errors += "Permission: Access denied" }
    if ($content -match "(?:signing failed|keystore)") { $errors += "Signing: Certificate/keystore issue" }
    if ($content -match "FAILURE: Build failed") { $errors += "Gradle: Build configuration error" }

    return $errors
}

# Usage
$errorTypes = Analyze-BuildLogs -logFile "workflow_logs_$runId.zip"
Write-Host "`n🔍 Detected Errors:" -ForegroundColor Cyan
$errorTypes | ForEach-Object { Write-Host "  • $_" -ForegroundColor Yellow }
```

### Phase 4: Automated Error Recovery & Rebuild

#### Step 7: Implement Fixes Based on Error Type

**Fix Strategies by Error Category:**

##### A. Compilation/Syntax Errors
```powershell
# 1. Read error details from log
# 2. Identify file and line number
# 3. Open file and navigate to error location
# 4. Apply fix based on error message
# 5. Run local build to verify: ./gradlew build
```

##### B. Test Failures
```powershell
# 1. Identify which test(s) failed
# 2. Review test output for assertion details
# 3. Fix either test expectations or production code
# 4. Run specific test locally: ./gradlew test --tests "com.example.FailingTest"
```

##### C. Missing Resources/Files
```powershell
# 1. Check what file is missing
# 2. Verify file exists in repository
# 3. If missing, create or restore file
# 4. Ensure file is tracked by git: git add <file>
```

##### D. Dependency Issues
```powershell
# 1. Check build.gradle.kts for problematic dependencies
# 2. Resolve version conflicts
# 3. Update dependency versions if needed
# 4. Clean and rebuild: ./gradlew clean build
```

##### E. Configuration Issues (JVM args, Signing)
```powershell
# Example: Fix JVM arguments in desktop/app/build.gradle.kts
# Example: Update keystore paths or credentials in secrets
```

#### Step 8: Commit Fix & Trigger Rebuild

After applying fixes:

```powershell
# Commit the fix
& $git -C $projectPath add -A
& $git -C $projectPath commit -m "fix(ci): resolve build failure - <error_summary>"

# Option A: Push to same branch (updates existing release)
& $git -C $projectPath push origin master

# Option B: Delete old tag, recreate, and push (if tag-specific issue)
& $git -C $projectPath tag -d v<version>
& $git -C $projectPath push origin :refs/tags/v<version>
& $git -C $projectPath tag -a v<version> -m "Release v<version> (fixed)"
& $git -C $projectPath push origin master --tags
```

#### Step 9: Monitor Rebuild & Validate Success

Return to **Phase 2 (Step 3)** to monitor the new build.

**Success Criteria Checklist:**
- [ ] GitHub Actions shows `success` (green checkmark)
- [ ] Artifacts generated (APK, EXE, etc.)
- [ ] Release published to GitHub Releases page
- [ ] Download and verify artifacts work correctly

## Advanced Features

### Automatic Retry Logic

```powershell
$maxRetries = 3
$retryCount = 0

do {
    $retryCount++
    Write-Host "`n🔄 Attempt $retryCount of $maxRetries"

    # Execute push and monitor
    Execute-ReleasePush -Version $version -Message $commitMessage
    $buildResult = Monitor-BuildStatus -MaxWaitMinutes 30

    if ($buildResult -eq "success") {
        Write-Host "✅ Release v$version completed successfully!" -ForegroundColor Green
        break
    }
    else {
        Write-Host "⚠️ Build failed. Starting error recovery..." -ForegroundColor Yellow

        # Download and analyze logs
        Download-BuildLogs -RunId $currentRunId
        $errors = Analyze-BuildErrors

        # Attempt automatic fix
        $fixApplied = Apply-AutomaticFix -ErrorType $errors

        if (-not $fixApplied) {
            Write-Host "❌ Cannot auto-fix. Manual intervention required." -ForegroundColor Red
            # Ask user for guidance
            break
        }
    }

} while ($retryCount -lt $maxRetries)
```

### Build History Tracking

Maintain a local log of all release attempts:

```powershell
$logEntry = @{
    timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    version = $version
    status = $buildResult
    run_id = $runId
    errors = $errors -join "; "
    fix_applied = $fixDescription
}

# Append to release history file
$logEntry | ConvertTo-Json | Out-File -Append -FilePath ".trae/release_history.json"
```

## Integration with Project Tools

### For Gdict Project Specifics

**Project Configuration:**
- **Workflow file**: `.github/workflows/release.yml`
- **Artifacts produced**: APK (Android), EXE (Windows Desktop)
- **Common issues**: Edge TTS WebSocket, JVM launch parameters, MDX parser memory

**Quick Reference Commands:**

```powershell
# Local build test (before pushing)
cd android && ./gradlew assembleRelease
cd desktop && ./gradlew packageReleaseDistribution

# Test APK installation
adb install app/build/outputs/apk/release/app-release.apk

# Test EXE execution
.\desktop\app\build\compose\binaries\main\exe\Gdict.exe
```

## Best Practices

### Pre-Push Validation
1. ✅ Run local build successfully: `./gradlew build`
2. ✅ Run all tests: `./gradlew test`
3. ✅ Verify no uncommitted critical files
4. ✅ Check `CHANGELOG.md` is updated
5. ✅ Confirm version number follows semantic versioning

### Post-Release Verification
1. ✅ Download artifacts from GitHub Releases
2. ✅ Install/test APK on physical device or emulator
3. ✅ Run EXE on clean Windows machine
4. ✅ Verify all features work (TTS, dictionary lookup, etc.)
5. ✅ Announce release to users (if applicable)

### Security Considerations
- 🔒 Store GitHub PAT in environment variable, not in code
- 🔄 Rotate tokens regularly
- 📝 Use minimal required scopes (`repo`, `actions`)
- 🚫 Never commit secrets to repository

## Troubleshooting Guide

### Common Issues & Solutions

**Issue**: Cannot authenticate with GitHub API
```
Solution:
1. Generate new PAT at github.com/settings/tokens
2. Set environment variable: $env:GITHUB_TOKEN = "ghp_xxxx"
3. Verify token has 'repo' and 'actions' scopes
```

**Issue**: Build keeps failing with same error
```
Solution:
1. Check if error is in CI environment only
2. Reproduce locally with same conditions
3. Check for flaky tests or race conditions
4. Review recent changes that might cause issue
```

**Issue**: Timeout waiting for build
```
Solution:
1. Check if workflow is actually triggered
2. Verify GitHub Actions runners are available
3. Check GitHub status page for outages
4. Consider splitting large workflow into smaller jobs
```

## Example Complete Session

**User**: "Push v1.3.0 and make sure it builds successfully"

**Assistant Execution**:
```
✅ Phase 1: Pushing code...
   ✓ Committed changes: "feat: add dark mode support"
   ✓ Created tag: v1.3.0
   ✓ Pushed to origin/master

✅ Phase 2: Monitoring build...
   ⏳ Run #1234 started (queued)
   ⏳ Run #1234 in progress...
   ❌ Run #1234 FAILED after 8m 32s

✅ Phase 3: Analyzing failure...
   📥 Downloaded logs: workflow_logs_1234.zip
   🔍 Detected error: Compilation error in MainActivity.kt line 45
   💡 Root cause: Missing import statement

✅ Phase 4: Applying fix...
   🔧 Added import: import androidx.compose.material.DarkTheme
   📝 Committed fix: "fix(ci): resolve compilation error in MainActivity"
   🚀 Retrying build...

✅ Phase 2: Monitoring rebuild...
   ⏳ Run #1235 started
   ✅ Run #1235 SUCCESS after 10m 15s

🎉 Release v1.3.0 completed successfully!
   📦 Artifacts: app-release.apk (12.3 MB), Gdict.exe (45.7 MB)
   🔗 Release URL: https://github.com/user/Gdict/releases/tag/v1.3.0
```

---

**Remember**: This skill handles the entire release lifecycle - not just pushing code, but ensuring the release actually succeeds and recovering from failures automatically!