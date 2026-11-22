#!/bin/sh
set -eu

: "${ANDROID_SDK_ROOT:=/usr/local/lib/android/sdk}"
: "${API_LEVEL:?API_LEVEL is required}"
: "${IMAGE_API_LEVEL:?IMAGE_API_LEVEL is required}"
: "${IMAGE_TARGET:?IMAGE_TARGET is required}"

SERIAL="${ANDROID_SERIAL:-emulator-5554}"
AVD_NAME="eyecare-api-${API_LEVEL}"
IMAGE_PACKAGE="system-images;android-${IMAGE_API_LEVEL};${IMAGE_TARGET};x86_64"
AVD_MANAGER="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/avdmanager"
EMULATOR="${ANDROID_SDK_ROOT}/emulator/emulator"
ADB="${ANDROID_SDK_ROOT}/platform-tools/adb"
EMULATOR_LOG="${RUNNER_TEMP:-/tmp}/eyecare-emulator-api-${API_LEVEL}.log"
APK_ARTIFACT_DIR="${APK_ARTIFACT_DIR:-artifacts}"
ANDROID_SDK_HOME="${ANDROID_SDK_HOME:-$HOME}"
ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ANDROID_SDK_HOME/.android/avd}"
EMULATOR_PID=""

export ANDROID_SDK_ROOT ANDROID_SDK_HOME ANDROID_AVD_HOME
mkdir -p "$ANDROID_AVD_HOME"

cleanup() {
    if [ -n "$EMULATOR_PID" ]; then
        "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1 || true
        kill "$EMULATOR_PID" >/dev/null 2>&1 || true
        wait "$EMULATOR_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT HUP INT TERM

if [ ! -x "$AVD_MANAGER" ] || [ ! -x "$EMULATOR" ] || [ ! -x "$ADB" ]; then
    echo "Android emulator tools are missing. Install cmdline-tools, emulator, platform-tools, and the selected system image first." >&2
    exit 2
fi

if [ ! -c /dev/kvm ] || [ ! -r /dev/kvm ] || [ ! -w /dev/kvm ]; then
    echo "KVM is not available to this runner. Enable /dev/kvm permissions before starting the emulator." >&2
    ls -l /dev/kvm >&2 2>/dev/null || true
    exit 2
fi
"$EMULATOR" -accel-check

"$ADB" kill-server >/dev/null 2>&1 || true
"$ADB" start-server

printf 'no\n' | "$AVD_MANAGER" create avd \
    -n "$AVD_NAME" \
    -k "$IMAGE_PACKAGE" \
    -d pixel_2 \
    -f \
    -p "$ANDROID_AVD_HOME/$AVD_NAME.avd"

if [ ! -f "$ANDROID_AVD_HOME/$AVD_NAME.ini" ]; then
    echo "avdmanager did not create $ANDROID_AVD_HOME/$AVD_NAME.ini" >&2
    "$AVD_MANAGER" list avd >&2 || true
    find "$ANDROID_SDK_HOME/.android" -maxdepth 4 -name "$AVD_NAME.ini" -print >&2 2>/dev/null || true
    exit 1
fi

AVD_CONFIG="$ANDROID_AVD_HOME/$AVD_NAME.avd/config.ini"
if [ ! -f "$AVD_CONFIG" ]; then
    echo "AVD config was not created: $AVD_CONFIG" >&2
    exit 1
fi
if grep -q '^disk.dataPartition.size=' "$AVD_CONFIG"; then
    sed -i 's/^disk.dataPartition.size=.*/disk.dataPartition.size=4096MB/' "$AVD_CONFIG"
else
    printf '\ndisk.dataPartition.size=4096MB\n' >> "$AVD_CONFIG"
fi
grep '^disk.dataPartition.size=' "$AVD_CONFIG"

"$EMULATOR" \
    -avd "$AVD_NAME" \
    -port 5554 \
    -accel on \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -no-snapshot \
    -wipe-data \
    -gpu swiftshader_indirect \
    -memory 3072 \
    -cores 2 \
    -camera-back none \
    -camera-front none \
    >"$EMULATOR_LOG" 2>&1 &
EMULATOR_PID=$!

ready=0
attempt=0
max_attempts=360
while [ "$attempt" -lt "$max_attempts" ]; do
    state="$("$ADB" -s "$SERIAL" get-state 2>/dev/null || true)"
    if [ "$state" = "device" ]; then
        boot_completed="$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
        if [ "$boot_completed" = "1" ]; then
            activity_user="$("$ADB" -s "$SERIAL" shell am get-current-user 2>/dev/null | tr -d '\r' || true)"
            case "$activity_user" in
                ''|*[!0-9]*) ;;
                *)
                    ready=1
                    break
                    ;;
            esac
        fi
    fi

    if ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
        echo "Emulator process exited before Android finished booting." >&2
        tail -n 120 "$EMULATOR_LOG" >&2 || true
        exit 1
    fi

    attempt=$((attempt + 1))
    sleep 2
done

if [ "$ready" -ne 1 ]; then
    echo "Emulator did not become fully ready within $((max_attempts * 2)) seconds." >&2
    "$ADB" devices -l >&2 || true
    "$ADB" -s "$SERIAL" shell getprop sys.boot_completed >&2 || true
    "$ADB" -s "$SERIAL" shell service check activity >&2 || true
    "$ADB" -s "$SERIAL" shell service check package >&2 || true
    "$ADB" -s "$SERIAL" shell am get-current-user >&2 || true
    "$ADB" -s "$SERIAL" shell df -h /data >&2 || true
    tail -n 160 "$EMULATOR_LOG" >&2 || true
    exit 1
