$ErrorActionPreference = "Continue"
Set-Location "d:\workspace\Gdict\android_project"
$logFile = "d:\workspace\Gdict\gradle_build.log"

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo.FileName = "d:\workspace\Gdict\android_project\gradlew.bat"
$proc.StartInfo.Arguments = "assembleDebug"
$proc.StartInfo.UseShellExecute = $false
$proc.StartInfo.RedirectStandardOutput = $true
$proc.StartInfo.RedirectStandardError = $true
$proc.StartInfo.WorkingDirectory = "d:\workspace\Gdict\android_project"

[void]$proc.Start()
stdout = $proc.StandardOutput.ReadToEnd()
stderr = $proc.StandardError.ReadToEnd()
$proc.WaitForExit()

$outContent = "=== STDOUT ===`n" + stdout + "`n=== STDERR ===`n" + stderr + "`n=== EXIT CODE: $($proc.ExitCode) ==="
[System.IO.File]::WriteAllText($logFile, $outContent, [System.Text.Encoding]::UTF8)
Write-Host "Build log saved to $logFile (exit code: $($proc.ExitCode))"

$apk = "d:\workspace\Gdict\android_project\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    $info = Get-Item $apk
    Write-Host "APK OK: $($info.LastWriteTime.ToString('HH:mm:ss'))  $($info.Length) bytes"
} else {
    Write-Host "APK NOT FOUND - build failed!"
}
