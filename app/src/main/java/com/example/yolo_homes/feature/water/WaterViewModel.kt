package com.example.yolo_homes.feature.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.model.Reading
import com.example.yolo_homes.data.model.WaterBill
import com.example.yolo_homes.data.SessionManager
import com.example.yolo_homes.data.repository.AuthRepository
import com.example.yolo_homes.data.repository.FlatRepository
import com.example.yolo_homes.data.repository.ReadingRepository
import com.example.yolo_homes.data.repository.SettingsRepository
import com.example.yolo_homes.ui.components.ChartEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A flat's consumption for a given period (may have no reading yet). */
data class FlatConsumption(
    val flat: Flat,
    val liters: Double,
    val amount: Double,
    val hasReading: Boolean,
    val previousReading: Double = 0.0,
    val currentReading: Double = 0.0,
    val excessLiters: Double = 0.0,
    val date: Long = 0L,
    val edited: Boolean = false
)

data class ConsumerStat(val flat: Flat, val liters: Double)

data class WaterUiState(
    val loading: Boolean = true,
    val currency: String = "₹",
    val settings: AppSettings = AppSettings(),
    val totalConsumption: Double = 0.0,
    val revenue: Double = 0.0,
    val highest: ConsumerStat? = null,
    val lowest: ConsumerStat? = null,
    val trend: List<ChartEntry> = emptyList(),
    val topConsumers: List<ConsumerStat> = emptyList(),
    val readings: List<Reading> = emptyList(),
    val flats: List<Flat> = emptyList(),
    val flatsById: Map<String, Flat> = emptyMap(),
    // --- Flat-wise consumption for the selected month ---
    val availablePeriods: List<String> = emptyList(),
    val selectedPeriod: String = Formatters.periodKey(),
    val flatConsumption: List<FlatConsumption> = emptyList(),
    val selectedMonthTotal: Double = 0.0,
    val selectedMonthRevenue: Double = 0.0
)

