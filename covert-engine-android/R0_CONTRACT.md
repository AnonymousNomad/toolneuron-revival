# Covert Engine R0 Android contract

This is the contract observed from the accepted laptop integration work.

## Identity

Engine source revision:

`3020b7f407b61addf872e6aff30d689d71ef0f20`

Target consumer:

ToolNeuron Revival / Covert Edge.

Target platform:

Android ARM64 / `arm64-v8a`.

## Architectural boundary

ToolNeuron must depend on a backend-neutral local inference contract.

The Covert runtime is one backend implementation.

The existing ToolNeuron llama.cpp/GGML path remains another backend.

Do not:
- replace the existing backend;
- duplicate application/business logic per runtime;
- silently switch runtimes after a failure;
- infer runtime qualification from model availability alone.

## Required backend semantics

The Covert backend must support:

1. backend availability/status;
2. load model from an app-owned filesystem path;
3. one active loaded local model/session consistent with ToolNeuron's existing inference topology;
4. begin generation;
5. streaming token delivery;
6. terminal generation completion;
7. error delivery;
8. cancellation;
9. unload/release;
10. clean session/resource lifecycle;
11. truthful unsupported/unavailable state.

## Model path rule

The native engine receives a normal filesystem path.

A content URI is not passed directly to the native runtime.

ToolNeuron first imports/copies a user-selected GGUF into durable app-owned storage, then registers/loads the resulting FILE path.

## Transport rule

The Android integration is in-process JNI.

No localhost HTTP server is required for the R0 path.

## JNI rule

R0 was integrated as an Android JNI cdylib.

The laptop integration required correcting JNI spec-escaped native export names using `_1` for Java/Kotlin identifiers containing underscores.

Do not invent final native symbol names from this note. The exact symbols must be taken from the transferred R0 source and matching Kotlin native declarations.

## Threading / streaming

Generation must not block the main/UI thread.

Streaming events cross the backend boundary incrementally.

Cancellation must terminate or cooperatively stop the active generation and leave the backend recoverable.

## Failure behavior

If the native library is missing, unsupported, ABI-incompatible or fails initialization:

- surface Covert Engine as unavailable/error;
- preserve the existing runtime;
- do not silently relabel fallback inference as Covert Engine;
- do not crash the application solely because an optional backend cannot load.

## Qualification

Keep distinct:

- engine installed;
- JNI library loadable;
- model structurally valid;
- runtime/model compatible;
- device-qualified;
- task-qualified;
- selected by operator/policy;
- successfully loaded;
- successfully generated.

No single boolean may collapse these states for product claims.

## Release gate

The following are required before claiming the production Covert Engine Android path verified:

- exact source identity recorded;
- clean source checkout;
- Android ARM64 native build PASS;
- produced native library identity/hash recorded;
- Kotlin/JNI symbols proven to match;
- native library loads on physical Android device;
- real GGUF loads from durable FILE path;
- streamed generation succeeds;
- cancellation succeeds;
- unload/reload succeeds;
- existing ToolNeuron backend still works;
- no silent fallback;
- exact APK source/hash recorded;
- physical-device evidence recorded.
