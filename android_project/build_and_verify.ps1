param(
    [string]$MdxFile = "D:\workspace\Xdictapk\[英-英] 柯林斯第三版 Collins 3rd （非wh_cxh、ldlcau版本）collins 3.mdx",
    [switch]$SkipTest,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Step($msg) {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Write-Ok($msg) {
    Write-Host "[OK] $msg" -ForegroundColor Green
}

function Write-Fail($msg) {
    Write-Host "[FAIL] $msg" -ForegroundColor Red
}

function Write-Info($msg) {
    Write-Host "[INFO] $msg" -ForegroundColor Yellow
}

# Step 1: Check MDX file
Write-Step "Step 1: Check MDX file"
if (Test-Path $MdxFile) {
    $file = Get-Item $MdxFile
    Write-Ok "MDX file found: $($file.Name) ($($file.Length) bytes)"
} else {
    Write-Fail "MDX file not found: $MdxFile"
    Write-Info "Usage: .\build_and_verify.ps1 -MdxFile <path_to_mdx>"
    exit 1
}

# Step 2: Run unit tests
if (-not $SkipTest) {
    Write-Step "Step 2: Run MdxParser unit tests"
    Push-Location $ProjectDir

    $testOutput = & .\gradlew.bat :core:test --info "-Dmdx.file.path=$MdxFile" 2>&1
    $testExit = $LASTEXITCODE

    Pop-Location

    if ($testExit -eq 0) {
        Write-Ok "All MdxParser tests passed"
        $testOutput | Select-String "=== |Results count|wordCount|First 5|Definition|Contains HTML" | ForEach-Object {
            Write-Host "  $($_.Line)" -ForegroundColor Gray
        }
    } else {
        Write-Fail "MdxParser tests FAILED (exit code: $testExit)"
        $testOutput | Select-String "FAIL|ERROR|AssertionError|garbled|GARBLED" | ForEach-Object {
            Write-Host "  $($_.Line)" -ForegroundColor Red
        }

        $reportDir = Join-Path $ProjectDir "core\build\reports\tests\test"
        if (Test-Path $reportDir) {
            Write-Info "Test report: $reportDir\index.html"
        }

        Write-Host "`n" -NoNewline
        $answer = Read-Host "Tests failed. Continue building anyway? (y/N)"
        if ($answer -ne "y" -and $answer -ne "Y") {
            Write-Fail "Build aborted"
            exit 1
        }
    }
} else {
    Write-Info "Tests skipped (-SkipTest)"
}

# Step 3: Build APK
if (-not $SkipBuild) {
    Write-Step "Step 3: Build APK"
    Push-Location $ProjectDir

    & .\gradlew.bat assembleDebug 2>&1 | ForEach-Object { Write-Host $_ }
    $buildExit = $LASTEXITCODE

    Pop-Location

    if ($buildExit -eq 0) {
        $apkPath = Join-Path $ProjectDir "app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $apkPath) {
            $apk = Get-Item $apkPath
            Write-Ok "APK built successfully: $($apk.FullName)"
            Write-Ok "APK size: $([math]::Round($apk.Length / 1MB, 2)) MB"
        } else {
            Write-Fail "APK file not found at expected path"
            exit 1
        }
    } else {
        Write-Fail "APK build FAILED (exit code: $buildExit)"
        exit 1
    }
} else {
    Write-Info "Build skipped (-SkipBuild)"
}

Write-Step "Done"
Write-Ok "All steps completed successfully"
