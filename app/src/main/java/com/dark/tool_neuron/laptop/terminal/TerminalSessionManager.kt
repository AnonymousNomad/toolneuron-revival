package com.dark.tool_neuron.laptop.terminal

import android.content.Context
import com.dark.terminalpty.TerminalPty
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class TerminalSessionState { RUNNING, EXITED, KILLED, FAILED }

data class TerminalSessionInfo(
    val id: String,
    val workspaceId: String,
    val pid: Int,
    val state: TerminalSessionState,
    val exitCode: Int?,
    val startedAtMs: Long,
    val finishedAtMs: Long?,
    val note: String?,
)

class TerminalSession internal constructor(
    val id: String,
    val workspaceId: String,
    val pid: Int,
    private val fd: Int,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long,
) {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _info = MutableStateFlow(
        TerminalSessionInfo(id, workspaceId, pid, TerminalSessionState.RUNNING, null, nowMs(), null, null),
    )
    val info: StateFlow<TerminalSessionInfo> = _info.asStateFlow()

    @Volatile private var closed = false
    private var readerJob: Job? = null

    internal fun start() {
        readerJob = scope.launch { readerLoop() }
    }

    private suspend fun readerLoop() {
        val buf = StringBuilder()
        while (!closed && scope.isActive) {
            val data = TerminalPty.read(fd, 250)
            if (data == null) continue
            if (data.isEmpty()) {
                val code = TerminalPty.waitExit(pid, 2000)
                if (!closed) {
                    _info.value = _info.value.copy(
                        state = TerminalSessionState.EXITED,
                        exitCode = if (code >= 0) code else null,
                        finishedAtMs = nowMs(),
                    )
                }
                return
            }
            buf.append(TerminalOutput.sanitize(data))
            if (buf.length > TerminalOutput.MAX_INTERACTIVE_BUFFER) {
                buf.delete(0, buf.length - TerminalOutput.MAX_INTERACTIVE_BUFFER)
            }
            _output.value = buf.toString()
        }
    }

    fun sendInput(text: String): Boolean {
        if (closed) return false
        return TerminalPty.write(fd, text.toByteArray(Charsets.UTF_8)) >= 0
    }

    fun sendInterrupt(): Boolean {
        if (closed) return false
        return TerminalPty.write(fd, byteArrayOf(0x03)) >= 0
    }

    fun clearOutput() {
        _output.value = ""
    }

    fun resize(rows: Int, cols: Int): Boolean = TerminalPty.resize(fd, rows, cols)

    fun close(reason: String = "closed by operator") {
        if (closed) return
        closed = true
        TerminalPty.killGroup(pid, TerminalPty.SIGTERM)
        var code = TerminalPty.waitExit(pid, 1500)
        if (code == -1) {
            TerminalPty.killGroup(pid, TerminalPty.SIGKILL)
            code = TerminalPty.waitExit(pid, 2000)
        }
        TerminalPty.close(fd)
        readerJob?.cancel()
        _info.value = _info.value.copy(
            state = TerminalSessionState.KILLED,
            exitCode = if (code >= 0) code else null,
            finishedAtMs = nowMs(),
            note = reason,
        )
    }
}

@Singleton
class TerminalSessionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val tmpDir: File = File(context.cacheDir, "terminal-tmp").apply { mkdirs() }

    private val _providerState = MutableStateFlow(TerminalProviderState())
    val providerState: StateFlow<TerminalProviderState> = _providerState.asStateFlow()

    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    private val probeLock = Mutex()
    private val maxSessions = 2

    suspend fun probe(): TerminalProviderState = probeLock.withLock {
        val marker = "TOOLNEURON_PROBE_OK"
        val result = withContext(Dispatchers.IO) {
            val spawn = TerminalPty.spawnPipes(
                context.filesDir.absolutePath,
                arrayOf(TerminalPolicy.SHELL, "-c", "echo $marker"),
                TerminalPolicy.buildEnv(context.filesDir, tmpDir),
            ) ?: return@withContext null
            val out = StringBuilder()
            val deadline = System.currentTimeMillis() + 5_000
            var eof = false
            while (!eof && System.currentTimeMillis() < deadline) {
                val d = TerminalPty.read(spawn.stdoutFd, 100)
                if (d == null) continue
                if (d.isEmpty()) { eof = true; break }
                out.append(d.toString(Charsets.UTF_8))
            }
            val code = TerminalPty.waitExit(spawn.pid, 3000)
            TerminalPty.close(spawn.stdoutFd)
            TerminalPty.close(spawn.stderrFd)
            TerminalPty.close(spawn.stdinFd)
            Pair(code, out.toString())
        }
        val state = if (result != null && result.first == 0 && result.second.trim() == marker) {
            TerminalProviderState(
                availability = TerminalAvailability.READY,
                probedAtMs = System.currentTimeMillis(),
                probeOutput = result.second.trim(),
            )
        } else {
            TerminalProviderState(
                availability = TerminalAvailability.FAILED,
                failureReason = if (result == null) "spawn failed" else "probe exit=${result.first} output='${result.second.trim().take(120)}'",
                probedAtMs = System.currentTimeMillis(),
                probeOutput = result?.second?.trim(),
            )
        }
        _providerState.value = state
        state
    }

    fun openSession(workspaceId: String, workspaceDir: File): Result<TerminalSession> {
        val running = _sessions.value.count { it.info.value.state == TerminalSessionState.RUNNING }
        if (running >= maxSessions) return Result.failure(IllegalStateException("session limit reached ($maxSessions)"))
        val env = TerminalPolicy.buildEnv(workspaceDir, tmpDir)
        val spawn = TerminalPty.spawnPty(workspaceDir.absolutePath, arrayOf(TerminalPolicy.SHELL), env, 30, 100)
            ?: return Result.failure(IllegalStateException("pty spawn failed"))
        val session = TerminalSession(
            id = "ts-" + UUID.randomUUID().toString().replace("-", "").take(8),
            workspaceId = workspaceId,
            pid = spawn.pid,
            fd = spawn.fd,
            scope = scope,
            nowMs = System::currentTimeMillis,
        )
        session.start()
        _sessions.value = _sessions.value.filter { it.info.value.state == TerminalSessionState.RUNNING } + session
        return Result.success(session)
    }

    fun closeSession(id: String) {
        _sessions.value.firstOrNull { it.id == id }?.close()
        _sessions.value = _sessions.value.filter { it.info.value.state == TerminalSessionState.RUNNING }
    }

    fun closeAll() {
        _sessions.value.forEach { it.close("closed (all)") }
        _sessions.value = emptyList()
    }
}
