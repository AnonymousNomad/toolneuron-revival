package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class CipherSlotTest {
    @get:Rule val temp = TemporaryFolder()
    private val assignment = CipherAssignment("model", "a".repeat(64), "ggml", 4096)
    private val scope = QualificationScope(assignment.artifactSha256, CipherRuntimeIdentity.revision("ggml", 4096), "device-build")
    @Test fun installedOrAssignedModelsAreNotAutomaticallyReady() {
        assertEquals(CipherSlotStatus.EMPTY, CipherSlotPolicy.assess(null, scope, emptyList(), true, true, true).status)
        assertEquals(CipherSlotStatus.UNQUALIFIED, CipherSlotPolicy.assess(assignment, scope, emptyList(), true, true, true).status)
    }
    @Test fun readinessRequiresEveryRequiredBatteryForExactScope() {
        val receipts = EdgeHarnessBatteries.all.map { definition ->
            QualificationRecord(scope, definition.battery.id, definition.checks.map {
                QualificationCheck(it.id, QualificationVerdict.PASS, listOf("real-evidence:${it.id}"))
            }, 1)
        }
        assertEquals(CipherSlotStatus.READY, CipherSlotPolicy.assess(assignment, scope, receipts, true, true, true).status)
        assertEquals(CipherSlotStatus.UNQUALIFIED, CipherSlotPolicy.assess(assignment, scope.copy(deviceBuild = "different"), receipts, true, true, true).status)
        assertEquals(CipherSlotStatus.UNAVAILABLE, CipherSlotPolicy.assess(assignment, scope, receipts, false, true, true).status)
        assertEquals(CipherSlotStatus.UNAVAILABLE, CipherSlotPolicy.assess(assignment, scope, receipts, true, false, true).status)
        assertEquals(CipherSlotStatus.UNAVAILABLE, CipherSlotPolicy.assess(assignment, scope, receipts, true, true, false).status)
    }
    @Test fun laterFailureInvalidatesEarlierPass() {
        val receipts = EdgeHarnessBatteries.all.map { definition -> QualificationRecord(scope, definition.battery.id,
            definition.checks.map { QualificationCheck(it.id, QualificationVerdict.PASS, listOf("evidence")) }, 1) }
        val failed = receipts.first().copy(checks = receipts.first().checks.map { it.copy(verdict = QualificationVerdict.FAIL) }, recordedAtMs = 2)
        assertEquals(CipherSlotStatus.UNQUALIFIED, CipherSlotPolicy.assess(assignment, scope, receipts + failed, true, true, true).status)
    }
    @Test fun assignmentRejectsInvalidIdentityAndChassis() {
        assertThrows(IllegalArgumentException::class.java) { assignment.copy(artifactSha256 = "unknown") }
        assertThrows(IllegalArgumentException::class.java) { assignment.copy(contextSize = 1000000) }
        assertThrows(IllegalArgumentException::class.java) { assignment.copy(runtimeId = "automatic") }
    }
    @Test fun receiptsCannotQualifyADifferentRuntimeOrChassisAndAppendOrderWins() {
        val receipts = EdgeHarnessBatteries.all.map { d -> QualificationRecord(scope, d.battery.id,
            d.checks.map { QualificationCheck(it.id, QualificationVerdict.PASS, listOf("evidence")) }, 100) }
        assertFalse(CipherSlotPolicy.assess(assignment.copy(runtimeId = "covert-r0"), scope, receipts, true, true, true).status == CipherSlotStatus.READY)
        assertFalse(CipherSlotPolicy.assess(assignment.copy(contextSize = 8192), scope, receipts, true, true, true).status == CipherSlotStatus.READY)
        val failure = receipts.first().copy(checks = receipts.first().checks.map { it.copy(verdict = QualificationVerdict.FAIL) }, recordedAtMs = 99)
        assertEquals(CipherSlotStatus.UNQUALIFIED, CipherSlotPolicy.assess(assignment, scope, receipts + failure, true, true, true).status)
    }

    @Test fun assignmentSurvivesRestartAndStaleStartsCannotReplaceActiveAuthority() {
        val store = object : LaptopPersistence {
            var bytes: ByteArray? = null
            var reject = false
            override fun load() = bytes
            override fun save(bytes: ByteArray) { check(!reject); this.bytes = bytes }
            override fun quarantineCorrupt(bytes: ByteArray) = Unit
        }
        val core = LaptopCore(store, temp.root)
        core.assignCipher(assignment).getOrThrow()
        assertEquals(assignment, LaptopCore(store, temp.root).state.value.cipherAssignment)
        val identity = com.dark.tool_neuron.laptop.resident.ResidentIdentity(modelId = "model", modelName = "model", artifactRef = "file", artifactBytes = 1,
            architecture = null, artifactSha256 = assignment.artifactSha256, contextTemplateIdentity = "profile", boundAtMs = 1)
        val changed = assignment.copy(contextSize = 8192)
        core.assignCipher(changed).getOrThrow()
        assertTrue(core.startResidentSession(identity, null, null, expectedAssignment = assignment).isFailure)
        assertNull(core.state.value.resident.activeSessionId)
        val active = core.startResidentSession(identity, null, null, expectedAssignment = changed).getOrThrow()
        assertTrue(core.assignCipher(null).isFailure)
        assertTrue(core.startResidentSession(identity, null, null, expectedAssignment = assignment).isFailure)
        assertEquals(active.id, core.state.value.resident.activeSessionId)
        core.endResidentSession(active.id, com.dark.tool_neuron.laptop.resident.ResidentSessionStatus.ENDED_BY_USER).getOrThrow()
        store.reject = true
        assertTrue(core.assignCipher(null).isFailure)
        assertEquals(changed, core.state.value.cipherAssignment)
    }
    @Test fun qualificationCannotReferenceMissingEvidence() {
        val record = QualificationRecord(scope, EdgeHarnessBatteries.all.first().battery.id,
            listOf(QualificationCheck("ready", QualificationVerdict.PASS, listOf("missing"))), 1)
        assertThrows(LaptopStateError::class.java) { LaptopCodec.validate(CipherLaptop(qualifications = listOf(record))) }
    }

}
