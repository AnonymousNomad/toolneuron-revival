# ToolNeuron web publication handoff

The canonical website is **not GitHub Pages**.

Canonical Floot project:
- Name: `Covert Nexus`
- Project ID: `01147976-3673-42c7-9220-2d028594ba86`

Covert Nexus is the existing shared ecosystem/marketplace/community/release/discovery site for ToolNeuron, Covert Desktop, Covert Engine and related work.

ToolNeuron material already exists in the Floot project across marketplace, publisher, games/GameBridge, Foundry notes/specifications and the shared catalog.

Cloud workers should:
1. inspect the current Floot project before editing;
2. extend the existing Covert Nexus surfaces;
3. keep ToolNeuron release/version/download/status information aligned with GitHub evidence;
4. preserve the shared catalog/taxonomy and existing package identities;
5. avoid fabricating download/installability/verification claims;
6. publish through Floot only when the worker has the required Floot access and the release state is eligible.

If direct Floot access is unavailable, place exact proposed Floot changes/copy under `ops/outbox/site/`. This directory is an outbox/fallback only; it is not a replacement website.

Do not publish ToolNeuron to `AnonymousNomad/anonymousnomad.github.io` as a substitute.
