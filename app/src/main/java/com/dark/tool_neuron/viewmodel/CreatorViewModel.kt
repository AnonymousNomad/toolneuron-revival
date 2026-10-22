package com.dark.tool_neuron.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.creation.*
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.util.ChatShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

enum class CreationEditKind { NEW_PROJECT, SYNOPSIS, SEASON, EPISODE, SCENE, SHOT, CHARACTER, ASSET, AUDIO }
data class CreationEditTarget(val kind: CreationEditKind, val workspaceId: String, val parentId: String? = null, val recordId: String? = null)
data class CreationForm(val name: String, val body: String, val secondary: String = "", val license: String = "",
    val creationKind: CreationKind = CreationKind.STORY, val assetKind: CreationAssetKind = CreationAssetKind.REFERENCE,
    val audioKind: CreationAudioKind = CreationAudioKind.VOICE, val rights: AssetRights = AssetRights.UNKNOWN,
    val seconds: String = "6", val characterIds: List<String> = emptyList(), val assetIds: List<String> = emptyList(), val audioSlotIds: List<String> = emptyList())
data class CreationExportPreview(val title: String, val json: String, val sha256: String, val manifest: CreationManifest)

@HiltViewModel
class CreatorViewModel @Inject constructor(private val laptop: LaptopManager, private val app: Application) : ViewModel() {
    val state = laptop.state
    private val _selectedId = MutableStateFlow(state.value.activeWorkspaceId?.takeIf { id -> state.value.workspaces.any { it.id == id && it.creation != null } }
        ?: state.value.workspaces.lastOrNull { it.creation != null }?.id)
    val selectedId = _selectedId.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()
    private val _preview = MutableStateFlow<CreationExportPreview?>(null)
    val preview = _preview.asStateFlow()
    init {
        viewModelScope.launch {
            state.map { snapshot -> snapshot.permissions.any { it.capabilityId == CreationPlan.CAPABILITY && it.granted } }
                .distinctUntilChanged().collect { if (!it) _preview.value = null }
        }
    }
    fun clearStatus() { _status.value = null }
    fun open(workspaceId: String, onFailed: (String) -> Unit = {}, onOpened: () -> Unit = {}) = action({
        require(state.value.workspaces.any { it.id == workspaceId && it.creation != null }) { "Creation project is missing" }
        laptop.core.openWorkspace(workspaceId).getOrThrow()
        workspaceId
    }, onError = onFailed) { id -> _selectedId.value = id; _preview.value = null; onOpened() }
    fun save(target: CreationEditTarget, form: CreationForm, onSaved: () -> Unit) = action({
        val core = laptop.core
        if (target.kind == CreationEditKind.NEW_PROJECT) core.createCreation(form.name, form.body, form.creationKind).getOrThrow().id
        else {
            when (target.kind) {
                CreationEditKind.SYNOPSIS -> core.updateCreationPlan(target.workspaceId) { it.copy(synopsis = form.body.trim()) }.getOrThrow()
                CreationEditKind.SEASON -> if (target.recordId == null) core.addCreationSeason(target.workspaceId, form.name).getOrThrow() else core.renameCreationNode(target.workspaceId, target.recordId, form.name).getOrThrow()
                CreationEditKind.EPISODE -> if (target.recordId == null) core.addCreationEpisode(target.workspaceId, target.parentId!!, form.name).getOrThrow() else core.renameCreationNode(target.workspaceId, target.recordId, form.name).getOrThrow()
                CreationEditKind.SCENE -> if (target.recordId == null) core.addCreationScene(target.workspaceId, target.parentId!!, form.name).getOrThrow() else core.renameCreationNode(target.workspaceId, target.recordId, form.name).getOrThrow()
                CreationEditKind.SHOT -> {
                    val durationMs = runCatching { form.seconds.toBigDecimal().multiply(java.math.BigDecimal(1000)).longValueExact() }.getOrElse { error("Duration must use seconds with at most millisecond precision") }
                    require(durationMs in 1000..600_000) { "Duration must be 1..600 seconds" }
                    if (target.recordId == null) core.addCreationShot(target.workspaceId, target.parentId!!, form.body, durationMs, form.secondary, form.characterIds, form.assetIds, form.audioSlotIds).getOrThrow()
                    else core.editCreationShot(target.workspaceId, target.recordId, form.body, durationMs, form.secondary, form.characterIds, form.assetIds, form.audioSlotIds).getOrThrow()
                }
                CreationEditKind.CHARACTER -> if (target.recordId == null) core.addCreationCharacter(target.workspaceId, form.name, form.body, form.assetIds).getOrThrow() else core.editCreationCharacter(target.workspaceId, target.recordId, form.name, form.body, form.assetIds).getOrThrow()
                CreationEditKind.ASSET -> if (target.recordId == null) core.registerCreationAsset(target.workspaceId, form.name, form.assetKind, form.secondary, form.rights, form.license, form.body).getOrThrow() else core.editCreationAsset(target.workspaceId, target.recordId, form.name, form.assetKind, form.secondary, form.rights, form.license, form.body).getOrThrow()
                CreationEditKind.AUDIO -> if (target.recordId == null) core.addCreationAudio(target.workspaceId, form.audioKind, form.name, form.body, form.assetIds.firstOrNull()).getOrThrow() else core.editCreationAudio(target.workspaceId, target.recordId, form.audioKind, form.name, form.body, form.assetIds.firstOrNull()).getOrThrow()
                CreationEditKind.NEW_PROJECT -> Unit
            }
            target.workspaceId
        }
    }) { id -> _selectedId.value = id; _status.value = "Saved locally. No media was generated."; onSaved() }
    fun prepareExport(workspaceId: String) = action({
        val manifest = laptop.core.creationManifest(workspaceId).getOrThrow()
        val json = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(CreationManifest.serializer(), manifest)
        val bytes = json.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 8 * 1024 * 1024) { "Manifest exceeds the local export bound" }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        CreationExportPreview(manifest.title, json, sha, manifest)
    }) { preview -> if (laptop.core.hasPermission(CreationPlan.CAPABILITY)) _preview.value = preview }
    fun dismissExport() { _preview.value = null }
    fun shareReviewed(preview: CreationExportPreview) = action({
        check(laptop.core.hasPermission(CreationPlan.CAPABILITY)) { "Creator planning permission denied" }
        ChatShareHelper.writeAndShare(app, "toolneuron-creation-${UUID.randomUUID()}.json", preview.json,
            "application/json", "${preview.title} production plan", "Share reviewed production plan")
    }) { _preview.value = null; _status.value = "Opened system sharing for the reviewed production plan." }
    private fun <T> action(work: () -> T, onError: (String) -> Unit = {}, onSuccess: (T) -> Unit = {}) {
        if (!_busy.compareAndSet(false, true)) {
            val message = "Finish the current Creator action first"
            _status.value = message
            onError(message)
            return
        }
        viewModelScope.launch {
            _status.value = null
            try { val value = withContext(Dispatchers.IO) { work() }; onSuccess(value) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { val message = failure.message ?: "Creator action failed"; _status.value = message; onError(message) }
            finally { _busy.value = false }
        }
    }
}
