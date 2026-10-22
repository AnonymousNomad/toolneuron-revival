# Laptop reconciliation — cloud-handoff-saul

Inspection started at `116a96ba8a7bd90598ee8c4ed518ae0aaf65c993`.
The initial app identified itself as version 34 / 3.0.3-covert. The reconstructed cloud development line now uses versionCode 40 / 3.1.0-cloud.1, distinct from the untransferred laptop version 39 / 3.0.8. Exact laptop bytes remain unavailable; cloud behavior is reconstructed against the documented contracts, not represented as that laptop commit.

| Laptop delta | Current cloud evidence | Status |
| --- | --- | --- |
| Abilities onboarding and Laptop connections | Shared Abilities screen now follows Cipher; completion persists after the last step; Laptop has a provider-state Connections panel | Reconstructed; app gates passed; device flow not run |
| Model Intelligence qualification | Artifact/runtime/profile/chassis/device-bound instruction, structured-output and resident-envelope smoke runner, UNKNOWN start receipts, observed output/native TTFT/tps and reviewed JSON export; semantic roles/runtime support/admissions stay distinct | Runner reconstructed; JVM/build gates only; actual model/device measurements NOT TESTED in Cloud |
| ModGame encoding/proven-cycle copy | No legacy ModGameScreen source available; Vibe game template and TV/Game companion now provide the bounded creation/strategy foundation | Contract reconstructed without claiming the unavailable screen or byte-level encoding repair |
| Vibe Studio, packs, batteries | Canonical governed template/editor/build/check/offline preview/capture/export UI, typed packs and bounded instruction/structured-output/resident-envelope batteries | V1 contract and text battery runner reconstructed; physical journey and model execution NOT TESTED |
| Inference foreground-service deferral | Promotion moved to explicit onStartCommand, rejected starts stopped, explicit foreground intent retained across crash rebind | Reconstructed; compiler/JVM/APK gates passed; Android lifecycle acceptance NOT TESTED |
| Window manager/taskbar/telemetry/boot | Canonical persisted windows/taskbar, minimize/close/reopen, Start Center, sampled Android telemetry and one-shot boot skip | Reconstructed; 8 JVM state/navigation-policy checks; physical layout/navigation NOT TESTED |
| Covert Engine R0 | Backend-neutral session, existing GGML adapter, explicit per-model selection and unavailable Covert boundary; text/VLM guards and contract-double lifecycle tests | ToolNeuron-side boundary reconstructed; production-native gate BLOCKED_LOCAL_SOURCE_UNAVAILABLE |
| Durable GGUF import | importLocalModel copies to private storage before publishing a FILE row; stable root picker survives app-lock navigation and setup exposes Continue after installation | Reconstructed in e3d97b5; 7 helper tests and app gates passed; device flow not run |
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

## Final integration additions

Creator hierarchy/provenance/audio planning lives in the canonical workspace, with reviewed production manifests and unavailable renderer adapters. TV/Game source declarations and strategy interfaces are explicit foundations with no playback/device transport claim. Encrypted drafts, non-destructive vault quarantine, failed-load protection, namespace alias guards and historical battery preservation close recovery contracts. Beta reports expose exact reviewed local bundles; orphan bytes remain preserved and counted. See WORK_LOG and FINAL_RECEIPT for final gate counts, source/artifact identity and publication outboxes.

All physical Samsung/Android/WebView/SAF/native-model gates remain NOT TESTED. HXS crash atomicity and process-wide native manifest routing are not certified. External release/Floot/Discord publication remains distinct from prepared source and outbox evidence.
