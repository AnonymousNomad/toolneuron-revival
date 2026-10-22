package com.dark.tool_neuron.laptop

import android.content.Context
import com.dark.tool_neuron.laptop.resident.ResidentModelSelection
import com.dark.tool_neuron.laptop.resident.ResidentSelector
import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.repo.ModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CipherAssignmentInspector @Inject constructor(@param:ApplicationContext private val context: Context,
    private val laptop: LaptopManager, private val models: ModelRepository) {
    data class Inspection(val assignment: CipherAssignment?, val model: ModelInfo?, val assessment: CipherSlotAssessment)
    suspend fun assign(modelId: String, runtimeId: String, contextSize: Int) = withContext(Dispatchers.IO) {
        val model = models.models.value.firstOrNull { it.id == modelId } ?: error("Model no longer installed")
        val sha = hash(model)
        val assignment = CipherAssignment(model.id, sha, runtimeId, contextSize)
        laptop.core.assignCipher(assignment).getOrThrow()
    }
    suspend fun inspect(): Inspection = withContext(Dispatchers.IO) {
        val assignment = laptop.state.value.cipherAssignment
            ?: return@withContext Inspection(null, null, CipherSlotAssessment(CipherSlotStatus.EMPTY, "No assistant model assigned."))
        val model = models.models.value.firstOrNull { it.id == assignment.modelId }
        val actualHash = model?.let { runCatching { hash(it) }.getOrNull() }
        val available = actualHash == assignment.artifactSha256
        val fit = model != null && ResidentSelector.select(listOf(model), availableMemory()) is ResidentModelSelection.Selected
        Inspection(assignment, model, CipherSlotPolicy.assess(assignment, CipherRuntimeIdentity.scope(assignment),
            laptop.state.value.qualifications, available, assignment.runtimeId == "ggml", fit))
    }
    suspend fun hash(model: ModelInfo): String = withContext(Dispatchers.IO) {
        require(model.pathType == PathType.FILE) { "Import this GGUF into durable app storage before assignment" }
        val file = File(model.path).canonicalFile
        require(file.toPath().startsWith(context.filesDir.canonicalFile.toPath()) && file.isFile) { "Model must be an app-owned file" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val magic = ByteArray(4)
            check(input.read(magic) == 4 && magic.contentEquals(byteArrayOf(71, 71, 85, 70))) { "Not a GGUF artifact" }
            digest.update(magic)
            val buffer = ByteArray(1 shl 20)
            while (true) {
                currentCoroutineContext().ensureActive()
                val size = input.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun availableMemory(): Long = runCatching {
        File("/proc/meminfo").useLines { lines -> lines.first { it.startsWith("MemAvailable:") }.trim().split(Regex("\\s+"))[1].toLong() * 1024 }
    }.getOrDefault(0)
}
