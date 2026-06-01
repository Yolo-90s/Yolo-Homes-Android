package com.example.yolo_homes.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.model.MaintenanceReceipt
import com.example.yolo_homes.data.model.Reading
import com.example.yolo_homes.data.repository.FlatRepository
import com.example.yolo_homes.data.repository.MaintenanceRepository
import com.example.yolo_homes.data.repository.ReadingRepository
import com.example.yolo_homes.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val apartmentName: String = "",
    val currency: String = "₹",
    val totalFlats: Int = 0,
    val maintenanceCollection: Double = 0.0,
    val waterRevenue: Double = 0.0,
    val waterConsumption: Double = 0.0,
    val recentReceipts: List<MaintenanceReceipt> = emptyList(),
    val recentReadings: List<Reading> = emptyList(),
    val flatsById: Map<String, Flat> = emptyMap()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    flatRepository: FlatRepository,
    maintenanceRepository: MaintenanceRepository,
    readingRepository: ReadingRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        flatRepository.observeFlats(),
        maintenanceRepository.observeReceipts(),
        readingRepository.observeReadings(),
        settingsRepository.observeSettings()
    ) { flats, receipts, readings, settings ->
        val currentPeriod = Formatters.periodKey()
        DashboardUiState(
            loading = false,
            apartmentName = settings.apartmentName,
            currency = settings.currency,
            totalFlats = flats.size,
            maintenanceCollection = receipts.filter { it.period == currentPeriod }.sumOf { it.amount },
            waterRevenue = readings.filter { Formatters.periodKey(it.date) == currentPeriod }
                .sumOf { it.amount },
            waterConsumption = readings.filter { Formatters.periodKey(it.date) == currentPeriod }
                .sumOf { it.usageLiters },
            recentReceipts = receipts.take(5),
            recentReadings = readings.take(5),
            flatsById = Flat.lookup(flats)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
