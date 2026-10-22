#!/usr/bin/env bash
set -euo pipefail

LEVEL="${1:-fast}"

echo "== ToolNeuron cloud verification: ${LEVEL} =="
git diff --check

if [[ ! -f ./gradlew ]]; then
  echo "gradlew missing"
  exit 1
fi
chmod +x ./gradlew

case "$LEVEL" in
  fast)
    ./gradlew :app:testDebugUnitTest --no-daemon --console=plain
    ;;
  slice)
    ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
    ;;
  release)
    ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
    echo "Cloud build gate complete. Release signing and physical device acceptance are separate gates."
    ;;
  *)
    echo "usage: scripts/cloud_verify.sh [fast|slice|release]"
    exit 2
    ;;
esac

echo "VERIFICATION_PASS level=$LEVEL"
