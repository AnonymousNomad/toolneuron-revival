# Laptop reconciliation — cloud-handoff-saul

Inspection started at `116a96ba8a7bd90598ee8c4ed518ae0aaf65c993`.
The app still identifies itself as version 34 / 3.0.3-covert. This is deliberate:
the untransferred laptop's version 39 is not evidence that these features exist here.

| Laptop delta | Current cloud evidence | Status |
| --- | --- | --- |
| Abilities onboarding and Laptop connections | Shared Abilities screen now follows Cipher; completion persists after the last step; Laptop has a provider-state Connections panel | Reconstructed; app gates passed; device flow not run |
| Model Intelligence qualification | Typed artifact/runtime/device/battery-bound evidence contract and UNKNOWN dossier projection; semantic roles, runtime support and admissions preserved | Contract reconstructed; measured battery runner still absent |
| ModGame encoding/proven-cycle copy | No ModGameScreen in this snapshot | No exact source available; inspect the future Vibe/Game entry before reconstructing |
| Vibe Studio, packs, batteries | Canonical governed template/editor/build/check/offline preview/capture/export UI, typed packs and bounded instruction/structured-output battery definitions | V1 contract reconstructed; real model battery runner remains Phase 3; physical journey NOT TESTED |
| Inference foreground-service deferral | Promotion moved to explicit onStartCommand, rejected starts stopped, explicit foreground intent retained across crash rebind | Reconstructed; compiler/JVM/APK gates passed; Android lifecycle acceptance NOT TESTED |
| Window manager/taskbar/telemetry/boot | Canonical persisted windows/taskbar, minimize/close/reopen, Start Center, sampled Android telemetry and one-shot boot skip | Reconstructed; 8 JVM state/navigation-policy checks; physical layout/navigation NOT TESTED |
| Covert Engine R0 | Backend-neutral session, existing GGML adapter, explicit per-model selection and unavailable Covert boundary; text/VLM guards and contract-double lifecycle tests | ToolNeuron-side boundary reconstructed; production-native gate BLOCKED_LOCAL_SOURCE_UNAVAILABLE |
| Durable GGUF import | importLocalModel now copies to private storage before publishing a FILE row and advances onboarding only on success | Reconstructed in e3d97b5; 7 helper tests and app gates passed; device flow not run |
| Corrected JNI exports | No Covert JNI source or binary to inspect | Not verified; never substitute a fabricated implementation |

## External-source checks

- The configured Git proxy successfully reads this repository and its handoff branch.
- Fetching laptop SHA `4599afbd956a0f111c471ec82d3c367bb76f9ae0` from this origin fails with `not our ref`.
- Fetching engine SHA `3020b7f407b61addf872e6aff30d689d71ef0f20` from this origin fails with `not our ref`.
- `AnonymousNomad/covert-engine` is not accessible through the existing authenticated Git route (`Repository not found`).
- The pinned `covert-coder` reference at `81aff88b924db05c689dc734aa3e67605f4b18ce` is readable. Its tree contains desktop/Node GGUF orchestration, not the required Android JNI engine. The recorded engine SHA is not present there either.
- The handoff explicitly calls the engine revision local-only. A supported source repository at that revision, or the matching ARM64 library plus its JNI contract and provenance, is needed to reconstruct and verify that part faithfully.

## Environment findings

Java 21 JDK, Gradle 9.4.1 (wrapper checksum verified), SDK platforms 36/36.1/37.0,
build tools 36/37, NDK 28.2.13676358 and CMake 3.31.4 are installed outside the checkout.
Source `/workspace/.setup/activate.sh` in each shell. SDK package 37 is named
`platforms;android-37.0`, rather than `platforms;android-37`.

A direct HTTPS probe to `boringssl.googlesource.com` is denied, but the actual
CMake dependency checkout succeeds through the existing Git proxy and resolves
the pinned `617634bc015344093f5ea0b0a5b653c924cfa20d` commit. The draft therefore
preserves the original network settings; no domain addition is required for that
checkout. TLS and artifact verification remain enabled. No signing material or
physical Android device is configured.

This document records inspection, not acceptance of Phase 0 or any later phase.
