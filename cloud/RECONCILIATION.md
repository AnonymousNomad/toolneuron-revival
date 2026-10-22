# Laptop reconciliation — cloud-handoff-saul

Inspection started at `116a96ba8a7bd90598ee8c4ed518ae0aaf65c993`.
The app still identifies itself as version 34 / 3.0.3-covert. This is deliberate:
the untransferred laptop's version 39 is not evidence that these features exist here.

| Laptop delta | Current cloud evidence | Status |
| --- | --- | --- |
| Abilities onboarding and Laptop connections | SetupCipherScreen and LaptopScreen exist; the additional onboarding step and connections panel are absent | Reconstruction required |
| Model Intelligence qualification | ModelRegistry.ModelDossier has a nullable qualification string; semantic roles, runtime support and admissions exist | Preserve existing separation; measured qualification machinery absent |
| ModGame encoding/proven-cycle copy | No ModGameScreen in this snapshot | No exact source available; inspect the future Vibe/Game entry before reconstructing |
| Vibe Studio, packs, batteries | No Vibe, battery or harness implementation found in app sources | Reconstruction required |
| Inference foreground-service deferral | InferenceService.onCreate unconditionally calls startForeground; InferenceClient starts the service before binding | Crash-safety deferral absent; Android lifecycle verification required |
| Window manager/taskbar/telemetry/boot | DesktopScreen has a tile grid, resident/terminal state and clock; DesktopViewModel has no window manager | Preserve the grid and real states; shell reconstruction required |
| Covert Engine R0 | No LocalModelBackend, CovertEngineBackend or Covert JNI artifact in source/libs | Required external source/artifact unavailable |
| Durable GGUF import | importLocalModel originally inserted a CONTENT_URI record without copying | Reconstruction in progress; acceptance requires app gates |
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

The live network policy denies `boringssl.googlesource.com`, required by the
existing native crypto CMake build. Its domain addition is saved in the environment
draft; saving a draft does not apply that policy. TLS and artifact verification
remain enabled. No signing material or physical Android device is configured.

This document records inspection, not acceptance of Phase 0 or any later phase.
