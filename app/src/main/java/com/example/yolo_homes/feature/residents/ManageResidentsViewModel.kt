package com.example.yolo_homes.feature.residents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.repository.FlatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageResidentsViewModel @Inject constructor(
    private val flatRepository: FlatRepository
) : ViewModel() {

    val flats: StateFlow<List<Flat>> = flatRepository.observeMasterFlats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _status = MutableStateFlow<ResidentSaveStatus>(ResidentSaveStatus.Idle)
    val status: StateFlow<ResidentSaveStatus> = _status.asStateFlow()

    fun save(flatNo: String, email: String, role: String) {
        viewModelScope.launch {
            _status.value = ResidentSaveStatus.Saving
            runCatching { flatRepository.updateResident(flatNo, email.trim(), role) }
                .onSuccess { _status.value = ResidentSaveStatus.Saved(flatNo) }
                .onFailure { _status.value = ResidentSaveStatus.Error(it.message ?: "Save failed") }
        }
    }

    fun clearStatus() { _status.value = ResidentSaveStatus.Idle }
}

sealed interface ResidentSaveStatus {
    data object Idle : ResidentSaveStatus
    data object Saving : ResidentSaveStatus
    data class Saved(val flatNo: String) : ResidentSaveStatus
    data class Error(val message: String) : ResidentSaveStatus
}
