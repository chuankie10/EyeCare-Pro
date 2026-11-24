#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)

: "${ANDROID_SDK_ROOT:=/usr/local/lib/android/sdk}"
: "${API_LEVEL:?API_LEVEL is required}"
: "${IMAGE_API_LEVEL:?IMAGE_API_LEVEL is required}"
: "${IMAGE_TARGET:?IMAGE_TARGET is required}"

SERIAL="${ANDROID_SERIAL:-emulator-5554}"
AVD_NAME="eyecare-api-${API_LEVEL}"
IMAGE_PACKAGE="system-images;android-${IMAGE_API_LEVEL};${IMAGE_TARGET};x86_64"
CMDLINE_TOOLS_VERSION="${ANDROID_CMDLINE_TOOLS_VERSION:-latest}"
CMDLINE_TOOLS_DIR="${ANDROID_SDK_ROOT}/cmdline-tools/${CMDLINE_TOOLS_VERSION}"
SDKMANAGER="${CMDLINE_TOOLS_DIR}/bin/sdkmanager"
AVD_MANAGER="${CMDLINE_TOOLS_DIR}/bin/avdmanager"
EMULATOR="${ANDROID_SDK_ROOT}/emulator/emulator"
ADB="${ANDROID_SDK_ROOT}/platform-tools/adb"
EMULATOR_LOG="${RUNNER_TEMP:-/tmp}/eyecare-emulator-api-${API_LEVEL}.log"
APK_ARTIFACT_DIR="${APK_ARTIFACT_DIR:-artifacts}"
ANDROID_SDK_HOME="${ANDROID_SDK_HOME:-$HOME}"
ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ANDROID_SDK_HOME/.android/avd}"
EMULATOR_PID=""

export ANDROID_SDK_ROOT ANDROID_SDK_HOME ANDROID_AVD_HOME
mkdir -p "$ANDROID_AVD_HOME"

print_android_diagnostics() {
    echo "----- Android service diagnostics -----" >&2
    "$ADB" -s "$SERIAL" shell service check activity >&2 || true
    "$ADB" -s "$SERIAL" shell service check package >&2 || true
    "$ADB" -s "$SERIAL" shell pm list packages >&2 || true
    "$ADB" -s "$SERIAL" shell df -h /data >&2 || true
    "$ADB" -s "$SERIAL" logcat -d -b all -t 2500 2>/dev/null |
        grep -Ei "PackageManager|system_server|SurfaceFlinger|RegionSampling|mapper.ranchu|ColorBufferDMA|FATAL EXCEPTION|Watchdog|Fatal signal|crash|No space left" |
        tail -n 120 >&2 || true
    echo "----- Emulator process log -----" >&2
    tail -n 160 "$EMULATOR_LOG" >&2 || true
}

collect_instrumentation_diagnostics() {
    echo "Collecting full logcat and DropBox records after instrumentation failure." >&2
    timeout --signal=TERM --kill-after=5s 30s "$ADB" -s "$SERIAL" logcat -b all -d -v threadtime >"$LOGCAT_LOG" 2>&1 || true
    timeout --signal=TERM --kill-after=5s 30s "$ADB" -s "$SERIAL" shell dumpsys dropbox --print >"$DROPBOX_LOG" 2>&1 || true
    echo "----- Relevant logcat failures -----" >&2
    grep -Ei "FATAL EXCEPTION|Fatal signal|system_server|Watchdog|INSTRUMENTATION_ABORTED|am_crash|am_anr|Process .* has died|tombstone|native crash" "$LOGCAT_LOG" | tail -n 240 >&2 || true
    echo "----- DropBox crash/ANR markers -----" >&2
    grep -Ei "system_server|system_app_crash|system_app_anr|system_server_crash|system_server_anr|native_crash|watchdog" "$DROPBOX_LOG" | tail -n 160 >&2 || true
}

mkdir -p test-results
RESULT_LOG="test-results/instrumentation-api-${API_LEVEL}.txt"
LOGCAT_LOG="test-results/logcat-api-${API_LEVEL}.txt"
DROPBOX_LOG="test-results/dropbox-api-${API_LEVEL}.txt"
: > "$RESULT_LOG"

