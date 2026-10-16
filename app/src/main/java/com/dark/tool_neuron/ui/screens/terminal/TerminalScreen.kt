package com.dark.tool_neuron.ui.screens.terminal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.ui.theme.LocalDimens
import com.dark.tool_neuron.viewmodel.TerminalViewModel

@Composable
fun TerminalScreen(
    innerPadding: PaddingValues,
    viewModel: TerminalViewModel = hiltViewModel(),
) {
    val provider by viewModel.providerState.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current

    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = sessions.firstOrNull { it.id == selectedId } ?: sessions.firstOrNull()
    var input by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(selected?.output) {
        scrollState.scrollTo(scrollState.maxValue)
    }

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
                Text("Terminal provider", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${provider.id} v${provider.version} — ${provider.availability}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (provider.availability.name) {
                        "READY" -> MaterialTheme.colorScheme.primary
                        "FAILED" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                provider.failureReason?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "shell: ${provider.shellPath} · footprint: ${provider.footprintBytes} B · bootstrap: ${provider.bootstrap}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "features: ${provider.supportedFeatures.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                provider.limitations.forEach {
                    Text("limit: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                    TextButton(onClick = { viewModel.probe() }) { Text("Probe") }
                    OutlinedButton(onClick = { viewModel.openSession() }) { Text("Open session") }
                }
            }
        }

        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (sessions.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs),
            ) {
                sessions.forEach { s ->
                    TextButton(onClick = { selectedId = s.id }) {
                        Text(
                            "${s.id} [${s.state}${s.exitCode?.let { " $it" } ?: ""}]",
                            fontWeight = if (s.id == selected?.id) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(dimens.spacingSm),
            ) {
                Text(
                    text = selected?.output?.takeLast(20_000) ?: "(no session — open one to start)",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
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
                placeholder = { Text("command") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    selected?.let {
                        viewModel.send(it.id, input)
                        input = ""
                    }
                }),
            )
            TextButton(onClick = {
                selected?.let {
                    viewModel.send(it.id, input)
                    input = ""
                }
            }) { Text("Send") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs),
        ) {
            TextButton(onClick = { selected?.let { viewModel.interrupt(it.id) } }) { Text("Ctrl+C") }
            TextButton(onClick = { selected?.let { viewModel.clear(it.id) } }) { Text("Clear") }
            TextButton(onClick = { selected?.let { viewModel.close(it.id) } }) { Text("Close session") }
            Spacer(Modifier.weight(1f))
            Text("line-oriented (P1)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(dimens.spacingSm))
    }
}
