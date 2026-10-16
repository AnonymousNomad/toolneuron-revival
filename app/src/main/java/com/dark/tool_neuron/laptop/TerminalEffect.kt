package com.dark.tool_neuron.laptop

data class TerminalRequest(
    val command: String,
    val workspaceId: String?,
    val argv: List<String>? = null,
    val cwd: String? = null,
    val timeoutMs: Int = 30_000,
    val stdin: String? = null,
)

sealed class TerminalResult {
    data class Completed(
        val provider: String,
        val sessionId: String,
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
        val stdoutTruncated: Boolean,
        val stderrTruncated: Boolean,
        val binarySuspected: Boolean,
        val totalStdoutBytes: Int,
        val totalStderrBytes: Int,
        val startedAtMs: Long,
        val finishedAtMs: Long,
        val cancelled: Boolean,
        val timedOut: Boolean,
    ) : TerminalResult()

    data class NotImplemented(val reason: String) : TerminalResult()

    data class Failed(val provider: String, val reason: String) : TerminalResult()
}

interface TerminalEffectProvider {
    suspend fun execute(request: TerminalRequest): TerminalResult
}

class StubTerminalEffectProvider : TerminalEffectProvider {
    override suspend fun execute(request: TerminalRequest): TerminalResult = TerminalResult.NotImplemented(
        "dev.terminal.execute@1 has no provider in this build",
    )
}
