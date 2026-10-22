# Autonomous execution queue

Do not wait for the owner between phases. Complete a phase, verify, commit, push, log, continue.

## Phase 0 — Ground and reconcile
- baseline tests/build;
- inspect public snapshot versus KNOWN_LOCAL_DELTA;
- restore/reimplement missing 3.0.4–3.0.8 behavior without rewriting working code;
- record feature-by-feature reconciliation.

Exit: truthful current version identity and all reconstructed local-delta features covered by contracts/tests.

## Phase 1 — Workstation product closure
- coherent desktop/window/taskbar/status shell;
- Start Center instead of blank desktop;
- full canonical-token visual convergence;
- coherent navigation/back behavior;
- useful empty/loading/error/recovery states.

Exit: first-time user can understand what to do without developer knowledge.

## Phase 2 — Cipher slot + canonical workspace
- EMPTY/READY Cipher slot;
- model/chassis assignment;
- Chat <-> Project binding;
- ProjectRecord conversation binding;
- artifact/result cards;
- Open in Vibe Studio;
- no duplicate project copies.

## Phase 3 — Model Lab / qualification
- surface existing Edge Harness/battery machinery;
- role/capability batteries;
- PASS/FAIL/UNKNOWN;
- runtime compatibility/device fit/task admission separated;
- real measurements only;
- export evidence bundle.

## Phase 4 — Vibe Studio completion
- templates;
- create/edit/build/test/preview loop;
- governed terminal/file actions;
- preview capture;
- artifact bundle;
- recent/resume;
- useful failure recovery.

Exit: build a small web/app/game artifact end-to-end from ToolNeuron.

## Phase 5 — Creator / Creations / Miniseries
Use pinned Covert Creation Studio as reference.
Implement a local-first creator system:
- Story/Series project;
- season/episode/scene/shot hierarchy;
- character bible and asset registry;
- prompt/provenance records;
- voice/audio slots;
- image/video provider adapters behind interfaces;
- continuity checks;
- render/export manifest;
- Creations library.
Provider generation must be explicit and credential-backed. Local project truth survives without providers.

Exit: persist a small episode/miniseries plan with assets and exportable production manifest.

## Phase 6 — TV + Game Station foundations
Implement only blueprinted/product-useful surfaces:
- legal media/provider classification;
- TV companion surface;
- game companion/GameAdapter contracts;
- no model control of frame-rate mechanics;
- device bridge contracts preserved.

## Phase 7 — Continuity + recovery
- restart-safe projects/tasks/Cipher slot/recent state;
- clear namespace boundaries;
- corrupted-state quarantine/recovery;
- no cross-project silent leakage.

## Phase 8 — Beta/reporting
- one-tap local report bundle;
- app/version/device/model manifest;
- bounded logs;
- optional screenshot;
- reproduction notes;
- preview before share;
- no silent telemetry upload.

## Phase 9 — Website
Preferred public home reference: AnonymousNomad/anonymousnomad.github.io.
If cross-repo write access is available, add/maintain a ToolNeuron page there.
Otherwise maintain deployable site content under site/ and record pending publication in ops/outbox/site/.
Claims must match evidence.

## Phase 10 — Discord/community
Use repository/cloud secrets only.
Publish meaningful build/release updates, known issues, beta testing calls, qualification results and release links.
If credentials are absent, write ready-to-send content to ops/outbox/discord/ and continue.

## Phase 11 — APK/release candidate
- unit/integration gates green;
- debug APK reproducible;
- release APK only with configured signing secrets;
- tag/release notes/evidence;
- GitHub release artifact;
- Discord + website synchronized.
Physical Android acceptance remains NOT TESTED if no device exists in Cloud.

## Final receipt
Create cloud/FINAL_RECEIPT.md with:
- final SHA/version;
- completed phases;
- tests/builds;
- release artifact hashes;
- website/Discord status;
- remaining device-only gates;
- known P0/P1/P2;
- reconciliation notes for laptop-local 4599afb line.