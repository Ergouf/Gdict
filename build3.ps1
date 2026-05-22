$ErrorActionPreference = "Stop"
$logFile = "d:\workspace\Gdict\build_log.txt"
Set-Location "d:\workspace\Gdict\android_project"

try {
    & .\gradlew.bat assembleDebug --stacktrace 2>&1 | Tee-Object -FilePath $logFile
    Write-Host "EXIT CODE: $LASTEXITCODE"
} catch {
    Write-Host "ERROR: $_"
}

Write-Host ""
Write-Host "=== APK Check ==="
$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    $info = Get-Item $apk
    Write-Host "APK: $($info.LastWriteTime) $($info.Length) bytes"
} else {
    Write-Host "APK NOT FOUND"
}
