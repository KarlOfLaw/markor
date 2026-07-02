@echo off
set JAVA_HOME=C:\Users\cxl\tools\jdk\jdk-17.0.12+7
set ANDROID_SDK_ROOT=C:\Users\cxl\tools\android-sdk
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d C:\Users\cxl\Desktop\markor

echo === Starting Markor APK build ===
echo JAVA_HOME=%JAVA_HOME%
echo ANDROID_SDK_ROOT=%ANDROID_SDK_ROOT%
echo.

call gradlew.bat assembleFlavorDefaultDebug --no-daemon

if %ERRORLEVEL% EQU 0 (
    echo.
    echo === Build SUCCESS ===
    echo APK location:
    dir /s /b app\build\outputs\apk\*.apk 2>nul
) else (
    echo.
    echo === Build FAILED (exit code: %ERRORLEVEL%) ===
)
