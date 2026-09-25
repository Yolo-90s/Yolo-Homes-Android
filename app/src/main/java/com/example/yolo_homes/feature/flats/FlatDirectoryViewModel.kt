package com.example.yolo_homes.feature.flats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.data.SessionManager
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.repository.FlatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlatDirectoryUiState(
    val loading: Boolean = true,
    val residentFlats: List<Flat> = emptyList(),
    val staffUnits: List<Flat> = emptyList(),
    val isAdmin: Boolean = false
)

sealed interface FlatSaveState {
    data object Idle : FlatSaveState
    data object Saving : FlatSaveState
    data object Saved : FlatSaveState
    data class Error(val message: String) : FlatSaveState
}

@HiltViewModel
class FlatDirectoryViewModel @Inject constructor(
    private val flatRepository: FlatRepository,
    sessionManager: SessionManager
) : ViewModel() {

    // Merge masterFlats (source of truth for email/role/unitType) with the operational `flats`
    // collection (source of truth for the doc id receipts/readings reference) — same merge shape
    // WaterViewModel already uses, keyed by flatNo, master wins but keeps the ops id if one exists.
    val uiState: StateFlow<FlatDirectoryUiState> = combine(
        flatRepository.observeMasterFlats(),
        flatRepository.observeFlats(),
        sessionManager.session
    ) { master, ops, session ->
        val byNo = LinkedHashMap<String, Flat>()
        ops.forEach { if (it.flatNo.isNotBlank()) byNo[it.flatNo] = it }
        master.forEach { m ->
            if (m.flatNo.isBlank()) return@forEach
            val existingId = byNo[m.flatNo]?.id
            byNo[m.flatNo] = if (!existingId.isNullOrBlank()) m.copy(id = existingId) else m
        }
        val all = byNo.values.sortedBy { it.flatNo }
        FlatDirectoryUiState(
            loading = false,
            residentFlats = all.filterNot { it.isStaff },
            staffUnits = all.filter { it.isStaff },
            isAdmin = session?.isAdmin == true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlatDirectoryUiState())

    private val _saveState = MutableStateFlow<FlatSaveState>(FlatSaveState.Idle)
    val saveState: StateFlow<FlatSaveState> = _saveState.asStateFlow()

    fun save(flat: Flat, isNew: Boolean) {
        viewModelScope.launch {
            _saveState.value = FlatSaveState.Saving
            runCatching {
                if (isNew) flatRepository.createFlat(flat) else flatRepository.updateFlat(flat)
            }.onSuccess { _saveState.value = FlatSaveState.Saved }
                .onFailure { _saveState.value = FlatSaveState.Error(it.message ?: "Save failed") }
        }
    }

    fun clearSaveState() { _saveState.value = FlatSaveState.Idle }
}
