#!/bin/sh
set -eu

if [ "$#" -ne 4 ]; then
    echo "Usage: verify-avd-metadata.sh AVD_METADATA AVD_CONFIG EXPECTED_TARGET EXPECTED_IMAGE_SYS_DIR" >&2
    exit 2
fi

AVD_METADATA="$1"
AVD_CONFIG="$2"
EXPECTED_TARGET="$3"
EXPECTED_IMAGE_SYS_DIR="$4"

if [ ! -f "$AVD_METADATA" ] || [ ! -f "$AVD_CONFIG" ]; then
    echo "AVD metadata or config file is missing." >&2
    exit 1
fi

ACTUAL_TARGET="$(sed -n 's/^target=//p' "$AVD_METADATA" | head -n 1)"
ACTUAL_IMAGE_SYS_DIR="$(sed -n 's/^image\.sysdir\.1=//p' "$AVD_CONFIG" | head -n 1)"

if [ "$ACTUAL_TARGET" = "$EXPECTED_TARGET" ]; then
    echo "AVD target verified: $ACTUAL_TARGET."
    exit 0
fi

# Android command-line tools 12.0 writes target=android-0 for the 37.0
# preview image. Do not trust that placeholder alone: accept it only when the
# generated AVD config points to the exact requested API/image/ABI directory.
case "$ACTUAL_IMAGE_SYS_DIR" in
    *"$EXPECTED_IMAGE_SYS_DIR")
        if [ "$EXPECTED_TARGET" = "android-37.0" ] && [ "$ACTUAL_TARGET" = "android-0" ]; then
            echo "::warning::avdmanager wrote target=android-0 for Android 37.0; the exact system-image path is verified, continuing."
            exit 0
        fi
        ;;
esac

echo "Invalid AVD metadata: expected target $EXPECTED_TARGET, found ${ACTUAL_TARGET:-<missing>}." >&2
echo "Expected image directory suffix: $EXPECTED_IMAGE_SYS_DIR" >&2
echo "Actual image directory: ${ACTUAL_IMAGE_SYS_DIR:-<missing>}" >&2
cat "$AVD_METADATA" >&2 || true
cat "$AVD_CONFIG" >&2 || true
exit 1
