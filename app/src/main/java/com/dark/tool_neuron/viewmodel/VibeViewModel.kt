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
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext

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
    private val _pendingSourceProject = MutableStateFlow<String?>(null)
    val pendingSourceProject = _pendingSourceProject.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()
    private val documents = StudioDocumentQueue()

    private val draftMutex = kotlinx.coroutines.sync.Mutex()
    private val draftRevision = java.util.concurrent.atomic.AtomicLong()
    init {
        viewModelScope.launch {
            combine(session.active, state) { active, s ->
                (!security.isLockEnabled || active) to s.permissions.any { it.capabilityId == VibeStudio.CAPABILITY && it.granted }
            }.distinctUntilChanged().collect { (_, granted) ->
                if (!granted) { draftRevision.incrementAndGet(); draftJob?.cancel(); _editor.value = StudioEditor(); _pendingSourceProject.value = null; documents.clear() }
                resumeDocuments()
            }
        }
    }
    fun message(text: String) { _status.value = text.take(1000) }
    private var draftJob: kotlinx.coroutines.Job? = null
    fun edit(text: String) {
        if (_busy.value || !laptop.core.hasPermission(VibeStudio.CAPABILITY) || text.length > 128000) return
        val next = _editor.value.copy(text = text, dirty = true)
        _editor.value = next
        val workspace = next.workspaceId ?: return
        val revision = draftRevision.incrementAndGet()
        draftJob?.cancel()
        message("Draft pending local save…")
        draftJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            try {
                withContext(Dispatchers.IO) { draftMutex.withLock { currentCoroutineContext().ensureActive(); laptop.core.saveVibeDraft(workspace, com.dark.tool_neuron.studio.VibeDraft(next.path, next.text, System.currentTimeMillis())) { draftRevision.get() == revision }.getOrThrow() } }
                if (_editor.value == next) message("Draft saved locally. Save source before building.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { message("Draft not saved: ${failure.message}. Save source before leaving.") }
        }
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
        action {
            val hasSources = withContext(Dispatchers.IO) { studio.hasSources(workspaceId) }
            if (!laptop.core.hasPermission(VibeStudio.CAPABILITY)) return@action
            if (!hasSources) {
                _pendingSourceProject.value = workspaceId
                message("This existing project has no Vibe source yet. Choose a template to initialize it without copying the project.")
            } else loadProject(workspaceId)
        }
    }
    fun cancelInitialization() { _pendingSourceProject.value = null }
    fun initializeExisting(templateId: String) {
        if (!maySwitch()) return
        val workspace = _pendingSourceProject.value ?: return
        action {
            withContext(Dispatchers.IO) { studio.initialize(workspace, templateId) }
            loadProject(workspace)
            message("Initialized source in the existing project. Its ID and other files were preserved.")
        }
    }
    private suspend fun loadProject(workspaceId: String) {
        val loaded = withContext(Dispatchers.IO) {
            val files = studio.files(workspaceId)
            val record = state.value.workspaces.firstOrNull { it.id == workspaceId }
            val draft = record?.vibeDraft
            val remembered = draft?.path ?: record?.metadata?.get("vibe.lastSource")
            val path = draft?.path ?: remembered?.takeIf { it in files } ?: files.firstOrNull { it == "index.html" } ?: files.firstOrNull {
                it.substringAfterLast('.').lowercase() in setOf("html", "css", "js", "json", "txt", "svg")
            } ?: error("Project has no editable source files")
            val text = draft?.text ?: studio.read(workspaceId, path)
            check(text.length <= 128_000) { "Source is too large for the editor; use the governed Terminal to reduce it" }
            laptop.core.rememberVibeSource(workspaceId, path).getOrThrow()
            StudioEditor(workspaceId, path, text, files, dirty = draft != null)
        }
        if (!laptop.core.hasPermission(VibeStudio.CAPABILITY)) return
        _editor.value = loaded
        _pendingSourceProject.value = null
        message(if (loaded.dirty) "Recovered local draft. Review against source before saving; preview remains a built snapshot." else "Opened project source. Preview uses a built snapshot.")
    }
    fun isCurrentPreview(workspaceId: String, buildId: String): Boolean =
        laptop.core.hasPermission(VibeStudio.CAPABILITY) && _editor.value.workspaceId == workspaceId &&
            state.value.vibeBuilds.any { it.workspaceId == workspaceId && it.id == buildId }
    fun previewMessage(workspaceId: String, buildId: String, text: String) {
        if (isCurrentPreview(workspaceId, buildId)) message(text)
    }
    fun openFile(path: String) {
        if (!maySwitch()) return
        val current = _editor.value
        val workspace = current.workspaceId ?: return
        action {
            val source = withContext(Dispatchers.IO) { studio.openSource(workspace, path) }
            val text = source ?: ""
            check(text.length <= 128_000) { "Source is too large for the editor; use the governed Terminal to reduce it" }
            if (source != null) withContext(Dispatchers.IO) { laptop.core.rememberVibeSource(workspace, path).getOrThrow() }
            if (!laptop.core.hasPermission(VibeStudio.CAPABILITY)) return@action
            _editor.value = current.copy(path = path, text = text, dirty = source == null)
            message("Opened $path. Save to create or update this source file.")
        }
    }
    fun save() {
        val current = _editor.value
        val workspace = current.workspaceId ?: return
        draftRevision.incrementAndGet()
        draftJob?.cancel()
        action {
            val files = withContext(Dispatchers.IO) { draftMutex.withLock { studio.save(workspace, current.path, current.text); laptop.core.saveVibeDraft(workspace, null).getOrThrow(); laptop.core.rememberVibeSource(workspace, current.path).getOrThrow(); studio.files(workspace) } }
            if (_editor.value.workspaceId == workspace && laptop.core.hasPermission(VibeStudio.CAPABILITY))
                _editor.value = current.copy(files = files, dirty = false)
            message("Saved ${current.path}. Rebuild to update the preview.")
        }
    }
    fun discard() {
        val current = _editor.value
        val workspace = current.workspaceId ?: return
        draftRevision.incrementAndGet()
        draftJob?.cancel()
        action {
            val text = withContext(Dispatchers.IO) { draftMutex.withLock { val source = studio.openSource(workspace, current.path) ?: ""; laptop.core.saveVibeDraft(workspace, null).getOrThrow(); source } }
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
    fun export(uri: Uri?) { if (!documents.completeArchive(uri?.toString())) message("Export target was interrupted. Select the build and export again; no archive was written."); resumeDocuments() }
    fun prepareCapture(bytes: ByteArray): Boolean = documents.prepareCapture(bytes).also {
        if (!it) message("Finish the current capture picker first, or capture a smaller preview.")
    }
    fun exportCapture(uri: Uri?) { if (!documents.completeCapture(uri?.toString())) message("Capture was interrupted. Capture again; no image was written."); resumeDocuments() }
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
