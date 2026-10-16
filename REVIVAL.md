# ToolNeuron Revival — Covert Edge

Community-led revival and continued development of **ToolNeuron** (upstream:
[Siddhesh2377/ToolNeuron](https://github.com/Siddhesh2377/ToolNeuron)), rebuilt into a
local-first Android AI assistant and edge creation workbench.

**Original attribution is preserved.** This is not an official Play Store/repository
transfer; it is a community revival effort building the next generation of the project
and its Covert integration.

## What this is

- **Cipher** — the resident local assistant (small local model, swappable)
- **Cipher's Laptop** — the operational control room (projects, tasks, evidence,
  capabilities, permissions)
- **Vibe Studio** — production packs for app / website / game / 3D / video / research work
- **Local-first** — models, chats, files and voice stay on-device; no analytics SDK
- **Retro workstation UI** — canonical token system, 10 theme families

## Current state (2026-10-16)

- Baselines: stabilization line + Cipher's Laptop P0–P2 (workspaces, tasks, evidence,
  governed terminal provider, Resident plumbing), model capability admission,
  BGE runtime compatibility guard
- Theme system: canonical tokens + 10 theme families (contrast-gated), Material mapped
  from tokens
- Inference: truthful pre-load refusal for models whose quantization the bundled runtime
  does not support (Q1_0 / ggml type 41 — no more native crashes)
- Identity: `com.anonymousnomad.toolneuron.covert` — release-signed maintained fork that
  can coexist with the original Play Store app
- Model Intelligence: shared dossier contract with per-device qualification evidence

Development/testing builds unless explicitly marked otherwise. Release artifacts publish
via [Releases](https://github.com/AnonymousNomad/toolneuron-revival/releases).

## Community

Discord — **ToolNeuron / Covert Revival** server: bug reports, device testing, model
qualification results, Vibe Studio feedback, releases.

## Build

```
gradlew :app:assembleDebug          # debug build
gradlew :app:testDebugUnitTest      # unit suite
```

Release signing reads `TN_KEYSTORE_PATH` / `TN_KEYSTORE_PASSWORD` / `TN_KEY_ALIAS` /
`TN_KEY_PASSWORD` from `local.properties` (never committed).

## License

Inherits the upstream MIT license (see LICENSE). Upstream base: `re-write` @
`5df5f8eaa1f199d6920ebe5cdd30cdf0db37f290`.
