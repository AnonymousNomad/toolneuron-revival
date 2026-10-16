package com.dark.tool_neuron.ui.screens.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

private data class DesktopTile(val label: String, val icon: ImageVector, val route: String, val note: String? = null)

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

    val activeWorkspace = laptopState.workspaces.firstOrNull { it.id == laptopState.activeWorkspaceId }

    val tiles = listOf(
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CIPHER",
                        fontFamily = RetroMono,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Retro.Emerald,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "LAPTOP ${if (laptopState.session.openCount > 0) "OK" else "--"}",
                        fontFamily = RetroMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = Retro.TextDim,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = clock,
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
