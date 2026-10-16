package com.dark.tool_neuron.laptop.resident

import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.repo.ModelCapabilityResolver
import com.dark.tool_neuron.util.TaskAdmission

sealed class ResidentModelSelection {
    data class Selected(val model: ModelInfo, val reason: String) : ResidentModelSelection()
    data class Unavailable(val reason: String) : ResidentModelSelection()
}

object ResidentSelector {
    fun select(models: List<ModelInfo>, memAvailableBytes: Long): ResidentModelSelection {
        val admitted = models.filter {
            TaskAdmission.CAN_CHAT in ModelCapabilityResolver.profileOf(it).admissions
        }
        if (admitted.isEmpty()) return ResidentModelSelection.Unavailable("no admitted GENERATION model is installed")

        val loadable = admitted.filter { m ->
            m.fileSize > 0 && m.fileSize * 6 / 5 <= memAvailableBytes
        }
        if (loadable.isEmpty()) {
            val need = admitted.minOf { it.fileSize } * 6 / 5
            return ResidentModelSelection.Unavailable(
                "no admitted model passes memory admission (need >= $need B, available $memAvailableBytes B)",
            )
        }

        val active = loadable.firstOrNull { it.isActive }
        if (active != null) {
            return ResidentModelSelection.Selected(active, "active admitted model passes memory admission")
        }
        val smallest = loadable.minByOrNull { it.fileSize }!!
        return ResidentModelSelection.Selected(smallest, "smallest admitted model passing memory admission")
    }
}
