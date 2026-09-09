#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"
ANDROID_DIR="$ROOT_DIR/apps/android"

capture_diagnostics() {
  adb devices -l > "$ROOT_DIR/adb-devices.txt" 2>&1 || true
  timeout 15s adb logcat -d -t 2000 > "$ROOT_DIR/emulator-logcat.txt" 2>&1 || true
}
trap capture_diagnostics EXIT

cd "$ANDROID_DIR"
./gradlew :app:connectedDebugAndroidTest --stacktrace
