package com.dark.tool_neuron.companion

import kotlinx.serialization.Serializable

@Serializable enum class MediaSourceClass { UNKNOWN, LOCAL_OWNED, PUBLIC_DOMAIN, LICENSED_PROVIDER }
@Serializable data class MediaReference(val id: String, val title: String, val locator: String, val classification: MediaSourceClass, val rightsDeclaration: String) {
    val eligible: Boolean get() = classification != MediaSourceClass.UNKNOWN && rightsDeclaration.isNotBlank()
}
@Serializable data class CompanionPlan(val media: List<MediaReference> = emptyList(), val gameNotes: String = "") {
    fun validate() {
        require(media.size <= 100 && media.map { it.id }.distinct().size == media.size)
        require(gameNotes.length <= 16000)
        media.forEach { require(it.id.isNotBlank() && it.title.length in 1..120 && it.locator.length in 1..2048 && it.rightsDeclaration.length <= 2000) }
    }
    companion object { const val CAPABILITY = "companion.plan@1" }
}
@Serializable data class GameIntent(val workspaceId: String, val gameId: String, val kind: String, val objective: String) {
    init {
        require(workspaceId.length in 1..128 && gameId.length in 1..128)
        require(kind in setOf("strategy", "explain", "plan") && objective.length in 1..2000)
    }
}
@Serializable data class DeviceIdentity(val id: String, val displayName: String, val pairingFingerprint: String)
interface GameAdapter {
    val available: Boolean
    fun submit(intent: GameIntent): Result<String>
}
interface DeviceBridge {
    val available: Boolean
    fun send(device: DeviceIdentity, intent: GameIntent): Result<String>
}
interface MediaPlaybackAdapter {
    val available: Boolean
    fun open(reference: MediaReference): Result<String>
}
object CompanionAdapters {
    val game: GameAdapter = object : GameAdapter {
        override val available = false
        override fun submit(intent: GameIntent) = Result.failure<String>(IllegalStateException("UNCONFIGURED: no authorized game adapter; models never control frame timing or raw input"))
    }
    val bridge: DeviceBridge = object : DeviceBridge {
        override val available = false
        override fun send(device: DeviceIdentity, intent: GameIntent) = Result.failure<String>(IllegalStateException("UNCONFIGURED: no authenticated paired transport; identity declaration does not establish pairing"))
    }
    val playback: MediaPlaybackAdapter = object : MediaPlaybackAdapter {
        override val available = false
        override fun open(reference: MediaReference) = Result.failure<String>(IllegalStateException("UNCONFIGURED: no licensed playback provider connected; rights declaration alone is insufficient"))
    }
}
