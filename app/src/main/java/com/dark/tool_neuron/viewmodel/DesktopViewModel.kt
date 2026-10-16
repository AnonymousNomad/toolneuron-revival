package com.dark.tool_neuron.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.laptop.CipherLaptop
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.laptop.resident.ResidentEngine
import com.dark.tool_neuron.laptop.terminal.TerminalProviderState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DesktopViewModel @Inject constructor(
    private val laptop: LaptopManager,
    private val resident: ResidentEngine,
) : ViewModel() {

    val residentState: StateFlow<ResidentEngine.ResidentUiState> = resident.state
    val laptopState: StateFlow<CipherLaptop> = laptop.state
    val terminalProvider: StateFlow<TerminalProviderState> = laptop.terminalSessions.providerState

    private val _clock = MutableStateFlow("--:--")
    val clock: StateFlow<String> = _clock.asStateFlow()

    init {
        viewModelScope.launch {
            val fmt = SimpleDateFormat("HH:mm", Locale.US)
            while (isActive) {
                _clock.value = fmt.format(Date())
                delay(30_000)
            }
        }
    }
}
