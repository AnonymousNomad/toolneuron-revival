package com.dark.tool_neuron.ui.screens.system_ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.tool_neuron.ui.screens.model_store.ModelImportTypePicker
import com.dark.tool_neuron.viewmodel.ModelStoreViewModel
import com.dark.tool_neuron.viewmodel.ScaffoldViewModel

val LocalModelImportPicker = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun ModelImportHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val models: ModelStoreViewModel = hiltViewModel()
    val scaffold: ScaffoldViewModel = hiltViewModel()
    val locked by scaffold.shouldLock.collectAsStateWithLifecycle()
    val progress by models.localImport.collectAsStateWithLifecycle()
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var imported by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pendingUri = uri.toString()
        }
    }
    CompositionLocalProvider(LocalModelImportPicker provides { if (!locked && progress == null) picker.launch(arrayOf("application/octet-stream", "*/*")) }) { content() }
    if (!locked) {
        pendingUri?.let { value ->
            val uri = Uri.parse(value)
            val metadata = remember(value) {
                runCatching {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (!cursor.moveToFirst()) "model.gguf" to 0L else {
                            val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val size = cursor.getColumnIndex(OpenableColumns.SIZE)
                            (if (name < 0) "model.gguf" else cursor.getString(name) ?: "model.gguf") to (if (size < 0 || cursor.isNull(size)) 0L else cursor.getLong(size))
                        }
                    } ?: ("model.gguf" to 0L)
                }.getOrElse { "model.gguf" to 0L }
            }
            ModelImportTypePicker(metadata.first, onPick = { type ->
                if (!locked && progress == null) {
                    models.importLocalModel(uri, metadata.first, metadata.second, type) { imported = true }
                    pendingUri = null
                }
            }, onDismiss = { pendingUri = null })
        }
        if (progress != null) AlertDialog(onDismissRequest = {}, title = { Text("Local model import") }, text = {
            val current = progress!!
            Text(current.error ?: "Copying ${current.fileName}: ${current.copiedBytes} bytes. Keep the app open until the durable import finishes.")
        }, confirmButton = { if (progress?.error != null) TextButton(onClick = models::dismissImportError) { Text("Close") } })
        if (imported) AlertDialog(onDismissRequest = { imported = false }, title = { Text("Model imported") }, text = { Text("The model is saved locally. Choose it in Models or continue setup. Installation does not imply qualification.") }, confirmButton = { TextButton(onClick = { imported = false }) { Text("Done") } })
        error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("Import unavailable") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Close") } }) }
    }
}
