package com.dark.tool_neuron.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.laptop.terminal.TerminalProviderState
import com.dark.tool_neuron.laptop.terminal.TerminalSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalSessionView(
    val id: String,
    val workspaceId: String,
    val pid: Int,
    val state: TerminalSessionState,
    val exitCode: Int?,
    val output: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val laptop: LaptopManager,
) : ViewModel() {

    val providerState: StateFlow<TerminalProviderState> = laptop.terminalSessions.providerState

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    val sessions: StateFlow<List<TerminalSessionView>> = laptop.terminalSessions.sessions
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(list.map { s -> combine(s.info, s.output) { info, out -> TerminalSessionView(info.id, info.workspaceId, info.pid, info.state, info.exitCode, out) } }) { it.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun probe() {
        viewModelScope.launch {
            val s = laptop.terminalSessions.probe()
            _status.value = "provider: ${s.availability}${s.failureReason?.let { " — $it" } ?: ""}"
        }
    }

    fun openSession() {
        if (!laptop.core.hasPermission("dev.terminal.execute@1")) {
            _status.value = "denied: grant Terminal execution in Abilities first"
            return
        }
        val ws = laptop.core.activeWorkspace()
        if (ws == null) {
            _status.value = "no active workspace (create one in the Laptop screen)"
            return
        }
        val dir = laptop.workspaceDir(ws.id)
        if (dir == null) {
            _status.value = "workspace directory missing"
            return
        }
        laptop.terminalSessions.openSession(ws.id, dir)
            .onSuccess { _status.value = "session ${it.id} opened in ${ws.name}" }
            .onFailure { _status.value = "open failed: ${it.message}" }
    }

    fun send(sessionId: String, text: String) {
        if (!laptop.core.hasPermission("dev.terminal.execute@1")) {
            _status.value = "denied: Terminal execution permission was revoked"
            return
        }
        val s = laptop.terminalSessions.sessions.value.firstOrNull { it.id == sessionId } ?: return
        if (text.isEmpty()) return
        if (!s.sendInput(text + "\n")) _status.value = "write failed"
    }

    fun interrupt(sessionId: String) {
        val s = laptop.terminalSessions.sessions.value.firstOrNull { it.id == sessionId } ?: return
        s.sendInterrupt()
        _status.value = "SIGINT sent"
    }

    fun clear(sessionId: String) {
        laptop.terminalSessions.sessions.value.firstOrNull { it.id == sessionId }?.clearOutput()
    }

    fun close(sessionId: String) {
        laptop.terminalSessions.closeSession(sessionId)
        _status.value = "session closed"
    }
}
