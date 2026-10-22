package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.QualificationCheck
import com.dark.tool_neuron.laptop.QualificationBattery
import com.dark.tool_neuron.laptop.QualificationMeasurements
import com.dark.tool_neuron.laptop.QualificationRecord
import com.dark.tool_neuron.laptop.QualificationScope
import com.dark.tool_neuron.laptop.QualificationVerdict
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class ModelQualificationTest {
    private val scope = QualificationScope("a".repeat(64), "ggml/rev1", "device/build1")
    private val battery = QualificationBattery("chat@1", setOf("stream"))
    private val pass = QualificationCheck("stream", QualificationVerdict.PASS, listOf("evidence:stream-1"))
    private fun record(checks: List<QualificationCheck> = listOf(pass)) =
        QualificationRecord(scope, "chat@1", checks, 1234)

    @Test fun noChecksMeansUnknown() {
        assertEquals(QualificationVerdict.UNKNOWN, record(emptyList()).verdictFor(scope, battery))
    }

    @Test fun measuredChecksPassOnlyForTheirExactScopeAndBattery() {
        assertEquals(QualificationVerdict.PASS, record().verdictFor(scope, battery))
        assertEquals(QualificationVerdict.UNKNOWN, record().verdictFor(scope.copy(modelSha256 = "b".repeat(64)), battery))
        assertEquals(QualificationVerdict.UNKNOWN, record().verdictFor(scope.copy(runtimeRevision = "ggml/rev2"), battery))
        assertEquals(QualificationVerdict.UNKNOWN, record().verdictFor(scope.copy(deviceBuild = "other-device"), battery))
        assertEquals(QualificationVerdict.UNKNOWN, record().verdictFor(scope, battery.copy(id = "code@1")))
    }

    @Test fun missingEvidenceOrIncompleteChecksCannotPass() {
        assertEquals(QualificationVerdict.UNKNOWN, record(listOf(pass.copy(evidenceRefs = emptyList()))).verdictFor(scope, battery))
        assertEquals(QualificationVerdict.UNKNOWN, record(listOf(pass.copy(verdict = QualificationVerdict.UNKNOWN))).verdictFor(scope, battery))
        assertEquals(QualificationVerdict.UNKNOWN, record(listOf(pass, pass)).verdictFor(scope, battery))
        assertEquals(QualificationVerdict.UNKNOWN, record().verdictFor(scope, battery.copy(checkIds = setOf("stream", "cancel"))))
    }

    @Test fun evidenceBackedFailureRemainsFailure() {
        val failed = pass.copy(id = "cancel", verdict = QualificationVerdict.FAIL)
        assertEquals(QualificationVerdict.FAIL, record(listOf(pass, failed)).verdictFor(scope, battery.copy(checkIds = setOf("stream", "cancel"))))
    }

    @Test fun unavailableMeasurementsRemainNull() {
        val decoded = Json.decodeFromString<QualificationRecord>(Json.encodeToString(record()))
        assertEquals(record(), decoded)
        assertNull(decoded.checks.single().measurements)
    }

    @Test fun invalidMeasurementsAndMissingFingerprintsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { QualificationMeasurements(tokensPerSecond = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { QualificationMeasurements(ttftMs = -1) }
        assertThrows(IllegalArgumentException::class.java) { QualificationMeasurements(peakRssBytes = -1) }
        assertThrows(IllegalArgumentException::class.java) { QualificationScope("", "runtime", "device") }
    }
}
