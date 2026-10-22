package com.dark.tool_neuron.ui.screens.abilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.laptop.CapabilityRegistry
import com.dark.tool_neuron.laptop.ToolRegistry
import com.dark.tool_neuron.ui.screens.desktop.Retro
import com.dark.tool_neuron.ui.screens.desktop.RetroButton
import com.dark.tool_neuron.ui.screens.desktop.RetroKeyValue
import com.dark.tool_neuron.ui.screens.desktop.RetroMono
import com.dark.tool_neuron.ui.screens.desktop.RetroPanel
import com.dark.tool_neuron.viewmodel.LaptopViewModel

@Composable
fun AbilitiesScreen(
    innerPadding: PaddingValues,
    viewModel: LaptopViewModel = hiltViewModel(),
    onContinue: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val terminalProvider by viewModel.terminalProviderState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Choose what Cipher may do. Abilities start denied; you can change them later. A grant does not install a missing provider or run an action.",
                        fontFamily = RetroMono,
                        color = Retro.TextDim,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
            items(viewModel.capabilities) { cap ->
                val ref = CapabilityRegistry.ref(cap)
                val granted = state.permissions.firstOrNull { it.capabilityId == ref }?.granted ?: false
                val effectStatus = cap.requiredEffects.mapNotNull { id ->
                    ToolRegistry.SEEDS.firstOrNull { it.id == id }?.let { "${it.id}=${it.status}" }
                }.joinToString(" ")
                RetroPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(ref, fontFamily = RetroMono, color = Retro.Emerald, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(cap.description, fontFamily = RetroMono, color = Retro.TextDim, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    RetroKeyValue("providers", cap.providers.joinToString(","))
                    RetroKeyValue("effects", effectStatus.ifBlank { "none" })
                    RetroKeyValue(
                        "permission",
                        if (granted) "GRANTED" else "DENIED (default)",
                        valueColor = if (granted) Retro.Emerald else Retro.Amber,
                    )
                    if (ref == "dev.terminal.execute@1") {
                        RetroKeyValue(
                            "runtime",
                            terminalProvider.availability.name,
                            valueColor = when (terminalProvider.availability.name) {
                                "READY" -> Retro.Emerald
                                "FAILED" -> Retro.Error
                                else -> Retro.Amber
                            },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RetroButton(
                            label = if (granted) "Revoke" else "Grant",
                            onClick = { viewModel.setPermission(ref, !granted) },
                            accent = if (granted) Retro.Amber else Retro.Emerald,
                        )
                    }
                }
            }
        }
        onContinue?.let { proceed ->
            RetroButton(label = "Continue with these permissions", onClick = proceed)
        }
    }
}
