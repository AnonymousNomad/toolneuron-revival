package com.dark.tool_neuron.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.laptop.resident.ResidentEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CipherViewModel @Inject constructor(
    private val engine: ResidentEngine,
) : ViewModel() {

    val state: StateFlow<ResidentEngine.ResidentUiState> = engine.state
    val messages: StateFlow<List<ResidentEngine.Message>> = engine.messages
    val streaming: StateFlow<String> = engine.streaming

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun start(contextSize: Int = 4096) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { engine.start(contextSize) }
            _busy.value = false
        }
    }

    fun send(text: String) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { engine.send(text) }
            _busy.value = false
        }
    }

    fun cancel() {
        engine.cancelGeneration()
    }

    fun stop() {
        viewModelScope.launch {
            _busy.value = true
            runCatching { engine.stop() }
            _busy.value = false
        }
    }
}
