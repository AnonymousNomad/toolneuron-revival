package com.dark.tool_neuron.util

enum class SemanticRole { GENERATION, EMBEDDING, RERANKER, VISION, MULTIMODAL, UNKNOWN }

enum class CapabilityRuntime { CHAT, RAG_EMBED, RERANK, VISION }

enum class RuntimeSupport { SUPPORTED, UNSUPPORTED, UNKNOWN }

enum class TaskAdmission { CAN_CHAT, CAN_EMBED, CAN_RERANK, CAN_VISION }

data class CapabilityProfile(
    val roles: Set<SemanticRole>,
    val runtimeSupport: Map<CapabilityRuntime, RuntimeSupport>,
    val admissions: Set<TaskAdmission>,
) {
    fun supportFor(runtime: CapabilityRuntime): RuntimeSupport =
        runtimeSupport[runtime] ?: RuntimeSupport.UNKNOWN
}
