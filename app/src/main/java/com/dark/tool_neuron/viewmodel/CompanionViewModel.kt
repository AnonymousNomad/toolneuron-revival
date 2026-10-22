package com.dark.tool_neuron.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.companion.*
import com.dark.tool_neuron.laptop.LaptopManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CompanionViewModel @Inject constructor(private val laptop: LaptopManager) : ViewModel() {
    val state = laptop.state
    val status = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    fun add(workspaceId: String, title: String, locator: String, classification: MediaSourceClass, rights: String, done: () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { laptop.core.addMediaReference(workspaceId, title, locator, classification, rights).getOrThrow() }
                status.value = "Reference saved in project; no playback or download performed"
                done()
            } catch (e: Exception) { status.value = e.message ?: "Reference could not be saved" }
            finally { busy.value = false }
        }
    }
}
