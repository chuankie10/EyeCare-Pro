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
EMULATOR_LOG="${RUNNER_TEMP:-/tmp}/eyecare-emulator-api-${API_LEVEL}.log"
EMULATOR_PID=""

cleanup() {
    if [ -n "$EMULATOR_PID" ]; then
        adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true
        kill "$EMULATOR_PID" >/dev/null 2>&1 || true
        wait "$EMULATOR_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT HUP INT TERM

if [ ! -x "$AVD_MANAGER" ] || [ ! -x "$EMULATOR" ]; then
    echo "Android emulator tools are missing. Install cmdline-tools, emulator, and the selected system image first." >&2
    exit 2
fi

printf 'no\n' | "$AVD_MANAGER" create avd \
    --name "$AVD_NAME" \
    --package "$IMAGE_PACKAGE" \
    --device pixel_2 \
    --force

"$EMULATOR" \
    -avd "$AVD_NAME" \
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
max_attempts=600
while [ "$attempt" -lt "$max_attempts" ]; do
    state="$(adb -s "$SERIAL" get-state 2>/dev/null || true)"
    if [ "$state" = "device" ]; then
        boot_completed="$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
        if [ "$boot_completed" = "1" ] && adb -s "$SERIAL" shell pm list packages >/dev/null 2>&1; then
            ready=1
            break
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
    echo "Emulator did not become ready within $((max_attempts * 2)) seconds." >&2
    adb devices -l >&2 || true
    tail -n 160 "$EMULATOR_LOG" >&2 || true
    exit 1
fi

# Unlock when accepted; this is not part of the readiness gate.
adb -s "$SERIAL" shell input keyevent 82 >/dev/null 2>&1 || true
adb -s "$SERIAL" shell pm list packages | sed -n '1,5p'
echo "Running connectedDebugAndroidTest on Android API ${API_LEVEL}."
./gradlew --no-daemon connectedDebugAndroidTest
