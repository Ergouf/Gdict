$env:JAVA_HOME = "C:\Users\fengjl\.jdks\temurin-21"
Set-Location "d:\workspace\Gdict\android_project"
$logFile = "d:\workspace\Gdict\build_result.txt"

$output = & .\gradlew.bat assembleDebug --no-daemon 2>&1
[System.IO.File]::WriteAllText($logFile, ($output -join "`r`n"), [System.Text.Encoding]::UTF8)

Write-Host "Gradle Exit Code: $LASTEXITCODE"
Write-Host ""

$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    $i = Get-Item $apk
    Write-Host "APK BUILT SUCCESSFULLY!"
    Write-Host "  Time: $($i.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))"
    Write-Host "  Size: $($i.Length) bytes"
} else {
    Write-Host "APK NOT FOUND - build failed"
    Write-Host ""
    Write-Host "--- Last 30 lines of build log ---"
    $lines = [System.IO.File]::ReadAllLines($logFile)
    $start = [Math]::Max(0, $lines.Length - 30)
    for ($i = $start; $i -lt $lines.Length; $i++) {
        Write-Host $lines[$i]
    }
}
