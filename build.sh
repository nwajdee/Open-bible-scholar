#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════════
# OpenBible Scholar — One-command local build script
# Usage:  ./build.sh [debug|release]
# ═══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

BUILD_TYPE="${1:-debug}"
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

log()  { echo -e "${BLUE}[OBS]${NC} $1"; }
ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; exit 1; }

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║  OpenBible Scholar APK Builder  v1.1             ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# ── Prerequisites check ───────────────────────────────────────────────────────
log "Checking prerequisites..."

command -v java &>/dev/null || err "Java not found. Install JDK 17: https://adoptium.net"
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
[ "$JAVA_VER" -ge 17 ] 2>/dev/null || warn "Java $JAVA_VER detected — JDK 17+ recommended"
ok "Java $JAVA_VER found"

if [ -z "${ANDROID_HOME:-}" ]; then
  # Common SDK locations
  for loc in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" "/usr/local/lib/android/sdk" "/opt/android-sdk"; do
    if [ -d "$loc" ]; then
      export ANDROID_HOME="$loc"
      break
    fi
  done
fi

[ -d "${ANDROID_HOME:-}" ] || err "Android SDK not found.\n  Install via Android Studio → SDK Manager, then:\n  export ANDROID_HOME=\$HOME/Library/Android/sdk  # macOS\n  export ANDROID_HOME=\$HOME/Android/Sdk           # Linux"
ok "Android SDK: $ANDROID_HOME"

# Check platform 34
[ -d "$ANDROID_HOME/platforms/android-34" ] || {
  warn "Android API 34 platform not found. Installing..."
  "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34" "build-tools;34.0.0" || \
  err "SDK install failed. Run Android Studio → SDK Manager → install API 34"
}
ok "Android API 34 platform found"

# ── Make gradlew executable ───────────────────────────────────────────────────
chmod +x gradlew
ok "gradlew ready"

# ── Build ─────────────────────────────────────────────────────────────────────
START_TIME=$SECONDS
echo ""
log "Building ${BUILD_TYPE^^} APK..."
echo ""

if [ "$BUILD_TYPE" == "release" ]; then
  # Check for keystore
  if [ -z "${KEYSTORE_PATH:-}" ]; then
    warn "No KEYSTORE_PATH set — building unsigned release"
    warn "To sign: KEYSTORE_PATH=/path/to/key.jks KEY_ALIAS=mykey KEYSTORE_PASS=xxx ./build.sh release"
    ./gradlew assembleRelease --stacktrace
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
  else
    ./gradlew assembleRelease \
      -Pandroid.injected.signing.store.file="$KEYSTORE_PATH" \
      -Pandroid.injected.signing.store.password="${KEYSTORE_PASS:-}" \
      -Pandroid.injected.signing.key.alias="${KEY_ALIAS:-}" \
      -Pandroid.injected.signing.key.password="${KEY_PASS:-$KEYSTORE_PASS}" \
      --stacktrace
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
  fi
else
  ./gradlew assembleDebug --stacktrace
  APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

ELAPSED=$((SECONDS - START_TIME))
echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║  BUILD SUCCESSFUL                                ║"
printf  "║  Time: %ds%-40s║\n" "$ELAPSED" ""
echo "╚══════════════════════════════════════════════════╝"
echo ""

if [ -f "$APK_PATH" ]; then
  APK_SIZE=$(du -sh "$APK_PATH" | cut -f1)
  ok "APK: $APK_PATH ($APK_SIZE)"
  echo ""
  echo "════════════════════════════════════════════════════"
  echo " INSTALL OPTIONS:"
  echo ""
  echo " 📱 USB (adb):    adb install $APK_PATH"
  echo " 📲 Direct:       Copy APK to phone → open file manager → tap APK"
  echo " ☁️  Share:        adb push $APK_PATH /sdcard/Download/"
  echo "════════════════════════════════════════════════════"
  echo ""
  
  # Auto-install if device connected
  if command -v adb &>/dev/null; then
    DEVICES=$(adb devices | grep -c "device$" 2>/dev/null || echo 0)
    if [ "$DEVICES" -gt 0 ]; then
      read -p "Android device detected! Install now? [Y/n] " -n 1 -r
      echo ""
      if [[ $REPLY =~ ^[Yy]$ ]] || [ -z "$REPLY" ]; then
        log "Installing on device..."
        adb install -r "$APK_PATH" && ok "Installed successfully!" || warn "Install failed — try manually"
      fi
    fi
  fi
else
  err "APK not found at $APK_PATH — check build output above"
fi
