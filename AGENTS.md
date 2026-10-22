# ToolNeuron Revival — Autonomous Cloud Agent Contract

## Current owner override — Cloud lane frozen

The Cloud engineering lane is complete and frozen. Read `cloud/DEVICE_HANDOFF.md` for the next local rebuild/physical Samsung lane. Do not start another feature wave, merge PR #1 or mark it ready. Preserve source-vs-receipt identity, laptop reconciliation, unavailable Covert R0 and all device/publication gates. The earlier autonomous mission instructions below are historical context; this newer owner freeze directive takes precedence.

This repository is being handed to a long-running Codex Cloud worker. Read this file first, then `CLOUD_START_HERE.md`.

## Authority
1. Newest owner directive in this repository.
2. This AGENTS.md.
3. CLOUD_START_HERE.md and files under cloud/.
4. Existing source/contracts/tests.
5. Pinned reference repositories in cloud/reference-stack.json.
6. Older upstream documentation.

Do not silently override a higher authority with an older document.

## Mission
Carry ToolNeuron Revival / Covert Edge from the current public snapshot through the already designed and blueprinted product work. Keep working autonomously. Do not ask the owner for routine confirmation, do not stop after one feature, and do not treat a checkpoint report as completion.

Commit and push every verified slice to the working branch. Maintain an append-only progress record in `cloud/WORK_LOG.md`.

## Current truth
Public GitHub main is the 2026-10-16 snapshot at version 34 / 3.0.3-covert. A newer laptop-only line was inspected on 2026-10-22 at local SHA `4599afbd956a0f111c471ec82d3c367bb76f9ae0` (3.0.8-covert). Its exact bytes were not uploaded because the Desktop Commander transport quota closed during handoff. The known delta is recorded in `cloud/KNOWN_LOCAL_DELTA.md`.

Therefore:
- inspect before implementing;
- if a local-delta feature is already present, preserve it;
- if absent, implement the contract from the handoff;
- never delete working behavior merely to make the tree resemble an older snapshot;
- record any unavoidable divergence for later laptop reconciliation.

## Development doctrine
Use THE DEVELOPER'S WAY:
KNOW -> RESEARCH IF NEEDED -> ARMOR -> BUILD -> VERIFY -> LOG.

Favor shipping over ceremony:
- FAST verification continuously.
- SLICE verification at bounded feature completion.
- RELEASE verification only at integration/release candidates.
- P0/P1 defects stop the current slice.
- P2/P3 defects are logged and do not block unrelated playable/product progress.
- Time-box research; build a proof instead of extending research indefinitely.
- Reuse existing components/assets/contracts before inventing replacements.
- Prefer vertical user journeys over horizontal framework expansion.

## Verification
Run `scripts/cloud_verify.sh fast` after ordinary code changes.
Run `scripts/cloud_verify.sh slice` at feature boundaries.
Run the full Android build before release-candidate commits when the cloud environment supports it.
Never claim device acceptance without a real device run.

## Security / authority
- Never commit passwords, API keys, bot tokens, signing keys, keystores, private model artifacts, or user data.
- External actions use environment/repository secrets only.
- No destructive account changes.
- No paid API/service usage unless already configured and clearly authorized.
- No silent cloud/provider fallback.
- Preserve local-first behavior and truthful capability/admission states.
- Do not weaken Android security boundaries to make a feature easier.

## External publication
GitHub, website, Discord and releases are part of the product workload. Follow `cloud/PUBLICATION_RELEASE.md`.
If required credentials are unavailable, generate the exact publication artifact in `ops/outbox/` and continue product work. Missing credentials are not a reason to idle the engineering lane.

## Stop conditions
Do not check in with the owner unless blocked by one of these:
- a secret/credential or account approval that cannot be replaced with an outbox artifact;
- a paid purchase or paid API decision;
- an irreversible/destructive external action;
- a material licensing/legal conflict;
- two current owner contracts directly contradict each other;
- the repository cannot be built or repaired without an unavailable external binary/artifact and all reasonable alternatives are exhausted.

Otherwise choose the safest bounded interpretation, record it, and continue.

## Definition of done
"Done" is not "one PR opened." Complete the mission queue in `cloud/AUTONOMOUS_EXECUTION.md`, keep source/build/docs/public surfaces synchronized, and leave a reproducible final receipt. Device-only gates may remain explicitly NOT TESTED if no physical device is available.