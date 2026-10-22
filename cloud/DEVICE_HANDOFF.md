# DeepSeek / Samsung device handoff — Cloud lane frozen

Classification: **TOOLNEURON_CLOUD_DEVELOPMENT_HANDOFF_READY**. This is a reproducible-source development handoff, not production or physical acceptance. Do not start another feature wave. Do not merge PR #1 or mark it ready. The next lane is local rebuild and physical verification.

## Canonical checkpoint

| Field | Value |
| --- | --- |
| Repository | `https://github.com/AnonymousNomad/toolneuron-revival.git` |
| Frozen branch | `cloud-handoff-saul` |
| Application-source SHA | `92d212648c659a219f39b2b1b3fb6cea0765314d` |
| Development tag | `v3.1.0-cloud.1` (peels to the application-source SHA) |
| VersionName / versionCode | `3.1.0-cloud.1` / `40` |
| ApplicationId | `com.anonymousnomad.toolneuron.covert` |
| Previous receipt checkpoint | `e16c2a8e4a92323ae229204bf1bc388d6046b2a3` |
| Final handoff/receipt HEAD | Capture the full `HANDOFF_HEAD` below from the frozen remote branch before source checkout; it is also returned explicitly in the owner-facing freeze receipt. |

The final receipt commit contains this file and cannot embed its own content-dependent commit hash. Resolve and record that exact final HEAD, not the previous checkpoint or application SHA. Documentation-only commits after application source do not change which source generated an APK.

```bash
git clone --branch cloud-handoff-saul https://github.com/AnonymousNomad/toolneuron-revival.git
cd toolneuron-revival
git fetch origin cloud-handoff-saul --tags
HANDOFF_HEAD=$(git rev-parse refs/remotes/origin/cloud-handoff-saul)
printf '%s\n' "$HANDOFF_HEAD"
git rev-parse 'v3.1.0-cloud.1^{commit}'
# Keep the frozen instructions/evidence outside the checkout before detaching.
mkdir -p ../toolneuron-device-handoff
cp cloud/DEVICE_HANDOFF.md cloud/FINAL_VERIFICATION.json ../toolneuron-device-handoff/
printf '%s\n' "$HANDOFF_HEAD" > ../toolneuron-device-handoff/HANDOFF_HEAD.txt
git checkout --detach 92d212648c659a219f39b2b1b3fb6cea0765314d
git rev-parse HEAD
git status --porcelain=v1
```

Expected detached HEAD is the full application-source SHA and the product tree is clean. Keep the handoff snapshot available: the older application commit predates this final documentation. Its older autonomous queue does not authorize another Cloud feature wave.

## Build environment

Cloud-verified tools: Temurin **JDK 21.0.8+9**, SDK platforms **36, 36.1, 37.0**, Build Tools **36.0.0 / 37.0.0**, NDK **28.2.13676358**, CMake **3.31.4**, Gradle wrapper **9.4.1**. The wrapper pins distribution SHA-256 `2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb`. Git, Android command-line tools/platform-tools, and a C/C++17 host compiler are needed. No database/server startup is required.

On the retained Cloud environment only, `source /workspace/.setup/activate.sh`. That path is not a portable prerequisite. On a fresh host, install JDK 21 and Android command-line tools, then configure the host's actual paths (examples below). Do not copy Cloud proxy/CA paths to a laptop or disable TLS. Use the host's approved proxy and CA only if required. Do not set `ANDROID_SDK_HOME`; it conflicts with Android user-home handling.

Bash activation example (replace the two installation paths):

```bash
export JAVA_HOME=/actual/path/to/jdk-21
export ANDROID_HOME=/actual/path/to/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/37.0.0:$PATH"
java -version
sdkmanager 'platform-tools' 'platforms;android-36' 'platforms;android-36.1' 'platforms;android-37.0' 'build-tools;36.0.0' 'build-tools;37.0.0' 'ndk;28.2.13676358' 'cmake;3.31.4'
sdkmanager --licenses
./gradlew --version
```

The SDK package is **`platforms;android-37.0`**, not `platforms;android-37`. The app compiles against 37; native library modules also require 36.1. `ANDROID_HOME` or ignored `local.properties` must locate the local SDK. Do not commit that file.

