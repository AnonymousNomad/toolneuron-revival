package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelLabTest {
    @get:Rule val temp = TemporaryFolder()
    private val definition = EdgeHarnessBatteries.all.first()
    private val assignment = CipherAssignment("model", "a".repeat(64), "ggml", 4096)
    private val scope = QualificationScope(assignment.artifactSha256, CipherRuntimeIdentity.revision("ggml", 4096), "device")
    private class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        var reject = false
        override fun load() = bytes
        override fun save(bytes: ByteArray) { check(!reject); this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    @Test fun matchingCompletedObservationsPersistWithTheirActualEvidenceAndScope() = runBlocking {
        val observations = ModelLabRunner.run(definition) { prompt ->
            ModelLabResponse(definition.checks.single { it.prompt == prompt }.expected, true)
        }
        assertTrue(observations.all { it.verdict == QualificationVerdict.PASS })
        assertTrue(observations.all { it.measurements == null })
        val store = Store()
        val core = LaptopCore(store, temp.root)
        core.setPermission("model.worker.delegate@1", true).getOrThrow()
        val run = ModelLabRun("lab-one", scope, definition.battery.id, observations, 1, 2)
        core.recordQualificationRun(run).getOrThrow()
        val restarted = LaptopCore(store, temp.root)
        assertEquals(run, restarted.state.value.modelLabRuns.single())
        val receipt = restarted.state.value.qualifications.single()
        assertEquals(QualificationVerdict.PASS, receipt.verdictFor(scope, definition.battery))
        assertTrue(receipt.checks.all { check -> check.evidenceRefs.all { ref -> restarted.state.value.evidence.any { it.id == ref } } })
    }
    @Test fun incompleteErrorAndMismatchAreNotPassingObservations() = runBlocking {
        val incomplete = ModelLabRunner.run(definition) { ModelLabResponse("READY", false) }
        assertTrue(incomplete.all { it.verdict == QualificationVerdict.UNKNOWN })
        val failed = ModelLabRunner.run(definition) { ModelLabResponse("wrong", true) }
        assertTrue(failed.all { it.verdict == QualificationVerdict.FAIL })
        val error = ModelLabRunner.run(definition) { throw IllegalStateException("native unavailable") }
        assertTrue(error.all { it.verdict == QualificationVerdict.UNKNOWN && it.error == "native unavailable" })
    }
    @Test fun startedRunInvalidatesEarlierPassAndFailedSaveNeverPublishesCompletion() = runBlocking {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        core.setPermission("model.worker.delegate@1", true).getOrThrow()
        val complete = ModelLabRunner.run(definition) { prompt -> ModelLabResponse(definition.checks.single { it.prompt == prompt }.expected, true) }
        core.recordQualificationRun(ModelLabRun("first", scope, definition.battery.id, complete, 1, 2)).getOrThrow()
        core.recordQualificationRun(ModelLabRun("second", scope, definition.battery.id, emptyList(), 3)).getOrThrow()
        assertEquals(QualificationVerdict.UNKNOWN, core.state.value.qualifications.last().verdictFor(scope, definition.battery))
        val before = core.state.value
        store.reject = true
        assertTrue(core.recordQualificationRun(ModelLabRun("second", scope, definition.battery.id, complete, 3, 4)).isFailure)
        assertEquals(before, core.state.value)
        assertEquals(0, core.state.value.modelLabRuns.last().observations.size)
    }
    @Test fun malformedObservationSetsAndMissingPermissionCannotPublish() = runBlocking {
        val core = LaptopCore(Store(), temp.root)
        val observations = ModelLabRunner.run(definition) { ModelLabResponse("READY", true) }
        val run = ModelLabRun("run", scope, definition.battery.id, observations, 1, 2)
        assertTrue(core.recordQualificationRun(run).isFailure)
        core.setPermission("model.worker.delegate@1", true).getOrThrow()
        assertTrue(core.recordQualificationRun(run.copy(observations = observations + observations.first())).isFailure)
        assertTrue(core.recordQualificationRun(run.copy(batteryId = "invented")).isFailure)
        assertTrue(core.state.value.qualifications.isEmpty())
    }
    @Test fun residentRoleBatteryRejectsExecutionAndMissingClarification() {
        val resident = EdgeHarnessBatteries.all.single { it.battery.id == "local.resident-envelope@1" }
        assertTrue(resident.checks.all { it.accepts(it.expected) })
        assertFalse(resident.checks.first().accepts(resident.checks.first().expected.replace("false", "true")))
        assertFalse(resident.checks.last().accepts("""{"user_objective":"help","missing_information":[],"execution_requested":false}"""))
    }

    @Test fun expiredRawRunsCannotLeaveQualificationWithoutAnExportableRun() = runBlocking {
        val core = LaptopCore(Store(), temp.root)
        core.setPermission("model.worker.delegate@1", true).getOrThrow()
        val observations = ModelLabRunner.run(definition) { prompt -> ModelLabResponse(definition.checks.single { it.prompt == prompt }.expected, true) }
        core.recordQualificationRun(ModelLabRun("old", scope, definition.battery.id, observations, 1, 2)).getOrThrow()
        repeat(100) { index -> core.recordQualificationRun(ModelLabRun("new-$index", scope.copy(deviceBuild = "another-device"), definition.battery.id, emptyList(), 3)).getOrThrow() }
        assertEquals(100, core.state.value.modelLabRuns.size)
        assertTrue(core.state.value.modelLabRuns.none { it.id == "old" })
        assertTrue(core.state.value.qualifications.none { it.scope == scope })
    }

}
