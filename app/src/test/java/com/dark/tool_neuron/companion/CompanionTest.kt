package com.dark.tool_neuron.companion

import com.dark.tool_neuron.laptop.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CompanionTest {
    @get:Rule val temp = TemporaryFolder()
    class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        var reject = false
        override fun load() = bytes
        override fun save(bytes: ByteArray) { check(!reject); this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    @Test fun sourcesRemainCanonicalAndUnknownRightsNeverBecomePlayable() {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        val project = core.createWorkspace("Companion").getOrThrow()
        assertTrue(core.addMediaReference(project.id, "Unknown", "https://example.org", MediaSourceClass.UNKNOWN, "").isFailure)
        core.setPermission(CompanionPlan.CAPABILITY, true).getOrThrow()
        core.addMediaReference(project.id, "Unknown", "https://example.org", MediaSourceClass.UNKNOWN, "").getOrThrow()
        val ref = core.state.value.workspaces.single().companion!!.media.single()
        assertFalse(ref.eligible)
        assertFalse(CompanionAdapters.playback.available)
        val resumed = LaptopCore(store, temp.root).state.value
        assertEquals(project.id, resumed.workspaces.single().id)
        assertEquals(ref, resumed.workspaces.single().companion!!.media.single())
    }
    @Test fun rightsAreDeclarationsAndFailureCannotPublishOrLeak() {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        core.setPermission(CompanionPlan.CAPABILITY, true).getOrThrow()
        val first = core.createWorkspace("A").getOrThrow()
        val second = core.createWorkspace("B").getOrThrow()
        core.addMediaReference(first.id, "Owned", "content://owner/video", MediaSourceClass.LOCAL_OWNED, "Personal recording").getOrThrow()
        assertTrue(core.state.value.workspaces.first().companion!!.media.single().eligible)
        assertNull(core.state.value.workspaces.last().companion)
        val before = core.state.value
        store.reject = true
        assertTrue(core.addMediaReference(second.id, "Other", "x", MediaSourceClass.PUBLIC_DOMAIN, "Declared PD").isFailure)
        assertEquals(before, core.state.value)
    }
    @Test fun gameIntentsCannotDriveFrameInputsOrUnpairedDevices() {
        val adapter = CompanionAdapters.game
        assertFalse(adapter.available)
        assertTrue(adapter.submit(GameIntent("workspace", "game", "strategy", "Protect the base")).isFailure)
        assertTrue(runCatching { GameIntent("workspace", "game", "frame_input", "press A") }.isFailure)
        assertFalse(CompanionAdapters.bridge.available)
        assertTrue(CompanionAdapters.bridge.send(DeviceIdentity("device", "Laptop", "unverified"), GameIntent("w", "g", "strategy", "defend")).isFailure)
    }
}