### Reproduce the Cloud native build selection

Cloud used four Gradle workers, Ninja `-j4`, and this environment-only init script to build the shipped `hxs_encryptor` target rather than unrelated upstream crypto test executables. This does not exclude application/JVM tests or alter tracked source. Create it in the local Gradle user home before building. Bash example:

```bash
TN_GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}"
mkdir -p "$TN_GRADLE_CACHE/init.d"
printf '\norg.gradle.workers.max=4\n' >> "$TN_GRADLE_CACHE/gradle.properties"
cat > "$TN_GRADLE_CACHE/init.d/toolneuron-native-targets.gradle" <<'GRADLE'
allprojects { project ->
    project.plugins.withId('com.android.library') {
        if (project.name == 'hxs_encryptor') {
            project.extensions.getByName('android').defaultConfig.externalNativeBuild.cmake.targets.add('hxs_encryptor')
        }
    }
}
GRADLE
export CMAKE_BUILD_PARALLEL_LEVEL=4
```

The retained Cloud SDK additionally wraps its Ninja binary with `-j4`. On a fresh host use bounded native parallelism appropriate to the host; preserve the original SDK binary and do not copy platform-specific binaries. A small-host OOM is an environment failure, not license to remove ABIs, disable tests, or alter the application source. Dependencies fetched by CMake include pinned BoringSSL, liboqs, LZ4, cpp-httplib and nlohmann/json. Diagnose the actual Git/CMake failure; a failed direct HTTP probe alone does not prove Git fetch fails. Keep existing certificate/checksum validation.

### Exact build and test commands

Run sequentially from the exact source checkout:

```bash
bash scripts/cloud_verify.sh fast
bash scripts/cloud_verify.sh slice
bash scripts/cloud_verify.sh release
./gradlew :app:testDebugUnitTest --rerun :app:assembleDebug -Ptn.sourceSha=92d212648c659a219f39b2b1b3fb6cea0765314d --max-workers=4 --no-daemon --console=plain
```

The last command forces the JVM test task and stamps the artifact. **Use the explicit `-Ptn.sourceSha=...` argument.** The gate script does not forward this property; its default builds can contain UNRECORDED. Do the source-stamped build last. Do not substitute the receipt HEAD or tag-object SHA for the application-source commit.

On Windows PowerShell, use the installed JDK/SDK Windows paths and `sdkmanager.bat`; set `$env:JAVA_HOME`, `$env:ANDROID_HOME`, `$env:ANDROID_SDK_ROOT` and `$env:Path` accordingly. Put the same init-script text at `$env:USERPROFILE\.gradle\init.d\toolneuron-native-targets.gradle` (or the selected `GRADLE_USER_HOME`). The exact source-stamped command is:

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun :app:assembleDebug -Ptn.sourceSha=92d212648c659a219f39b2b1b3fb6cea0765314d --max-workers=4 --no-daemon --console=plain
```

Use Git Bash for the three `scripts/cloud_verify.sh` gates, with valid JDK/SDK paths. Alternatively execute their documented equivalent Gradle tasks sequentially and retain the command/output for each named gate. Windows setup and a fresh-machine build are the local worker's verification, not already proven by Cloud.

Output: `app/build/outputs/apk/debug/app-debug.apk`. JUnit XML: `app/build/test-results/testDebugUnitTest/`. Copy outputs/logs to a private device-evidence directory; do not commit user data, models, logs with secrets, keys or screenshots.

### Verify the locally built APK

```bash
git rev-parse HEAD
cat app/build/outputs/apk/debug/output-metadata.json
apkanalyzer manifest application-id app/build/outputs/apk/debug/app-debug.apk
apkanalyzer manifest version-name app/build/outputs/apk/debug/app-debug.apk
apkanalyzer manifest version-code app/build/outputs/apk/debug/app-debug.apk
apkanalyzer manifest min-sdk app/build/outputs/apk/debug/app-debug.apk
apkanalyzer manifest target-sdk app/build/outputs/apk/debug/app-debug.apk
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

