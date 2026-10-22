package com.dark.tool_neuron.ui.screens.desktop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.dark.tool_neuron.laptop.WorkstationApp
import com.dark.tool_neuron.laptop.WorkstationSession
import com.dark.tool_neuron.ui.icons.TnIcons

@Composable
fun WorkstationTaskbar(
    session: WorkstationSession,
    current: WorkstationApp?,
    status: String?,
    onOpen: (WorkstationApp) -> Unit,
    onDesktop: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = Retro.Panel) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            status?.let { Text(it, Modifier.padding(horizontal = 8.dp), color = Retro.Amber, style = MaterialTheme.typography.labelSmall) }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RetroButton("Start", onClick = onDesktop)
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    session.windows.forEach { window ->
                        RetroButton(
                            label = window.app.title + if (window.minimized) " (minimized)" else "",
                            onClick = { onOpen(window.app) },
                            accent = if (current == window.app) Retro.Emerald else Retro.TextDim,
                        )
                    }
                }
                IconButton(onClick = onMinimize, enabled = current != null) {
                    Text("−", color = if (current != null) Retro.Emerald else Retro.TextDim,
                        modifier = Modifier.padding(4.dp).clearAndSetSemantics { contentDescription = "Minimize window" })
                }
                IconButton(onClick = onClose, enabled = current != null) {
                    Icon(TnIcons.X, contentDescription = "Close window", tint = if (current != null) Retro.Emerald else Retro.TextDim)
                }
            }
        }
    }
}
