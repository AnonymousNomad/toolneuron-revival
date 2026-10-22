package com.dark.tool_neuron.ui.screens.cipher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.dark.tool_neuron.laptop.CipherSlotStatus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.download_manager.formatBytes
import com.dark.tool_neuron.laptop.resident.ResidentEngine
import com.dark.tool_neuron.laptop.resident.ResidentEvent
import com.dark.tool_neuron.laptop.resident.ResidentTurnStatus
import com.dark.tool_neuron.ui.icons.TnIcons
import com.dark.tool_neuron.ui.theme.LocalDimens
import com.dark.tool_neuron.viewmodel.CipherViewModel

@Composable
fun CipherScreen(
    innerPadding: PaddingValues,
    viewModel: CipherViewModel = hiltViewModel(),
    onOpenModelLab: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val streamingText by viewModel.streaming.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val slot by viewModel.slot.collectAsStateWithLifecycle()
    val laptop by viewModel.laptopState.collectAsStateWithLifecycle()
    val installed by viewModel.installedModels.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var assigning by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var runtime by remember { mutableStateOf("ggml") }
    LaunchedEffect(laptop.cipherAssignment, laptop.qualifications, installed) { viewModel.refreshSlot() }
    val dimens = LocalDimens.current

    var input by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showInfo by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(4096) }
    val active = state is ResidentEngine.ResidentUiState.Active

    if (assigning) AlertDialog(onDismissRequest = { assigning = false }, title = { Text("Cipher assignment") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Select a model, runtime and context chassis. Assignment does not qualify a model.")
            viewModel.admittedModels().forEach { model -> TextButton(onClick = { selectedModel = model.id }) {
                Text((if (selectedModel == model.id) "● " else "○ ") + model.name)
            } }
            if (viewModel.admittedModels().isEmpty()) Text("Import a generation GGUF in Models first.")
            Row { TextButton(onClick = { profile = 4096 }) { Text(if (profile == 4096) "● 4K" else "4K") }
                TextButton(onClick = { profile = 8192 }) { Text(if (profile == 8192) "● 8K" else "8K") } }
            TextButton(onClick = { runtime = "ggml" }) { Text((if (runtime == "ggml") "● " else "") + "Existing GGML") }
            TextButton(onClick = { runtime = "covert-r0" }) { Text((if (runtime == "covert-r0") "● " else "") + "Covert R0 — native source unavailable") }
        } }, confirmButton = { TextButton(onClick = { selectedModel?.let { viewModel.assign(it, runtime, profile) }; assigning = false }, enabled = selectedModel != null && !busy) { Text("Save assignment") } },
        dismissButton = { TextButton(onClick = { assigning = false }) { Text("Cancel") } })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(dimens.cardPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Cipher · local/offline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showInfo = true }) { Text("Info") }
                }
                TextButton(onClick = onOpenModelLab) { Text("Model Lab / qualification") }
                Text("Slot: ${slot.status} · ${slot.reason}", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!active) Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                    TextButton(onClick = { assigning = true }, enabled = !busy) { Text("Assign model/chassis") }
                    TextButton(onClick = { viewModel.clearAssignment() }, enabled = !busy && laptop.cipherAssignment != null) { Text("Clear") }
                }
                when (val s = state) {
                    is ResidentEngine.ResidentUiState.Idle, is ResidentEngine.ResidentUiState.Stopped -> {
                        Text(
                            "Not running. Assign a durable model and qualify it in Model Lab. Cipher can read the Laptop; it cannot execute actions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { viewModel.start() }, enabled = !busy && slot.status == CipherSlotStatus.READY) { Text("Start Cipher") }
                    }
                    is ResidentEngine.ResidentUiState.Starting -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                            CircularProgressIndicator(Modifier.size(dimens.iconMd))
                            Text("Starting…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is ResidentEngine.ResidentUiState.Active -> {
                        Text(
                            "Resident: ${s.identity.modelName} · ${s.identity.contextProfile}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Laptop: ${s.identity.laptopContract} · session ${s.sessionId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { viewModel.stop() }, enabled = !busy) { Text("Stop Cipher") }
                    }
                    is ResidentEngine.ResidentUiState.Unavailable -> {
                        Text(
                            "Resident unavailable: ${s.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedButton(onClick = { viewModel.start() }, enabled = !busy) { Text("Retry") }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (msg.role == "user") {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(modifier = Modifier.padding(dimens.spacingSm)) {
                        Text(
                            if (msg.role == "user") "You" else if (msg.role == "system") "System" else "Cipher",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(msg.text, style = MaterialTheme.typography.bodySmall)
                        msg.events.forEach { ev -> EventCard(ev) }
                        msg.intent?.let { env ->
                            val isOpen = index in expanded
                            TextButton(onClick = {
                                expanded = if (isOpen) expanded - index else expanded + index
                            }) { Text(if (isOpen) "Hide intent" else "Show intent (${msg.status})") }
                            if (isOpen) {
                                IntentDebugCard(env, msg.status, msg.lookups, msg.approxTokens)
                            }
                        }
                    }
                }
            }
            if (busy && streamingText.isNotEmpty()) {
                item(key = "streaming") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(modifier = Modifier.padding(dimens.spacingSm)) {
                            Text(
                                "Cipher is working…",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                streamingText.takeLast(600),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (active) "Ask Cipher…" else "Start Cipher to chat") },
                enabled = active && !busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank() && active && !busy) {
                        viewModel.send(input)
                        input = ""
                    }
                }),
            )
            if (busy) {
                TextButton(onClick = { viewModel.cancel() }) { Text("Stop") }
            } else {
                TextButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.send(input)
                            input = ""
                        }
                    },
                    enabled = active && input.isNotBlank(),
                ) { Text("Send") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs),
        ) {
            IconButton(onClick = {}, enabled = false) {
                Icon(TnIcons.Photo, contentDescription = "Attach (future capability)")
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(TnIcons.Mic, contentDescription = "Voice (future capability)")
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(TnIcons.Eye, contentDescription = "Camera/media reference (future capability)")
            }
            Spacer(Modifier.weight(1f))
            Text(
                "text-only in this slice",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(dimens.spacingSm))
    }

    if (showInfo) {
        ResidentInfoDialog(state = state, onDismiss = { showInfo = false })
    }
}