On PowerShell use `Get-FileHash -Algorithm SHA256` and the SDK tools' `.bat`/`.exe` variants. Expect package/version above, minSdk 31, targetSdk 37, and both `arm64-v8a` and `x86_64` in `lib/`. Inspect `app/build/generated/source/buildConfig/debug/com/dark/tool_neuron/BuildConfig.java` for exact `SOURCE_REVISION`. Also verify the SHA is present in the APK dex (not only the generated file):

```bash
python3 - <<'PY'
import zipfile
expected = b'92d212648c659a219f39b2b1b3fb6cea0765314d'
with zipfile.ZipFile('app/build/outputs/apk/debug/app-debug.apk') as apk:
    abis = {name.split('/')[1] for name in apk.namelist() if name.startswith('lib/') and name.endswith('.so')}
    assert abis == {'arm64-v8a', 'x86_64'}
    assert any(expected in apk.read(name) for name in apk.namelist() if name.endswith('.dex'))
print('ABI and embedded source revision verified')
PY
```

Record the local APK's own size, SHA-256 and signer. Local debug signing generally uses a different key; do not transfer Cloud private signing material. Equality with the Cloud APK hash/certificate is not an acceptance requirement. Signature compatibility with an existing installation is a separate install gate.

## Cloud APK reference and transfer reality

| Field | Preserved Cloud reference |
| --- | --- |
| Filename | `toolneuron-3.1.0-cloud.1-debug.apk` |
| Bytes | `110407682` |
| SHA-256 | `1ea9c32bbb20ea531a1a12bc5ff2178c1c18e793a3e0efa6f44b3eac952c4148` |
| Signer certificate SHA-256 | `fa6f4eb2390691f3c18115afe8ada0283516f8eabdd74eb54c3b127bf52f53af` |
| Embedded application source | `92d212648c659a219f39b2b1b3fb6cea0765314d` |
| ABIs / minSdk / targetSdk | `arm64-v8a`, `x86_64` / `31` / `37` |

The reference survives in the retained Cloud workspace at `/workspace/artifacts/toolneuron-3.1.0-cloud.1-debug.apk`; this is not a public download URL or a required laptop path. GitHub has the source/tag but **no Release for this tag and no uploaded APK**. The primary transfer is exact-source reproduction, not downloading these exact bytes. No new storage system was created.

Fresh Cloud freeze verification rebuilt the same source to `110407682` bytes with SHA-256 `de4a11b9bac13b3cf4b79ccbc97767e1520ef3df9c16e0cd438a5aec0f95ccf3`, the same public debug certificate/package/version/ABIs/source revision. Its retained filename is `toolneuron-3.1.0-cloud.1-final-verification-debug.apk`. The reference was not overwritten. Byte-for-byte reproducibility is **not demonstrated**; require correct source/identity/gates and record the local result's hash.

Existing release upload instructions remain at `ops/outbox/releases/` on the handoff branch. GitHub API read became available during finalization and confirmed PR/release status, but no upload or other publication was attempted in this freeze. A later authorized owner may use the existing release mechanism after verifying bytes; no download claim exists until a real asset URL is recorded.

## Verified Cloud gates

Application source was checked out detached at the exact SHA, with no product changes. Fresh FAST, SLICE, RELEASE and final explicit-source build passed. Final JVM task was forced to execute: **222 tests / 0 failures / 0 errors / 0 skipped**. Both native ABIs, actual package/version/minSdk/targetSdk, APK signature and dex source revision were verified.

Five freshly compiled host native recovery fixtures passed (four malformed collection forms and one partial WAL). They verify host collection/WAL behavior, not Android encrypted JNI or power-loss acceptance. To rerun on a C++17 host after Gradle fetched LZ4, use a fresh temporary directory:

