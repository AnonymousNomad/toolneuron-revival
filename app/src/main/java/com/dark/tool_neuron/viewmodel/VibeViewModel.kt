package com.dark.tool_neuron.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.studio.VibeBuildRecord
import com.dark.tool_neuron.studio.VibeStudio
import com.dark.tool_neuron.studio.StudioDocumentQueue
import com.dark.tool_neuron.studio.StudioDocumentWork
import com.dark.tool_neuron.studio.StudioBuildTarget
import com.dark.tool_neuron.data.SecurityManager
import com.dark.tool_neuron.data.SessionHolder
import kotlinx.coroutines.flow.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StudioEditor(val workspaceId: String? = null, val path: String = "index.html", val text: String = "", val files: List<String> = emptyList(), val dirty: Boolean = false)

@HiltViewModel
class VibeViewModel @Inject constructor(
    private val laptop: LaptopManager,
    private val security: SecurityManager,
    private val session: SessionHolder,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = laptop.state
    val studio = laptop.vibe
    private val _editor = MutableStateFlow(StudioEditor())
    val editor = _editor.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()
    private val documents = StudioDocumentQueue()

    init {
        viewModelScope.launch {
            combine(session.active, state) { active, s ->
                (!security.isLockEnabled || active) to s.permissions.any { it.capabilityId == VibeStudio.CAPABILITY && it.granted }
            }.distinctUntilChanged().collect { (_, granted) ->
                if (!granted) { _editor.value = StudioEditor(); documents.clear() }
                resumeDocuments()
            }
        }
    }
    fun message(text: String) { _status.value = text.take(1000) }
    fun edit(text: String) {
        if (!_busy.value && laptop.core.hasPermission(VibeStudio.CAPABILITY)) _editor.value = _editor.value.copy(text = text, dirty = true)
    }
    private fun maySwitch(): Boolean {
        if (_editor.value.dirty) { message("Save or discard your edits before changing project or file."); return false }
        return true
    }
    fun create(name: String, templateId: String) {
        if (!maySwitch()) return
        action {
            val workspace = withContext(Dispatchers.IO) { studio.create(name, templateId) }
            loadProject(workspace.id)
            message("Created ${workspace.name}. Edit, save, then build.")
        }
    }
    fun open(workspaceId: String) {
        if (_busy.value || _editor.value.dirty) {
            val requested = state.value.workspaces.firstOrNull { it.id == workspaceId }?.name ?: workspaceId
            message("Could not open $requested. Finish the current action and save or discard edits, then select that project again.")
            return
        }
        action { loadProject(workspaceId) }
    }
    private suspend fun loadProject(workspaceId: String) {
        val loaded = withContext(Dispatchers.IO) {
            val files = studio.files(workspaceId)
            val path = files.firstOrNull { it == "index.html" } ?: files.firstOrNull {
                it.substringAfterLast('.').lowercase() in setOf("html", "css", "js", "json", "txt", "svg")
            } ?: error("Project has no editable source files")
            val text = studio.read(workspaceId, path)
            check(text.length <= 128_000) { "Source is too large for the editor; use the governed Terminal to reduce it" }
            laptop.core.openWorkspace(workspaceId).getOrThrow()
            StudioEditor(workspaceId, path, text, files)
        }
        if (!laptop.core.hasPermission(VibeStudio.CAPABILITY)) return
        _editor.value = loaded
        message("Opened project source. Preview uses a built snapshot.")
    }
    fun isCurrentPreview(workspaceId: String, buildId: String): Boolean =
        laptop.core.hasPermission(VibeStudio.CAPABILITY) && _editor.value.workspaceId == workspaceId &&
            state.value.vibeBuilds.lastOrNull { it.workspaceId == workspaceId }?.id == buildId
    fun previewMessage(workspaceId: String, buildId: String, text: String) {
        if (isCurrentPreview(workspaceId, buildId)) message(text)
    }
    fun openFile(path: String) {
        if (!maySwitch()) return
        val current = _editor.value
        val workspace = current.workspaceId ?: return
        action {
            val text = withContext(Dispatchers.IO) { studio.openSource(workspace, path) } ?: ""
            check(text.length <= 128_000) { "Source is too large for the editor; use the governed Terminal to reduce it" }
            if (!laptop.core.hasPermission(VibeStudio.CAPABILITY)) return@action
            _editor.value = current.copy(path = path, text = text, dirty = false)
            message("Opened $path. Save to create or update this source file.")
        }
    }
    fun save() {
        val current = _editor.value
        val workspace = current.workspaceId ?: return
        action {
            val files = withContext(Dispatchers.IO) { studio.save(workspace, current.path, current.text); studio.files(workspace) }
            if (_editor.value.workspaceId == workspace && laptop.core.hasPermission(VibeStudio.CAPABILITY))
                _editor.value = current.copy(files = files, dirty = false)
            message("Saved ${current.path}. Rebuild to update the preview.")
        }
    }
    fun discard() {
        val current = _editor.value
        val workspace = current.workspaceId ?: return
        action {
            val text = withContext(Dispatchers.IO) { studio.openSource(workspace, current.path) } ?: ""
            if (laptop.core.hasPermission(VibeStudio.CAPABILITY)) _editor.value = current.copy(text = text, dirty = false)
            message("Unsaved changes discarded.")
        }
    }
    fun build() {
        if (!maySwitch()) return
        val workspace = _editor.value.workspaceId ?: return
        action {
            val record = withContext(Dispatchers.IO) { studio.build(workspace) }
            message("Build ${record.verdict}. Structural checks only; test the preview yourself.")
        }
    }
    fun prepareExport(record: VibeBuildRecord): Boolean =
        documents.prepareArchive(StudioBuildTarget(record.workspaceId, record.id)).also {
            if (!it) message("Finish or cancel the current document picker first.")
        }
    fun export(uri: Uri?) { documents.completeArchive(uri?.toString()); resumeDocuments() }
    fun prepareCapture(bytes: ByteArray): Boolean = documents.prepareCapture(bytes).also {
        if (!it) message("Finish the current capture picker first, or capture a smaller preview.")
    }
    fun exportCapture(uri: Uri?) { documents.completeCapture(uri?.toString()); resumeDocuments() }
    private fun unlocked() = !security.isLockEnabled || session.active.value
    private fun resumeDocuments() {
        val work = documents.take(unlocked(), laptop.core.hasPermission(VibeStudio.CAPABILITY), _busy.value) ?: return
        action {
            val completed = withContext(Dispatchers.IO) {
                if (!unlocked()) return@withContext false
                check(laptop.core.hasPermission(VibeStudio.CAPABILITY)) { "Vibe Studio permission was revoked" }
                context.contentResolver.openOutputStream(Uri.parse(work.destination), "w")?.use { output ->
                    when (work) {
                        is StudioDocumentWork.Archive -> studio.export(work.target.workspaceId, work.target.buildId, output)
                        is StudioDocumentWork.Capture -> output.write(work.png)
                    }
                } ?: error("Cannot open export destination")
                true
            }
            if (!completed) documents.retry(work)
            else message(if (work is StudioDocumentWork.Archive) "Exported the selected snapshot and manifest." else "Saved the visible preview capture.")
        }
    }
    private fun action(operation: suspend () -> Unit) {
        if (!_busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            try { operation() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { message(failure.message ?: "Studio action failed") }
            finally { _busy.value = false; resumeDocuments() }
        }
    }
}
