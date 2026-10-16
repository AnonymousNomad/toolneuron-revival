package com.dark.tool_neuron

import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.model.enums.ProviderType
import com.dark.tool_neuron.repo.ModelCapabilityResolver
import com.dark.tool_neuron.repo.RagRuntimeCompatibility
import com.dark.tool_neuron.repo.RagRuntimeSupport
import com.dark.tool_neuron.util.CapabilityRuntime
import com.dark.tool_neuron.util.RuntimeSupport
import com.dark.tool_neuron.util.SemanticRole
import com.dark.tool_neuron.util.TaskAdmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCapabilityTest {
    private val modelsDir = "/data/user/0/com.dark.tool_neuron/files/models"

    private fun model(
        fileName: String,
        size: Long,
        type: ProviderType = ProviderType.EMBEDDING,
        dir: String = modelsDir,
    ) = ModelInfo(
        id = "test:$fileName",
        name = fileName,
        path = "$dir/$fileName",
        pathType = PathType.FILE,
        providerType = type,
        fileSize = size,
    )

    private fun nomic() = model("nomic-embed-text-v1.5-q4_k_m.gguf", 84_106_624L)

    private fun bge() = model("hf-compendiumlabs-bge-small-en-v1.5-gguf__bge-small-en-v1.5-q8_0.gguf", 36_806_944L)

    private fun chat() = model("lfm25-350m__LFM2.5-350M-Q4_K_M.gguf", 229_312_224L, ProviderType.GGUF)

    private fun vlm() = model("base.gguf", 1_000_000L, ProviderType.GGUF, "$modelsDir/vlm/Qwen3-VL-2B-Instruct-GGUF")

    @Test
    fun semanticRoleIsIndependentFromRuntimeCompatibility() {
        val profile = ModelCapabilityResolver.profileOf(bge())
        assertEquals(setOf(SemanticRole.EMBEDDING), profile.roles)
        assertEquals(RuntimeSupport.UNSUPPORTED, profile.supportFor(CapabilityRuntime.RAG_EMBED))
        assertFalse(TaskAdmission.CAN_EMBED in profile.admissions)
    }

    @Test
    fun nomicEmbeddingRagSupportedAndAdmitted() {
        val profile = ModelCapabilityResolver.profileOf(nomic())
        assertEquals(setOf(SemanticRole.EMBEDDING), profile.roles)
        assertEquals(RuntimeSupport.SUPPORTED, profile.supportFor(CapabilityRuntime.RAG_EMBED))
        assertTrue(TaskAdmission.CAN_EMBED in profile.admissions)
        assertEquals("Ready", ModelCapabilityResolver.ragStatusText(nomic()))
    }

    @Test
    fun bgeEmbeddingRagUnsupportedRefused() {
        val profile = ModelCapabilityResolver.profileOf(bge())
        assertEquals(RuntimeSupport.UNSUPPORTED, profile.supportFor(CapabilityRuntime.RAG_EMBED))
        assertFalse(TaskAdmission.CAN_EMBED in profile.admissions)
        assertEquals(
            "Not compatible with current RAG runtime",
            ModelCapabilityResolver.ragStatusText(bge()),
        )
    }

    @Test
    fun generationModelRejectedForRag() {
        val profile = ModelCapabilityResolver.profileOf(chat())
        assertEquals(setOf(SemanticRole.GENERATION), profile.roles)
        assertFalse(TaskAdmission.CAN_EMBED in profile.admissions)
        assertTrue(TaskAdmission.CAN_CHAT in profile.admissions)
    }

    @Test
    fun unknownRoleModelIsFailClosed() {
        val unknown = model("mystery.gguf", 5_000_000L, ProviderType.UNKNOWN)
        val profile = ModelCapabilityResolver.profileOf(unknown)
        assertEquals(setOf(SemanticRole.UNKNOWN), profile.roles)
        assertTrue(profile.admissions.isEmpty())
    }

    @Test
    fun unverifiedEmbeddingArtifactIsFailClosed() {
        val unverified = model("some-future-embedding-model.gguf", 12_345_678L)
        val profile = ModelCapabilityResolver.profileOf(unverified)
        assertEquals(setOf(SemanticRole.EMBEDDING), profile.roles)
        assertEquals(RuntimeSupport.UNKNOWN, profile.supportFor(CapabilityRuntime.RAG_EMBED))
        assertFalse(TaskAdmission.CAN_EMBED in profile.admissions)
        assertEquals(
            "Not verified for current RAG runtime",
            ModelCapabilityResolver.ragStatusText(unverified),
        )
    }

    @Test
    fun defaultSelectionChoosesOnlyAdmissibleModels() {
        val bge = bge()
        val nomic = nomic()
        assertEquals(nomic.id, ModelCapabilityResolver.selectRagEmbeddingModel(listOf(bge, nomic), null)?.id)
        assertEquals(nomic.id, ModelCapabilityResolver.selectRagEmbeddingModel(listOf(bge, nomic), nomic.id)?.id)
        assertEquals(bge.id, ModelCapabilityResolver.selectRagEmbeddingModel(listOf(bge, nomic), bge.id)?.id)
        assertEquals(bge.id, ModelCapabilityResolver.selectRagEmbeddingModel(listOf(bge), null)?.id)
        assertNull(ModelCapabilityResolver.selectRagEmbeddingModel(emptyList(), null))
        assertNull(ModelCapabilityResolver.selectRagEmbeddingModel(listOf(chat()), null))
    }

    @Test
    fun bgeCompatibilityGuardRemainsGreen() {
        assertEquals(RagRuntimeSupport.UNSUPPORTED, RagRuntimeCompatibility.evaluate(bge()))
        assertFalse(RagRuntimeCompatibility.admitsForRag(bge()))
        assertTrue(RagRuntimeCompatibility.admitsForRag(nomic()))
        assertFalse(RagRuntimeCompatibility.admitsForRag(model("some-future-embedding-model.gguf", 12_345_678L)))
    }

    @Test
    fun chatAndVisionAdmissionDoesNotRegress() {
        val chatProfile = ModelCapabilityResolver.profileOf(chat())
        assertTrue(TaskAdmission.CAN_CHAT in chatProfile.admissions)
        assertFalse(TaskAdmission.CAN_EMBED in chatProfile.admissions)

        val vlmProfile = ModelCapabilityResolver.profileOf(vlm())
        assertEquals(setOf(SemanticRole.GENERATION, SemanticRole.VISION), vlmProfile.roles)
        assertTrue(TaskAdmission.CAN_CHAT in vlmProfile.admissions)
        assertTrue(TaskAdmission.CAN_VISION in vlmProfile.admissions)
        assertFalse(TaskAdmission.CAN_EMBED in vlmProfile.admissions)
    }
}
