package com.dark.tool_neuron.laptop.terminal

enum class TerminalAvailability { UNKNOWN, READY, FAILED }

data class TerminalProviderState(
    val id: String = PROVIDER_ID,
    val version: String = "1",
    val availability: TerminalAvailability = TerminalAvailability.UNKNOWN,
    val failureReason: String? = null,
    val probedAtMs: Long? = null,
    val probeOutput: String? = null,
    val shellPath: String = TerminalPolicy.SHELL,
    val compatibility: String = "targetSdk 36 / minSdk 29; platform shell; app-uid child; app-private workspace; no bundled binaries; no root",
    val bootstrap: String = "NONE (platform /system/bin/sh + toybox; nothing installed)",
    val footprintBytes: Long = 0,
    val supportedFeatures: List<String> = listOf(
        "pty",
        "separated-stdio",
        "stdin",
        "resize",
        "cancel (SIGINT/SIGTERM/SIGKILL)",
        "process-group cleanup",
    ),
    val limitations: List<String> = listOf(
        "toybox utilities, not GNU coreutils (flag differences possible)",
        "no package manager; no on-device installs",
        "line-oriented rendering; full-screen TUI apps are not supported",
        "terminal sessions end when the app process ends (Laptop task state survives)",
    ),
) {
    companion object {
        const val PROVIDER_ID = "tn.terminal.system-sh"
    }
}
