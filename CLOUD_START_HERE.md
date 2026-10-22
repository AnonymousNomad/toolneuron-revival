# ToolNeuron Revival — Cloud Start Here

**Cloud lane frozen:** continue with [DEVICE_HANDOFF.md](cloud/DEVICE_HANDOFF.md), not another feature wave. PR #1 remains OPEN/DRAFT/UNMERGED until local rebuild, Samsung acceptance, laptop reconciliation and owner review.

## Purpose
This branch is the autonomous continuation lane for ToolNeuron Revival / Covert Edge. It is intended to let a faster/lower-cost reasoning worker carry the project without repeated owner check-ins.

## Public baseline
Repository: AnonymousNomad/toolneuron-revival
Public snapshot base: 00dcb428a3b8c55613c9eaaea523c658cfcb647f
Public version at handoff: 34 / 3.0.3-covert
Upstream provenance: Siddhesh2377/ToolNeuron, branch re-write, base 5df5f8eaa1f199d6920ebe5cdd30cdf0db37f290, MIT.

## Newer laptop checkpoint
Immediately before this handoff the laptop continuation was inspected:
- worktree: E:\toolneuron-revival\worktrees\stabilization
- branch: feat/cipher-laptop-p2
- HEAD: 4599afbd956a0f111c471ec82d3c367bb76f9ae0
- label: 3.0.8-covert
- worktree was clean.

The public repo is older than that laptop checkpoint. See cloud/KNOWN_LOCAL_DELTA.md.

## Product identity
ToolNeuron Revival is not just a local chat frontend. The target is a local-first Android AI workstation with:
- Cipher resident assistant;
- Cipher's Laptop: governed projects, tasks, evidence, permissions and terminal;
- workstation/desktop shell;
- Files, Terminal, Chat and Projects sharing one canonical workspace;
- Vibe Studio for app/web/game creation;
- Model Lab / Edge Harness for model qualification;
- Covert Engine runtime option behind a backend-neutral boundary;
- Creator/Creations lane including video/miniseries work;
- ToolNeuron TV / media companion direction;
- Game Station / companion bridge direction;
- continuity/memory;
- community beta, GitHub releases, Discord and the existing Floot **Covert Nexus** website/project (`01147976-3673-42c7-9220-2d028594ba86`).

## First actions
1. Read AGENTS.md.
2. Read every file in cloud/.
3. Inspect current source before editing.
4. Establish baseline: git status; git log -10 --oneline; git diff --check; fastest available unit/build gate.
5. Reconcile public code with cloud/KNOWN_LOCAL_DELTA.md.
6. Start the mission queue and keep moving until exhausted or a genuine owner boundary is reached.

## Core invariant
One project/workspace, multiple surfaces. Chat, Vibe, Files, Terminal and Cipher must converge on the same project identity and governed capability system. Do not build parallel state silos.

## UX direction
Retro workstation, but simple enough for a first-time user:
- clear Start Center;
- no blank-screen dead ends;
- progressive disclosure;
- strong empty/loading/error/recovery states;
- obvious model/Cipher state;
- consistent retro theme families;
- user value before advanced configuration.

## Truthfulness
Availability is not authority. Capability present != execution granted. Model downloaded != compatible. Test written != test run. Build produced != device accepted. Keep these states separate in code, UI and release notes.