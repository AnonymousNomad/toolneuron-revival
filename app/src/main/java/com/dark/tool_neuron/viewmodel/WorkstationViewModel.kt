package com.dark.tool_neuron.viewmodel

import androidx.lifecycle.ViewModel
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.laptop.WorkstationApp
import com.dark.tool_neuron.laptop.WorkstationSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WorkstationViewModel @Inject constructor(private val laptop: LaptopManager) : ViewModel() {
    val state = laptop.state
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    fun open(app: WorkstationApp) = report(laptop.core.openWorkstationApp(app))
    fun minimize(app: WorkstationApp) = report(laptop.core.minimizeWorkstationApp(app))
    fun close(app: WorkstationApp) = report(laptop.core.closeWorkstationApp(app))
    fun desktop() = report(laptop.core.showWorkstationDesktop())

    private fun report(result: Result<Unit>): Result<WorkstationSession> = result
        .onSuccess { _status.value = null }
        .onFailure { _status.value = "Could not save window layout: ${it.message}. It may not restore after restart." }
        .map { laptop.state.value.workstation }
}
