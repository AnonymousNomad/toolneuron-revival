package com.dark.tool_neuron.util

enum class ModelPurpose { CHAT, EMBEDDING, UNKNOWN }

object ModelPurposeClassifier {
    private val EMBEDDING_ARCHITECTURES = setOf(
        "bert", "nomic-bert", "nomic-bert-moe", "jina-bert-v2", "jina-bert-v3",
    )

    private val CHAT_ARCHITECTURES = setOf(
        "llama", "qwen2", "qwen2moe", "qwen3", "qwen3moe", "gemma", "gemma2", "gemma3", "gemma3n",
        "phi2", "phi3", "phi3moe", "lfm2", "lfm2moe", "mistral", "mixtral", "deepseek2", "deepseek3",
        "gpt2", "gptj", "gptneox", "mpt", "falcon", "starcoder2", "starcoder", "command-r", "cohere2",
        "olmo", "olmo2", "granite", "glm4", "minicpm", "internlm2", "exaone", "rwkv6", "rwkv7",
    )

    fun fromArchitecture(architecture: String?): ModelPurpose = when {
        architecture.isNullOrBlank() -> ModelPurpose.UNKNOWN
        architecture.lowercase() in EMBEDDING_ARCHITECTURES -> ModelPurpose.EMBEDDING
        architecture.lowercase() in CHAT_ARCHITECTURES -> ModelPurpose.CHAT
        else -> ModelPurpose.UNKNOWN
    }

    fun hintFromMetadata(pipelineTag: String, tags: List<String>, isMmproj: Boolean): String {
        if (isMmproj) return "gguf"
        val lowered = tags.map { it.lowercase() }
        return when {
            pipelineTag == "feature-extraction" || pipelineTag == "sentence-similarity" ||
                "sentence-transformers" in lowered || "feature-extraction" in lowered -> "embedding"
            pipelineTag == "text-generation" || pipelineTag == "image-text-to-text" -> "chat"
            else -> "gguf"
        }
    }

    fun resolve(artifactPurpose: ModelPurpose, discoveryHint: String?): ModelPurpose = when {
        artifactPurpose != ModelPurpose.UNKNOWN -> artifactPurpose
        discoveryHint == "embedding" -> ModelPurpose.EMBEDDING
        discoveryHint == "chat" -> ModelPurpose.CHAT
        else -> ModelPurpose.UNKNOWN
    }
}
