package com.dark.tool_neuron.ui.screens.evidence

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.laptop.Verdict
import com.dark.tool_neuron.ui.screens.desktop.Retro
import com.dark.tool_neuron.ui.screens.desktop.RetroMono
import com.dark.tool_neuron.ui.screens.desktop.RetroPanel
import com.dark.tool_neuron.viewmodel.LaptopViewModel

@Composable
fun EvidenceScreen(
    innerPadding: PaddingValues,
    viewModel: LaptopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                        "EVIDENCE — recorded results and verdicts",
                        fontFamily = RetroMono,
                        color = Retro.TextDim,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            items(state.verifications.reversed()) { v ->
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "[${v.verdict}] ${v.requestedAction.take(160)}",
                        fontFamily = RetroMono,
                        color = when (v.verdict) {
                            Verdict.PASS -> Retro.Emerald
                            Verdict.FAIL -> Retro.Error
                            Verdict.PENDING -> Retro.Amber
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("method: ${v.verificationMethod}", fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
                }
            }
            items(state.evidence.reversed().take(30)) { e ->
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "[${e.kind}] ${e.summary.take(220)}",
                        fontFamily = RetroMono,
                        color = Retro.Emerald,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            item {
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "CHECKPOINTS — ${state.checkpoints.size}",
                        fontFamily = RetroMono,
                        color = Retro.TextDim,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    state.checkpoints.reversed().take(10).forEach { c ->
                        Text("• ${c.label} (task ${c.taskId}, step ${c.step})", fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
