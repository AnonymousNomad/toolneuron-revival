package com.dark.tool_neuron.ui.screens.reporting

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.ui.screens.desktop.*
import com.dark.tool_neuron.viewmodel.BetaReportViewModel

@Composable
fun BetaReportScreen(padding: PaddingValues, pickScreenshot: () -> Unit, vm: BetaReportViewModel) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val screenshot by vm.screenshot.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = Retro.Background) {
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("LOCAL BETA REPORT", color = Retro.Emerald, fontFamily = RetroMono)
                Text("Prepare a local ZIP with app/device/model details, bounded status logs and your reproduction notes. Review everything before choosing where to share. No telemetry upload.", color = Retro.TextDim)
                OutlinedTextField(notes, vm::editNotes, label = { Text("What happened? Steps, expected result, observed result") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), minLines = 4)
                Text("Notes and screenshots can contain personal information. Include only what you intend to share.", color = Retro.Amber)
                RetroButton(if (screenshot == null) "Add optional screenshot" else "Replace screenshot", pickScreenshot, enabled = !busy)
                if (screenshot != null) RetroButton("Remove screenshot", vm::clearScreenshot, enabled = !busy)
                RetroButton("Prepare report for review", vm::prepare, enabled = !busy)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                status?.let { Text(it, color = Retro.Amber) }
            }
            preview?.let { selected ->
                item {
                    Text("EXACT SHARE PREVIEW · ${selected.archive.size} bytes", color = Retro.Emerald)
                    Text("ZIP SHA-256 ${selected.sha256}", color = Retro.TextDim)
                    Text(selected.json, color = Retro.TextPrimary, fontFamily = RetroMono)
                    RetroButton("Share reviewed ZIP", vm::share, enabled = !busy)
                }
                selected.screenshot?.let { png -> item {
                    val bitmap = remember(selected.sha256) { BitmapFactory.decodeByteArray(png, 0, png.size) }
                    bitmap?.let { Image(it.asImageBitmap(), "Screenshot included in this report", Modifier.fillMaxWidth().heightIn(max = 350.dp)) }
                } }
            }
        }
    }
}