/** Holds the live-calculated preview for the Add Reading form. */
data class ReadingDraft(
    val previous: Double = 0.0,
    val current: Double = 0.0,
    val bill: WaterBill = WaterBill(0.0, 0.0, 0.0)
)

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val readingRepository: ReadingRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    flatRepository: FlatRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(Formatters.periodKey())

    // The complete building registry lives in `masterFlats` (kept up to date via Manage Residents),
    // while the operational `flats` collection is what older readings reference by auto-id. We need
    // both: the union for "every flat", and the auto-id↔flatNo cross-reference so existing readings
    // still resolve to a flat. combine() maxes out at 5 flows, so fold the two flat sources first.
    private val flatsFlow = combine(
        flatRepository.observeMasterFlats(),
        flatRepository.observeFlats()
    ) { master, ops -> master to ops }

    val uiState: StateFlow<WaterUiState> = combine(
        readingRepository.observeReadings(limit = 500),
        flatsFlow,
        settingsRepository.observeSettings(),
        selectedPeriod,
        sessionManager.session
    ) { readingsAll, flatsPair, settings, period, session ->
        val (masterFlats, opsFlats) = flatsPair

        // A reading's flatId is usually the operational `flats` doc auto-id; map it back to a flatNo.
        val autoIdToNo = opsFlats
            .filter { it.id.isNotBlank() && it.flatNo.isNotBlank() }
            .associate { it.id to it.flatNo }
        fun canonicalNo(flatId: String): String = autoIdToNo[flatId] ?: flatId

        // Every flat known in either collection, keyed by flatNo. masterFlats wins (it carries the
        // maintained owner/tenant/block details); any flat present only in `flats` is still included.
        val byNo = LinkedHashMap<String, Flat>()
        opsFlats.forEach { if (it.flatNo.isNotBlank()) byNo[it.flatNo] = it }
        masterFlats.forEach { if (it.flatNo.isNotBlank()) byNo[it.flatNo] = it }
        val allFlats = byNo.values.sortedBy { it.flatNo }

        // Residents (owner/tenant) only see their own flat — resolve their flatNo from their flats-doc id.
        val residentNo = session?.flatId?.let { canonicalNo(it) }
        val resident = session != null && session.isResident && residentNo != null
        val flats = if (resident) allFlats.filter { it.flatNo == residentNo } else allFlats
        val readings = if (resident) readingsAll.filter { canonicalNo(it.flatId) == residentNo } else readingsAll

        val flatByNo = flats.associateBy { it.flatNo }
        // Lookup keyed by flatNo, doc id AND the operational auto-id, so any reading resolves to its flat.
        val flatsById = HashMap<String, Flat>()
        flats.forEach { f ->
            if (f.flatNo.isNotBlank()) flatsById[f.flatNo] = f
            if (f.id.isNotBlank()) flatsById[f.id] = f
        }
        autoIdToNo.forEach { (autoId, no) -> flatByNo[no]?.let { flatsById[autoId] = it } }

        val currentMonth = Formatters.periodKey()
        val currentReadings = readings.filter { Formatters.periodKey(it.date) == currentMonth }

        val perFlatCurrent = currentReadings.groupBy { canonicalNo(it.flatId) }
            .mapNotNull { (no, list) ->
                flatByNo[no]?.let { ConsumerStat(it, list.sumOf { r -> r.usageLiters }) }
            }
            .sortedByDescending { it.liters }

        val periods = Formatters.recentPeriods(12).reversed()
        val trend = Formatters.recentPeriods(6).map { p ->
            ChartEntry(
                label = Formatters.monthLabel(p).take(3),
                value = readings.filter { Formatters.periodKey(it.date) == p }.sumOf { it.usageLiters }
            )
        }

        // Flat-wise consumption for the *selected* month (all flats, even with no reading).
        val selectedReadings = readings.filter { Formatters.periodKey(it.date) == period }
        val byFlatNo = selectedReadings.groupBy { canonicalNo(it.flatId) }
        val consumption = flats.map { flat ->
            val list = (byFlatNo[flat.flatNo] ?: byFlatNo[flat.id] ?: emptyList()).sortedBy { it.date }
            FlatConsumption(
                flat = flat,
                liters = list.sumOf { it.usageLiters },
                amount = list.sumOf { it.amount },
                hasReading = list.isNotEmpty(),
                // For a month with multiple readings: open with the earliest, close with the latest.
                previousReading = list.firstOrNull()?.previousReading ?: 0.0,
                currentReading = list.lastOrNull()?.currentReading ?: 0.0,
                excessLiters = list.sumOf { it.excessLiters },
                date = list.lastOrNull()?.date ?: 0L,
                edited = list.any { it.edited }
            )
        }.sortedByDescending { it.liters }

        WaterUiState(
            loading = false,
            currency = settings.currency,
            settings = settings,
            totalConsumption = currentReadings.sumOf { it.usageLiters },
            revenue = currentReadings.sumOf { it.amount },
            highest = perFlatCurrent.firstOrNull(),
            lowest = perFlatCurrent.lastOrNull(),
            trend = trend,
            topConsumers = perFlatCurrent.take(5),
            readings = readings,
            flats = flats,
            flatsById = flatsById,
            availablePeriods = periods,
            selectedPeriod = period,
            flatConsumption = consumption,
            selectedMonthTotal = consumption.sumOf { it.liters },
            selectedMonthRevenue = consumption.sumOf { it.amount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WaterUiState())

    fun selectPeriod(period: String) { selectedPeriod.value = period }

    private val _draft = MutableStateFlow(ReadingDraft())
    val draft: StateFlow<ReadingDraft> = _draft.asStateFlow()

    private val _saveState = MutableStateFlow<WaterSaveState>(WaterSaveState.Idle)
    val saveState: StateFlow<WaterSaveState> = _saveState.asStateFlow()

    /** Loads the latest reading for a flat to seed the "previous reading" field. */
    fun onFlatSelected(flatId: String) {
        viewModelScope.launch {
            val latest = readingRepository.getLatestForFlat(flatId)
            val prev = latest?.currentReading ?: 0.0
            _draft.value = ReadingDraft(previous = prev, current = prev, bill = WaterBill(0.0, 0.0, 0.0))
        }
    }

    fun onCurrentReadingChanged(current: Double) {
        viewModelScope.launch {
            val settings = settingsRepository.observeSettings().first()
            val prev = _draft.value.previous
            _draft.value = _draft.value.copy(
                current = current,
                bill = settings.computeBill(prev, current)
            )
        }
    }

    fun saveReading(flatId: String, hasImage: Boolean = false) {
        viewModelScope.launch {
            _saveState.value = WaterSaveState.Saving
            val d = _draft.value
            runCatching {
                readingRepository.addReading(
                    Reading(
                        flatId = flatId,
                        previousReading = d.previous,
                        currentReading = d.current,
                        usageLiters = d.bill.usage,
                        excessLiters = d.bill.excess,
                        amount = d.bill.amount,
                        capturedBy = authRepository.currentUid ?: "",
                        edited = false,
                        hasImage = hasImage
                    )
                )
            }.onSuccess { _saveState.value = WaterSaveState.Success }
                .onFailure { _saveState.value = WaterSaveState.Error(it.message ?: "Save failed") }
        }
    }

    fun resetDraft() { _draft.value = ReadingDraft() }
    fun resetSaveState() { _saveState.value = WaterSaveState.Idle }
}

sealed interface WaterSaveState {
    data object Idle : WaterSaveState
    data object Saving : WaterSaveState
    data object Success : WaterSaveState
    data class Error(val message: String) : WaterSaveState
}
