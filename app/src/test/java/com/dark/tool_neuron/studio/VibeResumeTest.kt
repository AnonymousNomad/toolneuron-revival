package com.dark.tool_neuron.studio

import com.dark.tool_neuron.laptop.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VibeResumeTest {
    @get:Rule val temp = TemporaryFolder()
    private class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        var reject = false
        override fun load() = bytes
        override fun save(bytes: ByteArray) { check(!reject); this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    @Test fun sourceResumeLivesOnTheExistingProjectAndSurvivesRestart() {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        val first = core.createWorkspace("first").getOrThrow()
        core.createWorkspace("second").getOrThrow()
        core.rememberVibeSource(first.id, "scripts/app.js").getOrThrow()
        val restarted = LaptopCore(store, temp.root)
        assertEquals(first.id, restarted.state.value.activeWorkspaceId)
        assertEquals("scripts/app.js", restarted.state.value.workspaces.first().metadata["vibe.lastSource"])
        assertEquals(2, restarted.state.value.workspaces.size)
    }
    @Test fun deniedInvalidAndFailedResumeWritesPreservePriorSelectionAndFiles() {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        val ws = core.createWorkspace("project").getOrThrow()
        assertTrue(core.rememberVibeSource(ws.id, "index.html").isFailure)
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        for (path in listOf("../secret", "/absolute", ".vibe/build", "a//b", "a/../b")) assertTrue(core.rememberVibeSource(ws.id, path).isFailure)
        assertTrue(core.rememberVibeSource("missing", "index.html").isFailure)
        val before = core.state.value
        store.reject = true
        assertTrue(core.rememberVibeSource(ws.id, "index.html").isFailure)
        assertEquals(before, core.state.value)
        assertTrue(File(temp.root, "${ws.id}/README.md").isFile)
    }
    @Test fun aFailedNewBuildNeverChangesAnEarlierPassingSnapshot() {
        val core = LaptopCore(Store(), temp.root)
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        val studio = VibeStudio(core, temp.root)
        val project = studio.create("recover", "web")
        val pass = studio.build(project.id)
        val original = studio.preview(project.id, pass.id, "index.html")
        studio.save(project.id, "index.html", "broken")
        val failed = studio.build(project.id)
        assertEquals(Verdict.FAIL, failed.verdict)
        assertArrayEquals(original, studio.preview(project.id, pass.id, "index.html"))
        assertTrue(runCatching { studio.preview(project.id, failed.id, "index.html") }.isFailure)
    }
}
