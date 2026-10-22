package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.*
import com.dark.tool_neuron.util.VaultRecovery
import com.dark.tool_neuron.util.WorkspacePaths
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class RecoveryTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun failedStorageReadNeverSeedsOrOverwrites() {
        var saves = 0
        val store = object : LaptopPersistence {
            override fun load(): ByteArray? = error("storage unavailable")
            override fun save(bytes: ByteArray) { saves++ }
            override fun quarantineCorrupt(bytes: ByteArray) = Unit
        }
        assertTrue(runCatching { LaptopCore(store, temp.root) }.isFailure)
        assertEquals(0, saves)
    }
    @Test fun failedQuarantineCannotOverwriteCorruptState() {
        var saves = 0
        val store = object : LaptopPersistence {
            override fun load() = "broken".toByteArray()
            override fun save(bytes: ByteArray) { saves++ }
            override fun quarantineCorrupt(bytes: ByteArray) { error("preservation failed") }
        }
        assertTrue(runCatching { LaptopCore(store, temp.root) }.isFailure)
        assertEquals(0, saves)
    }
    @Test fun encryptedVaultMovesWithoutDeletingBytes() {
        val vault = temp.newFolder("vault")
        File(vault, "opaque").writeBytes(byteArrayOf(0, 1, 2, 3))
        val preserved = VaultRecovery.preserve(vault)
        assertFalse(vault.exists())
        assertArrayEquals(byteArrayOf(0, 1, 2, 3), File(preserved, "opaque").readBytes())
        assertTrue(preserved.name.startsWith("vault.quarantine-"))
    }
    @Test fun retiredBatteryHistoryCannotResetTheWorkspaceVault() {
        val scope = QualificationScope("a".repeat(64), "retired-runtime", "old-device")
        val run = ModelLabRun("historic", scope, "retired.battery@1", listOf(ModelLabObservation("old-check", "Original prompt", "Original expectation", "observed", true, null, false, QualificationVerdict.PASS)), 1, 2)
        val state = CipherLaptop(workspaces = listOf(WorkspaceRef("ws-one", "Preserved", 1)), modelLabRuns = listOf(run))
        val decoded = LaptopCodec.decode(LaptopCodec.encode(state))
        assertEquals("Preserved", decoded.workspaces.single().name)
        assertEquals(run, decoded.modelLabRuns.single())
        assertTrue(runCatching { run.validate() }.isFailure)
    }
    @Test fun workspaceAliasesCannotReadSiblingProjects() {
        val base = temp.newFolder("projects")
        val real = File(base, "ws-b").apply { mkdirs() }
        Files.createSymbolicLink(File(base, "ws-a").toPath(), real.toPath())
        assertTrue(runCatching { WorkspacePaths.root(base, "ws-a", setOf("ws-a", "ws-b")) }.isFailure)
        assertEquals(real.canonicalFile, WorkspacePaths.root(base, "ws-b", setOf("ws-b")))
        assertTrue(runCatching { WorkspacePaths.root(base, "../ws-b", setOf("../ws-b")) }.isFailure)
    }
}
