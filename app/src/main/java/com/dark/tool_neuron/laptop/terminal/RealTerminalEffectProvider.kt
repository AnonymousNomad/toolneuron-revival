package com.dark.tool_neuron.laptop.terminal

import com.dark.terminalpty.TerminalPty
import com.dark.tool_neuron.laptop.TerminalEffectProvider
import com.dark.tool_neuron.laptop.TerminalRequest
import com.dark.tool_neuron.laptop.TerminalResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class RealTerminalEffectProvider(
    private val workspacesRoot: File,
    private val tmpDir: File,
    private val workspaceDirOf: (String) -> File?,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : TerminalEffectProvider {

    override suspend fun execute(request: TerminalRequest): TerminalResult = withContext(Dispatchers.IO) {
        val startedAt = nowMs()

        val workspaceDir = request.workspaceId?.let { workspaceDirOf(it) }
            ?: return@withContext TerminalResult.Failed(TerminalProviderState.PROVIDER_ID, "no active workspace for request")

        val cwd = TerminalPolicy.containedCwd(workspacesRoot, workspaceDir, request.cwd)
            ?: return@withContext TerminalResult.Failed(TerminalProviderState.PROVIDER_ID, "cwd is outside the workspace")

        val argv: List<String> = request.argv ?: run {
            val err = TerminalPolicy.validateCommand(request.command)
            if (err != null) return@withContext TerminalResult.Failed(TerminalProviderState.PROVIDER_ID, err)
            listOf(TerminalPolicy.SHELL, "-c", request.command)
        }
        if (request.argv != null) {
            val err = TerminalPolicy.validateArgv(request.argv)
            if (err != null) return@withContext TerminalResult.Failed(TerminalProviderState.PROVIDER_ID, err)
        }

        val timeout = TerminalPolicy.clampTimeout(request.timeoutMs)
        val env = TerminalPolicy.buildEnv(cwd, tmpDir)
        val spawn = TerminalPty.spawnPipes(cwd.absolutePath, argv.toTypedArray(), env)
            ?: return@withContext TerminalResult.Failed(TerminalProviderState.PROVIDER_ID, "spawn failed")

        val sessionId = "te-" + UUID.randomUUID().toString().replace("-", "").take(8)
        val out = Sink(TerminalOutput.MAX_EFFECT_STDOUT)
        val err = Sink(TerminalOutput.MAX_EFFECT_STDERR)
        var timedOut = false

        try {
            request.stdin?.let { TerminalPty.write(spawn.stdinFd, it.toByteArray(Charsets.UTF_8)) }
            TerminalPty.close(spawn.stdinFd)

            val deadline = nowMs() + timeout
            while (!(out.eof && err.eof)) {
                if (nowMs() >= deadline) {
                    timedOut = true
                    break
                }
                if (!out.eof) {
                    val d = TerminalPty.read(spawn.stdoutFd, 100)
                    when {
                        d == null -> Unit
                        d.isEmpty() -> out.eof = true
                        else -> out.feed(d)
                    }
                }
                if (!err.eof) {
                    val d = TerminalPty.read(spawn.stderrFd, 100)
                    when {
                        d == null -> Unit
                        d.isEmpty() -> err.eof = true
                        else -> err.feed(d)
                    }
                }
            }

            if (timedOut || !(out.eof && err.eof)) {
                TerminalPty.killGroup(spawn.pid, TerminalPty.SIGTERM)
                var code = TerminalPty.waitExit(spawn.pid, 1500)
                if (code == -1) {
                    TerminalPty.killGroup(spawn.pid, TerminalPty.SIGKILL)
                    code = TerminalPty.waitExit(spawn.pid, 2000)
                }
            }

            val exit = TerminalPty.waitExit(spawn.pid, 3000)
            val stdout = TerminalOutput.bound(out.bytes(), TerminalOutput.MAX_EFFECT_STDOUT, out.total)
            val stderr = TerminalOutput.bound(err.bytes(), TerminalOutput.MAX_EFFECT_STDERR, err.total)
            val finishedAt = nowMs()

            TerminalResult.Completed(
                provider = TerminalProviderState.PROVIDER_ID,
                sessionId = sessionId,
                exitCode = if (exit >= 0) exit else -1,
                stdout = stdout.text,
                stderr = stderr.text,
                stdoutTruncated = stdout.truncated,
                stderrTruncated = stderr.truncated,
                binarySuspected = stdout.binarySuspected || stderr.binarySuspected,
                totalStdoutBytes = stdout.totalBytes,
                totalStderrBytes = stderr.totalBytes,
                startedAtMs = startedAt,
                finishedAtMs = finishedAt,
                cancelled = false,
                timedOut = timedOut,
            )
        } catch (ce: CancellationException) {
            TerminalPty.killGroup(spawn.pid, TerminalPty.SIGTERM)
            var code = TerminalPty.waitExit(spawn.pid, 1500)
            if (code == -1) {
                TerminalPty.killGroup(spawn.pid, TerminalPty.SIGKILL)
                TerminalPty.waitExit(spawn.pid, 2000)
            }
            throw ce
        } finally {
            TerminalPty.close(spawn.stdoutFd)
            TerminalPty.close(spawn.stderrFd)
        }
    }

    private class Sink(val cap: Int) {
        private val baos = ByteArrayOutputStream()
        var total: Int = 0
        var eof: Boolean = false

        fun feed(d: ByteArray) {
            total += d.size
            val keep = cap * 2
            if (baos.size() < keep) {
                val room = keep - baos.size()
                baos.write(d, 0, minOf(room, d.size))
            }
        }

        fun bytes(): ByteArray = baos.toByteArray()
    }
}
