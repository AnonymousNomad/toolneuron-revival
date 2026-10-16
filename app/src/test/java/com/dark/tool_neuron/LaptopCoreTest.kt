package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.CipherLaptop
import com.dark.tool_neuron.laptop.EvidenceKind
import com.dark.tool_neuron.laptop.LaptopCodec
import com.dark.tool_neuron.laptop.LaptopCore
import com.dark.tool_neuron.laptop.LaptopPersistence
import com.dark.tool_neuron.laptop.TaskStatus
import com.dark.tool_neuron.laptop.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FakePersistence : LaptopPersistence {
    var payload: ByteArray? = null
    var quarantined: ByteArray? = null
    override fun load(): ByteArray? = payload
    override fun save(bytes: ByteArray) {
        payload = bytes
    }

    override fun quarantineCorrupt(bytes: ByteArray) {
        quarantined = bytes
    }
}

class LaptopCoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private var clock = 1000L
    private fun core(p: LaptopPersistence) = LaptopCore(p, File(tmp.root, "workspaces"), nowMs = { clock })

    @Test
    fun freshStateSeedsAndPersists() {
        val p = FakePersistence()
        val c = core(p)
        val s = c.state.value
        assertEquals("cipher-laptop/1", s.format)
        assertEquals(3, s.knowledge.size)
        assertEquals(2, s.procedures.size)
        assertEquals(1L, s.session.openCount)
        assertNotNull(p.payload)
        assertEquals(s, LaptopCodec.decode(p.payload!!))
    }

    @Test
    fun workspaceLifecycleAndFileListing() {
        val p = FakePersistence()
        val c = core(p)
        val ws = c.createWorkspace("My Project").getOrThrow()
        assertEquals("My Project", ws.name)
        assertEquals(ws.id, c.state.value.activeWorkspaceId)
        val dir = File(tmp.root, "workspaces/${ws.id}")
        assertTrue(dir.isDirectory)
        assertTrue(File(dir, "README.md").isFile)

        File(dir, "src").mkdirs()
        File(dir, "src/main.kt").writeText("fun main() {}")
        val files = c.listWorkspaceFiles().getOrThrow()
        assertTrue(files.any { it.path == "README.md" && !it.isDir })
        assertTrue(files.any { it.path == "src" && it.isDir })
        assertTrue(files.any { it.path == "src/main.kt" && it.bytes > 0 })

        val ws2 = c.createWorkspace("Second").getOrThrow()
        assertEquals(ws2.id, c.state.value.activeWorkspaceId)
        c.openWorkspace(ws.id).getOrThrow()
        assertEquals(ws.id, c.state.value.activeWorkspaceId)
        assertTrue(c.openWorkspace("nope").isFailure)
    }

    @Test
    fun restartRecoveryPreservesState() {
        val p = FakePersistence()
        val c1 = core(p)
        val ws = c1.createWorkspace("Persisted").getOrThrow()
        val task = c1.createTask("do the thing", "dev.workspace.inspect@1").getOrThrow()
        c1.transitionTask(task.id, TaskStatus.PLANNED).getOrThrow()

        clock = 5000L
        val c2 = core(p)
        val s2 = c2.state.value
        assertEquals(2L, s2.session.openCount)
        assertEquals(ws.id, s2.activeWorkspaceId)
        assertEquals(1, s2.tasks.size)
        assertEquals(TaskStatus.PLANNED, s2.tasks[0].status)
        assertEquals(5000L, s2.session.lastOpenMs)
    }

    @Test
    fun corruptStateRecoversExplicitlyAndQuarantines() {
        val p = FakePersistence()
        p.payload = "{definitely not json".toByteArray()
        val c = core(p)
        val s = c.state.value
        assertTrue(s.session.recoveredFromCorruption)
        assertNotNull(s.session.recoveryReason)
        assertEquals("cipher-laptop/1", s.format)
        assertNotNull(p.quarantined)
        assertEquals("{definitely not json", p.quarantined!!.toString(Charsets.UTF_8))
        assertEquals(s, LaptopCodec.decode(p.payload!!))
    }

    @Test
    fun formatMismatchRecovers() {
        val p = FakePersistence()
        val bad = CipherLaptop(format = "cipher-laptop/99")
        p.payload = LaptopCodec.encode(bad)
        val c = core(p)
        assertTrue(c.state.value.session.recoveredFromCorruption)
    }

    @Test
    fun taskJournalTransitionsAreDeterministic() {
        val c = core(FakePersistence())
        val t = c.createTask("objective", null).getOrThrow()
        assertEquals(TaskStatus.CREATED, t.status)

        assertTrue(c.transitionTask(t.id, TaskStatus.EXECUTING).isFailure)
        assertTrue(c.transitionTask(t.id, TaskStatus.SUCCEEDED).isFailure)

        c.transitionTask(t.id, TaskStatus.PLANNED).getOrThrow()
        c.transitionTask(t.id, TaskStatus.EXECUTING).getOrThrow()
        c.transitionTask(t.id, TaskStatus.VERIFYING).getOrThrow()
        val done = c.transitionTask(t.id, TaskStatus.SUCCEEDED, "result recorded").getOrThrow()
        assertEquals(TaskStatus.SUCCEEDED, done.status)
        assertEquals("result recorded", done.result)
        assertTrue(c.transitionTask(t.id, TaskStatus.CANCELLED).isFailure)

        val t2 = c.createTask("cancel me", null).getOrThrow()
        val cancelled = c.transitionTask(t2.id, TaskStatus.CANCELLED).getOrThrow()
        assertEquals(TaskStatus.CANCELLED, cancelled.status)

        assertTrue(c.transitionTask("task-missing", TaskStatus.PLANNED).isFailure)
    }

    @Test
    fun checkpointEvidenceAndVerificationAssociate() {
        val c = core(FakePersistence())
        val t = c.createTask("verify me", "dev.terminal.execute@1").getOrThrow()

        val cp = c.addCheckpoint(t.id, "after planning").getOrThrow()
        val ev = c.addEvidence(t.id, EvidenceKind.COMMAND_OUTPUT, "stub returned NOT_IMPLEMENTED").getOrThrow()
        val v = c.addVerification(
            taskId = t.id,
            requestedAction = "run stub",
            expectedResult = "command executes",
            verificationMethod = "stub result check",
            evidenceId = ev.id,
            verdict = Verdict.FAIL,
        ).getOrThrow()

        val task = c.state.value.tasks.first { it.id == t.id }
        assertTrue(cp.id in task.checkpointIds)
        assertTrue(ev.id in task.evidenceRefs)
        assertEquals(Verdict.FAIL, v.verdict)
        assertEquals(ev.id, v.evidenceId)

        assertTrue(c.addVerification(t.id, "x", "y", "z", "ev-missing", Verdict.PENDING).isFailure)
        assertTrue(c.addEvidence("task-missing", EvidenceKind.NOTE, "n").isFailure)
        assertTrue(c.addCheckpoint("task-missing", "l").isFailure)
    }

    @Test
    fun permissionsDefaultDenyAndRoundTrip() {
        val p = FakePersistence()
        val c = core(p)
        val ref = "dev.terminal.execute@1"
        assertFalse(c.hasPermission(ref))
        c.setPermission(ref, true).getOrThrow()
        assertTrue(c.hasPermission(ref))

        val c2 = core(p)
        assertTrue(c2.hasPermission(ref))
        c2.setPermission(ref, false).getOrThrow()
        assertFalse(c2.hasPermission(ref))
        assertTrue(c.setPermission("bogus@1", true).isFailure)
    }

    @Test
    fun createTaskRejectsUnknownCapability() {
        val c = core(FakePersistence())
        assertTrue(c.createTask("x", "nope.nope@9").isFailure)
        assertTrue(c.createTask("  ", null).isFailure)
    }
}
