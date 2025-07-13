@echo off
echo Setting up environment...

REM Set environment variables
set JAVA_HOME=E:\java
set ANDROID_HOME=C:\Users\nenav\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=%ANDROID_HOME%
set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%ANDROID_HOME%\tools\bin
set PATH=%PATH%;%ANDROID_HOME%\cmdline-tools\latest\bin
set PATH=%PATH%;C:\Users\nenav\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin
set PATH=%PATH%;E:\Gradle\gradle-8.13\bin

echo Building Chess Rogue APK...

REM Check Java version
echo Checking Java version...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java not found! Please check JAVA_HOME setting.
    pause
    exit /b 1
)

REM Sync web assets
echo Syncing web assets...
npx cap sync android

REM Build APK
echo Building APK...
cd android
call gradlew assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS! APK created at:
    echo android\app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo You can install this APK on your Android device.
) else (
    echo.
    echo ERROR: Build failed!
    echo Please check the error messages above.
)

cd ..
pause 