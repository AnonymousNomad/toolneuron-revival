package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.CipherLaptop
import com.dark.tool_neuron.laptop.LaptopCodec
import com.dark.tool_neuron.laptop.LaptopCore
import com.dark.tool_neuron.laptop.LaptopPersistence
import com.dark.tool_neuron.laptop.WorkstationApp
import com.dark.tool_neuron.laptop.WorkstationSession
import com.dark.tool_neuron.laptop.WorkstationWindow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class WorkstationStateTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun reopeningAnAppRestoresOneWindowAndFocus() {
        val state = WorkstationSession().open(WorkstationApp.CHAT).open(WorkstationApp.FILES)
            .minimize(WorkstationApp.CHAT).open(WorkstationApp.CHAT)
        assertEquals(2, state.windows.size)
        assertEquals(WorkstationApp.CHAT, state.focused)
        assertFalse(state.windows.first { it.app == WorkstationApp.CHAT }.minimized)
    }

    @Test fun minimizeAndCloseChooseTheMostRecentVisibleWindow() {
        var state = WorkstationSession().open(WorkstationApp.CHAT).open(WorkstationApp.FILES)
        state = state.minimize(WorkstationApp.FILES)
        assertEquals(WorkstationApp.CHAT, state.focused)
        state = state.close(WorkstationApp.CHAT)
        assertNull(state.focused)
        assertEquals(listOf(WorkstationWindow(WorkstationApp.FILES, true)), state.windows)
        state = state.open(WorkstationApp.FILES).close(WorkstationApp.FILES)
        assertEquals(WorkstationSession(), state)
    }

    @Test fun invalidFocusedOrDuplicateWindowsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkstationSession(listOf(WorkstationWindow(WorkstationApp.CHAT), WorkstationWindow(WorkstationApp.CHAT)))
        }
        assertThrows(IllegalArgumentException::class.java) { WorkstationSession(focused = WorkstationApp.CHAT) }
        assertThrows(IllegalArgumentException::class.java) {
            WorkstationSession(listOf(WorkstationWindow(WorkstationApp.CHAT, true)), WorkstationApp.CHAT)
        }
    }

    @Test fun oldLaptopPayloadsDefaultToAnEmptyWorkstation() {
        val old = LaptopCodec.decode("""{"format":"cipher-laptop/1"}""".toByteArray())
        assertEquals(WorkstationSession(), old.workstation)
        val current = old.copy(workstation = old.workstation.open(WorkstationApp.CHAT))
        assertEquals(current, LaptopCodec.decode(LaptopCodec.encode(current)))
    }

    @Test fun restartRestoresWindowsWithoutDuplicatingCanonicalProjects() {
        val store = MemoryStore()
        val root = File(temp.root, "projects")
        val first = LaptopCore(store, root)
        val project = first.createWorkspace("canonical").getOrThrow()
        first.openWorkstationApp(WorkstationApp.CHAT).getOrThrow()
        first.openWorkstationApp(WorkstationApp.FILES).getOrThrow()
        first.minimizeWorkstationApp(WorkstationApp.CHAT).getOrThrow()
        val second = LaptopCore(store, root)
        assertEquals(first.state.value.workstation, second.state.value.workstation)
        assertEquals(listOf(project), second.state.value.workspaces)
        assertEquals(project.id, second.state.value.activeWorkspaceId)
        assertEquals(1, root.listFiles()!!.count { it.isDirectory })
    }

    @Test fun concurrentWindowOpensDoNotLoseOtherApps() {
        val core = LaptopCore(MemoryStore(), File(temp.root, "projects"))
        val start = CountDownLatch(1)
        val workers = WorkstationApp.entries.map { app ->
            thread {
                start.await()
                core.openWorkstationApp(app).getOrThrow()
            }
        }
        start.countDown()
        workers.forEach { it.join() }
        assertEquals(WorkstationApp.entries.toSet(), core.state.value.workstation.windows.map { it.app }.toSet())
    }

    private class MemoryStore : LaptopPersistence {
        var bytes: ByteArray? = null
        override fun load() = bytes
        override fun save(bytes: ByteArray) { this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
}
