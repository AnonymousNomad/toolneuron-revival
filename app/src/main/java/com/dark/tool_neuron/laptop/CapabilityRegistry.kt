package com.dark.tool_neuron.laptop

object CapabilityRegistry {
    data class CapabilityDescriptor(
        val id: String,
        val version: Int,
        val description: String,
        val requiredKnowledge: List<String> = emptyList(),
        val requiredProcedure: String? = null,
        val requiredEffects: List<String> = emptyList(),
        val workerProfile: String? = null,
        val providers: List<String> = emptyList(),
        val permissions: List<String> = emptyList(),
        val verification: List<String> = emptyList(),
    )

    val SEED: List<CapabilityDescriptor> = listOf(
        CapabilityDescriptor(id = "companion.plan", version = 1, description = "Register declared legal media references for a canonical project; no playback, game input or bridge transport is granted.", providers = listOf("tn.companion.plan"), permissions = listOf("filesystem.workspace.write")),
        CapabilityDescriptor(id = "media.creation.plan", version = 1,
            description = "Persist local story/series hierarchy, character bible, asset provenance and audio slots; check continuity and export a production plan. Rendering remains separately unavailable.",
            requiredEffects = listOf("filesystem.workspace"), providers = listOf("tn.creation.plan"),
            permissions = listOf("filesystem.workspace.write", "artifact.export"),
            verification = listOf("canonical project identity, scoped references, continuity report and honest unrendered manifest")),
        CapabilityDescriptor(
            id = "dev.vibe.workspace", version = 1,
            description = "Create/edit project source, build/check static web bundles, preview offline and export selected artifacts in Vibe Studio.",
            requiredEffects = listOf("filesystem.workspace", "preview.static"),
            providers = listOf("tn.vibe.static"),
            permissions = listOf("filesystem.workspace.read", "filesystem.workspace.write", "preview.static", "artifact.export"),
            verification = listOf("entrypoint and local asset checks; immutable file hashes; explicit export"),
        ),
        CapabilityDescriptor(
            id = "dev.workspace.inspect",
            version = 1,
            description = "Inspect the active workspace: list files, read metadata, summarize state.",
            requiredKnowledge = listOf("K.doctrine.small_model_first"),
            requiredProcedure = "P.workspace.inspect",
            requiredEffects = listOf("filesystem.workspace"),
            workerProfile = "edge-small",
            providers = listOf("local-tool"),
            permissions = listOf("filesystem.workspace.read"),
            verification = listOf("file listing matches workspace directory state"),
        ),
        CapabilityDescriptor(
            id = "dev.terminal.execute",
            version = 1,
            description = "Execute a bounded command in the embedded terminal workspace.",
            requiredKnowledge = listOf("K.doctrine.verification_separation"),
            requiredProcedure = "P.terminal.execute.stub",
            requiredEffects = listOf("terminal.execute"),
            workerProfile = "edge-small",
            providers = listOf("local-tool"),
            permissions = listOf("terminal.execute"),
            verification = listOf("exit status and captured output recorded as evidence"),
        ),
        CapabilityDescriptor(
            id = "model.worker.delegate",
            version = 1,
            description = "Delegate a bounded worker packet to a local model worker.",
            requiredKnowledge = listOf("K.doctrine.small_model_first"),
            requiredProcedure = null,
            requiredEffects = listOf("model.runtime"),
            workerProfile = "edge-small",
            providers = listOf("local-model"),
            permissions = listOf("model.worker.invoke"),
            verification = listOf("worker output validated against acceptance criteria"),
        ),
        CapabilityDescriptor(
            id = "media.video.create_short",
            version = 1,
            description = "Assemble a short-form video from assets, captions, and voice.",
            requiredKnowledge = emptyList(),
            requiredProcedure = null,
            requiredEffects = listOf("media.render"),
            workerProfile = null,
            providers = listOf("local-tool", "covert-link"),
            permissions = listOf("media.render"),
            verification = listOf("output duration/format checks"),
        ),
        CapabilityDescriptor(
            id = "gaming.apk.inspect",
            version = 1,
            description = "Inspect an authorized APK artifact: manifest, resources, structure.",
            requiredKnowledge = emptyList(),
            requiredProcedure = null,
            requiredEffects = listOf("filesystem.workspace"),
            workerProfile = null,
            providers = listOf("local-tool"),
            permissions = listOf("filesystem.workspace.read"),
            verification = listOf("inspection report matches artifact metadata"),
        ),
    )