```bash
TN_RECOVERY_TMP=$(mktemp -d)
TN_LZ4_DIR=$(python3 - <<'PY'
import glob, os
matches = sorted(glob.glob('hxs/.cxx/**/_deps/lz4-src/lib/lz4.c', recursive=True))
assert matches, 'Build native dependencies first'
print(os.path.dirname(matches[0]))
PY
)
mkdir -p "$TN_RECOVERY_TMP/include/android"
: > "$TN_RECOVERY_TMP/include/android/log.h"
cc -c "$TN_LZ4_DIR/lz4.c" -o "$TN_RECOVERY_TMP/lz4.o"
c++ -std=c++17 -pthread -I "$TN_RECOVERY_TMP/include" -I hxs/src/main/cpp -I "$TN_LZ4_DIR" hxs/src/test/cpp/recovery_test.cpp hxs/src/main/cpp/collection.cpp hxs/src/main/cpp/wire_format.cpp hxs/src/main/cpp/index.cpp hxs/src/main/cpp/wal.cpp "$TN_RECOVERY_TMP/lz4.o" -o "$TN_RECOVERY_TMP/recovery-test"
"$TN_RECOVERY_TMP/recovery-test" "$TN_RECOVERY_TMP/data"
```

The empty header substitutes only an unused Android logging include for host compilation; JNI is not stubbed or claimed tested. Do not compile the assert-based host fixture with `NDEBUG`.

Changed application-delta scan: **136 files**, no credential/private-key/distribution-artifact matches, no disabled-test candidates, no product `/workspace/` path leaks. Intentionally public documentation/build evidence contains generic Cloud tool paths and historical laptop paths; these are context, not product dependencies or secrets. MIT LICENSE is unchanged, preserving Siddhesh Sonar attribution. Independent final audit found no additional concrete P0/P1. Known P2s remain in `cloud/FINAL_RECEIPT.md`; they are not authorization to reopen Cloud implementation.

Machine-readable fresh evidence is `cloud/FINAL_VERIFICATION.json` at the handoff HEAD. The original artifact manifest remains a reference receipt; its freeze-verification supplement records the newer build separately.

## Not verified in Cloud

Physical Samsung install; upgrade behavior; UI/navigation; app lock; SAF; process death; WebView; screenshot/export/share; actual GGUF inference; generation streaming; cancel/reload; TTFT; tokens/sec; memory/RSS; thermal; VLM/projector; crash/power-loss recovery; production signing; production Covert Engine R0. All remain **NOT TESTED** or explicitly blocked. JVM doubles and build success do not close any of these gates.

Target: **Samsung SM-X528U, Android 16 / SDK 36, arm64-v8a**. Verify those facts before installation; do not silently substitute another device.

## Device battery — record PASS / FAIL / BLOCKED / NOT TESTED per step

Use a private evidence directory and record command exit codes, timestamps, device/model/runtime identity, screenshots and relevant logs. Redact secrets/private material before sharing. Never use root, clear app data, uninstall, downgrade, rotate signing keys or change account/security configuration to make a test pass.

