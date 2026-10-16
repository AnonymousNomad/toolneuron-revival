package com.dark.tool_neuron.laptop.resident

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class IntentEnvelope(
    val format: String = INTENT_FORMAT,
    val user_objective: String = "",
    val interpreted_goal: String = "",
    val active_project: String = "",
    val capability_status: String = "UNKNOWN",
    val candidate_capabilities: List<String> = emptyList(),
    val required_context_refs: List<String> = emptyList(),
    val missing_information: List<String> = emptyList(),
    val requested_lookups: List<String> = emptyList(),
    val worker_requirements: List<String> = emptyList(),
    val proposed_next_step: String = "",
    val execution_requested: Boolean = false,
    val user_response: String = "",
)

enum class ResidentTurnStatus { OK, NEEDS_USER_INPUT, CAPABILITY_NOT_FOUND, EXECUTION_REFUSED, INTENT_PARSE_FAILED }

sealed class IntentParseResult {
    data class Parsed(val envelope: IntentEnvelope) : IntentParseResult()
    data class Failed(val reason: String, val rawExcerpt: String) : IntentParseResult()
}

object IntentParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private const val MAX_EXCERPT = 400

    fun parse(raw: String): IntentParseResult {
        val objText = extractJsonObject(raw)
            ?: return IntentParseResult.Failed("no JSON object found in output", raw.trim().take(MAX_EXCERPT))
        val obj = try {
            json.parseToJsonElement(objText) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return IntentParseResult.Failed("JSON parse failed: ${raw.trim().take(80)}", raw.trim().take(MAX_EXCERPT))

        fun str(key: String, cap: Int = 400): String =
            runCatching { obj[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.take(cap)?.trim() ?: ""

        fun list(key: String, cap: Int = 8): List<String> =
            runCatching {
                obj[key]?.jsonArray?.mapNotNull { e -> runCatching { e.jsonPrimitive.contentOrNull }.getOrNull()?.take(300) }?.take(cap)
            }.getOrNull() ?: emptyList()

        fun bool(key: String): Boolean =
            runCatching { obj[key]?.jsonPrimitive?.booleanOrNull }.getOrNull() ?: false

        val envelope = IntentEnvelope(
            user_objective = str("user_objective"),
            interpreted_goal = str("interpreted_goal"),
            active_project = str("active_project"),
            capability_status = str("capability_status", 20).uppercase().ifBlank { "UNKNOWN" },
            candidate_capabilities = list("candidate_capabilities"),
            required_context_refs = list("required_context_refs"),
            missing_information = list("missing_information"),
            requested_lookups = list("requested_lookups"),
            worker_requirements = list("worker_requirements"),
            proposed_next_step = str("proposed_next_step"),
            execution_requested = bool("execution_requested"),
            user_response = str("user_response", 800),
        )
        if (envelope.user_objective.isBlank() && envelope.interpreted_goal.isBlank()) {
            return IntentParseResult.Failed("envelope carries neither objective nor goal", raw.trim().take(MAX_EXCERPT))
        }
        return IntentParseResult.Parsed(envelope)
    }

    fun status(envelope: IntentEnvelope): ResidentTurnStatus = when {
        envelope.execution_requested -> ResidentTurnStatus.EXECUTION_REFUSED
        envelope.missing_information.isNotEmpty() -> ResidentTurnStatus.NEEDS_USER_INPUT
        envelope.capability_status == "NONE" -> ResidentTurnStatus.CAPABILITY_NOT_FOUND
        else -> ResidentTurnStatus.OK
    }

    fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in start until raw.length) {
            val c = raw[i]
            if (inStr) {
                when {
                    esc -> esc = false
                    c == '\\' -> esc = true
                    c == '"' -> inStr = false
                }
                continue
            }
            when (c) {
                '"' -> inStr = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }
}

object IntentRender {
    fun naturalText(envelope: IntentEnvelope, status: ResidentTurnStatus): String {
        if (envelope.user_response.isNotBlank()) return envelope.user_response
        return buildString {
            append("Objective: ").append(envelope.interpreted_goal.ifBlank { envelope.user_objective }).append('\n')
            if (envelope.candidate_capabilities.isNotEmpty()) {
                append("Relevant capabilities: ").append(envelope.candidate_capabilities.joinToString(", ")).append('\n')
            }
            if (envelope.missing_information.isNotEmpty()) {
                append("I need from you: ").append(envelope.missing_information.joinToString("; ")).append('\n')
            }
            if (status == ResidentTurnStatus.EXECUTION_REFUSED) {
                append("I cannot execute commands in this phase.\n")
            }
            if (envelope.proposed_next_step.isNotBlank()) {
                append("Proposed next step: ").append(envelope.proposed_next_step)
            }
        }.trim().take(1200)
    }
}
