package com.dark.tool_neuron

import com.dark.tool_neuron.util.ModelPurpose
import com.dark.tool_neuron.util.ModelPurposeClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelPurposeClassifierTest {

    @Test
    fun embeddingArchitecturesClassifyAsEmbedding() {
        assertEquals(ModelPurpose.EMBEDDING, ModelPurposeClassifier.fromArchitecture("bert"))
        assertEquals(ModelPurpose.EMBEDDING, ModelPurposeClassifier.fromArchitecture("nomic-bert"))
        assertEquals(ModelPurpose.EMBEDDING, ModelPurposeClassifier.fromArchitecture("nomic-bert-moe"))
        assertEquals(ModelPurpose.EMBEDDING, ModelPurposeClassifier.fromArchitecture("BERT"))
    }

    @Test
    fun chatArchitecturesClassifyAsChat() {
        assertEquals(ModelPurpose.CHAT, ModelPurposeClassifier.fromArchitecture("llama"))
        assertEquals(ModelPurpose.CHAT, ModelPurposeClassifier.fromArchitecture("qwen2"))
        assertEquals(ModelPurpose.CHAT, ModelPurposeClassifier.fromArchitecture("lfm2"))
        assertEquals(ModelPurpose.CHAT, ModelPurposeClassifier.fromArchitecture("gemma3"))
        assertEquals(ModelPurpose.CHAT, ModelPurposeClassifier.fromArchitecture("deepseek2"))
    }

    @Test
    fun unknownArchitectureIsUnknownNotChat() {
        assertEquals(ModelPurpose.UNKNOWN, ModelPurposeClassifier.fromArchitecture(null))
        assertEquals(ModelPurpose.UNKNOWN, ModelPurposeClassifier.fromArchitecture(""))
        assertEquals(ModelPurpose.UNKNOWN, ModelPurposeClassifier.fromArchitecture("some-new-arch"))
        assertEquals(ModelPurpose.UNKNOWN, ModelPurposeClassifier.fromArchitecture("t5"))
    }

    @Test
    fun artifactPurposeWinsOverHint() {
        assertEquals(
            ModelPurpose.CHAT,
            ModelPurposeClassifier.resolve(ModelPurpose.CHAT, "embedding"),
        )
        assertEquals(
            ModelPurpose.EMBEDDING,
            ModelPurposeClassifier.resolve(ModelPurpose.EMBEDDING, "chat"),
        )
    }

    @Test
    fun unknownArtifactFallsBackToDeclaredHintOnly() {
        assertEquals(
            ModelPurpose.EMBEDDING,
            ModelPurposeClassifier.resolve(ModelPurpose.UNKNOWN, "embedding"),
        )
        assertEquals(
            ModelPurpose.CHAT,
            ModelPurposeClassifier.resolve(ModelPurpose.UNKNOWN, "chat"),
        )
        assertEquals(
            ModelPurpose.UNKNOWN,
            ModelPurposeClassifier.resolve(ModelPurpose.UNKNOWN, "gguf"),
        )
        assertEquals(
            ModelPurpose.UNKNOWN,
            ModelPurposeClassifier.resolve(ModelPurpose.UNKNOWN, null),
        )
    }

    @Test
    fun hintFromMetadataDetectsEmbeddingSignals() {
        assertEquals(
            "embedding",
            ModelPurposeClassifier.hintFromMetadata("feature-extraction", emptyList(), false),
        )
        assertEquals(
            "embedding",
            ModelPurposeClassifier.hintFromMetadata("sentence-similarity", emptyList(), false),
        )
        assertEquals(
            "embedding",
            ModelPurposeClassifier.hintFromMetadata("", listOf("gguf", "sentence-transformers"), false),
        )
        assertEquals(
            "embedding",
            ModelPurposeClassifier.hintFromMetadata("", listOf("feature-extraction"), false),
        )
    }

    @Test
    fun hintFromMetadataDetectsChatAndNeutral() {
        assertEquals(
            "chat",
            ModelPurposeClassifier.hintFromMetadata("text-generation", listOf("gguf"), false),
        )
        assertEquals(
            "gguf",
            ModelPurposeClassifier.hintFromMetadata("", emptyList(), false),
        )
        assertEquals(
            "gguf",
            ModelPurposeClassifier.hintFromMetadata("feature-extraction", emptyList(), true),
        )
    }
}
