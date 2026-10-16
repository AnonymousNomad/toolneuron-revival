package com.dark.tool_neuron.laptop.terminal

import java.io.File

object TerminalPolicy {
    const val MIN_TIMEOUT_MS = 1_000
    const val MAX_TIMEOUT_MS = 120_000
    const val DEFAULT_TIMEOUT_MS = 30_000
    const val MAX_COMMAND_LENGTH = 4096
    const val MAX_ARGV = 64
    const val SHELL = "/system/bin/sh"

    fun clampTimeout(ms: Int): Int = ms.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS)

    fun validateCommand(command: String): String? = when {
        command.isBlank() -> "command is empty"
        command.length > MAX_COMMAND_LENGTH -> "command exceeds $MAX_COMMAND_LENGTH characters"
        command.contains('\u0000') -> "command contains NUL"
        else -> null
    }

    fun validateArgv(argv: List<String>): String? = when {
        argv.isEmpty() -> "argv is empty"
        argv.size > MAX_ARGV -> "argv exceeds $MAX_ARGV entries"
        argv[0].isBlank() -> "argv[0] is empty"
        argv.any { it.contains('\u0000') } -> "argv contains NUL"
        else -> null
    }

    fun containedCwd(workspacesRoot: File, workspaceDir: File, requested: String?): File? {
        val root = workspacesRoot.canonicalFile
        val base = workspaceDir.canonicalFile
        if (!base.path.startsWith(root.path + File.separator)) return null
        if (requested.isNullOrBlank()) return base
        val target = File(base, requested).canonicalFile
        if (target != base && !target.path.startsWith(base.path + File.separator)) return null
        if (!target.isDirectory) return null
        return target
    }

    fun buildEnv(workspaceDir: File, tmpDir: File): Array<String> = arrayOf(
        "PATH=/system/bin:/system/xbin",
        "HOME=${workspaceDir.absolutePath}",
        "PWD=${workspaceDir.absolutePath}",
        "TMPDIR=${tmpDir.absolutePath}",
        "TERM=xterm-256color",
        "LANG=C.UTF-8",
    )
}
