package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.CapabilityRegistry
import com.dark.tool_neuron.laptop.ModelRegistry
import com.dark.tool_neuron.laptop.ToolRegistry
import com.dark.tool_neuron.laptop.ToolStatus
import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.model.enums.ProviderType
import com.dark.tool_neuron.util.SemanticRole
import com.dark.tool_neuron.util.TaskAdmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaptopRegistryTest {
    private val modelsDir = "/data/user/0/com.dark.tool_neuron/files/models"

    private fun model(
        fileName: String,
        size: Long,
        type: ProviderType = ProviderType.EMBEDDING,
    ) = ModelInfo(
        id = "test:$fileName",
        name = fileName,
        path = "$modelsDir/$fileName",
        pathType = PathType.FILE,
        providerType = type,
        fileSize = size,
    )

    private fun nomic() = model("nomic-embed-text-v1.5-q4_k_m.gguf", 84_106_624L)
    private fun bge() = model("hf-compendiumlabs-bge-small-en-v1.5-gguf__bge-small-en-v1.5-q8_0.gguf", 36_806_944L)
    private fun chat() = model("lfm25-350m__LFM2.5-350M-Q4_K_M.gguf", 229_312_224L, ProviderType.GGUF)

    @Test
    fun capabilityVocabularyLookup() {
        assertEquals(6, CapabilityRegistry.SEED.size)
        assertNotNull(CapabilityRegistry.lookup("dev.vibe.workspace@1"))
        val refs = CapabilityRegistry.SEED.map { CapabilityRegistry.ref(it) }
        assertEquals(refs.size, refs.toSet().size)

        val terminal = CapabilityRegistry.lookup("dev.terminal.execute@1")
        assertTrue(terminal != null)
        assertTrue(terminal!!.requiredEffects.contains("terminal.execute"))

        assertNull(CapabilityRegistry.lookup("dev.terminal.execute@2"))
        assertNull(CapabilityRegistry.lookup("dev.terminal.execute"))
        assertNull(CapabilityRegistry.lookup("@1"))
        assertNull(CapabilityRegistry.lookup("dev.terminal.execute@x"))
        assertNull(CapabilityRegistry.lookup("nope.nope@1"))
    }

    @Test
    fun toolRegistryReflectsRealTerminalProvider() {
        val terminal = ToolRegistry.SEEDS.first { it.id == "terminal.execute" }
        assertEquals(ToolStatus.AVAILABLE, terminal.status)
        assertTrue(terminal.note.contains("tn.terminal.system-sh"))
        assertTrue(ToolRegistry.SEEDS.any { it.id == "filesystem.workspace" && it.status == ToolStatus.AVAILABLE })
    }

    @Test
    fun dossierProjectsTruthfulModelState() {
        val nomicDossier = ModelRegistry.dossier(nomic(), architecture = "nomic-bert")
        assertEquals(setOf(SemanticRole.EMBEDDING), nomicDossier.roles)
        assertTrue(TaskAdmission.CAN_EMBED in nomicDossier.admissions)
        assertEquals("Ready", nomicDossier.ragStatus)
        assertEquals("nomic-bert", nomicDossier.architecture)

        val bgeDossier = ModelRegistry.dossier(bge(), architecture = "bert")
        assertEquals(setOf(SemanticRole.EMBEDDING), bgeDossier.roles)
        assertFalse(TaskAdmission.CAN_EMBED in bgeDossier.admissions)
        assertEquals("Not compatible with current RAG runtime", bgeDossier.ragStatus)

        val chatDossier = ModelRegistry.dossier(chat(), architecture = "lfm2")
        assertEquals(setOf(SemanticRole.GENERATION), chatDossier.roles)
        assertTrue(TaskAdmission.CAN_CHAT in chatDossier.admissions)
        assertFalse(TaskAdmission.CAN_EMBED in chatDossier.admissions)
        assertNull(chatDossier.ragStatus)
    }

    @Test
    fun dossierLabelsImportedContent() {
        val imported = ModelInfo(
            id = "import:1",
            name = "imported.gguf",
            path = "content://com.android.providers.downloads/document/1234",
            pathType = PathType.CONTENT_URI,
            providerType = ProviderType.GGUF,
            fileSize = 123L,
        )
        val dossier = ModelRegistry.dossier(imported, architecture = null)
        assertEquals("(imported content)", dossier.artifactFileName)
        assertNull(dossier.architecture)
    }
}
