package com.example.yolo_homes.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.MaintenanceReceipt
import com.example.yolo_homes.data.model.Reading
import com.example.yolo_homes.data.repository.MaintenanceRepository
import com.example.yolo_homes.data.repository.ReadingRepository
import com.example.yolo_homes.data.repository.SettingsRepository
import com.example.yolo_homes.ui.components.ChartEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

data class ReportsUiState(
    val loading: Boolean = true,
    val currency: String = "₹",
    val maintenanceByMonth: List<ChartEntry> = emptyList(),
    val waterByMonth: List<ChartEntry> = emptyList(),
    val totalMaintenance: Double = 0.0,
    val totalWaterRevenue: Double = 0.0,
    val totalConsumption: Double = 0.0,
    val receipts: List<MaintenanceReceipt> = emptyList(),
    val readings: List<Reading> = emptyList()
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    maintenanceRepository: MaintenanceRepository,
    readingRepository: ReadingRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<ReportsUiState> = combine(
        maintenanceRepository.observeReceipts(limit = 500),
        readingRepository.observeReadings(limit = 500),
        settingsRepository.observeSettings()
    ) { receipts, readings, settings ->
        val periods = Formatters.recentPeriods(12)
        ReportsUiState(
            loading = false,
            currency = settings.currency,
            maintenanceByMonth = periods.map { p ->
                ChartEntry(Formatters.monthLabel(p).take(3), receipts.filter { it.period == p }.sumOf { it.amount })
            },
            waterByMonth = periods.map { p ->
                ChartEntry(
                    Formatters.monthLabel(p).take(3),
                    readings.filter { Formatters.periodKey(it.date) == p }.sumOf { it.amount }
                )
            },
            totalMaintenance = receipts.sumOf { it.amount },
            totalWaterRevenue = readings.sumOf { it.amount },
            totalConsumption = readings.sumOf { it.usageLiters },
            receipts = receipts,
            readings = readings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}
