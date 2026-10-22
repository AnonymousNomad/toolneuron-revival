package com.dark.tool_neuron.studio

import com.dark.tool_neuron.laptop.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VibeInitializationTest {
    @get:Rule val temp = TemporaryFolder()
    private class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        override fun load() = bytes
        override fun save(bytes: ByteArray) { this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    @Test fun templateInitializationUsesTheExistingCanonicalProject() {
        val core = LaptopCore(Store(), temp.root)
        val project = core.createWorkspace("existing").getOrThrow()
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        val studio = VibeStudio(core, temp.root)
        assertFalse(studio.hasSources(project.id))
        val readme = File(temp.root, "${project.id}/README.md").readBytes()
        studio.initialize(project.id, "utility")
        assertTrue(studio.hasSources(project.id))
        assertEquals(1, core.state.value.workspaces.size)
        assertEquals(project.id, core.state.value.workspaces.single().id)
        assertArrayEquals(readme, File(temp.root, "${project.id}/README.md").readBytes())
        assertEquals(Verdict.PASS, studio.build(project.id).verdict)
    }
    @Test fun existingSourcesAndOtherProjectFilesCannotBeOverwrittenByInitialization() {
        val core = LaptopCore(Store(), temp.root)
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        val studio = VibeStudio(core, temp.root)
        val project = studio.create("keep", "web")
        studio.save(project.id, "app.js", "user source")
        assertTrue(runCatching { studio.initialize(project.id, "game") }.isFailure)
        assertEquals("user source", studio.read(project.id, "app.js"))
        assertTrue(File(temp.root, project.id).listFiles()!!.none { it.name.startsWith(".vibe-init-") })
    }
    @Test fun deniedOrUnknownInitializationDoesNotPublishSource() {
        val core = LaptopCore(Store(), temp.root)
        val project = core.createWorkspace("denied").getOrThrow()
        val studio = VibeStudio(core, temp.root)
        assertTrue(runCatching { studio.initialize(project.id, "web") }.isFailure)
        assertFalse(File(temp.root, "${project.id}/src").exists())
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        assertTrue(runCatching { studio.initialize("missing", "web") }.isFailure)
        assertTrue(runCatching { studio.initialize(project.id, "unknown") }.isFailure)
        assertFalse(File(temp.root, "${project.id}/src").exists())
    }
    @Test fun liveAndDanglingSourceSymlinksCannotRedirectInitialization() {
        val core = LaptopCore(Store(), temp.root)
        core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        val project = core.createWorkspace("project").getOrThrow()
        val other = core.createWorkspace("other").getOrThrow()
        val destination = File(temp.root, "${other.id}/src").apply { mkdirs() }
        val link = File(temp.root, "${project.id}/src").toPath()
        for (target in listOf(destination, File(temp.root, "absent-source"))) {
            java.nio.file.Files.createSymbolicLink(link, target.toPath())
            assertTrue(runCatching { VibeStudio(core, temp.root).initialize(project.id, "web") }.isFailure)
            assertTrue(runCatching { VibeStudio(core, temp.root).hasSources(project.id) }.isFailure)
            assertTrue(destination.listFiles()!!.isEmpty())
            java.nio.file.Files.delete(link)
        }
    }

}
