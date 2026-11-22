#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${1:?Usage: check-16k-alignment.sh path/to/app-debug.apk}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}"
ZIPALIGN="$ANDROID_SDK_ROOT/build-tools/36.0.0/zipalign"

if [[ ! -f "$APK_PATH" ]]; then
  echo "::error::APK not found: $APK_PATH"
  exit 2
fi
if [[ ! -x "$ZIPALIGN" ]]; then
  echo "::error::zipalign not found: $ZIPALIGN"
  exit 2
fi
command -v unzip >/dev/null
command -v readelf >/dev/null

echo "Checking 16 KB ZIP alignment for uncompressed native libraries."
"$ZIPALIGN" -c -P 16 4 "$APK_PATH"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
unzip -q "$APK_PATH" 'lib/arm64-v8a/*.so' 'lib/x86_64/*.so' -d "$TMP_DIR"

checked=0
shopt -s nullglob
for ABI in arm64-v8a x86_64; do
  for LIB in "$TMP_DIR/lib/$ABI"/*.so; do
    checked=$((checked + 1))
    LOAD_ALIGNMENTS="$(readelf -lW "$LIB" | awk '$1 == "LOAD" { print $NF }')"
    if [[ -z "$LOAD_ALIGNMENTS" ]]; then
      echo "::error::No ELF LOAD segments found in $LIB"
      exit 1
    fi

    while IFS= read -r ALIGNMENT; do
      ALIGNMENT_BYTES=$((ALIGNMENT))
      if (( ALIGNMENT_BYTES < 16384 )); then
        echo "::error::16 KB ELF alignment failure: $LIB has LOAD alignment $ALIGNMENT"
        exit 1
      fi
    done <<< "$LOAD_ALIGNMENTS"
    echo "ALIGNED: lib/$ABI/$(basename "$LIB")"
  done
done

if (( checked == 0 )); then
  echo "::error::No arm64-v8a or x86_64 native libraries were found in the APK."
  exit 1
fi

echo "16 KB compatibility pre-check passed for $checked 64-bit native libraries."
