# ToolNeuron Revival

A local-first Android AI workstation: Chat, canonical projects, Cipher, Files, governed Terminal, Vibe Studio, Model Lab, Creator and TV/Game companion foundations.

**3.1.0-cloud.1 / code 40 — DEVELOPMENT / TESTING.** This cloud reconstruction is distinct from the untransferred laptop 3.0.8 line. Automated JVM and debug-build gates pass; physical Android acceptance and actual model qualification remain **NOT TESTED**. Exact source/APK/signer evidence and publication status are recorded in [the cloud receipt](cloud/FINAL_RECEIPT.md).

[Source](https://github.com/AnonymousNomad/toolneuron-revival) · [Releases](https://github.com/AnonymousNomad/toolneuron-revival/releases) · [Issues](https://github.com/AnonymousNomad/toolneuron-revival/issues)

The canonical website is the existing Floot **Covert Nexus** project, `01147976-3673-42c7-9220-2d028594ba86`. No production website or APK download URL is invented when publication is pending.

## Workstation journeys

- Bind Chat to one canonical project; open that same project in Vibe, Files or Terminal. Cipher context stays scoped to its pinned project.
- Explicitly assign Cipher's artifact/runtime/chassis. Installation, compatibility, current device fit, qualification and permission are separate states.
- Run local text smoke batteries in Model Lab; observed outputs yield PASS/FAIL/UNKNOWN and reviewed evidence export. JVM test doubles are not model qualification.
- Create a web, utility or game template in Vibe; edit/save source, build/check immutable bundles, preview offline, capture and export. Saved drafts recover locally; pending edits are durable only after the saved message.
- Keep Story/Series seasons, episodes, scenes, shots, character bibles, declared asset rights/provenance and audio slots in Creator. Export a production plan. Render providers remain UNCONFIGURED; no generated media is claimed.
- Register legal-source declarations in TV/Game companion. Playback and device/game transports remain UNCONFIGURED; model intents cannot control frame timing or raw inputs.
- Prepare a bounded beta-report ZIP with reproduction notes and optional screenshot; review exact contents before explicit system sharing. No silent telemetry upload.

The existing GGML/llama.cpp, vision, RAG, voice, HTTP server, model browser and plugin subsystems are preserved. Covert Engine R0 is behind an explicit unavailable backend boundary: exact source `3020b7f407b61addf872e6aff30d689d71ef0f20` remains laptop-only ([issue #2](https://github.com/AnonymousNomad/toolneuron-revival/issues/2)). There is no silent backend/provider fallback.

## Verification

Use JDK 21, Android SDK 37.0, NDK 28.2.13676358 and CMake 3.31.4. In the prepared cloud environment, source `/workspace/.setup/activate.sh`, then run `bash scripts/cloud_verify.sh fast`, `slice` or `release` sequentially. Release signing requires configured signing material; the cloud artifact is debug-signed for testing. Physical app-lock, SAF, WebView, native model and Samsung acceptance must be performed on a real device. Do not uninstall an existing data-bearing installation to bypass a signer mismatch.

The following technical sections describe the inherited upstream subsystems; upstream provenance is Siddhesh2377/ToolNeuron (MIT). Current cloud capabilities and evidence above take precedence over older scope notes.

## Architecture

Three processes.

- `:app` is the UI and where trust decisions live.
- `:inference` runs `InferenceService` over the GGUF engine and sherpa-onnx. Dies with the app.
- `:server` is foreground (`dataSync`, `stopWithTask=false`) so it survives swipe-from-recents and keeps the HTTP listener alive.

Modules:

| Module | Purpose |
|---|---|
| `:app` | UI, viewmodels, Hilt graph |
| `:hxs` | Encrypted KV store with C++ core |
| `:hxs_encryptor` | Argon2id / AEAD / BoringSSL / ML-KEM-768 / ML-DSA-65 / Ed25519, plus the native policy + boot-integrity stack |
| `:native-server` | Embedded HTTP server (cpp-httplib + nlohmann/json) |
| `:download_manager` | Native downloader with JNI bridge |
| `:networking` | Network primitives in jniLibs |
| `:plugin-api` | Pure-Kotlin plugin contract — the only thing plugin authors compile against |
| `:plugin-exc` | Plugin runtime — DexClassLoader, capability gate, HF catalog client, dock |
| `:plugins:*` | First-party plugins (notes, counter, expense) — each is its own Android application module |

## Security

Trust decisions live in C++ so an obfuscation bypass at the Kotlin layer doesn't grant access.

- Auth is Argon2id (t=4, m=128 MiB, p=1, outLen=32). PIN must be 6 digits and pass weak-PIN rejection.
- After verify, a 32-byte opaque session token registers with the native `PolicyEngine`. Every gated feature crosses JNI as `PolicyEngine.isAllowed(Feature, sessionToken)`.
- DEK is wrapped by an Android Keystore AES-256-GCM key (StrongBox-preferred, TEE fallback) and stored XOR-masked at `<filesDir>/app_bootstrap/k.bin`. The crypto is the wrapped ciphertext; the XOR is anti-grep.
- Encrypted prefs at `<filesDir>/app_prefs/` are sealed under `HKDF(DEK, "tn.app_prefs.user_key.v1")`. `AuthState` gets a second AEAD layer keyed on `HKDF(DEK, "tn.app_prefs.auth_key.v1")`.
- Lockout: three free, then `1m → 5m → 15m → 1h → 4h → 12h → 24h`. Tenth wipes device-side state. Clock rollback past five minutes is double-penalized.
- Panic PIN, if set, triggers `hardWipe()` and returns `VerifyResult.Wiped` — indistinguishable from "attempts exceeded".
- TOFU manifest of every `.so` in `nativeLibraryDir`, rebound to install identity (`{signerHash, longVersionCode, lastUpdateTime}`). Hook baseline verify catches inline hooks.
- Debugger / Frida / Xposed scans run before any auth path. Detection strings are XOR-obfuscated at compile time so `strings libhxs_encryptor.so | grep -i frida` returns nothing.
- `FLAG_SECURE` is on for PIN entry only. Chats stay screenshottable.

`CLAUDE.md` is authoritative for the rest, including the planned pro-license hook (`PolicyEngine.is_pro_feature(fid >= 1000)`).

Found a vulnerability? Email <siddheshsonar2377@gmail.com> instead of opening a public issue.

## Plugins

Plugins live at [`Void2377/tool-neuron-plugins`](https://huggingface.co/Void2377/tool-neuron-plugins) on HuggingFace. Each one is a zip — `manifest.json` + `classes*.dex` + optional `lib/<abi>/*.so` — under `plugins/<id>/<version>/`. The app reads `plugins.json` on every screen open. No local cache. The repo is the source of truth, every time.

Install flow: tap a plugin in the in-app store, runtime streams the zip into `cacheDir`, verifies SHA-256 against the manifest, extracts via `PluginBundle`, locks dex/so as read-only (Android 14+ rejects writable dex), deletes the temp file. The plugin's classes load through `DexClassLoader` with `plugin-api` as the parent, and the host calls into `Plugin.Content()` — a `@Composable` that owns its own theme and scaffold.

Capabilities are declared in the manifest and gated by the host: `hxs.read` / `hxs.write` for storage, `ai.onnx` for ORT sessions, `internet` for network, plus camera, mic, filesystem, notifications, clipboard. If a plugin tries something it didn't declare, the gate throws `SecurityException`.

To add to the public store:

```sh
./gradlew :plugins:<name>:packagePlugin
# drop the zip under plugins/<id>/<version>/, add an entry to plugins.json
hf upload Void2377/tool-neuron-plugins . .
```

## Build

```sh
# Dev loop
./gradlew :app:compileDebugKotlin
./gradlew :app:installDebug

# Release (R8 + resource shrink)
./gradlew :app:assembleRelease
```

Release signing reads from `local.properties`:

```properties
TN_KEYSTORE_PATH=/abs/path/to/keystore.jks
TN_KEYSTORE_PASSWORD=...
TN_KEY_ALIAS=...
TN_KEY_PASSWORD=...
```

Missing keys fall back to an unsigned release so the dev flow stays open. `compileSdk 37`, `minSdk 31`, ABI filters `arm64-v8a` + `x86_64`, JVM 17.

`:hxs_encryptor` fetches BoringSSL and liboqs via CMake `FetchContent`. `:native-server` fetches cpp-httplib v0.18.5 and nlohmann/json v3.11.3 the same way. The LSP sometimes flags missing `openssl/mem.h`; that's a false positive — build-green is the source of truth.

## Repo conventions

- HXS-only persisted storage. No `SharedPreferences` / Room / DataStore / raw files, with two intentional exceptions: the bootstrap DEK blob and content-addressed RAG source bytes.
- Single `Scaffold`. `AppScaffold` is the only one. Per-route top bars dispatch from `AppTopBar.kt`; bottom bars from `AppBottomBar.kt`. Screens take `innerPadding: PaddingValues` and render plain `Column` / `LazyColumn` / `Box`.
- No comments in source except a one-liner for non-obvious *why*. No decorative banners. Names and structure self-document.
- Only `:app` minifies. Library modules collide on `Type a.a is defined multiple times` against pre-minified prebuilt jars (e.g. `gguf_lib-release-runtime.jar`) if they pre-minify too. Library rules go in each module's `consumer-rules.pro`.
- Conventional commits. No `Co-Authored-By` trailer.
- `CLAUDE.md` at the repo root is project memory. Spec / plan / research / TODO docs don't go in the tree.

## License

[MIT](LICENSE).

## Credits

- [llama.cpp](https://github.com/ggerganov/llama.cpp) / GGUF (Liquid AI fork) via prebuilt AAR
- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) via prebuilt AAR
- [BoringSSL](https://boringssl.googlesource.com/boringssl/), [liboqs](https://github.com/open-quantum-safe/liboqs), [cpp-httplib](https://github.com/yhirose/cpp-httplib), [nlohmann/json](https://github.com/nlohmann/json) — fetched at build time
- [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) for `.tar.bz2` voice archives
- [Tabler](https://tabler.io/icons)-derived icon set

---

Built by [Siddhesh Sonar](https://github.com/Siddhesh2377). Come hang on [Discord](https://discord.gg/mVPwHDhrAP).
