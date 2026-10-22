package com.dark.tool_neuron.studio

import kotlinx.serialization.Serializable

@Serializable data class VibeDraft(val path: String, val text: String, val updatedAtMs: Long) {
    fun validate() {
        require(path.length in 1..160 && path.matches(Regex("[A-Za-z0-9_./-]+")) && !path.startsWith('/') && path.split('/').all { it.isNotBlank() && !it.startsWith('.') })
        require(text.length <= 128000 && updatedAtMs > 0)
    }
}
