package com.dark.tool_neuron.runtime

import kotlinx.coroutines.flow.Flow
import java.io.File

data class LocalBackendAvailability(val available: Boolean, val reason: String? = null)
data class LocalLoadOptions(
    val contextSize: Int = 4096,
    val threadMode: Int = 1,
    val flashAttention: Boolean = true,
    val cacheTypeK: String = "q8_0",
    val cacheTypeV: String = "q8_0",
)

sealed interface LocalGenerationEvent {
    data class Token(val text: String) : LocalGenerationEvent
    data object Done : LocalGenerationEvent
    data class Error(val message: String) : LocalGenerationEvent
    data class Metrics(val values: Map<String, Double>) : LocalGenerationEvent
    data class Progress(val value: Float) : LocalGenerationEvent
}

interface LocalModelBackend {
    val id: String
    val availability: LocalBackendAvailability
    suspend fun load(file: File, options: LocalLoadOptions): String
    suspend fun loadLegacyDescriptor(fd: Int, options: LocalLoadOptions): String =
        throw UnsupportedOperationException("$id requires a durable app-owned GGUF file")
    fun generate(prompt: String, maxTokens: Int, multiTurn: Boolean = false): Flow<LocalGenerationEvent>
    fun cancel()
    suspend fun unload()
}

class CovertEngineBackend : LocalModelBackend {
    override val id = ID
    override val availability = LocalBackendAvailability(false,
        "BLOCKED_LOCAL_SOURCE_UNAVAILABLE: Covert Engine R0 $REVISION native source is not packaged. No runtime substitution.")
    override suspend fun load(file: File, options: LocalLoadOptions): String = unavailable()
    override fun generate(prompt: String, maxTokens: Int, multiTurn: Boolean): Flow<LocalGenerationEvent> = unavailable()
    override fun cancel() = Unit
    override suspend fun unload() = Unit
    private fun unavailable(): Nothing = throw IllegalStateException(availability.reason)
    companion object {
        const val ID = "covert-r0"
        const val REVISION = "3020b7f407b61addf872e6aff30d689d71ef0f20"
    }
}
