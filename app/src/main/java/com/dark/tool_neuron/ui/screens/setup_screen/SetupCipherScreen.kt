package com.dark.tool_neuron.ui.screens.setup_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.ui.screens.desktop.Retro
import com.dark.tool_neuron.ui.screens.desktop.RetroButton
import com.dark.tool_neuron.ui.screens.desktop.RetroHeader
import com.dark.tool_neuron.ui.screens.desktop.RetroKeyValue
import com.dark.tool_neuron.ui.screens.desktop.RetroMono
import com.dark.tool_neuron.ui.screens.desktop.RetroPanel
import com.dark.tool_neuron.viewmodel.SetupCipherViewModel

@Composable
fun SetupCipherScreen(
    innerPadding: PaddingValues,
    onComplete: () -> Unit,
    viewModel: SetupCipherViewModel = hiltViewModel(),
) {
    val admission by viewModel.admission.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val projectName by viewModel.projectName.collectAsStateWithLifecycle()
    var projectInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RetroHeader(title = "First run // Cipher setup")

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text("THIS DEVICE", fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                RetroKeyValue("device", viewModel.deviceInfo.device)
                RetroKeyValue("soc", viewModel.deviceInfo.soc)
                RetroKeyValue("abis", viewModel.deviceInfo.abis)
                RetroKeyValue("ram", "${viewModel.deviceInfo.ramMb} MB")
                RetroKeyValue("storage free", "${viewModel.deviceInfo.freeStorageMb} MB")
            }

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text("LOCAL-FIRST", fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Everything runs on this device. No account, no cloud, no tracking. " +
                        "Your projects and Cipher's memory live in an encrypted local Laptop.",
                    fontFamily = RetroMono,
                    color = Retro.TextDim,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text("RESIDENT", fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                RetroKeyValue(
                    "status",
                    admission.status,
                    valueColor = if (admission.status == "READY") Retro.Emerald else Retro.Amber,
                )
                Text(admission.detail, fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
                if (admission.status != "READY") {
                    Text(
                        "You can finish setup now and install a model later from the Store.",
                        fontFamily = RetroMono,
                        color = Retro.Amber,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.height(6.dp))
                RetroButton(label = "Refresh", onClick = { viewModel.refreshAdmission() }, accent = Retro.Amber)
            }

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text("FIRST PROJECT", fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                if (projectName != null) {
                    RetroKeyValue("project", projectName!!)
                } else {
                    OutlinedTextField(
                        value = projectInput,
                        onValueChange = { projectInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("my-first-project", fontFamily = RetroMono) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    RetroButton(
                        label = "Create project",
                        onClick = { if (projectInput.isNotBlank()) viewModel.createProject(projectInput) },
                        enabled = projectInput.isNotBlank(),
                    )
                }
            }

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text("HOW CIPHER WORKS", fontFamily = RetroMono, color = Retro.Emerald, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text("1. You ask in plain language.", fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
                Text("2. Cipher understands the goal and looks things up in the Laptop.", fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
                Text("3. Cipher prepares the work — nothing runs without your approval.", fontFamily = RetroMono, color = Retro.TextDim, style = MaterialTheme.typography.labelSmall)
            }

            status?.let {
                Text(it, fontFamily = RetroMono, color = Retro.Amber, style = MaterialTheme.typography.labelSmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RetroButton(label = "Finish → Desktop", onClick = onComplete)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
