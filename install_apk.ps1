$adb = "d:\workspace\Gdict\android_sdk\platform-tools\adb.exe"
& $adb kill-server
Start-Sleep -Seconds 2
& $adb start-server
Start-Sleep -Seconds 3
$result = & $adb devices 2>&1
Write-Host "ADB Devices:"
Write-Host $result

if ($result -match "device") {
    Write-Host ""
    Write-Host "Device found! Installing APK..."
    $apk = "d:\workspace\Gdict\android_project\app\build\outputs\apk\debug\app-debug.apk"
    $installResult = & $adb install -r $apk 2>&1
    Write-Host "Install result: $installResult"
} else {
    Write-Host "No device found. Please check USB connection."
}
