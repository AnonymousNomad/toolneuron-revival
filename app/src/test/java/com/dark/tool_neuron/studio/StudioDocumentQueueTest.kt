package com.dark.tool_neuron.studio

import org.junit.Assert.*
import org.junit.Test

class StudioDocumentQueueTest {
    @Test fun pickerResultWaitsForUnlockAndKeepsItsOriginalBuild() {
        val queue = StudioDocumentQueue()
        val original = StudioBuildTarget("workspace-a", "build-a")
        queue.prepareArchive(original)
        queue.completeArchive("content://destination/document")
        assertNull(queue.take(unlocked = false, granted = true, busy = false))
        assertNull(queue.take(unlocked = true, granted = true, busy = true))
        val request = queue.take(unlocked = true, granted = true, busy = false) as StudioDocumentWork.Archive
        assertEquals(original, request.target)
        assertEquals("content://destination/document", request.destination)
        assertNull(queue.take(unlocked = true, granted = true, busy = false))
    }
    @Test fun revocationDiscardsPendingDataInsteadOfResumingItAfterRegrant() {
        val queue = StudioDocumentQueue()
        queue.prepareArchive(StudioBuildTarget("a", "build"))
        queue.completeArchive("content://destination")
        assertNull(queue.take(unlocked = true, granted = false, busy = false))
        assertNull(queue.take(unlocked = true, granted = true, busy = false))
    }
    @Test fun capturesAreCopiedAndCancelledPickersDoNotWrite() {
        val queue = StudioDocumentQueue()
        val bytes = byteArrayOf(1, 2, 3)
        queue.prepareCapture(bytes)
        bytes[0] = 9
        queue.completeCapture("content://image")
        val work = queue.take(true, true, false) as StudioDocumentWork.Capture
        assertArrayEquals(byteArrayOf(1, 2, 3), work.png)
        queue.prepareArchive(StudioBuildTarget("a", "b"))
        queue.completeArchive(null)
        assertNull(queue.take(true, true, false))
    }
    @Test fun lockRaceCanRequeueTheSameOperationWithoutChangingItsDestination() {
        val queue = StudioDocumentQueue()
        queue.prepareArchive(StudioBuildTarget("a", "b"))
        queue.completeArchive("content://first")
        val work = queue.take(true, true, false)!!
        queue.retry(work)
        assertNull(queue.take(false, true, false))
        assertSame(work, queue.take(true, true, false))
    }
}
