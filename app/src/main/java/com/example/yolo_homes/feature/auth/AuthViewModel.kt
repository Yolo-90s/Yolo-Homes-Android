package com.example.yolo_homes.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(val session: UserSession) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Drives the whole app: splash → login or main shell. */
    val authState: StateFlow<AuthUiState> = authRepository.observeSession()
        .let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { session ->
                    emit(if (session == null) AuthUiState.SignedOut else AuthUiState.SignedIn(session))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState.Loading)

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun onIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            _signingIn.value = true
            _error.value = null
            authRepository.signInWithGoogle(idToken)
                .onFailure { _error.value = it.message ?: "Sign-in failed" }
            _signingIn.value = false
        }
    }

    fun onSignInError(message: String) {
        _signingIn.value = false
        _error.value = message
    }

    fun onSignInStarted() {
        _signingIn.value = true
        _error.value = null
    }

    fun signOut() = authRepository.signOut()

    fun clearError() { _error.value = null }
}
