package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.*
import com.dark.tool_neuron.studio.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StudioDraftTest {
    @get:Rule val temp = TemporaryFolder()
    class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        var reject = false
        override fun load() = bytes
        override fun save(bytes: ByteArray) { check(!reject); this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    @Test fun draftsSurviveRestartInTheirOwnCanonicalProject() {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        val a = core.createWorkspace("A").getOrThrow()
        val b = core.createWorkspace("B").getOrThrow()
        assertTrue(core.saveVibeDraft(a.id, VibeDraft("index.html", "unsaved", 1)).isFailure)
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        core.saveVibeDraft(a.id, VibeDraft("index.html", "unsaved", 1)).getOrThrow()
        val resumed = LaptopCore(store, temp.root).state.value
        assertEquals("unsaved", resumed.workspaces.first().vibeDraft!!.text)
        assertNull(resumed.workspaces.last().vibeDraft)
        assertEquals(b.id, resumed.activeWorkspaceId)
        store.reject = true
        assertTrue(core.saveVibeDraft(a.id, null).isFailure)
        assertNotNull(core.state.value.workspaces.first().vibeDraft)
    }
    @Test fun obsoleteDraftGenerationCannotPublishAfterDiscard() {
        val core = LaptopCore(Store(), temp.root)
        val a = core.createWorkspace("A").getOrThrow()
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        assertTrue(core.saveVibeDraft(a.id, VibeDraft("index.html", "discarded", 1)) { false }.isFailure)
        assertNull(core.state.value.workspaces.single().vibeDraft)
        assertFalse(StudioDocumentQueue().completeArchive("content://interrupted"))
        assertFalse(StudioDocumentQueue().completeCapture("content://interrupted"))
    }
    @Test fun draftTraversalAndOversizeCannotEnterTheVault() {
        val core = LaptopCore(Store(), temp.root)
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        val a = core.createWorkspace("A").getOrThrow()
        assertTrue(core.saveVibeDraft(a.id, VibeDraft("../private", "text", 1)).isFailure)
        assertTrue(core.saveVibeDraft(a.id, VibeDraft("index.html", "x".repeat(128001), 1)).isFailure)
        assertNull(core.state.value.workspaces.single().vibeDraft)
    }
}
