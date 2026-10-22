package com.dark.tool_neuron.laptop

import android.os.Build
import com.dark.tool_neuron.BuildConfig
import com.dark.tool_neuron.runtime.CovertEngineBackend

object CipherRuntimeIdentity {
    fun revision(runtimeId: String, contextSize: Int): String = when (runtimeId) {
        "ggml" -> "ggml:aar-${BuildConfig.GGML_ARTIFACT_SHA256}:ctx-$contextSize:threads-1:fa-true:k-q8_0:v-q8_0:profile-cipher-greedy-chatml-1"
        "covert-r0" -> "covert-r0:${CovertEngineBackend.REVISION}:ctx-$contextSize"
        else -> error("Unknown runtime")
    }
    fun scope(assignment: CipherAssignment) = QualificationScope(assignment.artifactSha256,
        revision(assignment.runtimeId, assignment.contextSize), Build.FINGERPRINT + ":" + Build.SUPPORTED_ABIS.joinToString(","))
    fun config(assignment: CipherAssignment) = """{"runtimeId":"${assignment.runtimeId}","contextSize":${assignment.contextSize},"threadMode":1,"flashAttn":true,"cacheTypeK":"q8_0","cacheTypeV":"q8_0","cipherProfile":"cipher-greedy-chatml/1"}"""
}
