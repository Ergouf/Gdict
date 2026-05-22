$ErrorActionPreference = "Continue"
cd d:\workspace\Gdict\android_project
$output = & .\gradlew.bat assembleDebug 2>&1
$outFile = "d:\workspace\Gdict\build_output.txt"
$output | Out-File -FilePath $outFile -Encoding UTF8
Write-Host "Build output saved to $outFile"
Write-Host "Exit code: $LASTEXITCODE"

$apk = "d:\workspace\Gdict\android_project\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    $info = Get-Item $apk
    Write-Host "APK OK: $($info.LastWriteTime) $($info.Length) bytes"
} else {
    Write-Host "APK NOT FOUND"
}