fi

echo "ActivityManager is ready for user $activity_user."
"$ADB" -s "$SERIAL" shell service check activity
"$ADB" -s "$SERIAL" shell df -h /data

# PackageManager can lag behind ActivityManager on preview/16 KB images.
# Require three consecutive successful service checks and real package queries
# before attempting APK installation; a single boot-complete signal is not enough.
package_ready_count=0
package_attempt=0
while [ "$package_attempt" -lt 60 ]; do
    package_service="$("$ADB" -s "$SERIAL" shell service check package 2>/dev/null | tr -d '\r' || true)"
    package_probe="$("$ADB" -s "$SERIAL" shell pm list packages 2>/dev/null | tr -d '\r' || true)"
    case "$package_service:$package_probe" in
        *"found"*"package:"*)
            package_ready_count=$((package_ready_count + 1))
            ;;
        *)
            package_ready_count=0
            echo "PackageManager is not stable yet (attempt $((package_attempt + 1))/60)." >&2
            ;;
    esac
    if [ "$package_ready_count" -ge 3 ]; then
        break
    fi
    package_attempt=$((package_attempt + 1))
    sleep 2
done

print_android_diagnostics() {
    echo "----- Android service diagnostics -----" >&2
    "$ADB" -s "$SERIAL" shell service check activity >&2 || true
    "$ADB" -s "$SERIAL" shell service check package >&2 || true
    "$ADB" -s "$SERIAL" shell pm list packages >&2 || true
    "$ADB" -s "$SERIAL" shell df -h /data >&2 || true
    "$ADB" -s "$SERIAL" logcat -d -b all -t 2500 2>/dev/null |
        grep -Ei 'PackageManager|system_server|FATAL EXCEPTION|Watchdog|Fatal signal|crash|No space left' |
        tail -n 120 >&2 || true
    echo "----- Emulator process log -----" >&2
    tail -n 160 "$EMULATOR_LOG" >&2 || true
}

if [ "$package_ready_count" -lt 3 ]; then
    echo "PackageManager did not remain healthy for three consecutive probes." >&2
    print_android_diagnostics
    exit 1
fi
echo "PackageManager is stable after three consecutive package queries."

APP_APK="$(find "$APK_ARTIFACT_DIR" -type f -name 'app-debug.apk' -print | head -n 1)"
TEST_APK="$(find "$APK_ARTIFACT_DIR" -type f -name 'app-debug-androidTest.apk' -print | head -n 1)"
if [ -z "$APP_APK" ] || [ -z "$TEST_APK" ]; then
    echo "Prebuilt app or instrumentation APK is missing under $APK_ARTIFACT_DIR." >&2
    find "$APK_ARTIFACT_DIR" -maxdepth 5 -type f -print >&2 || true
    exit 1
fi

install_apk() {
    apk_path="$1"
    echo "Installing APK: $apk_path"
    if "$ADB" -s "$SERIAL" install -r "$apk_path"; then
        return 0
    fi

    echo "Streaming install failed; checking PackageManager before one non-streaming retry." >&2
    "$ADB" -s "$SERIAL" shell service check package >&2 || true
    if "$ADB" -s "$SERIAL" install --no-streaming -r "$apk_path"; then
        return 0
    fi

    echo "APK installation failed after streaming and non-streaming attempts." >&2
    print_android_diagnostics
    return 1
}

install_apk "$APP_APK"
install_apk "$TEST_APK"

mkdir -p test-results
RESULT_LOG="test-results/instrumentation-api-${API_LEVEL}.txt"
echo "Running AndroidJUnitRunner on Android API ${API_LEVEL} (120-second hard timeout)."
if timeout --signal=TERM --kill-after=5s 120s "$ADB" -s "$SERIAL" shell am instrument -w -r \
    com.eyecare.pro.test/androidx.test.runner.AndroidJUnitRunner >"$RESULT_LOG" 2>&1; then
    instrumentation_exit=0
else
    instrumentation_exit=$?
fi
cat "$RESULT_LOG"

if grep -q 'FAILURES!!!' "$RESULT_LOG" ||
    grep -q 'INSTRUMENTATION_FAILED' "$RESULT_LOG" ||
    grep -q 'INSTRUMENTATION_ABORTED' "$RESULT_LOG"; then
    echo "Android instrumentation reported test failures." >&2
    exit 1
fi
if ! grep -Eq 'OK \([0-9]+ tests?\)' "$RESULT_LOG" ||
    ! grep -q 'INSTRUMENTATION_CODE: -1' "$RESULT_LOG"; then
    echo "AndroidJUnitRunner did not report a complete successful test summary (exit $instrumentation_exit)." >&2
    print_android_diagnostics
    exit 1
fi
if [ "$instrumentation_exit" -ne 0 ]; then
    echo "::warning::AndroidJUnitRunner reported all tests successful, but the ADB shell client did not exit cleanly (status $instrumentation_exit). The final instrumentation summary is complete, so test assertions are considered passed."
fi
echo "Android API ${API_LEVEL} instrumentation tests passed."
