package com.dark.tool_neuron.studio

import java.util.ArrayDeque

data class StudioBuildTarget(val workspaceId: String, val buildId: String)
sealed interface StudioDocumentWork {
    val destination: String
    data class Archive(val target: StudioBuildTarget, override val destination: String) : StudioDocumentWork
    data class Capture(val png: ByteArray, override val destination: String) : StudioDocumentWork
}
class StudioDocumentQueue {
    private var archive: StudioBuildTarget? = null
    private var capture: ByteArray? = null
    private val ready = ArrayDeque<StudioDocumentWork>()
    @Synchronized fun prepareArchive(target: StudioBuildTarget): Boolean {
        if (archive != null || ready.size >= 4) return false
        archive = target
        return true
    }
    @Synchronized fun prepareCapture(bytes: ByteArray): Boolean {
        if (capture != null || ready.size >= 4 || bytes.size > 8 * 1024 * 1024) return false
        capture = bytes.copyOf()
        return true
    }
    @Synchronized fun completeArchive(destination: String?) {
        val target = archive
        archive = null
        if (target != null && destination != null) ready.addLast(StudioDocumentWork.Archive(target, destination))
    }
    @Synchronized fun completeCapture(destination: String?) {
        val png = capture
        capture = null
        if (png != null && destination != null) ready.addLast(StudioDocumentWork.Capture(png, destination))
    }
    @Synchronized fun take(unlocked: Boolean, granted: Boolean, busy: Boolean): StudioDocumentWork? {
        if (!granted) { clear(); return null }
        if (!unlocked || busy) return null
        return ready.pollFirst()
    }
    @Synchronized fun retry(work: StudioDocumentWork) { ready.addFirst(work) }
    @Synchronized fun clear() { archive = null; capture = null; ready.clear() }
}
