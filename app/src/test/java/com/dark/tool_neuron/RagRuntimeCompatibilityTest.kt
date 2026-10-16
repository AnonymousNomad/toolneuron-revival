package com.dark.tool_neuron

import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.model.enums.ProviderType
import com.dark.tool_neuron.repo.RagRuntimeCompatibility
import com.dark.tool_neuron.repo.RagRuntimeSupport
import com.dark.tool_neuron.util.ModelPurpose
import com.dark.tool_neuron.util.ModelPurposeClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RagRuntimeCompatibilityTest {
    private fun embedding(fileName: String, size: Long) = ModelInfo(
        id = "test:$fileName",
        name = fileName,
        path = "/data/user/0/com.dark.tool_neuron/files/models/$fileName",
        pathType = PathType.FILE,
        providerType = ProviderType.EMBEDDING,
        fileSize = size,
    )

    @Test
    fun nomicBuiltinIsRuntimeSupported() {
        val nomic = embedding("nomic-embed-text-v1.5-q4_k_m.gguf", 84_106_624L)
        assertEquals(RagRuntimeSupport.SUPPORTED, RagRuntimeCompatibility.evaluate(nomic))
        assertTrue(RagRuntimeCompatibility.admitsForRag(nomic))
    }

    @Test
    fun nomicStaysSemanticallyEmbedding() {
        assertEquals(ModelPurpose.EMBEDDING, ModelPurposeClassifier.fromArchitecture("nomic-bert"))
    }

    @Test
    fun exactBgeArtifactIsRuntimeUnsupported() {
        val bge = embedding("hf-compendiumlabs-bge-small-en-v1.5-gguf__bge-small-en-v1.5-q8_0.gguf", 36_806_944L)
        assertEquals(RagRuntimeSupport.UNSUPPORTED, RagRuntimeCompatibility.evaluate(bge))
    }

    @Test
    fun bgeStaysSemanticallyEmbedding() {
        assertEquals(ModelPurpose.EMBEDDING, ModelPurposeClassifier.fromArchitecture("bert"))
    }

    @Test
    fun bgeIsNotAdmittedForRag() {
        val bge = embedding("hf-compendiumlabs-bge-small-en-v1.5-gguf__bge-small-en-v1.5-q8_0.gguf", 36_806_944L)
        assertFalse(RagRuntimeCompatibility.admitsForRag(bge))
    }

    @Test
    fun unknownEmbeddingArtifactStaysUnknownNeverPromoted() {
        val unknown = embedding("some-future-embedding-model.gguf", 12_345_678L)
        assertEquals(RagRuntimeSupport.UNKNOWN, RagRuntimeCompatibility.evaluate(unknown))
    }

    @Test
    fun nomicWithUnrecordedSizeStillSupported() {
        val nomic = embedding("nomic-embed-text-v1.5-q4_k_m.gguf", 0L)
        assertEquals(RagRuntimeSupport.SUPPORTED, RagRuntimeCompatibility.evaluate(nomic))
    }

    @Test
    fun chatModelIsNotAdmittedAsRagEmbedding() {
        val chat = ModelInfo(
            id = "test:chat",
            name = "lfm25-350m",
            path = "/data/user/0/com.dark.tool_neuron/files/models/lfm25-350m__LFM2.5-350M-Q4_K_M.gguf",
            pathType = PathType.FILE,
            providerType = ProviderType.GGUF,
            fileSize = 229_312_224L,
        )
        assertFalse(RagRuntimeCompatibility.admitsForRag(chat))
        assertEquals(RagRuntimeSupport.UNKNOWN, RagRuntimeCompatibility.evaluate(chat))
    }
}
