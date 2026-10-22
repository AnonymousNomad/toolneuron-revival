package com.dark.tool_neuron.ui.screens.model_store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.laptop.*
import com.dark.tool_neuron.model.NavScreens
import com.dark.tool_neuron.ui.screens.desktop.*
import com.dark.tool_neuron.viewmodel.ModelLabViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelLabScreen(innerPadding: PaddingValues, onNavigate: (String) -> Unit, viewModel: ModelLabViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val assessment by viewModel.assessment.collectAsStateWithLifecycle()
    val assignment = state.cipherAssignment
    val granted = state.permissions.any { it.capabilityId == "model.worker.delegate@1" && it.granted }
    var confirmRun by remember { mutableStateOf(false) }
    var reviewedExport by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(assignment, state.qualifications) { viewModel.refresh() }
    Surface(Modifier.fillMaxSize(), color = Retro.Background) {
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("MODEL LAB", color = Retro.Emerald, fontFamily = RetroMono)
                Text("Run local text smoke batteries and inspect actual responses. Installation, runtime compatibility, current device fit, task admission and measured qualification are separate facts.", color = Retro.TextDim)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroButton("Cipher assignment", { onNavigate(NavScreens.Cipher.route) }, enabled = !busy)
                    RetroButton("Abilities", { onNavigate(NavScreens.Abilities.route) })
                    RetroButton("Models", { onNavigate(NavScreens.ModelStore.route) })
                }
                if (!granted) Text("Grant local model invocation in Abilities to run checks. Reading/exporting prior evidence stays available.", color = Retro.Amber)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                status?.let { Text(it, color = Retro.Amber) }
            }
            item {
                RetroPanel(Modifier.fillMaxWidth()) {
                    Text(assignment?.modelId ?: "EMPTY — no assigned model", color = Retro.TextPrimary)
                    assignment?.let {
                        Text("${it.runtimeId} · ${it.contextSize} context\nArtifact SHA-256: ${it.artifactSha256}", color = Retro.TextDim)
                        Text("Runtime revision: ${CipherRuntimeIdentity.revision(it.runtimeId, it.contextSize)}", color = Retro.TextDim)
                    }
                    assessment?.let { Text("${it.status}: ${it.reason}", color = Retro.Amber) }
                    Text("TTFT and token rate appear only when the native runtime reports them. RSS is UNKNOWN; native peakMemoryMB is not relabeled as RSS.", color = Retro.TextDim)
                    if (!busy) RetroButton("Run local smoke batteries", { confirmRun = true }, enabled = granted && assignment != null && state.resident.activeSessionId == null)
                    else RetroButton("Stop checks", viewModel::cancel, accent = Retro.Amber)
                    if (state.resident.activeSessionId != null) Text("Stop Cipher before running checks.", color = Retro.Amber)
                }
            }
            EdgeHarnessBatteries.all.forEach { definition ->
                item {
                    val scope = assignment?.let { CipherRuntimeIdentity.scope(it) }
                    val record = state.qualifications.lastOrNull { it.scope == scope && it.batteryId == definition.battery.id }
                    Text("${definition.title}: ${if (scope == null) QualificationVerdict.UNKNOWN else record?.verdictFor(scope, definition.battery) ?: QualificationVerdict.UNKNOWN}", color = Retro.TextPrimary)
                }
            }
            if (state.modelLabRuns.isEmpty()) item { Text("No model has been tested here yet. Templates in Vibe work without model qualification.", color = Retro.TextDim) }
            state.modelLabRuns.takeLast(20).reversed().forEach { run ->
                item {
                    RetroPanel(Modifier.fillMaxWidth()) {
                        Text("${run.batteryId} · ${run.id}", color = Retro.TextPrimary)
                        Text(if (run.finishedAtMs == null) "UNKNOWN — unfinished run; no completed receipt" else "Recorded observations: ${run.observations.joinToString { it.verdict.name }}", color = Retro.Amber)
                        Text("Device: ${run.scope.deviceBuild}\nArtifact: ${run.scope.modelSha256}", color = Retro.TextDim)
                        run.observations.forEach { observed ->
                            Text("${observed.checkId}: ${observed.verdict}\nPrompt: ${observed.prompt}\nObserved: ${observed.output.ifEmpty { "(empty)" }}${observed.error?.let { "\nError: $it" } ?: ""}", color = Retro.TextDim)
                            observed.measurements?.let { Text("Native TTFT: ${it.ttftMs?.let { value -> "$value ms" } ?: "UNKNOWN"} · native tokens/s: ${it.tokensPerSecond ?: "UNKNOWN"} · RSS: UNKNOWN", color = Retro.TextDim) }
                        }
                        RetroButton("Review evidence export", { reviewedExport = viewModel.exportPreview(run.id) })
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    if (confirmRun) AlertDialog(onDismissRequest = { confirmRun = false }, title = { Text("Run on this device?") },
        text = { Text("This replaces the currently loaded chat model with the explicit Cipher assignment and runs bounded local prompts. Unfinished batteries are saved as UNKNOWN before loading. No cloud provider is called. Chat can be reloaded afterwards.") },
        confirmButton = { TextButton(onClick = { confirmRun = false; viewModel.runAll() }) { Text("Run checks") } },
        dismissButton = { TextButton(onClick = { confirmRun = false }) { Text("Cancel") } })
    reviewedExport?.let { json -> AlertDialog(onDismissRequest = { reviewedExport = null }, title = { Text("Review evidence bundle") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text("This exact snapshot includes prompts, observed output, identity, verdicts and reported measurements. Sharing is explicit."); Text(json) } },
        confirmButton = { TextButton(onClick = { reviewedExport = null; viewModel.shareReviewed(json) }) { Text("Share reviewed bundle") } },
        dismissButton = { TextButton(onClick = { reviewedExport = null }) { Text("Cancel") } }) }
}
