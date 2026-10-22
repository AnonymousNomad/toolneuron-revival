package com.dark.tool_neuron.ui.screens.home_screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.viewmodel.HomeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectBindingPanel(viewModel: HomeViewModel, onOpenVibe: (String) -> Unit, onOpenCreator: (String) -> Unit) {
    val state by viewModel.projectState.collectAsStateWithLifecycle()
    val chatId by viewModel.currentChatId.collectAsStateWithLifecycle()
    val error by viewModel.projectError.collectAsStateWithLifecycle()
    val project = if (chatId == null) state.workspaces.firstOrNull { it.id == state.activeWorkspaceId }
        else state.workspaces.firstOrNull { chatId in it.conversationIds }
    var choosing by remember { mutableStateOf(false) }
    var artifacts by remember { mutableStateOf(false) }
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { choosing = true }) { Text(project?.name ?: "Choose project") }
        if (project != null) TextButton(onClick = { artifacts = true }) { Text("Project results") }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Bind conversation to project") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Binding uses the existing project. Files and project IDs stay in place.")
            TextButton(onClick = { viewModel.bindCurrentProject(null); choosing = false }) { Text("No project") }
            state.workspaces.forEach { workspace ->
                TextButton(onClick = { viewModel.bindCurrentProject(workspace.id); choosing = false }) { Text(workspace.name) }
            }
            if (state.workspaces.isEmpty()) Text("Create a project in Vibe Studio first.")
        } }, confirmButton = { TextButton(onClick = { choosing = false }) { Text("Close") } })
    if (artifacts && project != null) AlertDialog(onDismissRequest = { artifacts = false }, title = { Text("${project.name} results") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val builds = state.vibeBuilds.filter { it.workspaceId == project.id }.takeLast(10).reversed()
            project.creation?.let { plan -> Text("Creation plan: ${plan.episodes().size} episodes, ${plan.scenes().size} scenes, ${plan.shots().size} shots. Planning only; no rendered media.") }
            if (builds.isEmpty() && project.creation == null) Text("No builds yet. Open this project in Vibe Studio to edit and build.")
            builds.forEach { build ->
                Text("${build.id}\n${build.verdict} · structural checks only\n${build.digest}")
            }
        } }, confirmButton = { TextButton(onClick = {
            viewModel.bindCurrentProject(project.id)
            if (viewModel.projectError.value == null) { artifacts = false; if (project.creation != null) onOpenCreator(project.id) else onOpenVibe(project.id) }
        }) { Text(if (project.creation != null) "Open in Creator" else "Open in Vibe Studio") } }, dismissButton = { TextButton(onClick = { artifacts = false }) { Text("Close") } })
}
