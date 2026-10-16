package com.dark.tool_neuron.repo

import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.ProviderType

enum class RagRuntimeSupport { SUPPORTED, UNSUPPORTED, UNKNOWN }

object RagRuntimeCompatibility {
    const val RAG_RUNTIME_INCOMPATIBLE_MESSAGE =
        "This embedding model is not compatible with the current RAG runtime."

    // Exact-artifact compatibility truth. Provenance (device evidence 2026-10-14, HEAD
    // 4ef443dea40eb616d5e13edca33bc9cb84770cae, APK 451606C1D1DA75B7B74AEA2A353614EE6ABA04216315CCFD68063BC30F3C0CDA):
    // - nomic-embed-text-v1.5-q4_k_m.gguf, sha256 d4e388894e09cf3816e8b0896d81d265b55e7a9fff9ab03fe8bf4ef5e11295ac:
    //   ingest PASS x2 (FTS5 16 chunks, vector snapshot 28->56 KB, :inference alive).
    // - hf-compendiumlabs-bge-small-en-v1.5-gguf__bge-small-en-v1.5-q8_0.gguf,
    //   sha256 ec38e8da142596baa913124ae50550de284b6916bf59577ef2f0cb9660c2f514:
    //   deterministic native SIGABRT in :inference ~214 ms into the first embed (x2 runs).
    // A general rule is NOT provable (no AAR source; n=1 failing artifact), so this layer
    // encodes exact-artifact truth only. Unknown artifacts stay UNKNOWN - never silently
    // promoted to SUPPORTED. Admission is fail-closed: only SUPPORTED artifacts pass
    // admitsForRag; the RAG embed path has a device-proven abort class, so unverified
    // artifacts are refused before any native execution (ModelCapabilityResolver carries
    // the full admission doctrine). Full evidence: evidence/bge-compat-metadata-comparison.md.
    private const val SUPPORTED_NOMIC_FILE = "nomic-embed-text-v1.5-q4_k_m.gguf"
    private const val SUPPORTED_NOMIC_SIZE = 84_106_624L
    private const val UNSUPPORTED_BGE_FILE =
        "hf-compendiumlabs-bge-small-en-v1.5-gguf__bge-small-en-v1.5-q8_0.gguf"

    fun evaluate(model: ModelInfo): RagRuntimeSupport {
        if (model.providerType != ProviderType.EMBEDDING) return RagRuntimeSupport.UNKNOWN
        val fileName = model.path.substringAfterLast('/').substringAfterLast('\\')
        return when {
            fileName == UNSUPPORTED_BGE_FILE -> RagRuntimeSupport.UNSUPPORTED
            fileName == SUPPORTED_NOMIC_FILE &&
                (model.fileSize == 0L || model.fileSize == SUPPORTED_NOMIC_SIZE) -> RagRuntimeSupport.SUPPORTED
            else -> RagRuntimeSupport.UNKNOWN
        }
    }

    fun admitsForRag(model: ModelInfo): Boolean =
        model.providerType == ProviderType.EMBEDDING && evaluate(model) == RagRuntimeSupport.SUPPORTED
}
