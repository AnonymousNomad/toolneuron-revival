package com.dark.tool_neuron.ui.screens.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.laptop.resident.ResidentEngine
import com.dark.tool_neuron.model.NavScreens
import com.dark.tool_neuron.ui.icons.TnIcons
import com.dark.tool_neuron.viewmodel.DesktopViewModel
import com.dark.download_manager.formatBytes

private data class DesktopTile(val label: String, val icon: ImageVector, val route: String, val note: String? = null)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopScreen(
    innerPadding: PaddingValues,
    onNavigate: (String) -> Unit,
    viewModel: DesktopViewModel = hiltViewModel(),
) {
    val residentState by viewModel.residentState.collectAsStateWithLifecycle()
    val laptopState by viewModel.laptopState.collectAsStateWithLifecycle()
    val terminalProvider by viewModel.terminalProvider.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    val activeWorkspace = laptopState.workspaces.firstOrNull { it.id == laptopState.activeWorkspaceId }

    val tiles = listOf(
        DesktopTile("Vibe Studio", TnIcons.Code, NavScreens.Vibe.route, note = "create + build"),
        DesktopTile("Cipher", TnIcons.Sparkles, NavScreens.Cipher.route),
        DesktopTile("Chat", TnIcons.MessageCircle, NavScreens.HomeScreen.route),
        DesktopTile("Projects", TnIcons.Package, NavScreens.Laptop.route),
        DesktopTile("Terminal", TnIcons.Code, NavScreens.Terminal.route),
        DesktopTile("Models", TnIcons.Database, NavScreens.ModelStore.route),
        DesktopTile("Abilities", TnIcons.Puzzle, NavScreens.Abilities.route),
        DesktopTile("Knowledge", TnIcons.BookOpen, NavScreens.Knowledge.route),
        DesktopTile("Workflows", TnIcons.Fork, NavScreens.Knowledge.route, note = "procedures"),
        DesktopTile("Files", TnIcons.FileText, NavScreens.Files.route),
        DesktopTile("Evidence", TnIcons.Eye, NavScreens.Evidence.route),
        DesktopTile("Settings", TnIcons.Settings, NavScreens.Settings.route),
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Retro.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RetroHeader(title = "ToolNeuron // Cipher Workstation", right = clock)

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text("START CENTER", fontFamily = RetroMono, color = Retro.Emerald)
                Text(
                    if (activeWorkspace == null) "Create a Vibe project from a template, or choose a model to chat. Your files and terminal share the same project."
                    else "Resume ${activeWorkspace.name}, or open an app below. All project surfaces use the same workspace.",
                    fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RetroButton("Create in Vibe", onClick = { onNavigate(NavScreens.Vibe.route) })
                    RetroButton("Models", onClick = { onNavigate(NavScreens.ModelStore.route) })
                    RetroButton("Projects", onClick = { onNavigate(NavScreens.Laptop.route) })
                    if (activeWorkspace != null) RetroButton("Files", onClick = { onNavigate(NavScreens.Files.route) })
                }
                laptopState.workstation.windows.lastOrNull()?.let { recent ->
                    RetroButton("Resume ${recent.app.title}", onClick = { onNavigate(WorkstationRoutes.route(recent.app)) })
                }
            }

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RetroKeyValue(
                            "cipher",
                            when (val s = residentState) {
                                is ResidentEngine.ResidentUiState.Active -> "ONLINE · ${s.identity.modelName}"
                                is ResidentEngine.ResidentUiState.Starting -> "STARTING"
                                is ResidentEngine.ResidentUiState.Unavailable -> "UNAVAILABLE"
                                else -> "IDLE"
                            },
                            valueColor = if (residentState is ResidentEngine.ResidentUiState.Active) Retro.Emerald else Retro.Amber,
                        )
                        RetroKeyValue("laptop", activeWorkspace?.name ?: "no active project")
                        RetroKeyValue(
                            "terminal",
                            terminalProvider.availability.name,
                            valueColor = when (terminalProvider.availability.name) {
                                "READY" -> Retro.Emerald
                                "FAILED" -> Retro.Error
                                else -> Retro.Amber
                            },
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RetroKeyValue("mode", "LOCAL · OFFLINE")
                        RetroKeyValue("opened", "${laptopState.session.openCount}x")
                        RetroKeyValue("tasks", "${laptopState.tasks.size}")
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(tiles) { tile ->
                    RetroIconTile(
                        icon = tile.icon,
                        label = tile.label,
                        note = tile.note,
                        onClick = { onNavigate(tile.route) },
                    )
                }
            }

            Surface(color = Retro.Panel, shape = androidx.compose.ui.graphics.RectangleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Retro.Border)) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = telemetry.availableMemoryBytes?.let { "RAM free ${formatBytes(it)}" } ?: "RAM --",
                        fontFamily = RetroMono,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Retro.Emerald,
                    )
                    Text(
                        text = telemetry.batteryPercent?.let { "BATTERY $it%" } ?: "BATTERY --",
                        fontFamily = RetroMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = Retro.TextDim,
                    )
                    Text(
                        text = "THERMAL " + when (telemetry.thermalStatus) {
                            0 -> "NORMAL"
                            1 -> "LIGHT"
                            2 -> "MODERATE"
                            3 -> "SEVERE"
                            4 -> "CRITICAL"
                            5 -> "EMERGENCY"
                            6 -> "SHUTDOWN"
                            else -> "--"
                        },
                        fontFamily = RetroMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = Retro.Emerald,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
