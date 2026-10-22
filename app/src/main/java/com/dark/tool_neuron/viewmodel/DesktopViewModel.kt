package com.dark.tool_neuron.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.laptop.CipherLaptop
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.laptop.resident.ResidentEngine
import com.dark.tool_neuron.laptop.terminal.TerminalProviderState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DesktopTelemetry(
    val availableMemoryBytes: Long? = null,
    val batteryPercent: Int? = null,
    val thermalStatus: Int? = null,
)

@HiltViewModel
class DesktopViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val laptop: LaptopManager,
    private val resident: ResidentEngine,
) : ViewModel() {

    val residentState: StateFlow<ResidentEngine.ResidentUiState> = resident.state
    val laptopState: StateFlow<CipherLaptop> = laptop.state
    val terminalProvider: StateFlow<TerminalProviderState> = laptop.terminalSessions.providerState

    val telemetry = flow {
        while (currentCoroutineContext().isActive) {
            val memory = runCatching {
                val manager = context.getSystemService(ActivityManager::class.java) ?: return@runCatching null
                ActivityManager.MemoryInfo().also(manager::getMemoryInfo).availMem
            }.getOrNull()
            val battery = runCatching {
                context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    ?.takeIf { it in 0..100 }
            }.getOrNull()
            val thermal = runCatching {
                context.getSystemService(PowerManager::class.java)?.currentThermalStatus
            }.getOrNull()
            emit(DesktopTelemetry(memory, battery, thermal))
            delay(5_000)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DesktopTelemetry())

    private val _clock = MutableStateFlow("--:--")
    val clock: StateFlow<String> = _clock.asStateFlow()

    init {
        viewModelScope.launch {
            val fmt = SimpleDateFormat("HH:mm", Locale.US)
            while (isActive) {
                _clock.value = fmt.format(Date())
                delay(30_000)
            }
        }
    }
}
