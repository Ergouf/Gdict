$env:JAVA_HOME = "C:\Users\fengjl\.jdks\temurin-21"
$javaExe = "$env:JAVA_HOME\bin\java.exe"
$wrapperJar = "d:\workspace\Gdict\android_project\gradle\wrapper\gradle-wrapper.jar"

Write-Host "Java: $javaExe"
Write-Host "Wrapper: $wrapperJar"
Write-Host "Java exists: $(Test-Path $javaExe)"
Write-Host "Jar exists: $(Test-Path $wrapperJar)"

if ((Test-Path $javaExe) -and (Test-Path $wrapperJar)) {
    Set-Location "d:\workspace\Gdict\android_project"
    
    $jvmArgs = @("-Xmx64m", "-Xms64m", 
        "-Dorg.gradle.appname=gradlew",
        "-classpath", $wrapperJar,
        "org.gradle.wrapper.GradleWrapperMain",
        "assembleDebug", "--no-daemon")
    
    Write-Host ""
    Write-Host "Running gradle build..."
    
    $procInfo = New-Object System.Diagnostics.ProcessStartInfo
    $procInfo.FileName = $javaExe
    $procInfo.Arguments = $jvmArgs -join " "
    $procInfo.UseShellExecute = $false
    $procInfo.RedirectStandardOutput = $true
    $procInfo.RedirectStandardError = $true
    $procInfo.WorkingDirectory = "d:\workspace\Gdict\android_project"
    $procInfo.EnvironmentVariables["JAVA_HOME"] = $env:JAVA_HOME
    
    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $procInfo
    [void]$proc.Start()
    
    # Read output in real-time
    $outputLines = [System.Collections.ArrayList]::new()
    while (!$proc.HasExited) {
        $line = $proc.StandardOutput.ReadLine()
        if ($line -ne $null) {
            [void]$outputLines.Add($line)
            Write-Host $line
        }
        Start-Sleep -Milliseconds 50
    }
    
    # Read remaining
    $remainingOut = $proc.StandardOutput.ReadToEnd()
    $stderr = $proc.StandardError.ReadToEnd()
    
    if ($remainingOut) { [void]$outputLines.AddRange($remainingOut -split "`n") }
    
    $allOutput = ($outputLines -join "`n") + "`nSTDERR:`n" + $stderr + "`nEXIT: $($proc.ExitCode)"
    [System.IO.File]::WriteAllText("d:\workspace\Gdict\build_result.txt", $allOutput, [System.Text.Encoding]::UTF8)
    
    Write-Host ""
    Write-Host "=== Build Complete: Exit Code $($proc.ExitCode) ==="
    
    $apk = "d:\workspace\Gdict\android_project\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        $i = Get-Item $apk
        Write-Host "APK BUILT: $($i.LastWriteTime.ToString('HH:mm:ss'))  $($i.Length) bytes"
    } else {
        Write-Host "APK NOT FOUND"
    }
} else {
    Write-Host "ERROR: Java or wrapper jar not found!"
}
