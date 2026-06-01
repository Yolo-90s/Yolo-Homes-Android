package com.example.yolo_homes.feature.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.model.MaintenanceReceipt
import com.example.yolo_homes.data.SessionManager
import com.example.yolo_homes.data.repository.AuthRepository
import com.example.yolo_homes.data.repository.FlatRepository
import com.example.yolo_homes.data.repository.MaintenanceRepository
import com.example.yolo_homes.data.repository.SettingsRepository
import com.example.yolo_homes.ui.components.ChartEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaintenanceUiState(
    val loading: Boolean = true,
    val currency: String = "₹",
    val currentMonthTotal: Double = 0.0,
    val totalEntries: Int = 0,
    val trend: List<ChartEntry> = emptyList(),
    val receipts: List<MaintenanceReceipt> = emptyList(),
    val flats: List<Flat> = emptyList(),
    val flatsById: Map<String, Flat> = emptyMap()
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val authRepository: AuthRepository,
    flatRepository: FlatRepository,
    settingsRepository: SettingsRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<MaintenanceUiState> = combine(
        maintenanceRepository.observeReceipts(),
        flatRepository.observeFlats(),
        settingsRepository.observeSettings(),
        sessionManager.session
    ) { receipts, flats, settings, session ->
        // Residents (owner/tenant) only see their own flat's receipts.
        val scoped = if (session != null && session.isResident && session.flatId != null) {
            receipts.filter { it.flatId == session.flatId }
        } else receipts

        val periods = Formatters.recentPeriods(6)
        val trend = periods.map { p ->
            ChartEntry(
                label = Formatters.monthLabel(p).take(3),
                value = scoped.filter { it.period == p }.sumOf { it.amount }
            )
        }
        MaintenanceUiState(
            loading = false,
            currency = settings.currency,
            currentMonthTotal = scoped.filter { it.period == Formatters.periodKey() }.sumOf { it.amount },
            totalEntries = scoped.size,
            trend = trend,
            receipts = scoped,
            flats = flats,
            flatsById = Flat.lookup(flats)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MaintenanceUiState())

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun addReceipt(flatId: String, amount: Double, period: String, paymentMethod: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            runCatching {
                maintenanceRepository.addReceipt(
                    MaintenanceReceipt(
                        flatId = flatId,
                        amount = amount,
                        period = period,
                        paymentMethod = paymentMethod,
                        capturedBy = authRepository.currentUid ?: "",
                        edited = false
                    )
                )
            }.onSuccess { _saveState.value = SaveState.Success }
                .onFailure { _saveState.value = SaveState.Error(it.message ?: "Save failed") }
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }
}

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data class Error(val message: String) : SaveState
}
