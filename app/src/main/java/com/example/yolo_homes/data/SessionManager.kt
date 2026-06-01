package com.example.yolo_homes.data

import com.example.yolo_homes.data.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently resolved session so feature ViewModels can read the user's role and
 * own-flat id (for permission gating and data scoping) without each re-resolving it.
 */
@Singleton
class SessionManager @Inject constructor() {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    fun update(session: UserSession?) { _session.value = session }
}
