package com.example.yolo_homes.data.repository

import com.example.yolo_homes.core.FirestoreCollections
import com.example.yolo_homes.data.asFlow
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.toFlat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlatRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    // Use the operational `flats` collection (auto-ids + flatNumber) that receipts/readings
    // reference, so flatId lookups resolve. `masterFlats` is used only for admin-role checks.
    private val collection get() = db.collection(FirestoreCollections.FLATS)

    fun observeFlats(): Flow<List<Flat>> =
        collection.asFlow { it.toFlat() }
            .map { it.sortedBy { f -> f.flatNo } }
            .catch { emit(emptyList()) }

    suspend fun getFlats(): List<Flat> =
        collection.get().await().documents.map { it.toFlat() }.sortedBy { it.flatNo }

    suspend fun getFlat(id: String): Flat? =
        collection.document(id).get().await().let { if (it.exists()) it.toFlat() else null }

    // ---- masterFlats: the role/email source used by Manage Residents & admin resolution ----

    private val masterFlats get() = db.collection(FirestoreCollections.MASTER_FLATS)

    fun observeMasterFlats(): Flow<List<Flat>> =
        masterFlats.asFlow { it.toFlat() }
            .map { it.sortedBy { f -> f.flatNo } }
            .catch { emit(emptyList()) }

    /** Sets the resident's linked email and role on their masterFlats doc (id == flatNo). */
    suspend fun updateResident(flatNo: String, email: String, role: String) {
        require(flatNo.isNotBlank()) { "flatNo required" }
        masterFlats.document(flatNo)
            .set(mapOf("email" to email, "role" to role), SetOptions.merge())
            .await()
    }
}
