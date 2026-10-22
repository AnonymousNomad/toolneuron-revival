package com.dark.tool_neuron.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import com.dark.tool_neuron.util.GgufHeader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

enum class LocalRuntimePhase { EMPTY, LOADING, READY, GENERATING, ERROR }
data class LocalRuntimeStatus(
    val requestedRuntime: String? = null,
    val loadedRuntime: String? = null,
    val phase: LocalRuntimePhase = LocalRuntimePhase.EMPTY,
    val error: String? = null,
)

class LocalRuntimeSession(backends: List<LocalModelBackend>, private val ownedRoots: List<File>) {
    private val backends = backends.associateBy { it.id }.also { require(it.size == backends.size) }
    private val mutex = Mutex()
    @Volatile private var active: LocalModelBackend? = null
    private val _status = MutableStateFlow(LocalRuntimeStatus())
    val status = _status.asStateFlow()

    suspend fun load(
        runtimeId: String,
        path: String,
        options: LocalLoadOptions = LocalLoadOptions(),
        validate: (File) -> Unit = {},
    ): String {
        lateinit var file: File
        return loadWith(runtimeId, prepare = { file = ownedGguf(path); validate(file) }) { it.load(file, options) }
    }

    suspend fun loadLegacyDescriptor(runtimeId: String, fd: Int, options: LocalLoadOptions): String =
        loadLegacyDescriptor(runtimeId, options) { fd }

    suspend fun loadLegacyDescriptor(runtimeId: String, options: LocalLoadOptions, acquire: () -> Int): String =
        loadWith(runtimeId, prepare = {
            require(runtimeId == GgmlBackend.ID) { "Only the existing GGML backend supports legacy descriptors; import a durable GGUF file" }
        }) { it.loadLegacyDescriptor(acquire(), options) }

    private suspend fun loadWith(
        runtimeId: String,
        prepare: () -> Unit,
        loader: suspend (LocalModelBackend) -> String,
    ): String {
        cancel()
        return mutex.withLock {
            _status.value = _status.value.copy(requestedRuntime = runtimeId, phase = LocalRuntimePhase.LOADING, error = null)
            var attempted: LocalModelBackend? = null
            try {
                val backend = backends[runtimeId] ?: error("Unknown local runtime: $runtimeId; no default substitution")
                check(backend.availability.available) { backend.availability.reason ?: "$runtimeId unavailable" }
                prepare()
                active?.unload()
                active = null
                _status.value = _status.value.copy(loadedRuntime = null)
                attempted = backend
                val info = loader(backend)
                active = backend
                _status.value = LocalRuntimeStatus(runtimeId, runtimeId, LocalRuntimePhase.READY)
                info
            } catch (failure: Throwable) {
                attempted?.let { backend ->
                    try { withContext(NonCancellable) { backend.unload() }; active = null }
                    catch (cleanup: Throwable) { failure.addSuppressed(cleanup); active = backend }
                }
                _status.value = _status.value.copy(loadedRuntime = active?.id, phase = LocalRuntimePhase.ERROR,
                    error = failure.message ?: failure.javaClass.simpleName)
                throw failure
            }
        }
    }

    fun generate(prompt: String, maxTokens: Int, multiTurn: Boolean = false): Flow<LocalGenerationEvent> =
        guardedGeneration(null) { backend ->
            flow {
                require(maxTokens > 0) { "Token limit must be positive" }
                var terminal = false
                flow { emitAll(backend.generate(prompt, maxTokens, multiTurn)) }.catch { failure ->
                    if (failure is CancellationException) throw failure
                    emit(LocalGenerationEvent.Error(failure.message ?: "Runtime generation failed"))
                }.collect { event ->
                    if (!terminal) {
                        terminal = event is LocalGenerationEvent.Done || event is LocalGenerationEvent.Error
                        emit(event)
                    }
                }
                if (!terminal) emit(LocalGenerationEvent.Error("Runtime stream ended without completion"))
            }
        }

    fun <T> legacyGeneration(runtimeId: String, source: () -> Flow<T>): Flow<T> =
        guardedGeneration(runtimeId) { source() }

    private fun <T> guardedGeneration(runtimeId: String?, source: (LocalModelBackend) -> Flow<T>): Flow<T> = flow {
        check(mutex.tryLock()) { "Another local runtime operation is active" }
        try {
            val backend = active
            check(backend != null && _status.value.phase == LocalRuntimePhase.READY &&
                _status.value.requestedRuntime == backend.id && (runtimeId == null || runtimeId == backend.id)) {
                _status.value.error ?: "No selected runtime is ready"
            }
            _status.value = _status.value.copy(phase = LocalRuntimePhase.GENERATING, error = null)
            var completed = false
            try {
                emitAll(source(backend))
                completed = true
            } finally {
                try { if (!completed) backend.cancel() }
                finally { _status.value = _status.value.copy(phase = LocalRuntimePhase.READY) }
            }
        } finally { mutex.unlock() }
    }

    fun cancel() { active?.cancel() }

    suspend fun unload() {
        cancel()
        mutex.withLock {
            try {
                active?.unload()
                active = null
                _status.value = LocalRuntimeStatus()
            } catch (failure: Throwable) {
                _status.value = _status.value.copy(phase = LocalRuntimePhase.ERROR, error = failure.message)
                throw failure
            }
        }
    }

    private fun ownedGguf(path: String): File {
        val file = File(path)
        require(file.isAbsolute) { "A durable app-owned filesystem path is required" }
        val canonical = file.canonicalFile
        require(ownedRoots.any { canonical.toPath().startsWith(it.canonicalFile.toPath()) }) { "Model path is outside app-owned storage" }
        require(canonical.isFile && canonical.canRead()) { "Model file is missing or unreadable" }
        require(GgufHeader.fileHasGgufHeader(canonical)) { "Model is not a GGUF file" }
        return canonical
    }
}
