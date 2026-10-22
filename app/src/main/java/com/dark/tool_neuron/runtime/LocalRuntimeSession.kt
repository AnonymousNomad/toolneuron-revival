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
import kotlinx.coroutines.flow.onCompletion
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
    val loadId: String? = null,
)

class LocalRuntimeSession(backends: List<LocalModelBackend>, private val ownedRoots: List<File>) {
    private val backends = backends.associateBy { it.id }.also { require(it.size == backends.size) }
    private val mutex = Mutex()
    @Volatile private var generationOwner: Pair<String?, kotlinx.coroutines.Job>? = null
    @Volatile private var active: LocalModelBackend? = null
    private val _status = MutableStateFlow(LocalRuntimeStatus())
    val status = _status.asStateFlow()

    suspend fun load(
        runtimeId: String,
        path: String,
        options: LocalLoadOptions = LocalLoadOptions(),
        loadId: String = java.util.UUID.randomUUID().toString(),
        configure: () -> Unit = {},
        validate: (File) -> Unit = {},
    ): String {
        lateinit var file: File
        return loadWith(runtimeId, loadId, prepare = { file = ownedGguf(path); validate(file) }) { backend -> backend.load(file, options).also { configure() } }
    }

    suspend fun loadLegacyDescriptor(runtimeId: String, fd: Int, options: LocalLoadOptions): String =
        loadLegacyDescriptor(runtimeId, options) { fd }

    suspend fun loadLegacyDescriptor(runtimeId: String, options: LocalLoadOptions, loadId: String = java.util.UUID.randomUUID().toString(), configure: () -> Unit = {}, acquire: () -> Int): String =
        loadWith(runtimeId, loadId, prepare = {
            require(runtimeId == GgmlBackend.ID) { "Only the existing GGML backend supports legacy descriptors; import a durable GGUF file" }
        }) { backend -> backend.loadLegacyDescriptor(acquire(), options).also { configure() } }

    private suspend fun loadWith(
        runtimeId: String,
        loadId: String,
        prepare: () -> Unit,
        loader: suspend (LocalModelBackend) -> String,
    ): String {
        cancel()
        return mutex.withLock {
            _status.value = _status.value.copy(requestedRuntime = runtimeId, phase = LocalRuntimePhase.LOADING, error = null, loadId = null)
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
                _status.value = LocalRuntimeStatus(runtimeId, runtimeId, LocalRuntimePhase.READY, loadId = loadId)
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

    fun generate(prompt: String, maxTokens: Int, multiTurn: Boolean = false, expectedLoadId: String? = null, cleanup: () -> Unit = {}): Flow<LocalGenerationEvent> =
        guardedGeneration(null, expectedLoadId) { backend ->
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
            }.onCompletion { cleanup() }
        }

    fun <T> legacyGeneration(runtimeId: String, expectedLoadId: String? = null, source: () -> Flow<T>): Flow<T> =
        guardedGeneration(runtimeId, expectedLoadId) { source() }

    private fun <T> guardedGeneration(runtimeId: String?, expectedLoadId: String?, source: (LocalModelBackend) -> Flow<T>): Flow<T> = flow {
        check(mutex.tryLock()) { "Another local runtime operation is active" }
        try {
            val backend = active
            check(backend != null && _status.value.phase == LocalRuntimePhase.READY &&
                _status.value.requestedRuntime == backend.id && (runtimeId == null || runtimeId == backend.id)) {
                _status.value.error ?: "No selected runtime is ready"
            }
            check(expectedLoadId == null || expectedLoadId == _status.value.loadId) { "Loaded model or configuration changed; reload explicitly" }
            generationOwner = _status.value.loadId to checkNotNull(kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job])
            _status.value = _status.value.copy(phase = LocalRuntimePhase.GENERATING, error = null)
            var completed = false
            try {
                emitAll(source(backend))
                completed = true
            } finally {
                try { if (!completed) backend.cancel() }
                finally { generationOwner = null; _status.value = _status.value.copy(phase = LocalRuntimePhase.READY) }
            }
        } finally { mutex.unlock() }
    }

    fun <T> configure(runtimeId: String, allowEmpty: Boolean = false, expectedLoadId: String? = null, nextLoadId: String = java.util.UUID.randomUUID().toString(), action: () -> T): T {
        check(mutex.tryLock()) { "Another local runtime operation is active" }
        var mutating = false
        try {
            check(expectedLoadId == null || expectedLoadId == _status.value.loadId) { "Loaded model changed; reload explicitly" }
            check((active?.id == runtimeId && _status.value.phase == LocalRuntimePhase.READY && _status.value.requestedRuntime == runtimeId) || (allowEmpty && active == null && _status.value.phase == LocalRuntimePhase.EMPTY)) { "Selected runtime is not ready" }
            mutating = true
            _status.value = _status.value.copy(loadId = null)
            return action().also { _status.value = _status.value.copy(loadId = nextLoadId) }
        } catch (failure: Throwable) {
            if (mutating) _status.value = _status.value.copy(phase = LocalRuntimePhase.ERROR, loadId = null, error = failure.message ?: "Configuration failed")
            throw failure
        } finally { mutex.unlock() }
    }

    fun configureForLoad(runtimeId: String, loadId: String, action: () -> Unit): String {
        require(loadId.isNotBlank())
        val next = java.util.UUID.randomUUID().toString()
        configure(runtimeId, expectedLoadId = loadId, nextLoadId = next, action = action)
        return next
    }
    fun cancelForLoad(loadId: String) { generationOwner?.takeIf { it.first == loadId }?.second?.cancel() }
    fun cancel() { active?.cancel() }

    suspend fun unloadIfCurrent(loadId: String): Boolean {
        return mutex.withLock {
            if (_status.value.loadId != loadId) return@withLock false
            try {
                active?.unload()
                active = null
                _status.value = LocalRuntimeStatus()
                true
            } catch (failure: Throwable) {
                _status.value = _status.value.copy(phase = LocalRuntimePhase.ERROR, loadId = null, error = failure.message)
                throw failure
            }
        }
    }

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