cleanup() {
    saved_status=$?
    trap - EXIT HUP INT TERM

    if [ -n "$EMULATOR_PID" ]; then
        # Do not let emulator shutdown hold a matrix job until its 45-minute timeout.
        timeout 8s "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1 || true
        kill -TERM "$EMULATOR_PID" >/dev/null 2>&1 || true

        stop_attempt=0
        while kill -0 "$EMULATOR_PID" 2>/dev/null; do
            if [ "$stop_attempt" -ge 8 ]; then
                kill -KILL "$EMULATOR_PID" >/dev/null 2>&1 || true
                break
            fi
            stop_attempt=$((stop_attempt + 1))
            sleep 1
        done
        wait "$EMULATOR_PID" 2>/dev/null || true
    fi

    exit "$saved_status"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

if [ ! -x "$SDKMANAGER" ] || [ ! -x "$AVD_MANAGER" ] || [ ! -x "$EMULATOR" ] || [ ! -x "$ADB" ]; then
    echo "Android emulator tools are missing. Install cmdline-tools, emulator, platform-tools, and the selected system image first." >&2
    exit 2
fi

cmdline_version="$("$SDKMANAGER" --version | tr -d "\r")"
if [ -z "$cmdline_version" ]; then
    echo "Android SDK Manager did not report a command-line tools version." >&2
    exit 2
fi
echo "Using Android command-line tools $cmdline_version from $CMDLINE_TOOLS_DIR."

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

AVD_METADATA="$ANDROID_AVD_HOME/$AVD_NAME.ini"
AVD_CONFIG="$ANDROID_AVD_HOME/$AVD_NAME.avd/config.ini"
if [ ! -f "$AVD_CONFIG" ]; then
    echo "AVD config was not created: $AVD_CONFIG" >&2
    exit 1
fi

EXPECTED_TARGET="android-${IMAGE_API_LEVEL}"
EXPECTED_IMAGE_SYS_DIR="system-images/android-${IMAGE_API_LEVEL}/${IMAGE_TARGET}/x86_64/"
sh "$SCRIPT_DIR/verify-avd-metadata.sh" \
    "$AVD_METADATA" \
    "$AVD_CONFIG" \
    "$EXPECTED_TARGET" \
    "$EXPECTED_IMAGE_SYS_DIR"

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

# SurfaceFlinger can restart after sys.boot_completed on malformed API 37 AVDs.
surfaceflinger_previous=""
surfaceflinger_stable_count=0
surfaceflinger_attempt=0
while [ "$surfaceflinger_attempt" -lt 15 ]; do
    surfaceflinger_pid="$("$ADB" -s "$SERIAL" shell pidof surfaceflinger 2>/dev/null | tr -d "\r" || true)"
    if [ -n "$surfaceflinger_pid" ] && [ "$surfaceflinger_pid" = "$surfaceflinger_previous" ]; then
        surfaceflinger_stable_count=$((surfaceflinger_stable_count + 1))
    else
        surfaceflinger_stable_count=0
    fi
    if [ "$surfaceflinger_stable_count" -ge 3 ]; then
        break
    fi
    surfaceflinger_previous="$surfaceflinger_pid"
    surfaceflinger_attempt=$((surfaceflinger_attempt + 1))
    sleep 2
done
if [ "$surfaceflinger_stable_count" -lt 3 ]; then
    echo "SurfaceFlinger did not keep a stable PID after boot." >&2
    print_android_diagnostics
    exit 1
fi
echo "SurfaceFlinger remained stable across four consecutive samples."

# PackageManager can lag behind ActivityManager on preview/16 KB images.
# Require three consecutive successful service checks and real package queries
# before attempting APK installation; a single boot-complete signal is not enough.
package_ready_count=0
package_attempt=0
max_package_attempts=90
while [ "$package_attempt" -lt "$max_package_attempts" ]; do
    package_service="$("$ADB" -s "$SERIAL" shell service check package 2>&1 | tr -d '\r' || true)"
    package_probe="$("$ADB" -s "$SERIAL" shell pm list packages 2>&1 | tr -d '\r' || true)"

    if printf '%s\n' "$package_service" | grep -q 'Service package: found' &&
        printf '%s\n' "$package_probe" | grep -q '^package:'; then
        package_ready_count=$((package_ready_count + 1))
    else
        package_ready_count=0
        echo "PackageManager is not stable yet (attempt $((package_attempt + 1))/$max_package_attempts)." >&2
        printf '%s\n' "$package_service" | tail -n 2 >&2 || true
        printf '%s\n' "$package_probe" | tail -n 2 >&2 || true
    fi

    if [ "$package_ready_count" -ge 3 ]; then
        break
    fi

    package_attempt=$((package_attempt + 1))
    sleep 2
done

if [ "$package_ready_count" -lt 3 ]; then
    echo "PackageManager did not remain healthy for three consecutive probes." >&2
    print_android_diagnostics
    exit 1
fi
echo "PackageManager remained healthy for three consecutive probes."

APP_APK="$(find "$APK_ARTIFACT_DIR" -type f -name 'app-debug.apk' -print | head -n 1)"
TEST_APK="$(find "$APK_ARTIFACT_DIR" -type f -name 'app-debug-androidTest.apk' -print | head -n 1)"
if [ -z "$APP_APK" ] || [ -z "$TEST_APK" ]; then
    echo "Prebuilt app or instrumentation APK is missing under $APK_ARTIFACT_DIR." >&2
    find "$APK_ARTIFACT_DIR" -maxdepth 5 -type f -print >&2 || true
    exit 1
fi

wait_for_package_manager() {
    retry_attempt=0
    retry_stable_count=0
    while [ "$retry_attempt" -lt 30 ]; do
        retry_service="$("$ADB" -s "$SERIAL" shell service check package 2>&1 | tr -d '\r' || true)"
        retry_probe="$("$ADB" -s "$SERIAL" shell pm list packages 2>&1 | tr -d '\r' || true)"
        if printf '%s\n' "$retry_service" | grep -q 'Service package: found' &&
            printf '%s\n' "$retry_probe" | grep -q '^package:'; then
            retry_stable_count=$((retry_stable_count + 1))
        else
            retry_stable_count=0
        fi
        if [ "$retry_stable_count" -ge 2 ]; then
            return 0
        fi
        retry_attempt=$((retry_attempt + 1))
        sleep 2
    done
    return 1
}

run_apk_install() {
    install_mode="$1"
    if [ -n "$install_mode" ]; then
        timeout --signal=TERM --kill-after=10s 180s \
            "$ADB" -s "$SERIAL" install "$install_mode" -r "$apk_path"
    else
        timeout --signal=TERM --kill-after=10s 180s \
            "$ADB" -s "$SERIAL" install -r "$apk_path"
    fi
}

install_apk() {
    apk_path="$1"
    echo "Installing APK with a 180-second hard timeout: $apk_path"

    # Older Android 7 package installers are more reliable with the legacy
    # push-then-pm-install path. Newer APIs keep streaming as the first attempt.
    if [ "$API_LEVEL" -le 25 ]; then
        first_install_mode="--no-streaming"
        second_install_mode=""
    else
        first_install_mode=""
        second_install_mode="--no-streaming"
    fi

    if run_apk_install "$first_install_mode"; then
        return 0
    else
        first_install_status=$?
    fi

    echo "First APK install attempt failed (exit $first_install_status); checking PackageManager before one bounded fallback." >&2
    if ! wait_for_package_manager; then
        echo "PackageManager did not recover after the first install attempt." >&2
        print_android_diagnostics
        return 1
    fi

    if run_apk_install "$second_install_mode"; then
        return 0
    else
        second_install_status=$?
    fi

    echo "APK installation failed after both bounded attempts (exit codes $first_install_status and $second_install_status)." >&2
    print_android_diagnostics
    return 1
}
install_apk "$APP_APK"
install_apk "$TEST_APK"

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
    echo "Android instrumentation reported test failures or the Android runtime aborted." >&2
    collect_instrumentation_diagnostics
    print_android_diagnostics
    exit 1
fi
if ! grep -Eq 'OK \([0-9]+ tests?\)' "$RESULT_LOG" ||
    ! grep -q 'INSTRUMENTATION_CODE: -1' "$RESULT_LOG"; then
    echo "AndroidJUnitRunner did not report a complete successful test summary (exit $instrumentation_exit)." >&2
    collect_instrumentation_diagnostics
    print_android_diagnostics
    exit 1
fi
if [ "$instrumentation_exit" -ne 0 ]; then
    echo "::warning::AndroidJUnitRunner reported all tests successful, but the ADB shell client did not exit cleanly (status $instrumentation_exit). The final instrumentation summary is complete, so test assertions are considered passed."
fi
echo "Android API ${API_LEVEL} instrumentation tests passed."
