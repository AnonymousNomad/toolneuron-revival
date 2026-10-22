package com.dark.tool_neuron.runtime

import com.dark.gguf_lib.GGMLEngine
import com.dark.gguf_lib.models.GenerationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.io.File

class GgmlBackend(private val engine: GGMLEngine) : LocalModelBackend {
    override val id = ID
    override val availability = LocalBackendAvailability(true)
    override suspend fun load(file: File, options: LocalLoadOptions): String {
        engine.setThreadMode(options.threadMode)
        check(engine.load(path = file.path, contextSize = options.contextSize,
            flashAttn = options.flashAttention, cacheTypeK = options.cacheTypeK, cacheTypeV = options.cacheTypeV)) { "Model load returned false" }
        return engine.getModelInfoJson() ?: "{}"
    }
    override suspend fun loadLegacyDescriptor(fd: Int, options: LocalLoadOptions): String {
        engine.setThreadMode(options.threadMode)
        check(engine.loadFromFd(fd = fd, contextSize = options.contextSize,
            flashAttn = options.flashAttention, cacheTypeK = options.cacheTypeK, cacheTypeV = options.cacheTypeV)) { "Failed to load model from FD" }
        return engine.getModelInfoJson() ?: "{}"
    }
    override fun generate(prompt: String, maxTokens: Int, multiTurn: Boolean): Flow<LocalGenerationEvent> =
        (if (multiTurn) engine.generateMultiTurnFlow(prompt, maxTokens) else engine.generateFlow(prompt, maxTokens))
            .mapNotNull(::mapEvent)
    override fun cancel() = engine.stopGeneration()
    override suspend fun unload() { if (engine.isLoaded) engine.unload() }

    companion object {
        const val ID = "ggml"
        fun mapEvent(event: GenerationEvent): LocalGenerationEvent? = when (event) {
            is GenerationEvent.Token -> LocalGenerationEvent.Token(event.text)
            is GenerationEvent.Done -> LocalGenerationEvent.Done
            is GenerationEvent.Error -> LocalGenerationEvent.Error(event.message)
            is GenerationEvent.Progress -> LocalGenerationEvent.Progress(event.progress)
            is GenerationEvent.Metrics -> event.metrics.let { m -> LocalGenerationEvent.Metrics(mapOf(
                "tokensPerSecond" to m.tokensPerSecond.toDouble(),
                "timeToFirstTokenMs" to m.timeToFirstTokenMs.toDouble(),
                "totalTimeMs" to m.totalTimeMs.toDouble(),
                "tokensEvaluated" to m.tokensEvaluated.toDouble(),
                "tokensPredicted" to m.tokensPredicted.toDouble(),
                "modelSizeMB" to m.modelSizeMB.toDouble(),
                "contextSizeMB" to m.contextSizeMB.toDouble(),
                "peakMemoryMB" to m.peakMemoryMB.toDouble(),
                "memoryUsagePercent" to m.memoryUsagePercent.toDouble(),
            )) }
            else -> null
        }
    }
}
