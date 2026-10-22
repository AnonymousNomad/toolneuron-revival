package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectBindingTest {
    @get:Rule val temp = TemporaryFolder()
    private class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        var reject = false
        override fun load() = bytes
        override fun save(bytes: ByteArray) { check(!reject) { "write rejected" }; this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    @Test fun movingAConversationChangesOnlyItsCanonicalBinding() {
        val core = LaptopCore(Store(), temp.root)
        val first = core.createWorkspace("first").getOrThrow()
        val second = core.createWorkspace("second").getOrThrow()
        core.bindConversation(first.id, "chat-a").getOrThrow()
        core.bindConversation(first.id, "chat-b").getOrThrow()
        core.bindConversation(second.id, "chat-a").getOrThrow()
        assertEquals(listOf("chat-b"), core.state.value.workspaces.single { it.id == first.id }.conversationIds)
        assertEquals(second.id, core.projectForConversation("chat-a")!!.id)
        assertEquals(first.id, core.projectForConversation("chat-b")!!.id)
        assertEquals(second.id, core.state.value.activeWorkspaceId)
        assertEquals(2, temp.root.listFiles()!!.count { it.isDirectory })
        assertEquals("# first\n\nToolNeuron workspace.\n", File(temp.root, "${first.id}/README.md").readText())
    }
    @Test fun bindingsAndProjectTasksSurviveRestartWithoutDuplicatingProjects() {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        val project = core.createWorkspace("project").getOrThrow()
        core.bindConversation(project.id, "chat").getOrThrow()
        val task = core.createTask("bound task", null).getOrThrow()
        assertEquals(project.id, task.workspaceId)
        val restarted = LaptopCore(store, temp.root)
        assertEquals(project.id, restarted.projectForConversation("chat")!!.id)
        assertEquals(project.id, restarted.state.value.tasks.single().workspaceId)
        assertEquals(1, restarted.state.value.workspaces.size)
    }
    @Test fun unbindingAndClearingSelectionNeverDeletesFiles() {
        val core = LaptopCore(Store(), temp.root)
        val project = core.createWorkspace("project").getOrThrow()
        core.bindConversation(project.id, "chat").getOrThrow()
        core.unbindConversation("chat").getOrThrow()
        core.clearActiveWorkspace().getOrThrow()
        assertNull(core.projectForConversation("chat"))
        assertNull(core.state.value.activeWorkspaceId)
        assertTrue(File(temp.root, "${project.id}/README.md").isFile)
    }
    @Test fun invalidOrFailedBindingsDoNotPublishProposedState() {
        val store = Store()
        val core = LaptopCore(store, temp.root)
        val project = core.createWorkspace("project").getOrThrow()
        assertTrue(core.bindConversation("missing", "chat").isFailure)
        assertTrue(core.bindConversation(project.id, " ").isFailure)
        val before = core.state.value
        store.reject = true
        assertTrue(core.bindConversation(project.id, "chat").isFailure)
        assertEquals(before, core.state.value)
    }
    @Test fun duplicateConversationOwnersAndMissingTaskProjectsAreRejectedByTheCodec() {
        val a = WorkspaceRef("a", "A", 1, conversationIds = listOf("chat"))
        val b = WorkspaceRef("b", "B", 1, conversationIds = listOf("chat"))
        assertThrows(LaptopStateError::class.java) { LaptopCodec.validate(CipherLaptop(workspaces = listOf(a, b))) }
        val task = TaskRecord("t", "task", null, TaskStatus.CREATED, createdAtMs = 1, updatedAtMs = 1, workspaceId = "missing")
        assertThrows(LaptopStateError::class.java) { LaptopCodec.validate(CipherLaptop(tasks = listOf(task))) }
    }
    @Test fun residentLookupsStayInThePinnedProjectIncludingUnboundLegacyTasks() {
        val core = LaptopCore(Store(), temp.root)
        val legacy = core.createTask("legacy objective", null).getOrThrow()
        val a = core.createWorkspace("A").getOrThrow()
        val taskA = core.createTask("private A objective", null).getOrThrow()
        val b = core.createWorkspace("B").getOrThrow()
        val taskB = core.createTask("private B objective", null).getOrThrow()
        val lookup = com.dark.tool_neuron.laptop.resident.LaptopLookupService(core, { emptyList() }, { _, _ -> null }, { emptyList() })
        val request = com.dark.tool_neuron.laptop.resident.LaptopLookupRequest(com.dark.tool_neuron.laptop.resident.LookupKind.TASK_CURRENT)
        assertEquals(listOf(taskA.id), lookup.execute(request, a.id).refs)
        assertEquals(listOf(taskB.id), lookup.execute(request, b.id).refs)
        assertEquals(listOf(legacy.id), lookup.execute(request, null).refs)
        val workspace = lookup.execute(com.dark.tool_neuron.laptop.resident.LaptopLookupRequest(com.dark.tool_neuron.laptop.resident.LookupKind.WORKSPACE_CURRENT), a.id)
        assertEquals(listOf(a.id), workspace.refs)
    }

    @Test fun tasklessBuildEvidenceAndTaskEvidenceCannotCrossProjectLookup() {
        val a = WorkspaceRef("a", "A", 1)
        val b = WorkspaceRef("b", "B", 1)
        val task = TaskRecord("t", "B secret", null, TaskStatus.CREATED, createdAtMs = 1, updatedAtMs = 1, workspaceId = "b")
        val events = listOf(
            EvidenceRecord("ea", null, EvidenceKind.CHECK, "A build", listOf("workspace:a"), createdAtMs = 1),
            EvidenceRecord("eb", null, EvidenceKind.CHECK, "B build secret", listOf("workspace:b"), createdAtMs = 1),
            EvidenceRecord("et", "t", EvidenceKind.NOTE, "B task secret", createdAtMs = 1))
        val store = Store().apply { bytes = LaptopCodec.encode(CipherLaptop(workspaces = listOf(a, b), tasks = listOf(task), evidence = events)) }
        val core = LaptopCore(store, temp.root)
        val lookup = com.dark.tool_neuron.laptop.resident.LaptopLookupService(core, { emptyList() }, { _, _ -> null }, { emptyList() })
        val result = lookup.execute(com.dark.tool_neuron.laptop.resident.LaptopLookupRequest(com.dark.tool_neuron.laptop.resident.LookupKind.EVIDENCE_FIND), "a")
        assertEquals(listOf("ea"), result.refs)
        assertFalse(result.summary.contains("secret"))
        assertTrue(lookup.execute(com.dark.tool_neuron.laptop.resident.LaptopLookupRequest(com.dark.tool_neuron.laptop.resident.LookupKind.EVIDENCE_FIND), null).refs.isEmpty())
    }

}
