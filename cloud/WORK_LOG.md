# Cloud work log

Append entries. Do not rewrite history.

## Handoff — 2026-10-22
- Cloud branch created from public ToolNeuron Revival snapshot.
- Autonomous execution contract added.
- Known newer laptop checkpoint recorded: 4599afb / 3.0.8-covert.
- Pinned Covert/Ghost/harness/creation/publication reference stack is part of this handoff.
- No device acceptance claimed.
- Desktop Commander monthly quota closed before the exact laptop delta could be uploaded; reconcile feature-by-feature rather than pretending the public snapshot is current.

Next mission: TN-CLOUD-00.

## TN-CLOUD-00 — grounding and toolchain, 2026-10-22
- Start SHA: 116a96ba8a7bd90598ee8c4ed518ae0aaf65c993; working branch: cloud-handoff-saul.
- Read AGENTS.md, CLOUD_START_HERE.md and all cloud contracts; inspected build files, existing Laptop/Desktop/model import/inference code and unit-test inventory.
- Added cloud/RECONCILIATION.md with feature-by-feature findings and exact missing-source checks. This documentation checkpoint does not accept Phase 0.
- Installed a checksum-verified JDK and Android tools outside the checkout. Repeated the installation script successfully; javac 21.0.8 and the checksum-pinned Gradle 9.4.1 wrapper execute.
- Saved complete install_script/start_skill and the required BoringSSL domain addition in the environment configuration draft. These are saved instructions, not proof of publication or applied network access.
- Required native destination currently returns proxy CONNECT 403. Required Covert engine revision and laptop checkpoint are unavailable from accessible sources; see reconciliation evidence.
- Import reconstruction is being tested separately; product acceptance remains pending FAST/SLICE. No APK, runtime/device acceptance or queue completion is claimed.
- Documentation validation: git diff --check. Next: finish app build/JVM gates, evaluate native build access and the required engine artifact boundary.

## TN-CLOUD-00.1 — durable GGUF import, 2026-10-22
- Start SHA: cad99bd. Scope: local GGUF/embedding import, onboarding handoff, existing Installed surface; existing inference and source files remain intact.
- Implemented app-owned FILE imports with unique paths, atomic completion, GGUF-header validation, byte progress, actual registered sizes and cooperative cancellation cleanup. Registration failure rolls back metadata before deleting the file; an indeterminate rollback preserves the backing file.
- Onboarding advances only on successful registration and displays import failures/progress. Persistable URI permission is no longer required. The Gradle wrapper is executable so the checked-in verification script works directly.
- Test-first helper checks expanded from 5 to 7 tests after fresh-context review. Fixed the onboarding integration finding and added registry rollback coverage. P2 deferred to Phase 7: abrupt process death can leave orphan partial/unregistered model files; normal failure/cancellation cleanup is covered.
- FAST: scripts/cloud_verify.sh fast PASS, 114 tests / 0 failures / 0 errors / 0 skipped. SLICE: scripts/cloud_verify.sh slice PASS, debug APK built with arm64-v8a and x86_64 native targets. App id/version remain com.anonymousnomad.toolneuron.covert / 34 / 3.0.3-covert. No physical-device acceptance claimed.
- APK SHA-256: ba09032833ffe487df339bb9f0ed77aba449609066e0dfe5d75ae5597e732660. Local artifact: app/build/outputs/apk/debug/app-debug.apk; development signing only, no release publication.
- Setup correction: direct BoringSSL HTTPS denial did not prevent its actual Git checkout. Removed the unnecessary domain addition from the draft. Existing Git proxy fetched the pinned dependency. The earlier networking note was provisional and is superseded by this verified operation.
- Setup correction: removed conflicting Android cache variables. Overlapping Gradle runs collided in generated Hilt files; cleaned only app build output and reran gates sequentially. A later in-flight run read a test before its helper update; the final sequential FAST/SLICE results above supersede those failed attempts.
- The initial native build selected unused BoringSSL test executables. Stopped that extra work and applied an environment-only Gradle target override for hxs_encryptor plus a four-job Ninja wrapper. Final SLICE reused valid native objects and passed with the shipped library target; no app tests were disabled.
- Saved installation/startup configuration; these fields do not publish the environment. Phase 0 remains in progress: abilities/connections, qualification contract, workstation/Vibe reconciliation, lifecycle deferral and the missing external Covert engine source/artifact.

## TN-CLOUD-00.2 — abilities, connections, qualification contract, 2026-10-22
- Start SHA: e3d97b5. Scope: reconstruct the af51a69 behavior contract without inventing device evidence.
- Fixed the bottom-bar onboarding path that bypassed Cipher. RAG now continues through Cipher and shared Abilities to Desktop, and modelSetupDone persists only at the final action. Continuing with all capabilities denied is valid; no permission is automatically granted.
- Added Laptop Connections with actual terminal probe state separated from declared provider presence. Added typed qualification scope/check/measurement/record contracts. PASS requires exact artifact SHA-256/runtime/device/battery, complete unique check IDs and evidence; absent measurements remain null. Dossiers show UNKNOWN, not inferred qualification. The battery runner remains a later mission.
- Test-first qualification checks cover missing evidence/coverage, scope changes, failure, serialization and invalid measurements. FAST and SLICE PASS: 120 tests, zero failures/errors/skips; both native ABIs and debug packaging passed. Physical Android navigation/acceptance NOT TESTED.
- Fresh-context review: no P0/P1. Fixed stale “Finish to Desktop” copy. Deferred inherited Phase 7 P2s: unlock can bypass incomplete onboarding, and permission persistence failure can leave UI state ahead of durable state. Earlier import process-death orphan cleanup also remains in Phase 7.
- Engine source prerequisite requested asynchronously; independent reconstruction continues. No Covert runtime operation or overall Phase 0 completion claimed.
- Next: inference foreground-promotion lifecycle deferral, followed by workstation/Vibe reconciliation.

## TN-CLOUD-00.3 — inference lifecycle deferral, 2026-10-22
- Start SHA: c7b83a6. Scope: preserve the existing inference backend and manifest while separating bind-only service initialization from explicit foreground starts.
- Removed unconditional promotion from onCreate. Explicit non-STOP starts promote from onStartCommand; platform runtime rejection stops that start ID. Existing STOP teardown remains separate.
- Fresh-context review caught a notification/foreground recovery regression in bind-only crash rebinds. The client now preserves prior explicit foreground intent across those rebinds, clears it on unbind, and uses the same explicit startup path for timeout recovery.
- FAST and SLICE PASS: existing 120 JVM tests green, debug APK with both native ABIs built. These checks establish compilation/regression readiness, not reproduction of Android foreground-service restrictions. Device background-start, notification Stop and process-death recovery acceptance remain NOT TESTED; no synthetic JVM platform test is claimed.
- No security permissions/service types or inference model behavior were changed. Next: workstation/window/taskbar/telemetry/boot reconstruction; Covert engine source request remains pending.
