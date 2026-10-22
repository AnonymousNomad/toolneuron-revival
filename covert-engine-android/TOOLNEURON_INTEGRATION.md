# ToolNeuron integration responsibilities

Cloud may continue all ToolNeuron-side work that does not require the unavailable production engine source.

## Implement now

### Backend-neutral contract

Create/maintain one local-model backend abstraction covering:

- identity;
- availability;
- load;
- generation stream;
- cancellation;
- unload;
- lifecycle/error reporting.

Keep application-level model/session logic outside engine-specific implementations.

### Existing backend

Wrap or preserve the existing ToolNeuron `GGMLEngine`/llama.cpp path as a backend implementation without changing its proven behavior unnecessarily.

### Covert backend boundary

Create the Covert backend behind the same interface.

Until production native source is transferred, its native availability must be truthfully detectable.

Do not require the optional Covert backend for the app to compile or launch.

### Runtime selection

Selection must be explicit and observable.

No silent substitution.

A missing Covert native library must produce an unavailable state, not a crash and not an invisible switch to llama.cpp.

### Durable model paths

Use the durable GGUF import path already being reconciled in the Cloud lane.

Native backends consume the durable app-owned FILE path.

### Tests Cloud can run before native transfer

Test:

- backend selection;
- availability state;
- absent-native-library behavior;
- no-silent-fallback behavior;
- lifecycle state machine;
- load validation before native call;
- filesystem-path enforcement;
- cancellation propagation contract;
- streaming event mapping contract with a test double;
- unload/reload state;
- existing backend regression.

Mocks/test doubles may prove the application contract only. Label them as such.

## Blocked until exact R0 source arrives

Do not mark these PASS yet:

- production native compile;
- exact JNI symbol match;
- real Covert Engine model load;
- real Covert Engine token generation;
- real native cancellation;
- ABI/device acceptance;
- native performance measurements.

Use:

`BLOCKED_LOCAL_SOURCE_UNAVAILABLE`

for the production-native gate.

## Continue the project

This dependency must not block independent missions:
- workstation shell;
- Cipher;
- canonical workspace;
- Model Lab UI;
- Vibe Studio;
- Creator/Creations/miniseries;
- continuity/recovery;
- beta/reporting;
- Floot Covert Nexus updates;
- Discord publication artifacts;
- existing-runtime APK work.
