package com.dark.tool_neuron.laptop

import kotlinx.serialization.Serializable

@Serializable
data class CipherAssignment(val modelId: String, val artifactSha256: String, val runtimeId: String, val contextSize: Int) {
    init {
        require(modelId.isNotBlank() && modelId.length <= 256)
        require(artifactSha256.matches(Regex("[a-f0-9]{64}")))
        require(runtimeId in setOf("ggml", "covert-r0"))
        require(contextSize in setOf(4096, 8192))
    }
}
enum class CipherSlotStatus { EMPTY, UNQUALIFIED, UNAVAILABLE, READY }
data class CipherSlotAssessment(val status: CipherSlotStatus, val reason: String)
object CipherSlotPolicy {
    fun assess(assignment: CipherAssignment?, scope: QualificationScope, records: List<QualificationRecord>,
        artifactAvailable: Boolean, runtimeAvailable: Boolean, deviceFit: Boolean): CipherSlotAssessment {
        if (assignment == null) return CipherSlotAssessment(CipherSlotStatus.EMPTY, "No assistant model assigned.")
        if (!artifactAvailable || scope.modelSha256 != assignment.artifactSha256)
            return CipherSlotAssessment(CipherSlotStatus.UNAVAILABLE, "Assigned model is missing or its artifact identity changed. Reassign explicitly.")
        if (scope.runtimeRevision != CipherRuntimeIdentity.revision(assignment.runtimeId, assignment.contextSize))
            return CipherSlotAssessment(CipherSlotStatus.UNQUALIFIED, "Qualification runtime/chassis scope does not match assignment.")
        if (!runtimeAvailable) return CipherSlotAssessment(CipherSlotStatus.UNAVAILABLE, "Selected native runtime unavailable. No fallback.")
        if (!deviceFit) return CipherSlotAssessment(CipherSlotStatus.UNAVAILABLE, "Assigned model does not pass current memory admission.")
        val qualified = EdgeHarnessBatteries.all.all { definition ->
            records.filter { it.scope == scope && it.batteryId == definition.battery.id }
                .lastOrNull()?.verdictFor(scope, definition.battery) == QualificationVerdict.PASS
        }
        return if (qualified) CipherSlotAssessment(CipherSlotStatus.READY, "Required local smoke batteries pass for this artifact/runtime/chassis/device. Broader ability is unverified.")
        else CipherSlotAssessment(CipherSlotStatus.UNQUALIFIED, "Assignment saved. Run Model Lab batteries on this device; installation is not qualification.")
    }
}
