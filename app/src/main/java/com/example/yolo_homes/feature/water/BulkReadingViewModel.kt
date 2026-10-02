package com.example.yolo_homes.feature.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.model.Reading
import com.example.yolo_homes.data.model.WaterBill
import com.example.yolo_homes.data.repository.AuthRepository
import com.example.yolo_homes.data.repository.FlatRepository
import com.example.yolo_homes.data.repository.ReadingRepository
import com.example.yolo_homes.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One editable row in the Bulk Upload review table. */
data class BulkRow(
    val rawLine: String,
    val flatId: String = "",
    val previousReadingText: String = "",
    val currentReadingText: String = ""
)

enum class RowStatus { OK, NO_FLAT, DUPLICATE, NO_PREVIOUS, NO_CURRENT, BELOW_PREVIOUS }

data class ComputedRow(
    val row: BulkRow,
    val index: Int,
    val current: Double?,
    val previous: Double?,
    val bill: WaterBill?,
    val status: RowStatus
)

data class BulkReadingUiState(
    val loading: Boolean = true,
    val flats: List<Flat> = emptyList(),
    val reportedFlatIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val targetPeriod: String = ""
)

sealed interface BulkStage {
    data object Pick : BulkStage
    data object Processing : BulkStage
    data class Review(val rows: List<BulkRow>) : BulkStage
}

sealed interface BulkSubmitState {
    data object Idle : BulkSubmitState
    data object Submitting : BulkSubmitState
    data object Success : BulkSubmitState
    data class Error(val message: String) : BulkSubmitState
}

@HiltViewModel
class BulkReadingViewModel @Inject constructor(
    flatRepository: FlatRepository,
    private val readingRepository: ReadingRepository,
    settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<BulkReadingUiState> = combine(
        flatRepository.observeFlats(),
        readingRepository.observeReadings(limit = 1000),
        settingsRepository.observeSettings()
    ) { flats, readings, settings ->
        val targetPeriod = Formatters.periodKey(Formatters.lastDayOfPreviousMonthMillis())
        val reported = readings.filter { Formatters.periodKey(it.date) == targetPeriod }
            .map { it.flatId }.toSet()
        BulkReadingUiState(
            loading = false,
            flats = flats,
            reportedFlatIds = reported,
            settings = settings,
            targetPeriod = targetPeriod
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BulkReadingUiState())

    private val _stage = MutableStateFlow<BulkStage>(BulkStage.Pick)
    val stage: StateFlow<BulkStage> = _stage.asStateFlow()

    private val _submitState = MutableStateFlow<BulkSubmitState>(BulkSubmitState.Idle)
    val submitState: StateFlow<BulkSubmitState> = _submitState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun onProcessingStarted() {
        _error.value = null
        _stage.value = BulkStage.Processing
    }

    fun onProcessingFailed(message: String) {
        _error.value = message
        _stage.value = BulkStage.Pick
    }

    /** Called once ML Kit returns recognized text from a photo. */
    fun onTextRecognized(text: String) {
        viewModelScope.launch {
            _stage.value = BulkStage.Review(buildRows(parseOcrLines(text, uiState.value.flats)))
        }
    }

    /** Called once a CSV file's contents have been read. */
    fun onCsvTextParsed(text: String) {
        viewModelScope.launch {
            _stage.value = BulkStage.Review(buildRows(parseCsvRows(text, uiState.value.flats)))
        }
    }

    // Shared by both input paths: if the source (OCR line or CSV row) already carried a
    // previous/"Old" reading, use it directly — it's the admin's own written record — otherwise
    // fall back to the last reading on file.
    private suspend fun buildRows(parsed: List<ParsedLine>): List<BulkRow> = parsed.map { p ->
        val previous = p.parsedPrevious
            ?: p.matchedFlat?.let { readingRepository.getLatestForFlat(it.id)?.currentReading }
        BulkRow(
            rawLine = p.rawLine,
            flatId = p.matchedFlat?.id ?: "",
            previousReadingText = previous?.toString() ?: "",
            currentReadingText = p.parsedCurrent?.toString() ?: ""
        )
    }

    fun updateCurrentReadingText(index: Int, text: String) {
        mutateRow(index) { it.copy(currentReadingText = text) }
    }

    fun updatePreviousReadingText(index: Int, text: String) {
        mutateRow(index) { it.copy(previousReadingText = text) }
    }

    private fun mutateRow(index: Int, transform: (BulkRow) -> BulkRow) {
        val stageNow = _stage.value
        if (stageNow is BulkStage.Review) {
            _stage.value = stageNow.copy(
                rows = stageNow.rows.mapIndexed { i, r -> if (i == index) transform(r) else r }
            )
        }
    }

    fun onFlatChanged(index: Int, flatId: String) {
        viewModelScope.launch {
            val previous = if (flatId.isNotBlank()) {
                readingRepository.getLatestForFlat(flatId)?.currentReading
            } else null
            mutateRow(index) { it.copy(flatId = flatId, previousReadingText = previous?.toString() ?: "") }
        }
    }

    fun removeRow(index: Int) {
        val stageNow = _stage.value
        if (stageNow is BulkStage.Review) {
            _stage.value = stageNow.copy(rows = stageNow.rows.filterIndexed { i, _ -> i != index })
        }
    }

    /** Pure derivation of the current review rows' validity — recomputed on every call. */
    fun computeRows(): List<ComputedRow> {
        val rows = (_stage.value as? BulkStage.Review)?.rows ?: return emptyList()
        val settings = uiState.value.settings
        val seenFlatIds = mutableSetOf<String>()
        return rows.mapIndexed { index, row ->
            val currentReading = row.currentReadingText.toDoubleOrNull()
            val previousReading = row.previousReadingText.toDoubleOrNull()
            val bill = if (currentReading != null && previousReading != null) {
                settings.computeBill(previousReading, currentReading)
            } else null
            val isDuplicate = row.flatId.isNotBlank() && !seenFlatIds.add(row.flatId)
            val status = when {
                row.flatId.isBlank() -> RowStatus.NO_FLAT
                isDuplicate -> RowStatus.DUPLICATE
                previousReading == null -> RowStatus.NO_PREVIOUS
                currentReading == null -> RowStatus.NO_CURRENT
                currentReading < previousReading -> RowStatus.BELOW_PREVIOUS
                else -> RowStatus.OK
            }
            ComputedRow(row, index, currentReading, previousReading, bill, status)
        }
    }

    fun submitAll(onDone: () -> Unit) {
        viewModelScope.launch {
            _submitState.value = BulkSubmitState.Submitting
            val valid = computeRows().filter { it.status == RowStatus.OK }
            val capturedBy = authRepository.currentUid ?: ""
            val readings = valid.map { r ->
                Reading(
                    flatId = r.row.flatId,
                    previousReading = r.previous ?: 0.0,
                    currentReading = r.current ?: 0.0,
                    usageLiters = r.bill?.usage ?: 0.0,
                    excessLiters = r.bill?.excess ?: 0.0,
                    amount = r.bill?.amount ?: 0.0,
                    capturedBy = capturedBy
                )
            }
            runCatching { readingRepository.addReadingsBatch(readings) }
                .onSuccess {
                    _submitState.value = BulkSubmitState.Success
                    onDone()
                }
                .onFailure { _submitState.value = BulkSubmitState.Error(it.message ?: "Submit failed") }
        }
    }

    fun resetToPick() {
        _stage.value = BulkStage.Pick
        _submitState.value = BulkSubmitState.Idle
    }

    fun clearError() { _error.value = null }
}
