package com.example.yolo_homes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _saveState = MutableStateFlow<SettingsSaveState>(SettingsSaveState.Idle)
    val saveState: StateFlow<SettingsSaveState> = _saveState.asStateFlow()

    fun save(updated: AppSettings) {
        viewModelScope.launch {
            _saveState.value = SettingsSaveState.Saving
            runCatching { settingsRepository.updateSettings(updated) }
                .onSuccess { _saveState.value = SettingsSaveState.Saved }
                .onFailure { _saveState.value = SettingsSaveState.Error(it.message ?: "Save failed") }
        }
    }

    fun resetSaveState() { _saveState.value = SettingsSaveState.Idle }
}

sealed interface SettingsSaveState {
    data object Idle : SettingsSaveState
    data object Saving : SettingsSaveState
    data object Saved : SettingsSaveState
    data class Error(val message: String) : SettingsSaveState
}
