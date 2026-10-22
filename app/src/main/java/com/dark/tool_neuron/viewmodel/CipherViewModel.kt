package com.dark.tool_neuron.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.laptop.*
import com.dark.tool_neuron.laptop.resident.ResidentEngine
import com.dark.tool_neuron.repo.ModelRepository
import com.dark.tool_neuron.repo.ModelCapabilityResolver
import com.dark.tool_neuron.util.TaskAdmission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CipherViewModel @Inject constructor(private val engine: ResidentEngine, private val inspector: CipherAssignmentInspector,
    private val laptop: LaptopManager, private val models: ModelRepository) : ViewModel() {
    val state = engine.state
    val messages = engine.messages
    val streaming = engine.streaming
    val laptopState = laptop.state
    val installedModels = models.models
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _slot = MutableStateFlow(CipherSlotAssessment(CipherSlotStatus.EMPTY, "Checking assignment…"))
    val slot = _slot.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun admittedModels() = installedModels.value.filter { TaskAdmission.CAN_CHAT in ModelCapabilityResolver.profileOf(it).admissions }
    fun refreshSlot() = action { _slot.value = inspector.inspect().assessment }
    fun assign(modelId: String, runtimeId: String, contextSize: Int) = action {
        inspector.assign(modelId, runtimeId, contextSize)
        _slot.value = inspector.inspect().assessment
    }
    fun clearAssignment() = action {
        laptop.core.assignCipher(null).getOrThrow()
        _slot.value = inspector.inspect().assessment
    }
    fun start(contextSize: Int = 4096) = action { engine.start(); _slot.value = inspector.inspect().assessment }
    fun send(text: String) = action { engine.send(text) }
    fun cancel() { engine.cancelGeneration() }
    fun stop() = action { engine.stop(); _slot.value = inspector.inspect().assessment }
    private fun action(operation: suspend () -> Unit) {
        if (!_busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            _error.value = null
            try { operation() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { _error.value = failure.message ?: "Cipher action failed" }
            finally { _busy.value = false }
        }
    }
}
