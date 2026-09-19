#!/usr/bin/env bash
set -euo pipefail

BASE_APK="${1:?baseline APK path is required}"
CANDIDATE_APK="${2:?candidate APK path is required}"
TEST_APK="${3:?candidate androidTest APK path is required}"

APP_ID="com.wakemyway.app"
TEST_ID="com.wakemyway.app.test"
RUNNER="${TEST_ID}/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="com.wakemyway.app.update.UpdatePersistenceContractInstrumentedTest"

capture_diagnostics() {
  adb devices -l > "${GITHUB_WORKSPACE:-.}/upgrade-adb-devices.txt" 2>&1 || true
  timeout 15s adb logcat -d -t 2500 > "${GITHUB_WORKSPACE:-.}/upgrade-emulator-logcat.txt" 2>&1 || true
  adb shell dumpsys package "$APP_ID" > "${GITHUB_WORKSPACE:-.}/upgrade-package-dumpsys.txt" 2>&1 || true
}
trap capture_diagnostics EXIT

run_test() {
  local method="$1"
  adb shell am instrument -w     -e class "${TEST_CLASS}#${method}"     "$RUNNER"
}

adb uninstall "$TEST_ID" >/dev/null 2>&1 || true
adb uninstall "$APP_ID" >/dev/null 2>&1 || true

adb install "$BASE_APK"
adb install "$TEST_APK"
run_test "seedPersistentStateForUpgrade"

first_install_before="$(
  adb shell dumpsys package "$APP_ID" |
    sed -n 's/^[[:space:]]*firstInstallTime=//p' |
    head -n 1 |
    tr -d '\r'
)"
test -n "$first_install_before"

# This is the contract under test: replace the package in place. Never uninstall and never clear data.
adb install -r "$CANDIDATE_APK"
adb install -r "$TEST_APK"

first_install_after="$(
  adb shell dumpsys package "$APP_ID" |
    sed -n 's/^[[:space:]]*firstInstallTime=//p' |
    head -n 1 |
    tr -d '\r'
)"

test "$first_install_before" = "$first_install_after" || {
  echo "firstInstallTime changed across package replacement; this was not an in-place update" >&2
  exit 1
}

run_test "verifyPersistentStateAfterUpgrade"
run_test "cleanupUpgradeContractState"

echo "WakeMyWay package replacement preserved durable alarm/product state."
