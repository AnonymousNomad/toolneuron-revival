package com.dark.tool_neuron.ui.screens.companion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.companion.*
import com.dark.tool_neuron.model.NavScreens
import com.dark.tool_neuron.ui.screens.desktop.*
import com.dark.tool_neuron.viewmodel.CompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionScreen(padding: PaddingValues, navigate: (String) -> Unit, vm: CompanionViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val project = state.workspaces.firstOrNull { it.id == state.activeWorkspaceId }
    val granted = state.permissions.any { it.capabilityId == CompanionPlan.CAPABILITY && it.granted }
    var title by remember { mutableStateOf("") }
    var locator by remember { mutableStateOf("") }
    var rights by remember { mutableStateOf("") }
    var classification by remember { mutableStateOf(MediaSourceClass.UNKNOWN) }
    Surface(Modifier.fillMaxSize(), color = Retro.Background) {
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("TV / GAME COMPANION", color = Retro.Emerald, fontFamily = RetroMono)
                Text("Keep legal media references with your project. Source rights are your declaration; they do not verify a license or enable playback.", color = Retro.TextDim)
                Text("Project: ${project?.name ?: "none selected"}", color = Retro.TextPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RetroButton("Choose project", { navigate(NavScreens.Laptop.route) })
                    RetroButton("Abilities", { navigate(NavScreens.Abilities.route) })
                }
                status?.let { Text(it, color = Retro.Amber) }
                if (!granted) Text("Grant Companion planning in Abilities before saving references.", color = Retro.Amber)
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("Reference title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(locator, { locator = it.take(2048) }, label = { Text("Source locator (stored only)") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaSourceClass.entries.forEach { value -> RetroButton(value.name, { classification = value }, accent = if (classification == value) Retro.Emerald else Retro.TextDim) }
                }
                OutlinedTextField(rights, { rights = it.take(2000) }, label = { Text("Rights / license declaration") }, modifier = Modifier.fillMaxWidth())
                RetroButton("Save reference", {
                    project?.let { vm.add(it.id, title, locator, classification, rights) { title = ""; locator = ""; rights = "" } }
                }, enabled = !busy && granted && project != null && title.isNotBlank() && locator.isNotBlank())
                Text("Playback UNCONFIGURED. No DRM bypass, stream extraction or automatic network access.", color = Retro.Amber)
            }
            project?.companion?.media?.reversed()?.forEach { reference -> item(key = reference.id) {
                RetroPanel(Modifier.fillMaxWidth()) {
                    Text(reference.title, color = Retro.TextPrimary)
                    Text("${reference.classification} · ${if (reference.eligible) "rights declared" else "rights unresolved"}", color = Retro.Amber)
                    Text(reference.locator, color = Retro.TextDim)
                    Text(reference.rightsDeclaration.ifBlank { "No rights declaration" }, color = Retro.TextDim)
                }
            } }
            item {
                Text("GAME STATION", color = Retro.Emerald)
                Text("Build your own game in Vibe Studio. Companion strategy, explanation and planning intents are separate from deterministic game mechanics; models cannot drive frame timing or raw inputs.", color = Retro.TextDim)
                RetroButton("Open Vibe Studio", { navigate(NavScreens.Vibe.route) })
                Text("Game adapter UNCONFIGURED · Device bridge UNPAIRED / UNCONFIGURED. No remote commands are sent.", color = Retro.Amber)
            }
        }
    }
}
