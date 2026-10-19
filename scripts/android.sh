#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="$ROOT/.local/android/jdk-21/Contents/Home"
export ANDROID_HOME="$ROOT/.local/android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$ROOT/.local/android/user"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
export GRADLE_USER_HOME="$ROOT/.local/android/gradle"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
AVD=KrPlay_API_35
mkdir -p "$ANDROID_AVD_HOME" "$ROOT/.local/android/logs"
case "${1:-help}" in
  env)
    printf 'export JAVA_HOME=%q\nexport ANDROID_HOME=%q\nexport ANDROID_SDK_ROOT=%q\nexport ANDROID_USER_HOME=%q\nexport ANDROID_AVD_HOME=%q\nexport GRADLE_USER_HOME=%q\nexport PATH=%q\n' "$JAVA_HOME" "$ANDROID_HOME" "$ANDROID_SDK_ROOT" "$ANDROID_USER_HOME" "$ANDROID_AVD_HOME" "$GRADLE_USER_HOME" "$PATH"
    ;;
  build) cd "$ROOT/krplay-client"; ./gradlew assembleDebug ;;
  emulator)
    if adb -s emulator-5554 get-state >/dev/null 2>&1; then
      echo 'KrPlay 模拟器已启动。'
      exit 0
    fi
    if [[ ! -f "$ANDROID_AVD_HOME/$AVD.ini" ]]; then
      printf 'no\n' | avdmanager create avd -n "$AVD" -k 'system-images;android-35;default;arm64-v8a' -d pixel_c
    fi
    exec emulator -avd "$AVD" -port 5554 -no-boot-anim -no-snapshot -no-metrics -crash-report-mode never -gpu auto -skin 1920x1080
    ;;
  run|debug)
    "$0" build
    # Set ANDROID_SERIAL to select a real device; default to our emulator.
    export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
    adb -s "$ANDROID_SERIAL" get-state >/dev/null
    adb install -r "$ROOT/krplay-client/build/outputs/apk/debug/KrPlay-Client-debug.apk"
    adb shell am force-stop com.krplay.client
    if [[ "$1" == debug ]]; then
      adb shell am set-debug-app -w com.krplay.client
    else
      adb shell am clear-debug-app
    fi
    adb shell am start -n com.krplay.client/.MainActivity
    ;;
  logs)
    export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
    PID="$(adb shell pidof -s com.krplay.client | tr -d '\r')"
    [[ -n "$PID" ]] || { echo 'KrPlay 未运行，请先执行 run。' >&2; exit 1; }
    adb logcat --pid="$PID"
    ;;
  devices) adb devices -l ;;
  studio)
    open -a "$ROOT/.local/android/Android Studio.app" "$ROOT/krplay-client"
    ;;
  *) echo '用法：scripts/android.sh {env|build|emulator|run|debug|logs|devices|studio}' ;;
esac
