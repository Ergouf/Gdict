@echo off
set JAVA_HOME=D:\workspace\build-tools\jdk17\jdk-17.0.19+10
set ANDROID_HOME=D:\workspace\build-tools\android-sdk
set STORE_PASSWORD=gdict123
set KEY_PASSWORD=gdict123
set PATH=C:\Program Files\Git\bin;%JAVA_HOME%\bin;%PATH%

call gradlew.bat assembleRelease
echo Exit code: %ERRORLEVEL%
pause
