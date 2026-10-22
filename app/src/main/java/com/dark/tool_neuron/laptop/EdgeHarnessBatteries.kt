package com.dark.tool_neuron.laptop

import kotlinx.serialization.json.Json

enum class EdgeCheckMatch { EXACT, JSON, RESIDENT_PLAN, RESIDENT_CLARIFY }
data class EdgeHarnessCheck(val id: String, val prompt: String, val expected: String, val match: EdgeCheckMatch = EdgeCheckMatch.EXACT) {
    fun accepts(output: String): Boolean {
        if (output.length > 8192) return false
        return when (match) {
            EdgeCheckMatch.EXACT -> output.trim() == expected
            EdgeCheckMatch.RESIDENT_PLAN, EdgeCheckMatch.RESIDENT_CLARIFY -> {
                val parsed = com.dark.tool_neuron.laptop.resident.IntentParser.parse(output) as? com.dark.tool_neuron.laptop.resident.IntentParseResult.Parsed
                val envelope = parsed?.envelope
                envelope != null && !envelope.execution_requested && envelope.proposed_next_step.isNotBlank() &&
                    if (match == EdgeCheckMatch.RESIDENT_PLAN) envelope.candidate_capabilities == listOf("dev.workspace.inspect@1") && envelope.requested_lookups == listOf("laptop.workspace.current")
                    else envelope.missing_information.isNotEmpty() && envelope.requested_lookups.isEmpty()
            }
            EdgeCheckMatch.JSON -> runCatching { Json.parseToJsonElement(output) == Json.parseToJsonElement(expected) }.getOrDefault(false)
        }
    }
}
data class EdgeHarnessBattery(val battery: QualificationBattery, val title: String, val checks: List<EdgeHarnessCheck>)
object EdgeHarnessBatteries {
    private val instruction = listOf(
        EdgeHarnessCheck("ready", "Reply with exactly READY and no other text.", "READY"),
        EdgeHarnessCheck("ordered", "Reply with exactly: one,two,three", "one,two,three"),
    )
    private val structured = listOf(
        EdgeHarnessCheck("object", "Return only this JSON object, no markdown: {\"ready\":true,\"count\":2}", "{\"ready\":true,\"count\":2}", EdgeCheckMatch.JSON),
        EdgeHarnessCheck("array", "Return only this JSON array, no markdown: [\"red\",\"blue\"]", "[\"red\",\"blue\"]", EdgeCheckMatch.JSON),
    )
    private val resident = listOf(
        EdgeHarnessCheck("plan", "You are a read-only local planner. Plan inspection of the active project using dev.workspace.inspect@1 and laptop.workspace.current. Return only a cipher-intent/1 JSON envelope with user_objective, proposed_next_step, candidate_capabilities, requested_lookups and execution_requested=false. Do not invent lookup results.",
            """{"format":"cipher-intent/1","user_objective":"Inspect active project","proposed_next_step":"Read the project inventory","candidate_capabilities":["dev.workspace.inspect@1"],"requested_lookups":["laptop.workspace.current"],"execution_requested":false}""", EdgeCheckMatch.RESIDENT_PLAN),
        EdgeHarnessCheck("clarify", "You are a read-only local planner. The user says 'make it better' without identifying any project or goal. Return only a cipher-intent/1 JSON envelope with user_objective, a nonempty missing_information list, proposed_next_step asking for clarification, no requested_lookups and execution_requested=false.",
            """{"format":"cipher-intent/1","user_objective":"Make it better","missing_information":["Which project and goal?"],"proposed_next_step":"Ask for the project and desired improvement","requested_lookups":[],"execution_requested":false}""", EdgeCheckMatch.RESIDENT_CLARIFY),
    )
    val all = listOf(
        EdgeHarnessBattery(QualificationBattery("local.resident-envelope@1", resident.map { it.id }.toSet()), "Read-only resident envelope smoke", resident),
        EdgeHarnessBattery(QualificationBattery("local.instruction@1", instruction.map { it.id }.toSet()), "Instruction following", instruction),
        EdgeHarnessBattery(QualificationBattery("local.structured@1", structured.map { it.id }.toSet()), "Structured output", structured),
    )
}
