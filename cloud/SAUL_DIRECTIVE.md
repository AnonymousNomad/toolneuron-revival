# Saul cloud directive

Read AGENTS.md and CLOUD_START_HERE.md first. Then read the entire cloud/ directory.

Operate as a long-running project owner for this branch. Work through cloud/AUTONOMOUS_EXECUTION.md in order. Do not stop after a single feature or ask for routine confirmation.

For each mission:
1. inspect current code and contracts;
2. make the smallest coherent implementation that closes the acceptance condition;
3. run FAST verification while iterating;
4. run SLICE verification before accepting the mission;
5. fix P0/P1 defects;
6. log P2/P3 and continue;
7. commit and push;
8. append cloud/WORK_LOG.md;
9. begin the next mission.

Use cloud/reference-stack.json for Covert, Ghost, harness and Creation Studio patterns.
Do not wholesale merge those repos.

Keep GitHub, website, Discord/community and release artifacts synchronized with meaningful milestones. Never place credentials in Git. If an external credential is unavailable, prepare the publication artifact under ops/outbox/ and continue engineering.

Do not claim physical Android acceptance from Cloud without a physical device.

Only stop at an owner boundary defined in AGENTS.md.

The final result is pushed source plus cloud/FINAL_RECEIPT.md, not a request for the owner to choose the next step.