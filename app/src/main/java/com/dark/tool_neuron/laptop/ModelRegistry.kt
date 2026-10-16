package com.dark.tool_neuron.laptop

import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.repo.ModelCapabilityResolver
import com.dark.tool_neuron.util.CapabilityRuntime
import com.dark.tool_neuron.util.RuntimeSupport
import com.dark.tool_neuron.util.SemanticRole
import com.dark.tool_neuron.util.TaskAdmission

object ModelRegistry {
    data class ModelDossier(
        val id: String,
        val name: String,
        val artifactFileName: String,
        val artifactBytes: Long,
        val architecture: String?,
        val roles: Set<SemanticRole>,
        val runtimeSupport: Map<CapabilityRuntime, RuntimeSupport>,
        val admissions: Set<TaskAdmission>,
        val ragStatus: String?,
        val qualification: String?,
    )

    fun dossier(model: ModelInfo, architecture: String?, qualification: String? = null): ModelDossier {
        val profile = ModelCapabilityResolver.profileOf(model)
        val fileName = model.path.substringAfterLast('/').substringAfterLast('\\')
        return ModelDossier(
            id = model.id,
            name = model.name,
            artifactFileName = if (model.pathType == PathType.CONTENT_URI) "(imported content)" else fileName,
            artifactBytes = model.fileSize,
            architecture = architecture,
            roles = profile.roles,
            runtimeSupport = profile.runtimeSupport,
            admissions = profile.admissions,
            ragStatus = if (SemanticRole.EMBEDDING in profile.roles) {
                ModelCapabilityResolver.ragStatusText(model)
            } else null,
            qualification = qualification,
        )
    }
}
