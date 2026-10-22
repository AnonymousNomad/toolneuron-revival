#!/usr/bin/env bash
set -euo pipefail
outbox_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
apk_path=${1:-/workspace/artifacts/toolneuron-3.1.0-cloud.1-debug.apk}
python3 - "$outbox_dir/artifact-manifest.json" "$apk_path" <<'PY'
import hashlib,json,pathlib,sys
manifest=json.loads(pathlib.Path(sys.argv[1]).read_text())
apk=pathlib.Path(sys.argv[2])
assert apk.is_file() and apk.stat().st_size == manifest['bytes'], 'APK missing or size changed'
assert hashlib.sha256(apk.read_bytes()).hexdigest() == manifest['sha256'], 'APK hash differs from verified manifest'
assert manifest['gates']['release']=='PASS' and manifest['signing']=='ANDROID_DEBUG_TESTING_ONLY'
PY
gh release create v3.1.0-cloud.1 "$apk_path" "$outbox_dir/artifact-manifest.json" \
  --repo AnonymousNomad/toolneuron-revival \
  --target 92d212648c659a219f39b2b1b3fb6cea0765314d \
  --title 'ToolNeuron Revival 3.1.0-cloud.1 — DEVELOPMENT / TESTING' \
  --notes-file "$outbox_dir/RELEASE_NOTES.md" --prerelease
