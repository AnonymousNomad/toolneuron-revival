package com.dark.tool_neuron.repo

import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.model.enums.ProviderType
import com.dark.tool_neuron.util.CapabilityProfile
import com.dark.tool_neuron.util.CapabilityRuntime
import com.dark.tool_neuron.util.RuntimeSupport
import com.dark.tool_neuron.util.SemanticRole
import com.dark.tool_neuron.util.TaskAdmission
import com.dark.tool_neuron.util.VlmPaths

object ModelCapabilityResolver {
    const val RAG_RUNTIME_UNVERIFIED_MESSAGE =
        "This embedding model has not been verified for the current RAG runtime."

    // Admission doctrine: a task is admitted only when the role is established AND the
    // required runtime is not proven incompatible. Runtimes with a device-proven failure
    // class (RAG_EMBED: the bge-small SIGABRT, see RagRuntimeCompatibility) additionally
    // require SUPPORTED evidence - fail-closed. Runtimes with no proven failure class
    // (CHAT, VISION) admit UNKNOWN, because refusing would fabricate an incompatibility
    // claim; a future proven-unsupported artifact flips them to UNSUPPORTED here.
    fun profileOf(model: ModelInfo): CapabilityProfile {
        val roles = rolesOf(model)
        val support = runtimeSupportOf(model, roles)
        return CapabilityProfile(roles, support, admissionsOf(roles, support, model))
    }

    fun ragStatusText(model: ModelInfo): String =
        when (profileOf(model).supportFor(CapabilityRuntime.RAG_EMBED)) {
            RuntimeSupport.SUPPORTED -> "Ready"
            RuntimeSupport.UNSUPPORTED -> "Not compatible with current RAG runtime"
            RuntimeSupport.UNKNOWN -> "Not verified for current RAG runtime"
        }

    fun selectRagEmbeddingModel(installed: List<ModelInfo>, preferredId: String?): ModelInfo? {
        val candidates = installed.filter { it.providerType == ProviderType.EMBEDDING }
        if (candidates.isEmpty()) return null
        candidates.firstOrNull { it.id == preferredId }?.let { return it }
        return candidates.firstOrNull { TaskAdmission.CAN_EMBED in profileOf(it).admissions }
            ?: candidates.first()
    }

    private fun rolesOf(model: ModelInfo): Set<SemanticRole> = when (model.providerType) {
        ProviderType.GGUF ->
            if (isVlmPath(model)) setOf(SemanticRole.GENERATION, SemanticRole.VISION)
            else setOf(SemanticRole.GENERATION)
        ProviderType.EMBEDDING -> setOf(SemanticRole.EMBEDDING)
        ProviderType.UNKNOWN -> setOf(SemanticRole.UNKNOWN)
        else -> emptySet()
    }

    private fun isVlmPath(model: ModelInfo): Boolean =
        model.pathType == PathType.FILE && model.path.contains("/${VlmPaths.VLM_DIR}/")

    private fun runtimeSupportOf(
        model: ModelInfo,
        roles: Set<SemanticRole>,
    ): Map<CapabilityRuntime, RuntimeSupport> {
        val support = mutableMapOf<CapabilityRuntime, RuntimeSupport>()
        if (SemanticRole.GENERATION in roles) support[CapabilityRuntime.CHAT] = RuntimeSupport.UNKNOWN
        if (SemanticRole.EMBEDDING in roles) {
            support[CapabilityRuntime.RAG_EMBED] = mapRagSupport(RagRuntimeCompatibility.evaluate(model))
        }
        if (SemanticRole.VISION in roles) support[CapabilityRuntime.VISION] = RuntimeSupport.UNKNOWN
        return support
    }

    private fun mapRagSupport(support: RagRuntimeSupport): RuntimeSupport = when (support) {
        RagRuntimeSupport.SUPPORTED -> RuntimeSupport.SUPPORTED
        RagRuntimeSupport.UNSUPPORTED -> RuntimeSupport.UNSUPPORTED
        RagRuntimeSupport.UNKNOWN -> RuntimeSupport.UNKNOWN
    }

    private fun admissionsOf(
        roles: Set<SemanticRole>,
        support: Map<CapabilityRuntime, RuntimeSupport>,
        model: ModelInfo,
    ): Set<TaskAdmission> {
        if (model.path.isBlank()) return emptySet()
        val admissions = mutableSetOf<TaskAdmission>()
        if (SemanticRole.GENERATION in roles &&
            support[CapabilityRuntime.CHAT] != RuntimeSupport.UNSUPPORTED
        ) admissions += TaskAdmission.CAN_CHAT
        if (SemanticRole.EMBEDDING in roles &&
            support[CapabilityRuntime.RAG_EMBED] == RuntimeSupport.SUPPORTED
        ) admissions += TaskAdmission.CAN_EMBED
        if (SemanticRole.RERANKER in roles &&
            support[CapabilityRuntime.RERANK] != RuntimeSupport.UNSUPPORTED
        ) admissions += TaskAdmission.CAN_RERANK
        if (SemanticRole.VISION in roles &&
            support[CapabilityRuntime.VISION] != RuntimeSupport.UNSUPPORTED
        ) admissions += TaskAdmission.CAN_VISION
        return admissions
    }
}
