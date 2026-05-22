$env:JAVA_HOME = "C:\Users\fengjl\.jdks\temurin-21"
Set-Location "d:\workspace\Gdict\android_project"

Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "Java exists: $(Test-Path "$env:JAVA_HOME\bin\java.exe")"

# Use shell execute for .bat files
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "d:\workspace\Gdict\android_project\gradlew.bat"
$psi.Arguments = "assembleDebug --no-daemon --info 2>&1"
$psi.UseShellExecute = $true
$psi.WorkingDirectory = "d:\workspace\Gdict\android_project"

# Create temp file to capture output
$tempOut = "$env:TEMP\gradle_out_$(Get-Random).txt"
$psi.Arguments = "assembleDebug --no-daemon > `"$tempOut`" 2>&1"

$p = New-Object System.Diagnostics.Process
$p.StartInfo = $psi

try {
    [void]$p.Start()
    $timeouted = !$p.WaitForExit(300000) # 5 min timeout
    
    if ($timeouted) {
        Write-Host "BUILD TIMED OUT after 5 minutes!"
        $p.Kill()
    } else {
        Write-Host "Exit Code: $($p.ExitCode)"
    }
    
    if (Test-Path $tempOut) {
        $content = [System.IO.File]::ReadAllText($tempOut)
        Write-Host ""
        Write-Host "=== Build Output ==="
        Write-Host $content
        
        [System.IO.File]::Copy($tempOut, "d:\workspace\Gdict\build_result.txt", $true)
    }
} finally {
    if (Test-Path $tempOut) { Remove-Item $tempOut -ErrorAction SilentlyContinue }
}

$apk = "d:\workspace\Gdict\android_project\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    $i = Get-Item $apk
    Write-Host ""
    Write-Host "APK BUILT: $($i.LastWriteTime.ToString('HH:mm:ss'))  $($i.Length) bytes"
} else {
    Write-Host ""
    Write-Host "APK NOT FOUND"
}
