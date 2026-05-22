$env:JAVA_HOME = "C:\Users\fengjl\.jdks\temurin-21"
Set-Location "d:\workspace\Gdict\android_project"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "d:\workspace\Gdict\android_project\gradlew.bat"
$psi.Arguments = "assembleDebug --no-daemon --console=plain"
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.WorkingDirectory = "d:\workspace\Gdict\android_project"
$psi.EnvironmentVariables["JAVA_HOME"] = $env:JAVA_HOME

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()

$stdoutTask = $proc.StandardOutput.ReadToEndAsync()
$stderrTask = $proc.StandardError.ReadToEndAsync()
$proc.WaitForExit()

$stdout = $stdoutTask.Result
$stderr = $stderrTask.Result

$allOutput = "STDOUT:`n" + stdout + "`n`nSTDERR:`n" + stderr + "`n`nEXIT CODE: $($proc.ExitCode)"
[System.IO.File]::WriteAllText("d:\workspace\Gdict\build_result.txt", $allOutput, [System.Text.Encoding]::UTF8)

Write-Host "Exit Code: $($proc.ExitCode)"

$apk = "d:\workspace\Gdict\android_project\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    $i = Get-Item $apk
    Write-Host "APK OK: $($i.LastWriteTime.ToString('HH:mm:ss'))  $($i.Length) bytes"
} else {
    Write-Host "NO APK"
}