@Composable
private fun EventCard(event: ResidentEvent) {
    val dimens = LocalDimens.current
    val (label, body) = when (event) {
        is ResidentEvent.Lookup -> "LOOKUP" to "${event.source} · ${if (event.trust.name == "PROJECT_DATA") "project data" else "system"}${if (event.truncated) " · truncated" else ""}\n${event.line}"
        is ResidentEvent.Capability -> "CAPABILITY" to event.refs.joinToString(", ")
        is ResidentEvent.MissingInfo -> "MISSING INFO" to event.items.joinToString("; ")
        is ResidentEvent.Worker -> "WORKER" to event.items.joinToString("; ")
        is ResidentEvent.ExecutionRefused -> "PERMISSION" to "execution requested — refused in this phase"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spacingXs),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(dimens.spacingSm)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResidentInfoDialog(
    state: ResidentEngine.ResidentUiState,
    onDismiss: () -> Unit,
) {
    val identity = (state as? ResidentEngine.ResidentUiState.Active)?.identity
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resident status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LocalDimens.current.spacingXs)) {
                if (identity == null) {
                    Text("Resident not active.", style = MaterialTheme.typography.bodySmall)
                } else {
                    @Composable
                    fun row(label: String, value: String) {
                        Text("$label: $value", style = MaterialTheme.typography.bodySmall)
                    }
                    row("format", identity.format)
                    row("role", identity.role)
                    row("model", identity.modelName)
                    row("artifact", "${identity.artifactRef} (${formatBytes(identity.artifactBytes)})")
                    row("arch", identity.architecture ?: "unknown")
                    row("sha256", identity.artifactSha256 ?: "(unavailable: ${identity.hashUnavailableReason})")
                    row("runtime", identity.runtime)
                    row("admission", identity.admission.joinToString(",").ifEmpty { "none" })
                    row("context profile", identity.contextProfile)
                    row("selection", identity.selectionReason)
                    row("template", identity.contextTemplateIdentity)
                    row("capability rev", identity.capabilityRevision)
                    row("laptop contract", identity.laptopContract)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun IntentDebugCard(
    env: com.dark.tool_neuron.laptop.resident.IntentEnvelope,
    status: ResidentTurnStatus?,
    lookups: List<String>,
    approxTokens: Int,
) {
    val dimens = LocalDimens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(dimens.spacingSm)) {
            @Composable
            fun line(label: String, value: String) {
                if (value.isNotBlank()) {
                    Text("$label: $value", style = MaterialTheme.typography.bodySmall)
                }
            }
            line("format", env.format)
            line("status", status?.name ?: "?")
            line("objective", env.user_objective)
            line("goal", env.interpreted_goal)
            line("project", env.active_project)
            line("capability_status", env.capability_status)
            line("capabilities", env.candidate_capabilities.joinToString(", "))
            line("context_refs", env.required_context_refs.joinToString(", "))
            line("missing", env.missing_information.joinToString("; "))
            line("lookups_run", lookups.joinToString(", "))
            line("worker_reqs", env.worker_requirements.joinToString(", "))
            line("next_step", env.proposed_next_step)
            line("execution_requested", env.execution_requested.toString())
            line("prompt_tokens_approx", approxTokens.toString())
        }
    }
}
