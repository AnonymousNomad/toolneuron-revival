package com.dark.tool_neuron.laptop

import kotlinx.serialization.Serializable

@Serializable
enum class WorkstationApp(val title: String) {
    REPORT("Beta Report"), COMPANION("TV / Game"), CREATOR("Creator"), VIBE("Vibe Studio"), CIPHER("Cipher"), CHAT("Chat"), PROJECTS("Projects"), TERMINAL("Terminal"),
    MODELS("Models"), ABILITIES("Abilities"), KNOWLEDGE("Knowledge"),
    FILES("Files"), EVIDENCE("Evidence"), SETTINGS("Settings"),
}

@Serializable
data class WorkstationWindow(val app: WorkstationApp, val minimized: Boolean = false)

@Serializable
data class WorkstationSession(
    val windows: List<WorkstationWindow> = emptyList(),
    val focused: WorkstationApp? = null,
) {
    init {
        require(windows.map { it.app }.distinct().size == windows.size) { "Duplicate app window" }
        require(focused == null || windows.any { it.app == focused && !it.minimized }) { "Focused window is missing or minimized" }
    }

    fun open(app: WorkstationApp): WorkstationSession = copy(
        windows = windows.filterNot { it.app == app } + WorkstationWindow(app),
        focused = app,
    )

    fun minimize(app: WorkstationApp): WorkstationSession {
        val next = windows.map { if (it.app == app) it.copy(minimized = true) else it }
        return copy(windows = next, focused = if (focused == app) next.lastOrNull { !it.minimized }?.app else focused)
    }

    fun close(app: WorkstationApp): WorkstationSession {
        val next = windows.filterNot { it.app == app }
        return copy(windows = next, focused = if (focused == app) next.lastOrNull { !it.minimized }?.app else focused)
    }

    fun desktop(): WorkstationSession = copy(focused = null)
}