1. **Clone:** execute the canonical clone/fetch block, capture final `HANDOFF_HEAD`, save these instructions outside the checkout. Confirm origin is the canonical repository.
2. **Exact source:** detach at `92d212648c659a219f39b2b1b3fb6cea0765314d`; verify HEAD and clean product tree, tag target and unchanged MIT license. Do not build receipt HEAD while reporting the application SHA.
3. **Reproduce build:** configure the local toolchain/init selection above, execute FAST → SLICE → RELEASE sequentially, then the exact explicit-property build. Record OS/JDK/SDK/NDK/CMake/Gradle versions and failures without patching application source silently.
4. **Local tests:** inspect all JUnit XML; expect 222 / 0 / 0 / 0. Run five host recovery fixtures where supported. Mark host-tool unavailability separately from Android runtime failures.
5. **APK identity:** verify package, version 3.1.0-cloud.1/code40, min31/target37, both ABIs, signature and embedded application SHA. Record local bytes/hash/certificate. No byte-equality requirement against Cloud.
6. **Inspect before mutation:** select the intended `adb` serial using `adb devices -l`. Run `adb shell getprop ro.product.model`, `adb shell getprop ro.build.version.release`, `adb shell getprop ro.build.version.sdk`, `adb shell getprop ro.product.cpu.abi`. Require SM-X528U/16/36/arm64-v8a. Run `adb shell pm list packages com.anonymousnomad.toolneuron.covert`, `adb shell dumpsys package com.anonymousnomad.toolneuron.covert`, and `adb shell pm path com.anonymousnomad.toolneuron.covert`; record installed version/user/splits. If installed, pull the reported base APK to the private evidence folder and run `apksigner verify --print-certs` on it.
7. **Preserve data:** if installed, use its supported export/backup facilities to preserve projects, chats, settings and test artifacts with the owner. Confirm exports can be read. App-private encrypted data may depend on Android Keystore; raw file copies and deprecated `adb backup` are not proven restores. If preservation or signer compatibility is uncertain, stop installation and report BLOCKED. Do not expose user data or keys to Git/Cloud.
8. **Safe install/upgrade:** if absent, `adb install app/build/outputs/apk/debug/app-debug.apk`. If present, upgrade only when installed signer and local signer are compatible and installed version is not newer, then `adb install -r app/build/outputs/apk/debug/app-debug.apk`. No `-d`, uninstall, `pm clear`, or signature bypass. On mismatch, record `BLOCKED_SIGNATURE_MISMATCH` and request an owner-approved safe test arrangement; do not sacrifice existing data. A data-free install cannot prove an upgrade gate.
9. **Launch:** run `adb shell am start -W -n com.anonymousnomad.toolneuron.covert/com.dark.tool_neuron.activity.MainActivity`. Record startup result and actual UI. Capture app/inference-process logs using their actual PIDs; do not clear pre-existing logs or infer crash freedom from an empty filtered stream.
10. **Onboarding:** verify Terms/security/theme/model/RAG/Cipher/Abilities progression, useful empty states, explicit skip paths and retained progress. Do not bypass auth, weak-PIN rules or license policy. A missing model must not be labeled READY.
11. **Cipher:** verify EMPTY, explicit artifact/runtime/4K-or-8K assignment and UNQUALIFIED until exact-scope real Model Lab smoke receipts exist. Missing Covert runtime must show unavailable without fallback. If a safe GGML model is available later, run batteries and only then test resident startup/context isolation; broad skills/permissions are not conferred by smoke PASS.
12. **Abilities:** confirm default denial. Grant only capabilities required for this battery; revoke and retry to confirm denial/recovery. Availability must not silently grant execution. Record any legitimate security/license prerequisite as BLOCKED, never disable it.
13. **Desktop/workstation:** open Start Center, Chat, Projects, Files, Evidence, Models, Creator, TV/Game and Beta Report. Verify empty/loading/error states, consistent project chips and navigation/back behavior without blank dead ends.
14. **Taskbar/windows:** open multiple windows, focus/minimize/close/reopen and use Back. Relaunch later and verify the persisted window/taskbar/project state; distinguish resume from intentionally closed windows.
15. **Connections:** inspect Laptop Connections/runtime/provider states. Missing credentials/native components stay unavailable. Do not configure paid providers or claim transport pairing/playback from interface metadata.
16. **Local model import:** use an owner-authorized GGUF with recorded SHA/license/size and supported GGML architecture. Import as chat or embedding type explicitly; verify durable app-owned FILE registration only after copy, failure/cancel feedback and no partial item falsely installed. Models must fit current device admission. Do not commit the model.
17. **Picker continuity:** open local-model picker, background/return through app lock, unlock and complete/cancel the type selection. Verify selection stays pinned, no import starts before type choice, setup exposes Continue after success and retries do not duplicate rows. Cancel/error/relaunch should not silently publish a partial model.
18. **Vibe:** create a small web/utility/game template; edit/save/build/check/preview/capture/export under its grant. Confirm actual WebView execution and exported bytes on device. Build a new FAIL and select an earlier PASS snapshot without relabeling current source. Open an existing Chat-created project without src and explicitly initialize in place; verify ID/bindings/files preserved. Test recovered draft review/save/discard and a lost document target's retry message.
19. **Creator:** create a small Series with season/episode/scene/shot, character bible, declared visual reference/rights/provenance and voice/music slot. Edit duration/dialogue and restart. Review/export exact manifest, verify hierarchy/references/job dependencies/continuity flags and project identity. All render jobs/providers remain BLOCKED/UNCONFIGURED; no media is synthesized. Cross-project references must not be adopted.
20. **Beta Report:** enter harmless reproduction notes, prepare and inspect exact JSON/ZIP hash. Add/remove a nonprivate screenshot via picker across lock, inspect the included sanitized image, then explicitly share to a local recipient. Verify ZIP content and error/oversize recovery. No automatic upload or transcripts/project source/model paths/keys/device IDs should appear. Recovery candidates are preserved, not deleted.
21. **Terminal/workstation:** grant the bounded terminal capability; inspect `pwd`, create a harmless named text file in the selected test workspace, then read it from Files and Terminal. Repeat in another project and verify project selection/context does not leak. No root/destructive commands or arbitrary frame/game control. TV/Game rights remain declarations with unavailable execution.
22. **Save/restart:** create two test projects, a task, Chat binding, saved source/draft and Creator plan. Wait for “Draft saved locally” before testing durability. Close/reopen app and confirm IDs, file content, bindings, evidence and assignment persist; old preview snapshots stay immutable. Do not treat unsaved 250ms pending text as durable.
23. **App lock:** background and return from each sensitive surface, including external pickers/share. Authentication must precede resumed UI/actions. Confirm permission revocation, lock during report/export and project change produce honest retry/denial, not authority leakage. Keep Android security boundaries intact.
24. **Process death/relaunch:** after saving, background the app, then use `adb shell am kill com.anonymousnomad.toolneuron.covert`; verify whether its actual PIDs disappeared before claiming death. If protected by a running foreground service, record that fact. Separately test `adb shell am force-stop com.anonymousnomad.toolneuron.covert` and relaunch (force-stop is a separate condition, not proof of OS low-memory death). Neither command clears data. Verify auth-first recovery, projects/drafts/tasks and honest interrupted document/Model Lab state. Do not simulate power loss on existing user data.
25. **Actual GGUF inference:** if model/runtime prerequisites are present, select explicit GGML, load the recorded artifact, send harmless text and verify real response. Run Model Lab on the actual device and export observed evidence; retain exact artifact/runtime/profile/chassis/device scope. If no suitable model, mark this battery BLOCKED_MODEL_PREREQUISITE. Missing Covert R0 does not block existing GGML testing.
26. **Streaming/cancel/reload:** observe token streaming, cancel mid-generation, reload and send again. Switch Chat/Model Lab/Cipher loads and verify stale operations fail rather than silently executing on the replacement. Include web-search synthesis only with explicit network permission. Record real TTFT/token rate only if measured; collect RSS/thermal with a defined tool/time interval or leave UNKNOWN. Test VLM only with a compatible model/projector and recorded prerequisites; otherwise mark BLOCKED, not PASS.
27. **Clean regression:** repeat launch/unlock, one canonical Chat→project→Vibe/Files/Terminal journey, Creator restart/manifest and Beta Report share. Check own-process crash/ANR logs and data integrity. Return per-step evidence plus all blocked/unrun gates; retain source/local APK/certificate/device identities. Do not merge or mark PR ready. Stop at owner review.

