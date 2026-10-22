package com.dark.tool_neuron.ui.screens.vibe

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.laptop.Verdict
import com.dark.tool_neuron.model.NavScreens
import com.dark.tool_neuron.studio.*
import com.dark.tool_neuron.ui.screens.desktop.*
import com.dark.tool_neuron.viewmodel.VibeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VibeScreen(innerPadding: PaddingValues, onNavigate: (String) -> Unit, viewModel: VibeViewModel,
    onExport: (String) -> Unit, onCapture: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val pendingProjectId by viewModel.pendingSourceProject.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val granted = state.permissions.any { it.capabilityId == VibeStudio.CAPABILITY && it.granted }
    val workspace = state.workspaces.firstOrNull { it.id == editor.workspaceId }
    val builds = state.vibeBuilds.filter { it.workspaceId == editor.workspaceId }
    var selectedBuildId by remember(editor.workspaceId) { mutableStateOf<String?>(null) }
    val latest = builds.firstOrNull { it.id == selectedBuildId } ?: builds.lastOrNull()
    var name by remember { mutableStateOf("My web project") }
    var template by remember { mutableStateOf("web") }
    var path by remember(editor.path) { mutableStateOf(editor.path) }
    var preview by remember(editor.workspaceId, latest?.id) { mutableStateOf(false) }
    val activePreviewId by rememberUpdatedState(if (preview) latest?.id else null)
    var previewView by remember(editor.workspaceId, latest?.id) { mutableStateOf<WebView?>(null) }
    var exportRecord by remember(editor.workspaceId) { mutableStateOf<VibeBuildRecord?>(null) }

    LaunchedEffect(granted) {
        if (!granted) { preview = false; exportRecord = null }
        else if (editor.workspaceId == null) state.activeWorkspaceId?.let(viewModel::open)
    }
    Surface(Modifier.fillMaxSize(), color = Retro.Background) {
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("VIBE STUDIO", color = Retro.Emerald, fontFamily = RetroMono)
                Text("Start with a template. Save source, build/check, preview, then export. Templates work without a model.", color = Retro.TextDim)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroButton("Abilities", { onNavigate(NavScreens.Abilities.route) })
                    RetroButton("Files", { onNavigate(NavScreens.Files.route) })
                    RetroButton("Terminal", { onNavigate(NavScreens.Terminal.route) })
                }
                if (!granted) Text("Grant Vibe Studio in Abilities to open or change project files.", color = Retro.Amber)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                status?.let { Text(it, color = Retro.Amber) }
            }
            if (granted) {
                val pendingProject = state.workspaces.firstOrNull { it.id == pendingProjectId }
                if (pendingProject != null) item {
                    RetroPanel(Modifier.fillMaxWidth()) {
                        Text("Initialize ${pendingProject.name}", color = Retro.Emerald)
                        Text("Choose source for this existing project. This preserves its ID, bindings and other files.", color = Retro.TextDim)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VibeTemplates.all.forEach { option -> RetroButton(option.title, { viewModel.initializeExisting(option.id) }, enabled = !busy) }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RetroButton("Retry open", { viewModel.open(pendingProject.id) }, enabled = !busy)
                            RetroButton("Cancel", viewModel::cancelInitialization, enabled = !busy, accent = Retro.Amber)
                        }
                    }
                }
                item {
                    RetroPanel(Modifier.fillMaxWidth()) {
                        OutlinedTextField(name, { name = it }, label = { Text("New project name") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VibeTemplates.all.forEach { option -> RetroButton(option.title, { template = option.id }, enabled = !busy, accent = if (template == option.id) Retro.Emerald else Retro.TextDim) }
                        }
                        RetroButton("Create project", { viewModel.create(name, template) }, enabled = !busy && name.isNotBlank())
                        Text("Recent projects", color = Retro.TextDim)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.workspaces.sortedByDescending { it.metadata["vibe.lastOpenedAt"]?.toLongOrNull() ?: it.createdAtMs }.take(8).forEach { project -> RetroButton(project.name, { viewModel.open(project.id) }, enabled = !busy, accent = if (project.id == editor.workspaceId) Retro.Emerald else Retro.TextDim) }
                        }
                    }
                }
                if (workspace != null) {
                    item {
                        Text("Editing ${workspace.name}" + if (editor.dirty) " · UNSAVED" else "", color = Retro.Emerald)
                        if (state.activeWorkspaceId != workspace.id) Text("Another project is active elsewhere. This editor and its actions stay bound to ${workspace.name}.", color = Retro.Amber)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RetroButton("Source", { preview = false }, enabled = !busy)
                            RetroButton("Save", viewModel::save, enabled = !busy && editor.dirty)
                            RetroButton("Discard edits", viewModel::discard, enabled = !busy && editor.dirty, accent = Retro.Amber)
                            RetroButton("Build + check", { preview = false; selectedBuildId = null; viewModel.build() }, enabled = !busy && !editor.dirty)
                        }
                    }
                    if (!preview) {
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                editor.files.forEach { file -> RetroButton(file, { viewModel.openFile(file) }, enabled = !busy) }
                            }
                            OutlinedTextField(path, { path = it }, label = { Text("Source path (open or create)") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            RetroButton("Open / new file", { viewModel.openFile(path) }, enabled = !busy && path.isNotBlank())
                            OutlinedTextField(editor.text, viewModel::edit, label = { Text(editor.path) }, enabled = !busy,
                                textStyle = LocalTextStyle.current.copy(fontFamily = RetroMono), modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 440.dp))
                        }
                    }
                    if (latest != null) {
                        item {
                            Text("Build history · choose a recorded snapshot", color = Retro.TextDim)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                builds.takeLast(10).reversed().forEach { record ->
                                    RetroButton("${record.id.takeLast(6)} ${record.verdict}", { selectedBuildId = record.id; preview = false },
                                        enabled = !busy, accent = if (record.id == latest.id) Retro.Emerald else Retro.TextDim)
                                }
                            }
                            if (builds.lastOrNull()?.verdict == Verdict.FAIL) {
                                builds.lastOrNull { it.verdict == Verdict.PASS }?.let { passing ->
                                    RetroButton("Use earlier passing snapshot", { selectedBuildId = passing.id; preview = false }, enabled = !busy, accent = Retro.Amber)
                                    Text("Earlier snapshots preserve their recorded files. They do not repair current source or prove browser behavior.", color = Retro.TextDim)
                                }
                            }
                        }
                        item {
                            RetroPanel(Modifier.fillMaxWidth()) {
                                Text((if (latest.id == builds.lastOrNull()?.id) "Latest recorded build" else "Historical snapshot") + " · ${latest.verdict}", color = if (latest.verdict == Verdict.PASS) Retro.Emerald else Retro.Error)
                                Text("Structural checks only. Browser behavior and Android acceptance are not inferred.", color = Retro.TextDim, style = MaterialTheme.typography.bodySmall)
                                latest.checks.forEach { check -> Text("${if (check.passed) "PASS" else "FAIL"} ${check.detail}", color = if (check.passed) Retro.TextDim else Retro.Error, style = MaterialTheme.typography.bodySmall) }
                                Text("${latest.files.size} files · ${latest.digest.take(16)}", color = Retro.TextDim)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    RetroButton("Preview snapshot", { preview = true }, enabled = !busy && latest.verdict == Verdict.PASS)
                                    RetroButton("Review export", { exportRecord = latest }, enabled = !busy && latest.verdict == Verdict.PASS)
                                }
                                Text("Preview/export use this immutable build. Save and rebuild after editing source.", color = Retro.TextDim, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (preview && latest.verdict == Verdict.PASS) {
                            item {
                                key(latest.id) {
                                    VibePreview(viewModel.studio, workspace.id, latest.id, Modifier.fillMaxWidth().height(400.dp),
                                        onView = { previewView = it },
                                        onStatus = { if (activePreviewId == latest.id) viewModel.previewMessage(workspace.id, latest.id, it) },
                                        onFatal = { if (activePreviewId == latest.id && viewModel.isCurrentPreview(workspace.id, latest.id)) preview = false })
                                }
                                RetroButton("Capture visible preview", {
                                    runCatching { captureVisiblePreview(checkNotNull(previewView)) }
                                        .onSuccess { if (viewModel.prepareCapture(it)) onCapture("toolneuron-preview-${latest.id}.png") }
                                        .onFailure { viewModel.message(it.message ?: "Capture failed") }
                                }, enabled = previewView != null && !busy)
                            }
                        }
                    }
                }
                item {
                    Text("Workstation packs", color = Retro.Emerald)
                    WorkstationPacks.all.forEach { pack -> Text("${pack.title}: ${pack.templates.joinToString()} · model batteries unqualified until run", color = Retro.TextDim, style = MaterialTheme.typography.bodySmall) }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    exportRecord?.let { record ->
        AlertDialog(onDismissRequest = { exportRecord = null }, title = { Text("Export this build?") },
            text = { Column { Text("ZIP contains source snapshot and its manifest. Nothing uploads automatically."); record.files.forEach { Text("${it.path} · ${it.bytes} bytes · ${it.sha256.take(10)}", style = MaterialTheme.typography.bodySmall) } } },
            confirmButton = { TextButton(onClick = { if (viewModel.prepareExport(record)) { exportRecord = null; onExport("toolneuron-${record.id}.zip") } }) { Text("Choose destination") } },
            dismissButton = { TextButton(onClick = { exportRecord = null }) { Text("Cancel") } })
    }
}
