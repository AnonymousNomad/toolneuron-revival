package com.dark.tool_neuron.ui.screens.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.download_manager.formatBytes
import com.dark.tool_neuron.ui.screens.desktop.Retro
import com.dark.tool_neuron.ui.screens.desktop.RetroMono
import com.dark.tool_neuron.ui.screens.desktop.RetroPanel
import com.dark.tool_neuron.viewmodel.LaptopViewModel

@Composable
fun FilesScreen(
    innerPadding: PaddingValues,
    viewModel: LaptopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val files by viewModel.workspaceFiles.collectAsStateWithLifecycle()
    val active = state.workspaces.firstOrNull { it.id == state.activeWorkspaceId }
    var preview by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 12.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "FILES — ${active?.name ?: "no active project"}",
                        fontFamily = RetroMono,
                        color = Retro.Emerald,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (files.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("(empty workspace)", fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(files) { f ->
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        (if (f.isDir) "[DIR] " else "      ") + f.path + if (f.isDir) "" else "  ${formatBytes(f.bytes)}",
                        fontFamily = RetroMono,
                        color = if (f.isDir) Retro.Amber else Retro.Emerald,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (!f.isDir && f.bytes in 1..200_000) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = {
                            active?.let { ws ->
                                viewModel.readWorkspaceFile(ws.id, f.path)?.let { content ->
                                    preview = f.path to content.take(4000)
                                }
                            }
                        }) { Text("VIEW", fontFamily = RetroMono) }
                    }
                }
            }
        }
    }

    preview?.let { (path, content) ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(path, fontFamily = RetroMono, style = MaterialTheme.typography.labelMedium) },
            text = {
                Text(content, fontFamily = RetroMono, style = MaterialTheme.typography.labelSmall)
            },
            confirmButton = { TextButton(onClick = { preview = null }) { Text("CLOSE") } },
        )
    }
}
