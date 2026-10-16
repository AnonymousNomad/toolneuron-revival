package com.dark.tool_neuron.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.laptop.CapabilityRegistry
import com.dark.tool_neuron.laptop.CipherLaptop
import com.dark.tool_neuron.laptop.EvidenceKind
import com.dark.tool_neuron.laptop.LaptopCore
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.laptop.ModelRegistry
import com.dark.tool_neuron.laptop.TerminalRequest
import com.dark.tool_neuron.laptop.TerminalResult
import com.dark.tool_neuron.laptop.TaskStatus
import com.dark.tool_neuron.laptop.ToolRef
import com.dark.tool_neuron.laptop.Verdict
import com.dark.tool_neuron.laptop.terminal.TerminalOutput
import com.dark.tool_neuron.laptop.terminal.TerminalProviderState
import com.dark.tool_neuron.repo.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaptopViewModel @Inject constructor(
    private val laptop: LaptopManager,
    modelRepo: ModelRepository,
) : ViewModel() {

    val state: StateFlow<CipherLaptop> = laptop.state
    val capabilities: List<CapabilityRegistry.CapabilityDescriptor> = CapabilityRegistry.SEED
    val tools: List<ToolRef> = laptop.tools
    val terminalProviderState: StateFlow<TerminalProviderState> = laptop.terminalSessions.providerState

    private val _dossiers = MutableStateFlow<List<ModelRegistry.ModelDossier>>(emptyList())
    val dossiers: StateFlow<List<ModelRegistry.ModelDossier>> = _dossiers.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    val workspaceFiles: StateFlow<List<LaptopCore.WorkspaceEntry>> = laptop.state
        .map { laptop.core.listWorkspaceFiles().getOrDefault(emptyList()) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            modelRepo.models.collect { list ->
                _dossiers.value = laptop.modelDossiers(list)
            }
        }
        viewModelScope.launch {
            val current = laptop.terminalSessions.providerState.value
            if (current.availability == com.dark.tool_neuron.laptop.terminal.TerminalAvailability.UNKNOWN) {
                laptop.terminalSessions.probe()
            }
        }
    }

    fun createWorkspace(name: String) = report(laptop.core.createWorkspace(name))
    fun openWorkspace(id: String) = report(laptop.core.openWorkspace(id))

    fun readWorkspaceFile(id: String, path: String): String? = laptop.readWorkspaceFile(id, path)

    fun createTask(objective: String, capabilityRef: String?) =
        report(laptop.core.createTask(objective, capabilityRef))

    fun advanceTask(taskId: String) {
        val task = state.value.tasks.firstOrNull { it.id == taskId } ?: return
        val next = laptop.core.nextStatus(task.status) ?: return
        report(laptop.core.transitionTask(taskId, next))
    }

    fun cancelTask(taskId: String) = report(laptop.core.transitionTask(taskId, TaskStatus.CANCELLED))
    fun failTask(taskId: String) = report(laptop.core.transitionTask(taskId, TaskStatus.FAILED, "marked failed by operator"))
    fun addCheckpoint(taskId: String, label: String) = report(laptop.core.addCheckpoint(taskId, label))
    fun addNote(taskId: String?, text: String) = report(laptop.core.addEvidence(taskId, EvidenceKind.NOTE, text))
    fun setPermission(capabilityRef: String, granted: Boolean) = report(laptop.core.setPermission(capabilityRef, granted))

    fun probeTerminalProvider() {
        viewModelScope.launch {
            val s = laptop.terminalSessions.probe()
            _status.value = "terminal provider: ${s.availability}${s.failureReason?.let { " — $it" } ?: ""}"
        }
    }

    fun runTerminalEffect(taskId: String?, command: String, expected: String) {
        viewModelScope.launch {
            val capRef = "dev.terminal.execute@1"
            if (!laptop.core.hasPermission(capRef)) {
                _status.value = "denied: grant $capRef in Capabilities first"
                return@launch
            }
            val ws = laptop.core.activeWorkspace()
            if (ws == null) {
                _status.value = "no active workspace"
                return@launch
            }
            _status.value = "terminal effect running: $command"
            val result = laptop.terminal.execute(TerminalRequest(command = command, workspaceId = ws.id))
            when (result) {
                is TerminalResult.Completed -> {
                    val summary = TerminalOutput.evidenceSummary(result)
                    val evidence = laptop.core.addEvidence(taskId, EvidenceKind.COMMAND_OUTPUT, summary).getOrNull()
                    val pass = result.exitCode == 0 && !result.cancelled && !result.timedOut &&
                        (expected.isBlank() || result.stdout.contains(expected))
                    laptop.core.addVerification(
                        taskId = taskId,
                        requestedAction = "dev.terminal.execute@1: $command",
                        expectedResult = if (expected.isBlank()) "exit 0" else "exit 0 and stdout contains \"$expected\"",
                        verificationMethod = "deterministic expected-output containment check",
                        evidenceId = evidence?.id,
                        verdict = if (pass) Verdict.PASS else Verdict.FAIL,
                    )
                    _status.value = "terminal exit=${result.exitCode} → ${if (pass) "PASS" else "FAIL"}"
                }
                is TerminalResult.NotImplemented -> _status.value = "terminal provider unavailable: ${result.reason}"
                is TerminalResult.Failed -> _status.value = "terminal failed: ${result.reason}"
            }
        }
    }

    private fun report(result: Result<*>) {
        _status.value = result.exceptionOrNull()?.message ?: "ok"
    }
}
