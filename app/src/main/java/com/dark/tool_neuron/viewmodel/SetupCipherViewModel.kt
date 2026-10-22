package com.dark.tool_neuron.viewmodel

import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.lifecycle.ViewModel
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.laptop.resident.ResidentModelSelection
import com.dark.tool_neuron.laptop.resident.ResidentSelector
import com.dark.tool_neuron.repo.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SetupCipherViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val laptop: LaptopManager,
    private val modelRepo: ModelRepository,
) : ViewModel() {

    data class DeviceInfo(
        val device: String,
        val soc: String,
        val abis: String,
        val ramMb: Long,
        val freeStorageMb: Long,
    )

    data class ResidentAdmission(val status: String, val detail: String)

    val deviceInfo: DeviceInfo = computeDeviceInfo()

    private val _admission = MutableStateFlow(computeAdmission())
    val admission: StateFlow<ResidentAdmission> = _admission.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _projectName = MutableStateFlow(laptop.core.activeWorkspace()?.name)
    val projectName: StateFlow<String?> = _projectName.asStateFlow()

    fun refreshAdmission() {
        _admission.value = computeAdmission()
    }

    fun createProject(name: String) {
        laptop.core.createWorkspace(name)
            .onSuccess {
                _projectName.value = it.name
                _status.value = "project created: ${it.name}"
            }
            .onFailure { _status.value = "could not create project: ${it.message}" }
    }

    private fun computeAdmission(): ResidentAdmission {
        val assignment = laptop.state.value.cipherAssignment
        return if (assignment == null) ResidentAdmission("EMPTY", "No assistant assigned. Import a model, then assign and qualify it in Cipher / Model Lab after setup.")
        else ResidentAdmission("ASSIGNED", "${assignment.modelId} · ${assignment.contextSize} context · ${assignment.runtimeId}. Readiness is checked in Cipher against actual file/runtime/device evidence.")
    }

    private fun computeDeviceInfo(): DeviceInfo {
        val ramMb = runCatching {
            File("/proc/meminfo").readLines().firstOrNull { it.startsWith("MemTotal:") }
                ?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull()?.div(1024)
        }.getOrNull() ?: 0L
        val freeMb = runCatching {
            val st = StatFs(context.filesDir.absolutePath)
            st.availableBytes / (1024 * 1024)
        }.getOrNull() ?: 0L
        return DeviceInfo(
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else "unknown",
            abis = Build.SUPPORTED_ABIS.joinToString(","),
            ramMb = ramMb,
            freeStorageMb = freeMb,
        )
    }

    private fun readMemAvailableBytes(): Long = runCatching {
        File("/proc/meminfo").readLines().firstOrNull { it.startsWith("MemAvailable:") }
            ?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull()?.times(1024L)
    }.getOrNull() ?: 0L
}
