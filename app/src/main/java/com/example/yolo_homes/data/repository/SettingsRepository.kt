package com.example.yolo_homes.data.repository

import com.example.yolo_homes.core.FirestoreCollections
import com.example.yolo_homes.data.asFlow
import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.data.toSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val docRef
        get() = db.collection(FirestoreCollections.APP_SETTINGS)
            .document(FirestoreCollections.APP_SETTINGS_DOC)

    /** Live stream of the global settings, falling back to defaults if missing. */
    fun observeSettings(): Flow<AppSettings> =
        docRef.asFlow { if (it.exists()) it.toSettings() else AppSettings() }
            .catch { emit(AppSettings()) }

    suspend fun getSettings(): AppSettings =
        docRef.get().await().let { if (it.exists()) it.toSettings() else AppSettings() }

    suspend fun updateSettings(settings: AppSettings) {
        docRef.set(settings, SetOptions.merge()).await()
    }
}
