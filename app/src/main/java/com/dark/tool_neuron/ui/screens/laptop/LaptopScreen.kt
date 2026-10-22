package com.dark.tool_neuron.ui.screens.laptop

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.download_manager.formatBytes
import com.dark.tool_neuron.laptop.CapabilityRegistry
import com.dark.tool_neuron.laptop.EvidenceRecord
import com.dark.tool_neuron.laptop.TaskRecord
import com.dark.tool_neuron.laptop.TaskStatus
import com.dark.tool_neuron.laptop.VerificationRecord
import com.dark.tool_neuron.ui.theme.LocalDimens
import com.dark.tool_neuron.viewmodel.LaptopViewModel

@Composable
fun LaptopScreen(
    innerPadding: PaddingValues,
    onOpenTerminal: () -> Unit = {},
    onStartCipher: () -> Unit = {},
    viewModel: LaptopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dossiers by viewModel.dossiers.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val workspaceFiles by viewModel.workspaceFiles.collectAsStateWithLifecycle()
    val terminalProvider by viewModel.terminalProviderState.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current

    var showNewWorkspace by remember { mutableStateOf(false) }
    var showNewTask by remember { mutableStateOf(false) }
    var noteForTask by remember { mutableStateOf<String?>(null) }
    var checkpointForTask by remember { mutableStateOf<String?>(null) }
    var effectForTask by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = dimens.spacingMd, vertical = dimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
        ) {
            status?.let { s ->
                item(key = "status") { SectionCard(title = "Status") { CaptionText(s) } }
            }

            item(key = "session") {
                SectionCard(title = "Session") {
                    val session = state.session
                    CaptionText("format: ${state.format}")
                    CaptionText("opened ${session.openCount} time(s); last open ${relative(session.lastOpenMs)}")
                    if (session.recoveredFromCorruption) {
                        Text(
                            text = "Recovered from corrupt state: ${session.recoveryReason ?: "unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item(key = "cipher") {
                SectionCard(title = "Cipher Resident") {
                    val resident = state.resident
                    val active = resident.sessions.firstOrNull { it.id == resident.activeSessionId }
                    if (active != null) {
                        CaptionText("active session ${active.id} · ${active.identity.modelName} · turns ${active.turns} · lookups ${active.lookupCount}")
                    } else {
                        val last = resident.sessions.lastOrNull()
                        if (last != null) {
                            CaptionText("last session ${last.id} [${last.status}] · ${last.identity.modelName} · turns ${last.turns}")
                        } else {
                            CaptionText("no resident sessions yet")
                        }
                    }
                    Spacer(Modifier.height(dimens.spacingXs))
                    OutlinedButton(onClick = onStartCipher) { Text("Open Cipher") }
                }
            }

            item(key = "workspace") {
                SectionCard(title = "Workspace") {
                    val active = state.workspaces.firstOrNull { it.id == state.activeWorkspaceId }
                    if (active == null) {
                        CaptionText("No active workspace")
                    } else {
                        CaptionText("active: ${active.name} (${active.id})")
                        if (workspaceFiles.isEmpty()) CaptionText("(empty)")
                        workspaceFiles.take(40).forEach { f ->
                            CaptionText("${if (f.isDir) "[dir] " else "      "}${f.path}${if (f.isDir) "" else "  ${formatBytes(f.bytes)}"}")
                        }
                        if (workspaceFiles.size > 40) CaptionText("… ${workspaceFiles.size - 40} more entries")
                    }
                    if (state.workspaces.size > 1) {
                        Spacer(Modifier.height(dimens.spacingXs))
                        state.workspaces.forEach { ws ->
                            if (ws.id != state.activeWorkspaceId) {
                                TextButton(onClick = { viewModel.openWorkspace(ws.id) }) { Text("Open ${ws.name}") }
                            }
                        }
                    }
                    Spacer(Modifier.height(dimens.spacingXs))
                    OutlinedButton(onClick = { showNewWorkspace = true }) { Text("New workspace") }
                }
            }

            item(key = "tasks") {
                SectionCard(title = "Tasks (${state.tasks.size})") {
                    OutlinedButton(onClick = { showNewTask = true }) { Text("New task") }
                    state.tasks.takeLast(10).reversed().forEach { task ->
                        TaskRow(
                            task = task,
                            onAdvance = { viewModel.advanceTask(task.id) },
                            onFail = { viewModel.failTask(task.id) },
                            onCancel = { viewModel.cancelTask(task.id) },
                            onNote = { noteForTask = task.id },
                            onCheckpoint = { checkpointForTask = task.id },
                            onTerminalEffect = { effectForTask = task.id },
                        )
                    }
                }
            }

            item(key = "capabilities") {
                SectionCard(title = "Capabilities") {
                    viewModel.capabilities.forEach { cap ->
                        val ref = CapabilityRegistry.ref(cap)
                        val granted = state.permissions.firstOrNull { it.capabilityId == ref }?.granted ?: false
                        Column(modifier = Modifier.padding(vertical = dimens.spacingXs)) {
                            Text(ref, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            CaptionText(cap.description)
                            CaptionText("effects: ${cap.requiredEffects.joinToString(", ").ifEmpty { "none" }} · providers: ${cap.providers.joinToString(", ")}")
                            CaptionText("permission: ${if (granted) "GRANTED" else "denied (default)"}")
                            TextButton(onClick = { viewModel.setPermission(ref, !granted) }) {
                                Text(if (granted) "Revoke" else "Grant")
                            }
                        }
                    }
                }
            }

            item(key = "models") {
                SectionCard(title = "Models (${dossiers.size})") {
                    if (dossiers.isEmpty()) CaptionText("(none installed)")
                    dossiers.forEach { d ->
                        Column(modifier = Modifier.padding(vertical = dimens.spacingXs)) {
                            Text(d.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            CaptionText("arch: ${d.architecture ?: "unknown"} · artifact: ${d.artifactFileName} (${formatBytes(d.artifactBytes)})")
                            CaptionText("roles: ${d.roles.joinToString(", ").ifEmpty { "none" }} · admissions: ${d.admissions.joinToString(", ").ifEmpty { "none" }}")
                            CaptionText("qualification: ${d.qualificationVerdict} — no device battery evidence recorded")
                            d.ragStatus?.let { CaptionText("RAG runtime: $it") }
                        }
                    }
                }
            }

            item(key = "tools") {
                SectionCard(title = "Tools / Effects") {
                    viewModel.tools.forEach { t ->
                        CaptionText("${t.id} [${t.kind}] — ${t.status}${if (t.note.isNotBlank()) " · ${t.note}" else ""}")
                    }
                    Spacer(Modifier.height(dimens.spacingXs))
                    OutlinedButton(onClick = { viewModel.probeTerminalProvider() }) { Text("Probe terminal provider") }
                    Spacer(Modifier.height(dimens.spacingXs))
                    OutlinedButton(onClick = onOpenTerminal) { Text("Open terminal") }
                }
            }

            item(key = "connections") {
                SectionCard(title = "Connections") {
                    CaptionText("Local-first. Available providers still require an explicit capability grant.")
                    viewModel.tools.forEach { tool ->
                        val observed = if (tool.id == "terminal.execute") terminalProvider.availability.name else tool.status.name
                        CaptionText("${tool.title}: $observed")
                    }
                    CaptionText("No cloud provider fallback. Model download services and the optional HTTP server are configured separately in Settings.")
                }
            }

            item(key = "terminal") {
                SectionCard(title = "Terminal") {
                    CaptionText("provider: ${terminalProvider.id} v${terminalProvider.version} — ${terminalProvider.availability}")
                    terminalProvider.failureReason?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    CaptionText("features: ${terminalProvider.supportedFeatures.joinToString(", ")}")
                    terminalProvider.limitations.forEach { CaptionText("limit: $it") }
                }
            }

            item(key = "evidence") {
                SectionCard(title = "Evidence (${state.evidence.size}) · Verifications (${state.verifications.size}) · Checkpoints (${state.checkpoints.size})") {
                    state.verifications.takeLast(5).reversed().forEach { v: VerificationRecord ->
                        CaptionText("[${v.verdict}] ${v.requestedAction} — expected: ${v.expectedResult}")
                    }
                    state.evidence.takeLast(8).reversed().forEach { e: EvidenceRecord ->
                        CaptionText("[${e.kind}] ${e.summary.take(160)}")
                    }
                    state.checkpoints.takeLast(5).reversed().forEach { c ->
                        CaptionText("[cp] ${c.label} (task ${c.taskId}, step ${c.step})")
                    }
                }
            }
        }
    }

    if (showNewWorkspace) {
        TextInputDialog(
            title = "New workspace",
            label = "Name",
            onDismiss = { showNewWorkspace = false },
            onConfirm = { name ->
                viewModel.createWorkspace(name)
                showNewWorkspace = false
            },
        )
    }
    if (showNewTask) {
        NewTaskDialog(
            capabilities = viewModel.capabilities,
            onDismiss = { showNewTask = false },
            onConfirm = { objective, capabilityRef ->
                viewModel.createTask(objective, capabilityRef)
                showNewTask = false
            },
        )
    }
    noteForTask?.let { taskId ->
        TextInputDialog(
            title = "Add note evidence",
            label = "Note",
            onDismiss = { noteForTask = null },
            onConfirm = { text ->
                viewModel.addNote(taskId, text)
                noteForTask = null
            },
        )
    }
    checkpointForTask?.let { taskId ->
        TextInputDialog(
            title = "Add checkpoint",
            label = "Label",
            onDismiss = { checkpointForTask = null },
            onConfirm = { label ->
                viewModel.addCheckpoint(taskId, label)
                checkpointForTask = null
            },
        )
    }
    effectForTask?.let { taskId ->
        TerminalEffectDialog(
            onDismiss = { effectForTask = null },
            onRun = { command, expected ->
                viewModel.runTerminalEffect(taskId, command, expected)
                effectForTask = null
            },
        )
    }
}

@Composable
private fun TerminalEffectDialog(
    onDismiss: () -> Unit,
    onRun: (String, String) -> Unit,
) {
    var command by remember { mutableStateOf("echo TOOLNEURON_TERMINAL_OK") }
    var expected by remember { mutableStateOf("TOOLNEURON_TERMINAL_OK") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminal effect (governed)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LocalDimens.current.spacingXs)) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Command (sh -c)") },
                )
                OutlinedTextField(
                    value = expected,
                    onValueChange = { expected = it },
                    label = { Text("Expected stdout (deterministic check)") },
                    singleLine = true,
                )
                Text(
                    "Requires permission dev.terminal.execute@1 (grant in Capabilities).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (command.isNotBlank()) onRun(command, expected) },
                enabled = command.isNotBlank(),
            ) { Text("Run") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TaskRow(
    task: TaskRecord,
    onAdvance: () -> Unit,
    onFail: () -> Unit,
    onCancel: () -> Unit,
    onNote: () -> Unit,
    onCheckpoint: () -> Unit,
    onTerminalEffect: () -> Unit,
) {
    val dimens = LocalDimens.current
    val terminal = task.status == TaskStatus.SUCCEEDED || task.status == TaskStatus.FAILED || task.status == TaskStatus.CANCELLED
    Column(modifier = Modifier.padding(vertical = dimens.spacingXs)) {
        Text(
            "${task.id} · ${task.status}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        CaptionText(task.userObjective.take(200))
        task.capabilityId?.let { CaptionText("capability: $it") }
        if (task.checkpointIds.isNotEmpty() || task.evidenceRefs.isNotEmpty()) {
            CaptionText("checkpoints: ${task.checkpointIds.size} · evidence: ${task.evidenceRefs.size}")
        }
        if (!terminal) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                TextButton(onClick = onAdvance) { Text("Advance") }
                TextButton(onClick = onFail) { Text("Fail") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
            TextButton(onClick = onNote) { Text("+Note") }
            TextButton(onClick = onCheckpoint) { Text("+Checkpoint") }
            TextButton(onClick = onTerminalEffect) { Text("Terminal effect") }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val dimens = LocalDimens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(dimens.spacingXs))
            content()
        }
    }
}

@Composable
private fun CaptionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun relative(ms: Long): String =
    if (ms <= 0) "never" else DateUtils.getRelativeTimeSpanString(ms, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (value.isNotBlank()) onConfirm(value) },
                enabled = value.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NewTaskDialog(
    capabilities: List<CapabilityRegistry.CapabilityDescriptor>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var objective by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LocalDimens.current.spacingXs)) {
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    label = { Text("Objective") },
                )
                CaptionText("capability (optional)")
                capabilities.forEach { cap ->
                    val ref = CapabilityRegistry.ref(cap)
                    TextButton(onClick = { selected = if (selected == ref) null else ref }) {
                        Text((if (selected == ref) "● " else "○ ") + ref)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (objective.isNotBlank()) onConfirm(objective, selected) },
                enabled = objective.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