## Provenance, remaining gates and merge policy

Laptop checkpoint **`4599afbd956a0f111c471ec82d3c367bb76f9ae0` / 3.0.8-covert** remains unreconciled because exact bytes were never transferred. This line is the reconstructed successor **3.1.0-cloud.1**; passing device tests does not prove laptop equivalence.

Production Covert Engine R0 **`3020b7f407b61addf872e6aff30d689d71ef0f20`** remains **BLOCKED_LOCAL_SOURCE_UNAVAILABLE**, issue #2. Do not fabricate JNI, substitute another engine, or count GGML PASS as Covert PASS. Real Covert ARM64 build/JNI/load/stream/cancel tests cannot run until exact provenance-controlled source arrives. Retain UNKNOWN/NOT TESTED for its native measurements.

PR #1 is confirmed **OPEN / DRAFT / UNMERGED**. Freeze does not change that state. Merge order is **CLOUD COMPLETE → LOCAL REBUILD → PHYSICAL SAMSUNG ACCEPTANCE → LAPTOP RECONCILIATION → OWNER REVIEW → MERGE DECISION**. Production signing and external Release/Floot/Discord publication remain separate gates. Preserve prepared outboxes; do not invent a production URL or send community messages from this handoff.

Return the final handoff HEAD, application-source SHA, local APK hash/certificate, device identity, exact commands, gate counts, per-step outcomes and defects to the owner. Do not silently patch application source while reporting this frozen source as verified. An actual release defect requires its own new source SHA and full re-verification; optional P2s stay follow-ups.