    fun ref(descriptor: CapabilityDescriptor): String = "${descriptor.id}@${descriptor.version}"

    fun lookup(capabilityRef: String): CapabilityDescriptor? {
        val at = capabilityRef.lastIndexOf('@')
        if (at <= 0 || at == capabilityRef.length - 1) return null
        val id = capabilityRef.substring(0, at)
        val version = capabilityRef.substring(at + 1).toIntOrNull() ?: return null
        return SEED.firstOrNull { it.id == id && it.version == version }
    }
}

object KnowledgeRegistry {
    val SEEDS: List<KnowledgeRef> = listOf(
        KnowledgeRef(
            id = "K.doctrine.small_model_first",
            kind = KnowledgeKind.SOP,
            title = "Small-model-first rule",
            text = "Every worker packet assumes a constrained edge model: explicit, hierarchical, concise, task-specific, tool-grounded, bounded, verification-bound. No giant document dumps; no assumption that the worker infers missing procedure.",
        ),
        KnowledgeRef(
            id = "K.doctrine.verification_separation",
            kind = KnowledgeKind.SOP,
            title = "Verification separation",
            text = "Deterministic systems verify. A model-provided statement such as 'done' is never verification; only recorded evidence and deterministic checks produce a verdict.",
        ),
        KnowledgeRef(
            id = "K.doctrine.acceptance",
            kind = KnowledgeKind.SOP,
            title = "Acceptance doctrine",
            text = "No feature is done because a model says it worked. Slices pass only on deterministic acceptance criteria plus recorded evidence; stop on failure and report the narrow blocker.",
        ),
    )
}

object ProcedureRegistry {
    val SEEDS: List<ProcedureRef> = listOf(
        ProcedureRef(
            id = "P.workspace.inspect",
            title = "Workspace inspect",
            steps = listOf(
                "List workspace files and directories",
                "Read project metadata recorded in the Laptop",
                "Summarize current state for the requester",
                "Record the summary as evidence",
            ),
        ),
        ProcedureRef(
            id = "P.terminal.execute.stub",
            title = "Terminal execute (stub)",
            steps = listOf(
                "Resolve the terminal effect provider",
                "Attempt bounded command execution",
                "Record the provider result as evidence",
                "Record a verification verdict from the result",
            ),
        ),
    )
}

object ToolRegistry {
    val SEEDS: List<ToolRef> = listOf(
        ToolRef("preview.static", ToolKind.FILESYSTEM, "Offline static preview", ToolStatus.AVAILABLE, "Per-build origin; no network, filesystem URLs or native JavaScript bridge"),
        ToolRef("filesystem.workspace", ToolKind.FILESYSTEM, "Workspace files", ToolStatus.AVAILABLE, "Laptop workspace read/write under the app-owned directory"),
        ToolRef("terminal.execute", ToolKind.TERMINAL, "Embedded terminal", ToolStatus.AVAILABLE, "Platform shell (/system/bin/sh) via app-owned PTY; governed effect provider tn.terminal.system-sh"),
        ToolRef("git.cli", ToolKind.GIT, "Git", ToolStatus.PLANNED, "Requires the embedded development environment"),
        ToolRef("mcp.registry", ToolKind.MCP, "MCP servers", ToolStatus.PLANNED, "Tool/MCP registry slice"),
        ToolRef("plugin.runtime", ToolKind.PLUGIN, "First-party plugins", ToolStatus.AVAILABLE, "Existing ToolNeuron plugin host with capability gates"),
        ToolRef("model.runtime", ToolKind.PLUGIN, "Local model runtime", ToolStatus.AVAILABLE, "Existing ToolNeuron GGUF inference stack"),
    )
}
