# Publication and release operations

Public surfaces are part of the workload but never justify leaking credentials.

## GitHub
Primary repo: AnonymousNomad/toolneuron-revival.
Push every verified slice to the cloud working branch.
Use Releases for APKs that meet the stated gate. Development APKs are labeled DEVELOPMENT/TESTING.

## Discord
The owner already operates a ToolNeuron Revival / Covert Edge community. Earlier work established a bot/application, but its token is intentionally not in Git.

Rules:
- credentials only from configured environment/repository secret slots;
- never print, rotate or commit credentials;
- do not change server ownership/security;
- publish only evidence-backed updates;
- if credentials are unavailable, write ready-to-send Markdown to ops/outbox/discord/ and continue.

## Website
Canonical website workspace: Floot project **Covert Nexus**.
Floot project ID: `01147976-3673-42c7-9220-2d028594ba86`.

This project is the canonical public ecosystem/marketplace/community/release/discovery hub for ToolNeuron, Covert Desktop, Covert Engine and related work. ToolNeuron updates belong in this Floot project; `AnonymousNomad/anonymousnomad.github.io` is NOT the ToolNeuron publication target.

Known ToolNeuron-related Floot surfaces already present include marketplace, publisher, games/GameBridge, Foundry research/specs and the shared ecosystem catalog.

When the Cloud worker has Floot access:
- inspect the live Floot project before editing;
- update the existing Covert Nexus surfaces rather than creating a competing standalone site;
- keep ToolNeuron release/version/download/status content synchronized with GitHub evidence;
- publish through Floot only when the project state and owner authorization permit it.

If the Cloud runtime cannot reach Floot directly, prepare the exact Floot-targeted copy/change plan under `ops/outbox/site/` and continue engineering. Do not redirect the work to GitHub Pages.

Website content:
- what ToolNeuron Revival is;
- current version/status;
- download/release link;
- verified capabilities;
- known limitations;
- screenshots/evidence where available;
- GitHub/community links.
No unverified benchmark or compatibility claims.

## APK identity
Keep application id com.anonymousnomad.toolneuron.covert unless a newer owner directive changes it.
Never commit signing material.
Release signing uses secure environment/secret store.
Record versionCode/versionName, source SHA, APK SHA-256, signer identity if available and verification commands.

## Publish cadence
Meaningful milestones only:
- feature slice accepted;
- beta build;
- release candidate;
- known P0/P1;
- fixed user-visible defect;
- model/device qualification result.

Missing publication credentials block only publication. Prepare the artifact and continue engineering. Destructive, paid, credential-rotating or ownership-changing external actions require the owner.