package com.dark.tool_neuron.laptop

import kotlinx.serialization.Serializable

@Serializable
enum class QualificationVerdict { UNKNOWN, PASS, FAIL }

@Serializable
data class QualificationScope(
    val modelSha256: String,
    val runtimeRevision: String,
    val deviceBuild: String,
) {
    init {
        require(modelSha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(runtimeRevision.isNotBlank() && deviceBuild.isNotBlank())
    }
}

data class QualificationBattery(val id: String, val checkIds: Set<String>) {
    init {
        require(id.isNotBlank() && checkIds.isNotEmpty() && checkIds.all { it.isNotBlank() })
    }
}

@Serializable
data class QualificationMeasurements(
    val ttftMs: Long? = null,
    val tokensPerSecond: Double? = null,
    val peakRssBytes: Long? = null,
) {
    init {
        require(ttftMs == null || ttftMs >= 0)
        require(tokensPerSecond == null || (tokensPerSecond.isFinite() && tokensPerSecond >= 0))
        require(peakRssBytes == null || peakRssBytes >= 0)
    }
}

@Serializable
data class QualificationCheck(
    val id: String,
    val verdict: QualificationVerdict,
    val evidenceRefs: List<String>,
    val measurements: QualificationMeasurements? = null,
)

@Serializable
data class QualificationRecord(
    val scope: QualificationScope,
    val batteryId: String,
    val checks: List<QualificationCheck>,
    val recordedAtMs: Long,
) {
    fun verdictFor(expectedScope: QualificationScope, battery: QualificationBattery): QualificationVerdict {
        if (scope != expectedScope || batteryId != battery.id || recordedAtMs <= 0) return QualificationVerdict.UNKNOWN
        if (checks.map { it.id }.toSet() != battery.checkIds || checks.size != battery.checkIds.size) {
            return QualificationVerdict.UNKNOWN
        }
        if (checks.any { it.evidenceRefs.isEmpty() || it.evidenceRefs.any(String::isBlank) }) {
            return QualificationVerdict.UNKNOWN
        }
        return when {
            checks.any { it.verdict == QualificationVerdict.FAIL } -> QualificationVerdict.FAIL
            checks.all { it.verdict == QualificationVerdict.PASS } -> QualificationVerdict.PASS
            else -> QualificationVerdict.UNKNOWN
        }
    }
}
