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

if [ "$ACTUAL_TARGET" != "$EXPECTED_TARGET" ]; then
    echo "Invalid AVD metadata: expected target $EXPECTED_TARGET, found ${ACTUAL_TARGET:-<missing>}." >&2
    cat "$AVD_METADATA" >&2 || true
    exit 1
fi

case "$ACTUAL_IMAGE_SYS_DIR" in
    *"$EXPECTED_IMAGE_SYS_DIR")
        echo "AVD target and system image verified: $ACTUAL_TARGET ($ACTUAL_IMAGE_SYS_DIR)."
        ;;
    *)
        echo "Invalid AVD system image: expected suffix $EXPECTED_IMAGE_SYS_DIR, found ${ACTUAL_IMAGE_SYS_DIR:-<missing>}." >&2
        cat "$AVD_CONFIG" >&2 || true
        exit 1
        ;;
esac
