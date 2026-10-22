package com.dark.tool_neuron.laptop

import kotlinx.serialization.json.Json

enum class EdgeCheckMatch { EXACT, JSON }
data class EdgeHarnessCheck(val id: String, val prompt: String, val expected: String, val match: EdgeCheckMatch = EdgeCheckMatch.EXACT) {
    fun accepts(output: String): Boolean {
        if (output.length > 8192) return false
        return when (match) {
            EdgeCheckMatch.EXACT -> output.trim() == expected
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
    val all = listOf(
        EdgeHarnessBattery(QualificationBattery("local.instruction@1", instruction.map { it.id }.toSet()), "Instruction following", instruction),
        EdgeHarnessBattery(QualificationBattery("local.structured@1", structured.map { it.id }.toSet()), "Structured output", structured),
    )
}
