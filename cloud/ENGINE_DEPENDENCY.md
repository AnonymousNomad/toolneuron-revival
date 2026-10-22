# Covert Engine Android dependency

A canonical ToolNeuron-facing dependency package is available in this repository on branch:

`covert-engine-android-contract`

Path:

`covert-engine-android/`

It freezes the observed R0 Android contract, integration responsibilities, dependency status and exact-source transfer checklist.

Production engine source identity:

`3020b7f407b61addf872e6aff30d689d71ef0f20`

Current production-native gate:

`BLOCKED_LOCAL_SOURCE_UNAVAILABLE`

Tracked transfer issue:

`#2 — Publish exact Covert Engine R0 Android source for Cloud`

## Cloud instruction

Fetch/read the `covert-engine-android-contract` branch and continue all ToolNeuron-side integration work described in `TOOLNEURON_INTEGRATION.md`.

You may implement and test:
- backend-neutral runtime abstraction;
- existing backend preservation;
- explicit runtime selection;
- missing-native-library behavior;
- no-silent-fallback behavior;
- model filesystem-path enforcement;
- lifecycle/stream/cancel event mapping using test doubles;
- regression tests.

Do not fabricate the native implementation and do not mark production-native verification PASS.

The exact R0 source will be transferred from `E:\covert-engine` at revision `3020b7f...` when laptop transport access is available again.

This dependency blocks only the production Covert Engine native gate. It does not block the rest of the autonomous mission queue.