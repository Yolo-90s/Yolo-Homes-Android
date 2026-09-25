package com.example.yolo_homes.feature.flats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class YearGroup<T>(val year: String, val items: List<T>)

data class FlatDetailUiState(
    val loading: Boolean = true,
    val flat: Flat? = null,
    val currency: String = "₹",
    val maintenanceMonthlyRate: Double = 1500.0,
    val receiptsByYear: List<YearGroup<MaintenanceReceipt>> = emptyList(),
    val readingsByYear: List<YearGroup<Reading>> = emptyList()
)

/**
 * Per-flat bill history: every maintenance receipt + water reading ever recorded for one unit,
 * grouped by year. `flatId` (the nav arg) may be either the flat number or the operational
 * `flats` doc id (Flat Directory rows use whichever they have) — resolved to the real ops id
 * before querying, so receipts/readings (which key off that id) are found.
 */
@HiltViewModel
class FlatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    flatRepository: FlatRepository,
    maintenanceRepository: MaintenanceRepository,
    readingRepository: ReadingRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val flatKey: String = savedStateHandle.get<String>("flatId").orEmpty()

    private val flatFlow = combine(
        flatRepository.observeFlats(),
        flatRepository.observeMasterFlats()
    ) { ops, master ->
        ops.firstOrNull { it.id == flatKey || it.flatNo == flatKey }
            ?: master.firstOrNull { it.id == flatKey || it.flatNo == flatKey }
    }

    val uiState: StateFlow<FlatDetailUiState> = combine(
        flatFlow,
        settingsRepository.observeSettings()
    ) { flat, settings -> flat to settings }
        .flatMapLatest { (flat, settings) ->
            val opsId = flat?.id?.takeIf { it.isNotBlank() } ?: flatKey
            combine(
                maintenanceRepository.observeReceiptsForFlat(opsId),
                readingRepository.observeReadingsForFlat(opsId)
            ) { receipts, readings ->
                FlatDetailUiState(
                    loading = false,
                    flat = flat,
                    currency = settings.currency,
                    maintenanceMonthlyRate = settings.maintenanceMonthlyRate,
                    receiptsByYear = groupByYear(receipts) { it.paidDate },
                    readingsByYear = groupByYear(readings) { it.date }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlatDetailUiState())

    private fun <T> groupByYear(items: List<T>, dateOf: (T) -> Long): List<YearGroup<T>> {
        val fmt = SimpleDateFormat("yyyy", Locale.getDefault())
        return items
            .groupBy { fmt.format(Date(dateOf(it))) }
            .toList()
            .sortedByDescending { it.first }
            .map { (year, list) -> YearGroup(year, list) }
    }
}
