@echo off
REM ═══════════════════════════════════════════════════════════════════════════
REM OpenBible Scholar — Windows Build Script
REM Usage: build.bat [debug|release]
REM ═══════════════════════════════════════════════════════════════════════════
setlocal enabledelayedexpansion

set BUILD_TYPE=%1
if "%BUILD_TYPE%"=="" set BUILD_TYPE=debug

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║  OpenBible Scholar APK Builder  v1.1             ║
echo ╚══════════════════════════════════════════════════╝
echo.

REM ── Check Java ──────────────────────────────────────────────────────────────
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo [ERROR] Java not found. Download JDK 17 from https://adoptium.net
  exit /b 1
)
echo [OK] Java found

REM ── Check ANDROID_HOME ──────────────────────────────────────────────────────
if "%ANDROID_HOME%"=="" (
  if exist "%LOCALAPPDATA%\Android\Sdk" set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
  if exist "%USERPROFILE%\AppData\Local\Android\Sdk" set ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk
)

if "%ANDROID_HOME%"=="" (
  echo [ERROR] ANDROID_HOME not set. Install Android Studio and set:
  echo   set ANDROID_HOME=%%LOCALAPPDATA%%\Android\Sdk
  exit /b 1
)
echo [OK] ANDROID_HOME: %ANDROID_HOME%

REM ── Build ────────────────────────────────────────────────────────────────────
echo.
echo [INFO] Building %BUILD_TYPE% APK...
echo.

if "%BUILD_TYPE%"=="release" (
  call gradlew.bat assembleRelease --stacktrace
  set APK_PATH=app\build\outputs\apk\release\app-release-unsigned.apk
) else (
  call gradlew.bat assembleDebug --stacktrace
  set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
)

if %ERRORLEVEL% NEQ 0 (
  echo [ERROR] Build failed. Check output above.
  exit /b 1
)

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║  BUILD SUCCESSFUL                                ║
echo ╚══════════════════════════════════════════════════╝
echo.
echo [OK] APK: %APK_PATH%
echo.
echo  Install via USB:  adb install %APK_PATH%
echo.

REM Auto-install if adb available
adb devices >nul 2>&1
if %ERRORLEVEL%==0 (
  set /p INSTALL="Device detected. Install now? [Y/n]: "
  if /i "!INSTALL!"=="Y" (
    adb install -r %APK_PATH%
  )
  if "!INSTALL!"=="" (
    adb install -r %APK_PATH%
  )
)

endlocal
