package com.dark.tool_neuron.ui.screens.knowledge

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dark.tool_neuron.laptop.KnowledgeRegistry
import com.dark.tool_neuron.laptop.ProcedureRegistry
import com.dark.tool_neuron.ui.screens.desktop.Retro
import com.dark.tool_neuron.ui.screens.desktop.RetroMono
import com.dark.tool_neuron.ui.screens.desktop.RetroPanel

@Composable
fun KnowledgeScreen(innerPadding: PaddingValues) {
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
                        "KNOWLEDGE — skills, SOPs, guidance available to Cipher",
                        fontFamily = RetroMono,
                        color = Retro.TextDim,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            items(KnowledgeRegistry.SEEDS) { k ->
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(k.id, fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(k.title, fontFamily = RetroMono, color = Retro.Amber, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(k.text, fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
                }
            }
            item {
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "WORKFLOWS — deterministic procedures",
                        fontFamily = RetroMono,
                        color = Retro.TextDim,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            items(ProcedureRegistry.SEEDS) { p ->
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(p.id, fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(p.title, fontFamily = RetroMono, color = Retro.Amber, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    p.steps.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
