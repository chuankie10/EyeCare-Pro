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

# Do not silently fall back to TCG: x86_64 Android images are too slow and
# unreliable without KVM on hosted Linux runners.
if [ ! -c /dev/kvm ] || [ ! -r /dev/kvm ] || [ ! -w /dev/kvm ]; then
    echo "KVM is not available to this runner. Enable /dev/kvm permissions before starting the emulator." >&2
    ls -l /dev/kvm >&2 2>/dev/null || true
    exit 2
fi
"$EMULATOR" -accel-check

# Start ADB before the emulator so the emulator can register with a ready daemon.
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

# The debug APK is about 60 MB; keep enough internal data space for both
# the app APK and the instrumented test APK on older/16 KB system images.
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
        if [ "$boot_completed" = "1" ] && "$ADB" -s "$SERIAL" shell pm list packages >/dev/null 2>&1; then
            # sys.boot_completed can be set before system_server has published
            # ActivityManager. AndroidJUnitRunner/ddmlib invokes "am" during
            # install and test cleanup, so wait for that service explicitly.
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
"$ADB" -s "$SERIAL" shell input keyevent 82 >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm list packages | sed -n '1,5p'
echo "Running connectedDebugAndroidTest on Android API ${API_LEVEL}."
./gradlew --no-daemon connectedDebugAndroidTest
