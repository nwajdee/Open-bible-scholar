# How to Build OpenBible Scholar

## Method 1: GitHub Actions (Easiest — 5 minutes, no setup)

OpenBible Scholar can be built automatically with GitHub Actions, requiring no local setup. You can download the generated APK from the workflow artifacts.

## Method 2: Local Build

You can also build the APK locally using Gradle:
1. Clone the repository.
2. Install JDK 17 and Android SDK.
3. Run `./gradlew assembleDebug`.
4. Find the APK in `app/build/outputs/apk/debug/app-debug.apk`.

## CI Workflow

See `.github/workflows/build.yml` for details of the automated build.

---
For further details, refer to the README or documentation folder if available.