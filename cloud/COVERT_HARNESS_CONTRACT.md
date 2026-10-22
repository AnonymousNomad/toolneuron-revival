# Project-local Covert harness contract

The full Covert desktop stack is not embedded here. This contract brings its working method into ToolNeuron so a cloud worker can use the same discipline without the laptop.

## Pipeline
MISSION ENVELOPE
-> bounded context
-> skill/procedure selection
-> builder
-> deterministic tools/tests
-> Veritas-style verification
-> Ghost-style independent review for risky/integration changes
-> repair loop
-> checkpoint
-> mission receipt
-> next mission.

## Mission envelope
Every substantial slice records:
- mission id;
- objective;
- allowed scope;
- denied scope;
- start SHA;
- relevant contracts;
- required tests;
- stop conditions;
- expected user-visible artifact/outcome.

## Verification tiers
FAST — continuously:
- git diff --check;
- targeted unit tests;
- focused compile/static checks;
- changed-file secret scan;
- contract/invariant checks.

SLICE — at feature completion:
- relevant unit suite;
- debug build when supported;
- focused integration test;
- restart/persistence check if stateful;
- empty/error/recovery review for UI;
- no known P0/P1 in changed journey.

RELEASE — integration candidates only:
- broad regression;
- reproducible APK identity;
- release signing only through configured secrets;
- device acceptance on real hardware;
- upgrade/restart/data continuity;
- crash/log review;
- publication receipt.

Do not spend RELEASE-level ceremony on every small change.

## Builder vs verifier
The builder cannot declare risky/integration work accepted solely because it compiles. For integration/release changes perform a fresh-context review:
- inspect diff and contracts;
- actively search for missing paths, stale state, false positives and silent fallback;
- return PASS or concrete defects;
- repair and rerun.

## Recovery
After each accepted slice:
- commit;
- push;
- append cloud/WORK_LOG.md with start/end SHA, tests, unresolved items;
- leave the next mission recoverable from repository state alone.

## Authority
Capability availability never grants execution permission.
Credential references may identify a secret slot but secret values never enter receipts/logs.
Network/publication is an explicit capability.
Unknown conditions fail truthfully.

Pinned reference implementations are listed in cloud/reference-stack.json. Use them as patterns, not wholesale merge sources.