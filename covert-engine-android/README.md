# Covert Engine Android — ToolNeuron dependency package

This directory exists to give the ToolNeuron Cloud lane one canonical, Git-visible dependency contract for the Covert Engine Android integration.

## Status

Production Covert Engine R0 source revision:

`3020b7f407b61addf872e6aff30d689d71ef0f20`

Observed local checkout:

`E:\covert-engine`

Observed ToolNeuron integration checkpoint:

`4599afbd956a0f111c471ec82d3c367bb76f9ae0` / `3.0.8-covert`

The exact production engine source is currently **not present in any accessible GitHub repository**. It remains laptop-only because the remote-laptop transport quota closed before the source could be transferred.

This package therefore does two things:

1. freezes the exact ToolNeuron-facing R0 contract so Cloud work can proceed without guessing;
2. makes the missing production source an explicit build gate instead of allowing a fabricated substitute.

## Important

This directory is **not** the Covert Engine implementation.

Do not turn a mock or test shim into a release runtime.

A contract/mock may be used to test ToolNeuron-side lifecycle, selection and failure behavior, but:

`COVERT_ENGINE_NATIVE_VERIFIED`

cannot be emitted until the exact source for revision `3020b7f...` or an owner-approved successor is transferred, built for Android ARM64, and exercised.

Read:
- `R0_CONTRACT.md`
- `TOOLNEURON_INTEGRATION.md`
- `SOURCE_TRANSFER_CHECKLIST.md`
- `DEPENDENCY.json`
