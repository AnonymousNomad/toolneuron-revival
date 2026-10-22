package com.dark.tool_neuron.laptop.resident

import android.content.Context
import android.net.Uri
import com.dark.tool_neuron.laptop.CapabilityRegistry
import com.dark.tool_neuron.laptop.LaptopCore
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.laptop.ModelRegistry
import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.repo.ModelRepository
import com.dark.tool_neuron.service.inference.InferenceClient
import com.dark.tool_neuron.service.inference.InferenceEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResidentEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val laptop: LaptopManager,
    private val modelRepo: ModelRepository,
    private val assignmentInspector: com.dark.tool_neuron.laptop.CipherAssignmentInspector,
) {
    sealed class ResidentUiState {
        data object Idle : ResidentUiState()
        data object Starting : ResidentUiState()
        data class Active(val identity: ResidentIdentity, val sessionId: String) : ResidentUiState()
        data class Unavailable(val reason: String) : ResidentUiState()
        data object Stopped : ResidentUiState()
    }

    data class Message(
        val role: String,
        val text: String,
        val intent: IntentEnvelope?,
        val status: ResidentTurnStatus?,
        val lookups: List<String>,
        val approxTokens: Int,
        val events: List<ResidentEvent> = emptyList(),
    )

    private val _state = MutableStateFlow<ResidentUiState>(ResidentUiState.Idle)
    val state: StateFlow<ResidentUiState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _streaming = MutableStateFlow("")
    val streaming: StateFlow<String> = _streaming.asStateFlow()

    @Volatile private var cancelRequested = false

    private class Session(
        val id: String,
        val identity: ResidentIdentity,
        val model: ModelInfo,
        var turns: Int,
        var lookups: Int,
        var previousIntent: IntentEnvelope?,
        val workspaceId: String? = null,
        val loadId: String? = null,
        val recentExchanges: ArrayDeque<Pair<String, String>> = ArrayDeque(),
    )

    private var session: Session? = null
    private val lifecycleMutex = Mutex()

    @Volatile private var dossierCache: List<ModelRegistry.ModelDossier> = emptyList()

    private val lookupService: LaptopLookupService by lazy {
        LaptopLookupService(
            core = laptop.core,
            workspaceFileNames = { id -> laptop.listWorkspaceEntries(id).map { it.path } },
            workspaceFileContent = { id, path -> laptop.readWorkspaceFile(id, path) },
            modelDossiers = { dossierCache },
        )
    }

    init {
        runCatching { laptop.core.recoverResidentOnBoot() }
    }

    suspend fun start(contextSize: Int = 4096): ResidentUiState = lifecycleMutex.withLock { startAssigned() }

    private suspend fun startAssigned(): ResidentUiState {
        if (_state.value is ResidentUiState.Active) return _state.value
        _state.value = ResidentUiState.Starting

        val inspection = assignmentInspector.inspect()
        if (inspection.assessment.status != com.dark.tool_neuron.laptop.CipherSlotStatus.READY) {
            _state.value = ResidentUiState.Unavailable(inspection.assessment.reason)
            return _state.value
        }
        val assignment = inspection.assignment!!
        val model = inspection.model!!
        val dossier = laptop.modelDossiers(listOf(model)).firstOrNull() ?: ModelRegistry.dossier(model, null, null)
        val identity = buildIdentity(model, dossier, "explicit qualified Cipher assignment", assignment.contextSize)
            .copy(runtime = com.dark.tool_neuron.laptop.CipherRuntimeIdentity.revision(assignment.runtimeId, assignment.contextSize))
        if (identity.artifactSha256 != assignment.artifactSha256) {
            _state.value = ResidentUiState.Unavailable("Assigned artifact changed during load preparation")
            return _state.value
        }
        val configJson = com.dark.tool_neuron.laptop.CipherRuntimeIdentity.config(assignment)
        val load = when (model.pathType) {
            PathType.FILE -> InferenceClient.loadModel(model.path, configJson)
            PathType.CONTENT_URI -> InferenceClient.loadModelFromUri(context, Uri.parse(model.path), configJson)
        }
        if (load.isFailure) {
            val reason = "resident model failed to load: ${load.exceptionOrNull()?.message}"
            laptop.core.startResidentSession(identity, laptop.core.activeWorkspace()?.id, null, expectedAssignment = assignment)
                .onSuccess { rec ->
                    laptop.core.endResidentSession(rec.id, ResidentSessionStatus.FAILED, reason)
                }
            _state.value = ResidentUiState.Unavailable(reason)
            return _state.value
        }

        val loadId = runCatching { JSONObject(load.getOrThrow()).getString("loadId").takeIf { it.isNotBlank() && it != "null" } }.getOrNull()
        if (loadId == null) {
            _state.value = ResidentUiState.Unavailable("Runtime did not return a loaded model identity")
            return _state.value
        }
        val ws = laptop.core.activeWorkspace()
        val record = laptop.core.startResidentSession(identity, ws?.id, null, expectedAssignment = assignment).getOrElse {
            runCatching { InferenceClient.unloadModelForLoad(loadId) }
            _state.value = ResidentUiState.Unavailable("could not persist resident session: ${it.message}")
            return _state.value
        }
        session = Session(record.id, identity, model, 0, 0, null, ws?.id, loadId)
        _messages.value = emptyList()
        _streaming.value = ""
        dossierCache = laptop.modelDossiers(modelRepo.models.value)
        _state.value = ResidentUiState.Active(identity, record.id)
        return _state.value
    }

    suspend fun send(userText: String) {
        val s = session ?: run {
            appendMessage(Message("system", "Resident is not active. Start it first.", null, null, emptyList(), 0))
            return
        }
        if (s.workspaceId != laptop.core.state.value.activeWorkspaceId) {
            appendMessage(Message("system", "Project changed. Stop and restart Cipher to use the selected project without prior project context.", null, null, emptyList(), 0))
            return
        }
        val text = userText.trim().take(1500)
        if (text.isEmpty()) return
        appendMessage(Message("user", text, null, null, emptyList(), 0))

        dossierCache = laptop.modelDossiers(modelRepo.models.value)
        val ctx = buildTurnContext(s.workspaceId)

        val turn1 = ResidentContext.buildTurn(text, ctx, emptyList(), s.previousIntent, s.recentExchanges.toList(), finalTurn = false)
        val r1 = generateText(turn1.system, turn1.user, MAX_NEW_TOKENS, s.loadId)
        if (r1.isFailure) {
            val msg = if (cancelRequested) "generation cancelled by user"
            else "resident generation failed: ${r1.exceptionOrNull()?.message}"
            appendMessage(Message("system", msg, null, null, emptyList(), turn1.approxTokens))
            return
        }
        val parsed1 = IntentParser.parse(r1.getOrThrow())

        val requested = when (parsed1) {
            is IntentParseResult.Parsed -> parsed1.envelope.requested_lookups
                .mapNotNull { raw ->
                    val parts = raw.split(':', limit = 2)
                    val kind = LookupKind.fromId(parts[0]) ?: return@mapNotNull null
                    kind to (parts.getOrNull(1)?.trim().orEmpty())
                }
                .take(MAX_LOOKUPS_PER_TURN)
            is IntentParseResult.Failed -> emptyList()
        }

        val lookupResults = mutableListOf<LaptopLookupResult>()
        requested.forEach { (kind, query) ->
            val q = query.ifBlank { if (kind == LookupKind.WORKSPACE_READ) "" else text }
            lookupResults += lookupService.execute(LaptopLookupRequest(kind, query = q), s.workspaceId)
            s.lookups++
        }

        val finalEnvelope: IntentEnvelope?
        val finalStatus: ResidentTurnStatus
        var tokens = turn1.approxTokens

        if (requested.isNotEmpty()) {
            val turn2 = ResidentContext.buildTurn(text, ctx, lookupResults, s.previousIntent, s.recentExchanges.toList(), finalTurn = true)
            tokens += turn2.approxTokens
            val r2 = generateText(turn2.system, turn2.user, MAX_NEW_TOKENS, s.loadId)
            if (r2.isFailure) {
                val msg = if (cancelRequested) "generation cancelled by user"
                else "resident generation failed: ${r2.exceptionOrNull()?.message}"
                appendMessage(Message("system", msg, null, null, requested.map { it.first.id }, tokens))
                return
            }
            when (val parsed2 = IntentParser.parse(r2.getOrThrow())) {
                is IntentParseResult.Parsed -> {
                    finalEnvelope = parsed2.envelope
                    finalStatus = IntentParser.status(parsed2.envelope)
                }
                is IntentParseResult.Failed -> {
                    finalEnvelope = null
                    finalStatus = ResidentTurnStatus.INTENT_PARSE_FAILED
                }
            }
        } else {
            when (parsed1) {
                is IntentParseResult.Parsed -> {
                    finalEnvelope = parsed1.envelope
                    finalStatus = IntentParser.status(parsed1.envelope)
                }
                is IntentParseResult.Failed -> {
                    finalEnvelope = null
                    finalStatus = ResidentTurnStatus.INTENT_PARSE_FAILED
                }
            }
        }

        val events = ResidentEvents.derive(lookupResults, finalEnvelope)

        if (finalEnvelope == null) {
            appendMessage(
                Message(
                    "cipher",
                    "I could not produce a structured intent for that request (intent parse failed).",
                    null,
                    ResidentTurnStatus.INTENT_PARSE_FAILED,
                    requested.map { it.first.id },
                    tokens,
                    events,
                ),
            )
        } else {
            s.turns++
            s.previousIntent = finalEnvelope
            val natural = IntentRender.naturalText(finalEnvelope, finalStatus)
            appendMessage(
                Message(
                    "cipher",
                    natural,
                    finalEnvelope,
                    finalStatus,
                    requested.map { it.first.id },
                    tokens,
                    events,
                ),
            )
            s.recentExchanges.addLast(text to natural)
            while (s.recentExchanges.size > MAX_RECENT_EXCHANGES) s.recentExchanges.removeFirst()
        }
        laptop.core.updateResidentSession(s.id, s.turns, s.lookups)
    }

    fun cancelGeneration() {
        cancelRequested = true
        session?.loadId?.let { runCatching { InferenceClient.stopGenerationForLoad(it) } }
    }

    suspend fun stop() = lifecycleMutex.withLock { stopAssigned() }

    private suspend fun stopAssigned() {
        val s = session ?: return
        s.loadId?.let { runCatching { InferenceClient.unloadModelForLoad(it) } }
        laptop.core.endResidentSession(s.id, ResidentSessionStatus.ENDED_BY_USER)
        session = null
        _streaming.value = ""
        _state.value = ResidentUiState.Stopped
    }

    private fun appendMessage(message: Message) {
        _messages.value = (_messages.value + message).takeLast(MAX_TRANSCRIPT)
    }

    private suspend fun generateText(system: String, user: String, maxTokens: Int, loadId: String?): Result<String> =
        withContext(Dispatchers.IO) {
            _streaming.value = ""
            cancelRequested = false
            val arr = JSONArray()
            arr.put(JSONObject().put("role", "system").put("content", system))
            arr.put(JSONObject().put("role", "user").put("content", user))
            val sb = StringBuilder()
            try {
                withTimeout(GENERATE_TIMEOUT_MS) {
                    InferenceClient.generateMultiTurn(arr.toString(), maxTokens, loadId).collect { ev ->
                        when (ev) {
                            is InferenceEvent.Token -> {
                                sb.append(ev.text)
                                _streaming.value = sb.toString().takeLast(4000)
                            }
                            is InferenceEvent.Error -> throw IllegalStateException(ev.message)
                            else -> Unit
                        }
                    }
                }
                Result.success(sb.toString())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun buildIdentity(
        model: ModelInfo,
        dossier: ModelRegistry.ModelDossier,
        selectionReason: String,
        contextSize: Int,
    ): ResidentIdentity =
        withContext(Dispatchers.IO) {
            var sha: String? = null
            var hashReason: String? = null
            if (model.pathType == PathType.FILE) {
                sha = runCatching { sha256File(File(model.path)) }.getOrNull()
                if (sha == null) hashReason = "hash computation failed"
            } else {
                hashReason = "content-uri import (stream hash not computed in P2)"
            }
            ResidentIdentity(
                modelId = model.id,
                modelName = model.name,
                artifactRef = model.path,
                artifactBytes = model.fileSize,
                architecture = dossier.architecture,
                artifactSha256 = sha,
                hashUnavailableReason = hashReason,
                admission = dossier.admissions.map { it.name }.sorted(),
                contextTemplateIdentity = "cipher-greedy-chatml/1 · fixed ChatML template",
                contextProfile = "ctx-$contextSize",
                selectionReason = selectionReason,
                boundAtMs = System.currentTimeMillis(),
            )
        }

    private fun buildTurnContext(workspaceId: String?): ResidentContext.TurnContext {
        val ws = laptop.core.state.value.workspaces.firstOrNull { it.id == workspaceId }
        val files = ws?.let {
            laptop.listWorkspaceEntries(it.id).take(30).map { e -> e.path }
        } ?: emptyList()
        val wsSummary = ws?.let {
            "active workspace: ${it.name} (${it.id}); files: ${files.joinToString(", ").ifEmpty { "(none)" }}"
        }
        val task = laptop.core.state.value.tasks.filter { it.workspaceId == workspaceId }.maxByOrNull { it.updatedAtMs }
        val taskSummary = task?.let { "latest task: ${it.id} [${it.status}] ${it.userObjective.take(160)}" }
        val caps = CapabilityRegistry.SEED.map { cap ->
            "${CapabilityRegistry.ref(cap)} — ${cap.description.take(120)}"
        }
        return ResidentContext.TurnContext(wsSummary, taskSummary, caps)
    }

    private fun workspaceEntriesFor(id: String): List<String> = laptop.listWorkspaceEntries(id).map { it.path }

    private fun sha256File(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readMemAvailableBytes(): Long {
        return runCatching {
            File("/proc/meminfo").readLines().firstOrNull { it.startsWith("MemAvailable:") }
                ?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull()?.times(1024L)
        }.getOrNull() ?: 0L
    }

    private companion object {
        const val MAX_NEW_TOKENS = 512
        const val GENERATE_TIMEOUT_MS = 90_000L
        const val MAX_LOOKUPS_PER_TURN = 4
        const val MAX_TRANSCRIPT = 40
        const val MAX_RECENT_EXCHANGES = 3
    }
}
