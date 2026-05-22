$env:JAVA_HOME = "C:\Users\fengjl\.jdks\temurin-21"
Set-Location "d:\workspace\Gdict\android_project"

Write-Host "Testing gradlew.bat --version..."
$testProc = New-Object System.Diagnostics.ProcessStartInfo
$testProc.FileName = "d:\workspace\Gdict\android_project\gradlew.bat"
$testProc.Arguments = "--version"
$testProc.UseShellExecute = $false
$testProc.RedirectStandardOutput = $true
$testProc.RedirectStandardError = $true
$testProc.WorkingDirectory = "d:\workspace\Gdict\android_project"

$p = New-Object System.Diagnostics.Process
$p.StartInfo = $testProc
[void]$p.Start()
$out = $p.StandardOutput.ReadToEnd()
$err = $p.StandardError.ReadToEnd()
$p.WaitForExit()

Write-Host "Version Exit Code: $($p.ExitCode)"
Write-Host "STDOUT: $out"
Write-Host "STDERR: $err"

if ($p.ExitCode -eq 0) {
    Write-Host ""
    Write-Host "Now running assembleDebug..."
    $psi2 = New-Object System.Diagnostics.ProcessStartInfo
    $psi2.FileName = "d:\workspace\Gdict\android_project\gradlew.bat"
    $psi2.Arguments = "assembleDebug --no-daemon"
    $psi2.UseShellExecute = $false
    $psi2.RedirectStandardOutput = $true
    $psi2.RedirectStandardError = $true
    $psi2.WorkingDirectory = "d:\workspace\Gdict\android_project"
    
    $p2 = New-Object System.Diagnostics.Process
    $p2.StartInfo = $psi2
    [void]$p2.Start()
    
    $out2 = $p2.StandardOutput.ReadToEnd()
    $err2 = $p2.StandardError.ReadToEnd()
    $p2.WaitForExit()
    
    $result = "EXIT: $($p2.ExitCode)`nSTDOUT: $out2`nSTDERR: $err2"
    [System.IO.File]::WriteAllText("d:\workspace\Gdict\build_result.txt", $result, [System.Text.Encoding]::UTF8)
    
    Write-Host "Build Exit Code: $($p2.ExitCode)"
    Write-Host "Output length: $($out2.Length) chars, Error length: $($err2.Length) chars"
}
