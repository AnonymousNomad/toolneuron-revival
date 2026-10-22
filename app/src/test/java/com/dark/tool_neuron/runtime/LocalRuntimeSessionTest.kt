package com.dark.tool_neuron.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalRuntimeSessionTest {
    @get:Rule val temp = TemporaryFolder()
    private fun model() = temp.newFile().apply { writeBytes("GGUFpayload".toByteArray()) }

    @Test fun unavailableCovertNeverInvokesTheExistingBackend() = runBlocking {
        val existing = ContractBackend()
        val session = LocalRuntimeSession(listOf(existing, CovertEngineBackend()), listOf(temp.root))
        val error = runCatching { session.load("covert-r0", model().path) }.exceptionOrNull()
        assertTrue(error!!.message!!.contains("BLOCKED_LOCAL_SOURCE_UNAVAILABLE"))
        assertEquals(0, existing.loads)
        assertEquals("covert-r0", session.status.value.requestedRuntime)
        assertNull(session.status.value.loadedRuntime)
        assertTrue(runCatching { session.generate("hi", 2).toList() }.isFailure)
        assertEquals(0, existing.generations)
    }

    @Test fun failedPostLoadConfigurationNeverPublishesReady() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        val result = runCatching { session.load(backend.id, model().path, configure = { error("profile rejected") }) }
        assertTrue(result.isFailure)
        assertEquals(LocalRuntimePhase.ERROR, session.status.value.phase)
        assertNull(session.status.value.loadedRuntime)
        assertTrue(backend.unloads > 0)
        assertTrue(runCatching { session.generate("prompt", 2).toList() }.isFailure)
    }

    @Test fun generationRejectsReplacedOrReconfiguredLoadIdentity() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path, loadId = "first")
        assertEquals("first", session.status.value.loadId)
        session.load(backend.id, model().path, loadId = "second")
        assertTrue(runCatching { session.generate("prompt", 2, expectedLoadId = "first").toList() }.isFailure)
        session.configure(backend.id) { }
        assertTrue(runCatching { session.generate("prompt", 2, expectedLoadId = "second").toList() }.isFailure)
        assertEquals(0, backend.generations)
    }

    @Test fun configurationCannotMutateAnActiveGeneration() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val backend = ContractBackend(events = flow { started.complete(Unit); finish.await(); emit(LocalGenerationEvent.Done) })
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path, loadId = "selected")
        val running = launch { session.generate("prompt", 2, expectedLoadId = "selected").toList() }
        withTimeout(1000) { started.await() }
        var mutated = false
        assertTrue(runCatching { session.configure(backend.id) { mutated = true } }.isFailure)
        assertFalse(mutated)
        finish.complete(Unit)
        running.join()
    }

    @Test fun failedConfigurationCannotLeaveAUsableRuntime() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path)
        assertTrue(runCatching { session.configure(backend.id) { error("invalid sampler") } }.isFailure)
        assertEquals(LocalRuntimePhase.ERROR, session.status.value.phase)
        assertTrue(runCatching { session.generate("prompt", 2).toList() }.isFailure)
    }

    @Test fun oldConsumersCannotUnloadAReplacementModel() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path, loadId = "first")
        session.load(backend.id, model().path, loadId = "second")
        val unloaded = backend.unloads
        assertFalse(session.unloadIfCurrent("first"))
        assertEquals(unloaded, backend.unloads)
        assertEquals("second", session.status.value.loadId)
        assertTrue(session.unloadIfCurrent("second"))
        assertEquals(LocalRuntimePhase.EMPTY, session.status.value.phase)
    }

    @Test fun generationCleanupRunsBeforeTheGuardIsReleased() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path)
        var cleaned = false
        session.generate("prompt", 2, cleanup = {
            assertEquals(LocalRuntimePhase.GENERATING, session.status.value.phase)
            assertTrue(runCatching { session.configure(backend.id) { error("must not enter") } }.isFailure)
            cleaned = true
        }).toList()
        assertTrue(cleaned)
        assertEquals(LocalRuntimePhase.READY, session.status.value.phase)
    }

    @Test fun unknownRuntimeDoesNotSelectADefault() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        assertTrue(runCatching { session.load("unknown", model().path) }.isFailure)
        assertEquals(0, backend.loads)
    }

    @Test fun invalidOrEscapedPathsAreRejectedBeforeLoad() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(File(temp.root, "owned")))
        for (path in listOf("content://document/1", "relative.gguf", model().path)) {
            assertTrue(runCatching { session.load(backend.id, path) }.isFailure)
        }
        assertEquals(0, backend.loads)
        val bad = temp.newFile().apply { writeText("not GGUF") }
        val local = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        assertTrue(runCatching { local.load(backend.id, bad.path) }.isFailure)
        assertEquals(0, backend.loads)
    }

    @Test fun lifecycleStreamsRealEventsThenUnloadsAndReloads() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        val file = model()
        assertEquals("model-info", session.load(backend.id, file.path))
        assertEquals(LocalRuntimePhase.READY, session.status.value.phase)
        assertEquals(listOf(LocalGenerationEvent.Token("hello"), LocalGenerationEvent.Done), session.generate("prompt", 8).toList())
        assertEquals(LocalRuntimePhase.READY, session.status.value.phase)
        session.cancel()
        assertEquals(1, backend.cancels)
        session.unload()
        assertEquals(1, backend.unloads)
        assertNull(session.status.value.loadedRuntime)
        assertTrue(runCatching { session.generate("prompt", 8).toList() }.isFailure)
        session.load(backend.id, file.path)
        assertEquals(2, backend.loads)
    }

    @Test fun brokenStreamHasOneErrorAndNoInventedCompletion() = runBlocking {
        val backend = ContractBackend(events = flow { emit(LocalGenerationEvent.Token("partial")); error("broken") })
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path)
        val events = session.generate("prompt", 8).toList()
        assertEquals(LocalGenerationEvent.Token("partial"), events.first())
        assertEquals(LocalGenerationEvent.Error("broken"), events.last())
        assertFalse(events.contains(LocalGenerationEvent.Done))
        assertEquals(1, events.count { it is LocalGenerationEvent.Error })
    }

    @Test fun coroutineCancellationPropagatesToBackendWithoutFakeDone() = runBlocking {
        val backend = ContractBackend(events = flow { throw CancellationException("cancel") })
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path)
        assertTrue(runCatching { session.generate("prompt", 8).toList() }.exceptionOrNull() is CancellationException)
        assertEquals(1, backend.cancels)
        assertEquals(LocalRuntimePhase.READY, session.status.value.phase)
    }

    @Test fun failedLoadReleasesPartialNativeState() = runBlocking {
        val backend = ContractBackend(loadFailure = IllegalStateException("partial load"))
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        assertTrue(runCatching { session.load(backend.id, model().path) }.isFailure)
        assertEquals(1, backend.unloads)
        assertNull(session.status.value.loadedRuntime)
    }

    @Test fun consumerFailuresAreNotRewrittenAsEngineEvents() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path)
        val failure = runCatching { session.generate("prompt", 8).collect { error("consumer disappeared") } }.exceptionOrNull()
        assertEquals("consumer disappeared", failure!!.message)
    }

    @Test fun unloadCancelsGenerationBeforeWaitingForTheSessionLock() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val backend = ContractBackend(events = flow { started.complete(Unit); cancelled.await(); emit(LocalGenerationEvent.Done) },
            onCancel = { cancelled.complete(Unit) })
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path)
        val generation = launch { session.generate("prompt", 8).toList() }
        started.await()
        withTimeout(1000) { session.unload() }
        generation.join()
        assertNull(session.status.value.loadedRuntime)
        assertEquals(1, backend.unloads)
    }

    @Test fun rejectedDescriptorSelectionDisablesThePreviouslyLoadedRuntime() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend, CovertEngineBackend()), listOf(temp.root))
        session.load(backend.id, model().path)
        assertTrue(runCatching { session.loadLegacyDescriptor(CovertEngineBackend.ID, -1, LocalLoadOptions()) }.isFailure)
        assertEquals(CovertEngineBackend.ID, session.status.value.requestedRuntime)
        assertEquals(LocalRuntimePhase.ERROR, session.status.value.phase)
        assertTrue(runCatching { session.generate("prompt", 8).toList() }.isFailure)
        assertEquals(0, backend.generations)
    }

    @Test fun concurrentGenerationIsRejectedInsteadOfQueuedAfterStop() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val backend = ContractBackend(events = flow { started.complete(Unit); cancelled.await(); emit(LocalGenerationEvent.Done) },
            onCancel = { cancelled.complete(Unit) })
        val session = LocalRuntimeSession(listOf(backend), listOf(temp.root))
        session.load(backend.id, model().path)
        val generation = launch { session.generate("first", 8).toList() }
        started.await()
        val failure = runCatching { withTimeout(1000) { session.generate("second", 8).toList() } }.exceptionOrNull()
        session.cancel()
        generation.join()
        assertEquals(IllegalStateException::class.java, failure?.javaClass)
        assertEquals(1, backend.generations)
    }

    @Test fun unavailableDescriptorSelectionNeverTransfersOwnership() = runBlocking {
        var acquired = false
        val session = LocalRuntimeSession(listOf(CovertEngineBackend()), listOf(temp.root))
        assertTrue(runCatching {
            session.loadLegacyDescriptor(CovertEngineBackend.ID, LocalLoadOptions()) { acquired = true; 42 }
        }.isFailure)
        assertFalse(acquired)
        assertEquals(CovertEngineBackend.ID, session.status.value.requestedRuntime)
    }

    @Test fun legacyGenerationCannotBypassAFailedSelection() = runBlocking {
        val backend = ContractBackend()
        val session = LocalRuntimeSession(listOf(backend, CovertEngineBackend()), listOf(temp.root))
        session.load(backend.id, model().path)
        runCatching { session.load(CovertEngineBackend.ID, model().path) }
        var invoked = false
        val failure = runCatching {
            session.legacyGeneration(backend.id) { invoked = true; flow { emit("legacy-token") } }.toList()
        }
        assertTrue(failure.isFailure)
        assertFalse(invoked)
    }

    private class ContractBackend(
        val events: Flow<LocalGenerationEvent> = flow { emit(LocalGenerationEvent.Token("hello")); emit(LocalGenerationEvent.Done) },
        val loadFailure: Exception? = null,
        val onCancel: () -> Unit = {},
    ) : LocalModelBackend {
        override val id = "contract-double"
        override val availability = LocalBackendAvailability(true)
        var loads = 0; var unloads = 0; var generations = 0; var cancels = 0
        override suspend fun load(file: File, options: LocalLoadOptions): String { loads++; loadFailure?.let { throw it }; return "model-info" }
        override fun generate(prompt: String, maxTokens: Int, multiTurn: Boolean): Flow<LocalGenerationEvent> { generations++; return events }
        override fun cancel() { cancels++; onCancel() }
        override suspend fun unload() { unloads++ }
    }
}
