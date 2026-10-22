# Canonical blueprint reconstruction

The original laptop blueprint bundle contained these files:
- 00_README.md
- 01_PRODUCT_ARCHITECTURE.md
- 02_CIPHER_LAPTOP_CONTRACT.md
- 03_CAPABILITY_PACK_CONTRACT.md
- 04_EDGE_HARNESS_CONTRACT.md
- 05_EXECUTION_PROVIDER_CONTRACT.md
- 06_TOOLNEURON_ROADMAP.md
- 07_COVERT_ROADMAP.md
- 08_GAMING_CREATOR_MATRIX.md
- 09_OSS_COMPONENT_REGISTER.md
- 10_PERMISSION_AUTHORITY_MODEL.md
- 11_IMPLEMENTATION_SEQUENCE.md
- 12_CAPABILITY_VOCABULARY_SCHEMA.md
- COVERT_TOOLNEURON_CANONICAL_BLUEPRINT_2026-10-15.md

Desktop Commander quota closed before those exact files could be copied into Git. This document reconstructs the contracts that matter for implementation from the inspected project evidence. If a future exact blueprint copy is added, prefer it where it does not conflict with newer owner directives.

## Product architecture
ToolNeuron Revival is an Android edge workstation around one canonical project/workspace model.

Primary surfaces:
- Start Center / Desktop;
- Cipher;
- Chat;
- Cipher's Laptop;
- Projects;
- Files;
- Terminal;
- Vibe Studio;
- Model Lab;
- Creator / Creations;
- Settings/Connections/Models.

Shared services:
- project/workspace registry;
- task journal;
- capability registry;
- permission/authority checks;
- model capability/admission;
- evidence/verification;
- local inference backends;
- state persistence;
- publication/export.

## Cipher's Laptop contract
Laptop owns durable operational state, not prose-only context:
- workspace lifecycle;
- project/task identity;
- deterministic task transitions;
- capability refs;
- permission state;
- evidence refs;
- verification refs;
- checkpoint/recovery refs;
- model dossier projection;
- worker packet assembly.

Manual UI and Cipher must operate on the same underlying commands/state.

## Capability pack contract
A capability is versioned and typed. Minimum identity is id@version.
Capabilities declare:
- purpose;
- inputs/outputs;
- authority requirements;
- provider/effect implementation;
- failure modes;
- evidence requirements.
Do not infer authority from the presence of a capability.

## Edge Harness contract
Use bounded missions, deterministic verification, evidence and recovery.
Builder work is followed by fresh-context verification for risky/integration changes.
The harness should reduce model cognitive load, not pretend the model itself became more capable.

## Execution provider contract
Execution is behind typed providers, not arbitrary prose execution.
Providers expose capability/status and truthful failure.
Current important providers include:
- app-owned terminal/PTY;
- file/workspace operations;
- local model runtime;
- preview/build/test adapters;
- publication/export adapters.
No silent fallback to a different provider with different trust/cost semantics.

## Permission / authority model
Default deny for effects.
Keep separate:
- capability exists;
- capability is available on this device;
- user/authority grants it;
- resource admission allows it;
- execution succeeds;
- verification accepts the result.

Credential values never enter normal evidence.

## Capability vocabulary
Use stable ids rather than UI labels. Existing example:
dev.terminal.execute@1
New capabilities should use the same versioned pattern and be shared by manual UI and Cipher/AppCommands.

## Model intelligence
Keep distinct:
- semantic role;
- runtime compatibility;
- task admission;
- qualification evidence.
A model name or quantization label alone is not admission evidence.

## Vibe / creator matrix
Vibe Studio is the project workstation.
Creator/Creations is the media/story production workstation.
Game work uses the same project/evidence model but does not give model output direct frame-rate control.
All creation lanes share files, terminal, projects, evidence and export conventions rather than creating isolated mini-app state stores.

## Implementation sequence
1. restore/reconcile known local delta;
2. workstation shell and onboarding closure;
3. Cipher slot and canonical project binding;
4. Model Lab;
5. Vibe Studio end-to-end;
6. Creator/Creations/miniseries;
7. TV/Game companion foundations;
8. continuity/recovery;
9. beta/reporting;
10. website/Discord/release;
11. device acceptance.

## Newer owner override
Older beta notes that deferred Video Creator/Creations are superseded by the 2026-10-22 owner direction: Creator/Creations/miniseries is now required work after the core workstation journey is stable.