package com.dark.tool_neuron.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.BuildConfig
import com.dark.tool_neuron.laptop.*
import com.dark.tool_neuron.service.inference.InferenceClient
import com.dark.tool_neuron.service.inference.InferenceEvent
import com.dark.tool_neuron.util.ChatShareHelper
import com.dark.tool_neuron.viewmodel.home_vm.ModelSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ModelLabViewModel @Inject constructor(private val app: Application, private val laptop: LaptopManager,
    private val inspector: CipherAssignmentInspector, private val chatSession: ModelSessionManager) : ViewModel() {
    val state = laptop.state
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()
    private val _assessment = MutableStateFlow<CipherSlotAssessment?>(null)
    val assessment = _assessment.asStateFlow()
    private var running: Job? = null
    init {
        viewModelScope.launch {
            state.map { snapshot -> snapshot.permissions.any { it.capabilityId == "model.worker.delegate@1" && it.granted } }
                .distinctUntilChanged().collect { granted -> if (!granted) running?.cancel() }
        }
    }
    fun refresh() {
        if (_busy.value) return
        viewModelScope.launch {
            try { _assessment.value = inspector.inspect().assessment }
            catch (failure: Exception) { _status.value = failure.message }
        }
    }
    fun runAll() {
        if (!_busy.compareAndSet(false, true)) return
        _assessment.value = null
        running = viewModelScope.launch(Dispatchers.IO) {
            var loadId: String? = null
            try {
                check(laptop.core.hasPermission("model.worker.delegate@1")) { "Grant local model invocation in Abilities first" }
                check(laptop.state.value.resident.activeSessionId == null) { "Stop Cipher before running batteries" }
                val inspection = inspector.inspect()
                val assignment = inspection.assignment ?: error("Assign a model/chassis in Cipher first")
                val model = inspection.model ?: error("Assigned model is missing")
                check(inspection.assessment.status != CipherSlotStatus.UNAVAILABLE) { inspection.assessment.reason }
                check(assignment.runtimeId == "ggml") { "Covert R0 native source unavailable; no fallback" }
                val scope = CipherRuntimeIdentity.scope(assignment)
                val starts = EdgeHarnessBatteries.all.map { definition -> ModelLabRun("lab-" + UUID.randomUUID().toString().replace("-", ""), scope,
                    definition.battery.id, emptyList(), System.currentTimeMillis()) }
                starts.forEach { laptop.core.recordQualificationRun(it).getOrThrow() }
                chatSession.invalidateForExternalLoad()
                _status.value = "Loading the assigned artifact for local smoke checks…"
                val info = InferenceClient.loadModel(model.path, CipherRuntimeIdentity.config(assignment)).getOrThrow()
                loadId = JSONObject(info).getString("loadId").also { check(it.isNotBlank() && it != "null") }
                check(inspector.hash(model) == assignment.artifactSha256) { "Artifact changed during load; reassign explicitly" }
                for ((index, definition) in EdgeHarnessBatteries.all.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    check(laptop.core.hasPermission("model.worker.delegate@1")) { "Model invocation permission revoked" }
                    _status.value = "Running ${definition.title} (${index + 1}/${starts.size})…"
                    val observations = ModelLabRunner.run(definition) { prompt ->
                        check(laptop.core.hasPermission("model.worker.delegate@1")) { "Model invocation permission revoked" }
                        generate(prompt, loadId!!)
                    }
                    check(inspector.hash(model) == assignment.artifactSha256) { "Artifact changed during qualification" }
                    check(JSONObject(InferenceClient.getModelInfo() ?: "{}").optString("loadId") == loadId) { "Loaded model/configuration changed during qualification" }
                    laptop.core.recordQualificationRun(starts[index].copy(observations = observations, finishedAtMs = System.currentTimeMillis())).getOrThrow()
                }
                _status.value = "Finished local smoke checks. Review each recorded result; broader skills and release acceptance remain unverified."
            } catch (cancelled: CancellationException) {
                _status.value = "Stopped. Unfinished batteries remain UNKNOWN; completed observations are retained."
                throw cancelled
            } catch (failure: Exception) {
                _status.value = (failure.message ?: "Qualification failed").take(600) + ". Unfinished batteries remain UNKNOWN."
            } finally {
                withContext(NonCancellable) {
                    loadId?.let { runCatching { InferenceClient.unloadModelForLoad(it) } }
                    runCatching { _assessment.value = inspector.inspect().assessment }
                }
                _busy.value = false
            }
        }
    }
    fun cancel() { running?.cancel() }
    private suspend fun generate(prompt: String, loadId: String): ModelLabResponse {
        val output = StringBuilder()
        var completed = false
        var error: String? = null
        var measurements: QualificationMeasurements? = null
        try {
            withTimeout(120_000) {
                val messages = JSONArray().put(JSONObject().put("role", "user").put("content", prompt))
                InferenceClient.generateMultiTurn(messages.toString(), 256, loadId).transformWhile { event ->
                    when (event) {
                        is InferenceEvent.Token -> { output.append(event.text.take((8193 - output.length).coerceAtLeast(0))); if (output.length > 8192) error = "Output exceeded evidence bound" }
                        is InferenceEvent.Done -> completed = true
                        is InferenceEvent.Error -> error = event.message.take(400)
                        is InferenceEvent.Metrics -> {
                            val metrics = if (event.metricsJson.length <= 4096) runCatching { JSONObject(event.metricsJson) }.getOrNull() else null
                            val ttft = metrics?.optDouble("timeToFirstTokenMs", Double.NaN)?.takeIf { it.isFinite() && it >= 0 && it <= Long.MAX_VALUE.toDouble() }?.toLong()
                            val tps = metrics?.optDouble("tokensPerSecond", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }
                            if (ttft != null || tps != null) measurements = QualificationMeasurements(ttft, tps)
                        }
                        else -> Unit
                    }
                    emit(Unit)
                    !completed && error == null
                }.collect()
            }
        } catch (_: TimeoutCancellationException) { error = "Native check timed out" }
        return ModelLabResponse(output.toString(), completed, error, measurements)
    }
    fun exportPreview(runId: String): String? {
        val snapshot = state.value
        val run = snapshot.modelLabRuns.firstOrNull { it.id == runId } ?: run { _status.value = "Evidence run is no longer retained"; return null }
        val evidence = snapshot.evidence.filter { "model-lab:$runId" in it.refs }
        val ids = evidence.map { it.id }.toSet()
        val record = snapshot.qualifications.lastOrNull { it.scope == run.scope && it.batteryId == run.batteryId && it.checks.any { check -> check.evidenceRefs.any { it in ids } } }
        return Json { prettyPrint = true; encodeDefaults = true }.encodeToString(ModelLabExport.serializer(), ModelLabExport(run, evidence, record, BuildConfig.VERSION_NAME))
    }
    fun shareReviewed(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                require(json.toByteArray().size <= 256 * 1024)
                ChatShareHelper.writeAndShare(app, "toolneuron-model-lab-${UUID.randomUUID()}.json", json,
                    "application/json", "ToolNeuron Model Lab evidence", "Share reviewed evidence")
            } catch (failure: Exception) { _status.value = failure.message ?: "Could not share evidence" }
        }
    }
}
