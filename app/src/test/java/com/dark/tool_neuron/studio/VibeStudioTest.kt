package com.dark.tool_neuron.studio

import com.dark.tool_neuron.laptop.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class VibeStudioTest {
    @get:Rule val temp = TemporaryFolder()
    private class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        override fun load() = bytes
        override fun save(bytes: ByteArray) { this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    private fun fixture(grant: Boolean = true): Triple<LaptopCore, VibeStudio, Store> {
        val store = Store()
        val core = LaptopCore(store, File(temp.root, "projects"))
        if (grant) core.setPermission(VibeStudio.CAPABILITY, true).getOrThrow()
        return Triple(core, VibeStudio(core, File(temp.root, "projects")), store)
    }

    @Test fun deniedStudioDoesNotCreateAProjectOrWriteFiles() {
        val (core, studio) = fixture(false)
        assertTrue(runCatching { studio.create("sample", "web") }.isFailure)
        assertTrue(core.state.value.workspaces.isEmpty())
        assertFalse(File(temp.root, "projects").exists())
    }

    @Test fun templatesBuildInTheCanonicalProjectAndSurviveRestart() {
        val (core, studio, store) = fixture()
        for (template in VibeTemplates.all) {
            val ws = studio.create("test <script>", template.id)
            assertEquals(ws.id, core.state.value.activeWorkspaceId)
            assertFalse(studio.read(ws.id, "index.html").contains("<title>test <script>"))
            val build = studio.build(ws.id)
            assertEquals(Verdict.PASS, build.verdict)
            assertTrue(build.checks.all { it.passed })
            assertTrue(core.state.value.evidence.any { it.id == build.evidenceId })
            assertTrue(core.state.value.verifications.any { it.evidenceId == build.evidenceId && it.verdict == Verdict.PASS })
        }
        val restarted = LaptopCore(store, File(temp.root, "projects"))
        assertEquals(core.state.value.workspaces, restarted.state.value.workspaces)
        assertEquals(core.state.value.vibeBuilds, restarted.state.value.vibeBuilds)
        assertEquals(VibeTemplates.all.size, restarted.state.value.workspaces.size)
    }

    @Test fun fileTraversalSymlinkEscapeAndOversizedEditsAreDenied() {
        val (_, studio) = fixture()
        val ws = studio.create("safe", "web")
        for (path in listOf("../outside", "/etc/passwd", ".env", "a/../../outside", "a\\outside")) {
            assertTrue(runCatching { studio.save(ws.id, path, "bad") }.isFailure)
        }
        val outside = temp.newFolder("outside")
        java.nio.file.Files.createSymbolicLink(File(temp.root, "projects/${ws.id}/src/escape").toPath(), outside.toPath())
        assertTrue(runCatching { studio.save(ws.id, "escape/payload.js", "bad") }.isFailure)
        assertFalse(File(outside, "payload.js").exists())
        assertTrue(runCatching { studio.save(ws.id, "big.js", "x".repeat(600_000)) }.isFailure)
    }

    @Test fun failedStructuralChecksCannotProducePreviewOrExport() {
        val (core, studio) = fixture()
        val ws = studio.create("broken", "web")
        studio.save(ws.id, "index.html", "<html><script src=\"missing.js\"></script></html>")
        val build = studio.build(ws.id)
        assertEquals(Verdict.FAIL, build.verdict)
        assertTrue(build.checks.any { !it.passed })
        assertTrue(runCatching { studio.preview(ws.id, build.id, "index.html") }.isFailure)
        assertTrue(runCatching { studio.export(ws.id, build.id, ByteArrayOutputStream()) }.isFailure)
        assertTrue(core.state.value.verifications.any { it.evidenceId == build.evidenceId && it.verdict == Verdict.FAIL })
    }

    @Test fun exportsContainOnlyTheVerifiedSnapshotAndHashManifest() {
        val (_, studio) = fixture()
        val ws = studio.create("export", "web")
        val original = studio.read(ws.id, "index.html")
        val build = studio.build(ws.id)
        studio.save(ws.id, "index.html", "<html>new draft</html>")
        assertEquals(original, studio.preview(ws.id, build.id, "index.html").toString(Charsets.UTF_8))
        val output = ByteArrayOutputStream()
        studio.export(ws.id, build.id, output)
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; files[entry.name] = zip.readBytes() }
        }
        assertEquals(build.files.map { "site/${it.path}" }.toSet() + "manifest.json", files.keys)
        build.files.forEach { artifact ->
            val hash = MessageDigest.getInstance("SHA-256").digest(files.getValue("site/${artifact.path}")).joinToString("") { "%02x".format(it) }
            assertEquals(artifact.sha256, hash)
        }
    }

    @Test fun cssImportsAndUnsupportedEmbeddedDependenciesDoNotPassSilently() {
        val (_, studio) = fixture()
        val ws = studio.create("dependencies", "web")
        studio.save(ws.id, "style.css", "@import \"missing.css\";")
        assertEquals(Verdict.FAIL, studio.build(ws.id).verdict)
        studio.save(ws.id, "style.css", "body{color:white}")
        studio.save(ws.id, "index.html", "<html><img srcset=\"missing.png 1x\"></html>")
        assertEquals(Verdict.FAIL, studio.build(ws.id).verdict)
    }

    @Test fun malformedArtifactRecordsCannotEncodeUnsafeExportPaths() {
        assertThrows(IllegalArgumentException::class.java) { VibeArtifactFile("../escape", "a".repeat(64), 1) }
        assertThrows(IllegalArgumentException::class.java) { VibeArtifactFile("index.html", "invalid", 1) }
        assertThrows(IllegalArgumentException::class.java) { VibeArtifactFile("index.html", "a".repeat(64), -1) }
    }

    @Test fun tamperedBuildAndRevokedPermissionCannotBeExported() {
        val (core, studio) = fixture()
        val ws = studio.create("guard", "web")
        val build = studio.build(ws.id)
        File(temp.root, "projects/${ws.id}/.vibe/${build.id}/index.html").writeText("tampered")
        assertTrue(runCatching { studio.export(ws.id, build.id, ByteArrayOutputStream()) }.isFailure)
        core.setPermission(VibeStudio.CAPABILITY, false).getOrThrow()
        assertTrue(runCatching { studio.read(ws.id, "index.html") }.isFailure)
        assertTrue(runCatching { studio.save(ws.id, "index.html", "replacement") }.isFailure)
        assertTrue(runCatching { studio.build(ws.id) }.isFailure)
    }
}
