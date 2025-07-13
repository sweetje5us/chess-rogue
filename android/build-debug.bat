@echo off
set JAVA_HOME=E:\java
set ANDROID_HOME=C:\Users\nenav\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=%ANDROID_HOME%
set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%ANDROID_HOME%\tools\bin
set PATH=%PATH%;%ANDROID_HOME%\cmdline-tools\latest\bin
set PATH=%PATH%;C:\Users\nenav\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin
set PATH=%PATH%;E:\Gradle\gradle-8.13\bin

REM === Копирование актуального AdPlugin.java ===
REM Создаем папку если её нет
if not exist "app\src\main\java\com\medievaldash\adplugin" mkdir "app\src\main\java\com\medievaldash\adplugin"
copy /Y "..\adPlugin\src\android\AdPlugin.java" "app\src\main\java\com\medievaldash\adplugin\AdPlugin.java"

echo Cleaning previous build...
call gradlew clean

echo.
echo Building APK...
call gradlew assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS! APK created at:
    echo app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo ERROR: Build failed!
)

pause 