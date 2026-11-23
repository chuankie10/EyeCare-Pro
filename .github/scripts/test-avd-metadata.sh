#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
VERIFY="$SCRIPT_DIR/verify-avd-metadata.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM

cat > "$TMP_DIR/api24.ini" <<'EOF'
target=android-24
EOF
cat > "$TMP_DIR/api24-config.ini" <<'EOF'
image.sysdir.1=system-images/android-24/default/x86_64/
EOF
sh "$VERIFY" "$TMP_DIR/api24.ini" "$TMP_DIR/api24-config.ini" \
    "android-24" "system-images/android-24/default/x86_64/"

cat > "$TMP_DIR/api35.ini" <<'EOF'
target=android-35
EOF
cat > "$TMP_DIR/api35-config.ini" <<'EOF'
image.sysdir.1=system-images/android-35/google_apis/x86_64/
EOF
sh "$VERIFY" "$TMP_DIR/api35.ini" "$TMP_DIR/api35-config.ini" \
    "android-35" "system-images/android-35/google_apis/x86_64/"

# Regression case observed with command-line tools 12.0 and the API 37.0 image.
cat > "$TMP_DIR/api37.ini" <<'EOF'
target=android-0
EOF
cat > "$TMP_DIR/api37-config.ini" <<'EOF'
image.sysdir.1=system-images/android-37.0/google_apis_ps16k/x86_64/
EOF
sh "$VERIFY" "$TMP_DIR/api37.ini" "$TMP_DIR/api37-config.ini" \
    "android-37.0" "system-images/android-37.0/google_apis_ps16k/x86_64/"

# A mismatched image must still be rejected even when avdmanager wrote android-0.
cat > "$TMP_DIR/api37-wrong-config.ini" <<'EOF'
image.sysdir.1=system-images/android-35/google_apis/x86_64/
EOF
if sh "$VERIFY" "$TMP_DIR/api37.ini" "$TMP_DIR/api37-wrong-config.ini" \
    "android-37.0" "system-images/android-37.0/google_apis_ps16k/x86_64/"; then
    echo "Verifier incorrectly accepted an API 35 image as API 37." >&2
    exit 1
fi

echo "AVD metadata verifier tests passed (API 24, API 35, API 37.0 fallback, mismatch rejection)."
