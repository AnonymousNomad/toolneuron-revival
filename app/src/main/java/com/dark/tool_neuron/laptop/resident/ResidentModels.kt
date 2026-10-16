package com.dark.tool_neuron.laptop.resident

import kotlinx.serialization.Serializable

const val RESIDENT_FORMAT = "cipher-resident/1"
const val INTENT_FORMAT = "cipher-intent/1"

@Serializable
data class ResidentIdentity(
    val format: String = RESIDENT_FORMAT,
    val role: String = "resident.orchestrator",
    val modelId: String,
    val modelName: String,
    val artifactRef: String,
    val artifactBytes: Long,
    val architecture: String?,
    val artifactSha256: String?,
    val hashUnavailableReason: String? = null,
    val runtime: String = "local llama.cpp via :inference",
    val admission: List<String> = emptyList(),
    val capabilityRevision: String = "capability-registry/1",
    val contextTemplateIdentity: String,
    val contextProfile: String = "ctx-4096",
    val selectionReason: String = "",
    val laptopContract: String = "cipher-laptop/1",
    val boundAtMs: Long,
)

/** Structured resident activity. Derived from real application data (lookup results,
 *  parsed envelope fields) — never fabricated from model prose. */
sealed class ResidentEvent {
    data class Lookup(
        val source: String,
        val line: String,
        val trust: LookupTrust,
        val truncated: Boolean,
    ) : ResidentEvent()

    data class Capability(val refs: List<String>) : ResidentEvent()
    data class MissingInfo(val items: List<String>) : ResidentEvent()
    data class Worker(val items: List<String>) : ResidentEvent()
    data object ExecutionRefused : ResidentEvent()
}

object ResidentLoadConfig {
    fun jsonFor(contextSize: Int): String = """{"contextSize":$contextSize}"""
}

object ResidentEvents {
    fun derive(lookupResults: List<LaptopLookupResult>, envelope: IntentEnvelope?): List<ResidentEvent> = buildList {
        lookupResults.forEach { r ->
            add(
                ResidentEvent.Lookup(
                    source = r.source,
                    line = r.summary.lineSequence().firstOrNull()?.take(160).orEmpty(),
                    trust = r.trust,
                    truncated = r.truncated,
                ),
            )
        }
        envelope?.let { env ->
            if (env.candidate_capabilities.isNotEmpty()) add(ResidentEvent.Capability(env.candidate_capabilities))
            if (env.missing_information.isNotEmpty()) add(ResidentEvent.MissingInfo(env.missing_information))
            if (env.worker_requirements.isNotEmpty()) add(ResidentEvent.Worker(env.worker_requirements))
            if (env.execution_requested) add(ResidentEvent.ExecutionRefused)
        }
    }
}

@Serializable
enum class ResidentSessionStatus { ACTIVE, ENDED_BY_USER, ENDED_PROCESS_RESTART, FAILED }

@Serializable
data class ResidentSessionRecord(
    val id: String,
    val identity: ResidentIdentity,
    val activeWorkspaceId: String?,
    val activeTaskId: String?,
    val turns: Int,
    val lookupCount: Int,
    val status: ResidentSessionStatus,
    val statusReason: String? = null,
    val startedAtMs: Long,
    val updatedAtMs: Long,
    val endedAtMs: Long? = null,
)

@Serializable
data class ResidentState(
    val sessions: List<ResidentSessionRecord> = emptyList(),
    val activeSessionId: String? = null,
)
