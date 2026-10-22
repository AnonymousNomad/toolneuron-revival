# Known laptop delta newer than public GitHub snapshot

This file prevents the cloud worker from treating public 3.0.3 as the latest design truth.

Inspected laptop line: feat/cipher-laptop-p2
Latest local SHA: 4599afbd956a0f111c471ec82d3c367bb76f9ae0
Latest local version: 3.0.8-covert (39)

Observed commit sequence after the public snapshot included:

- af51a69 — onboarding abilities step + Laptop connections panel + Model Intelligence qualification contract.
- 7f07e7d — 3.0.4-covert autonomous product-completion candidate.
- 7c812fb — ModGameScreen encoding repair + proven-cycle copy.
- be19f59 — Vibe Studio V1 + workstation packs + Edge Harness batteries.
- 82e4b70 — 3.0.5-covert.
- 719be5f — inference foreground-service crash-safety deferral.
- fb42802 — workstation shell: window manager, taskbar, real telemetry status strip, skippable boot, desktop host.
- 69bab24 — Covert Engine R0 backend integration: ARM64 JNI cdylib, LocalModelBackend contract, developer runtime surface.
- 87b83ff — 3.0.6-covert.
- d1909e2 — durable local GGUF import: copy SAF-picked model into app storage as FILE pathType, import progress + row.
- 6127ed4 — 3.0.7-covert.
- 4599afb — 3.0.8-covert; corrected JNI spec-escaped export names (_1) via rebuilt covert-jni; engine integration revision 3020b7f.

The exact local code bytes for these commits were not transferred before the remote-laptop tool quota closed. This is a reconciliation fact, not permission to ignore the work.

## Reconciliation rule
For each item:
1. inspect the public/cloud branch;
2. preserve it if already present;
3. otherwise implement the behavior from the contract and tests;
4. do not fabricate device evidence;
5. record cloud implementation commits in cloud/WORK_LOG.md;
6. keep changes modular so later laptop reconciliation can compare by feature rather than giant rewrite.

## Covert Engine R0 contract observed on laptop
- backend-neutral LocalModelBackend abstraction;
- JNI-backed CovertEngineBackend;
- one loaded local model/session model consistent with existing ToolNeuron inference topology;
- streaming token callback;
- cancellation;
- load/unload/session lifecycle;
- filesystem-path model input, not content URI;
- no localhost HTTP dependency;
- ARM64 Android JNI library;
- Covert Engine reference revision: 3020b7f407b61addf872e6aff30d689d71ef0f20.

Do not replace the existing llama.cpp path. Runtime choice is additive behind one contract.