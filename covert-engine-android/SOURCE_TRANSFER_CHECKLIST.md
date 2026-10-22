# Exact production source transfer checklist

When laptop access returns, transfer the exact Covert Engine R0 source before broadening or replacing it.

## Source checkout

Expected local checkout:

`E:\covert-engine`

Expected integration revision:

`3020b7f407b61addf872e6aff30d689d71ef0f20`

Before transfer:

1. record `git status --short --branch`;
2. record `git rev-parse HEAD`;
3. record `git remote -v`;
4. record branch name;
5. separate unrelated dirty/post-R0 work from the R0 revision;
6. export the exact committed tree at `3020b7f...`, not the working directory by assumption;
7. scan for secrets, local paths, credentials, signing material and generated binaries;
8. preserve licenses and dependency attribution.

## Publish

Preferred outcome:

Create an owner-controlled repository dedicated to Covert Engine source and push the exact R0 revision with provenance.

If repository creation is unavailable, place the exact exported source under an isolated source branch or directory with its original revision recorded.

Never rewrite the historical revision identity to make the transfer convenient.

## After transfer

Cloud must:

1. fetch exact source;
2. build Android ARM64;
3. record toolchain versions;
4. record native output hash;
5. reconcile JNI declarations/exports;
6. wire ToolNeuron backend;
7. run contract tests;
8. produce APK;
9. perform physical-device acceptance later on the exact APK.

## Do not

- reconstruct production source from memory;
- use a mock as release engine;
- commit signing credentials;
- include unrelated dirty local work;
- claim `3020b7f` if the published source is not exactly that tree.
