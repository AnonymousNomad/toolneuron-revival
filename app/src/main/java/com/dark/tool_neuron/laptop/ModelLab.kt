package com.dark.tool_neuron.laptop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable

data class ModelLabResponse(val output: String, val completed: Boolean, val error: String? = null, val measurements: QualificationMeasurements? = null)
@Serializable
data class ModelLabObservation(val checkId: String, val prompt: String, val expected: String, val output: String,
    val completed: Boolean, val error: String?, val truncated: Boolean, val verdict: QualificationVerdict,
    val measurements: QualificationMeasurements? = null)
@Serializable
data class ModelLabRun(val id: String, val scope: QualificationScope, val batteryId: String,
    val observations: List<ModelLabObservation>, val startedAtMs: Long, val finishedAtMs: Long? = null) {
    fun validate(): EdgeHarnessBattery {
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,80}")) && startedAtMs > 0 && (finishedAtMs == null || finishedAtMs > 0))
        val definition = EdgeHarnessBatteries.all.single { it.battery.id == batteryId }
        require(observations.map { it.checkId }.distinct().size == observations.size)
        require(if (finishedAtMs == null) observations.isEmpty() else observations.map { it.checkId }.toSet() == definition.battery.checkIds)
        observations.forEach { observed ->
            val check = definition.checks.single { it.id == observed.checkId }
            require(observed.prompt == check.prompt && observed.expected == check.expected && observed.output.length <= 8192 && (observed.error?.length ?: 0) <= 400)
            require(observed.verdict == ModelLabRunner.verdict(check, ModelLabResponse(observed.output, observed.completed, observed.error), observed.truncated))
        }
        return definition
    }
}
object ModelLabRunner {
    fun verdict(check: EdgeHarnessCheck, response: ModelLabResponse, truncated: Boolean = response.output.length > 8192): QualificationVerdict = when {
        !response.completed || response.error != null -> QualificationVerdict.UNKNOWN
        truncated || !check.accepts(response.output) -> QualificationVerdict.FAIL
        else -> QualificationVerdict.PASS
    }
    suspend fun run(definition: EdgeHarnessBattery, generate: suspend (String) -> ModelLabResponse): List<ModelLabObservation> = definition.checks.map { check ->
        currentCoroutineContext().ensureActive()
        val response = try { generate(check.prompt) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { ModelLabResponse("", false, (failure.message ?: failure.javaClass.simpleName).take(400)) }
        ModelLabObservation(check.id, check.prompt, check.expected, response.output.take(8192), response.completed,
            response.error?.take(400), response.output.length > 8192, verdict(check, response), response.measurements)
    }
}

@Serializable
data class ModelLabExport(val run: ModelLabRun, val evidence: List<EvidenceRecord>, val qualification: QualificationRecord?,
    val appVersion: String, val format: String = "toolneuron-model-lab/1", val limitations: String = "Text smoke checks only. No VLM, broad skill, execution permission, cloud service or physical release acceptance is conferred.")
