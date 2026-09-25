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

    // ---- masterFlats: the role/email source used by the Flat Directory & admin resolution ----

    private val masterFlats get() = db.collection(FirestoreCollections.MASTER_FLATS)

    fun observeMasterFlats(): Flow<List<Flat>> =
        masterFlats.asFlow { it.toFlat() }
            .map { it.sortedBy { f -> f.flatNo } }
            .catch { emit(emptyList()) }

    /**
     * Creates a brand-new unit (resident flat or staff/watchman unit) in BOTH collections —
     * the operational `flats` doc (auto-id, what receipts/readings will reference) and the
     * `masterFlats` doc (id == flatNo, the role/email source of truth). Neither app had any
     * "add a flat" UI before the Flat Directory — units were seeded directly in Firestore.
     */
    suspend fun createFlat(flat: Flat): String {
        require(flat.flatNo.isNotBlank()) { "flatNo required" }
        val opsRef = collection.document()
        val batch = db.batch()
        batch.set(opsRef, flatData(flat))
        batch.set(masterFlats.document(flat.flatNo), flatData(flat), SetOptions.merge())
        batch.commit().await()
        return opsRef.id
    }

    /** Updates an existing unit's details in both collections (keeps them from drifting apart). */
    suspend fun updateFlat(flat: Flat) {
        require(flat.flatNo.isNotBlank()) { "flatNo required" }
        val batch = db.batch()
        if (flat.id.isNotBlank()) batch.set(collection.document(flat.id), flatData(flat), SetOptions.merge())
        batch.set(masterFlats.document(flat.flatNo), flatData(flat), SetOptions.merge())
        batch.commit().await()
    }

    private fun flatData(flat: Flat): Map<String, Any> = mapOf(
        "flatNo" to flat.flatNo,
        "block" to flat.block,
        "ownerName" to flat.ownerName,
        "ownerPhone" to flat.ownerPhone,
        "tenantName" to flat.tenantName,
        "tenantPhone" to flat.tenantPhone,
        "role" to flat.role,
        "email" to flat.email,
        "unitType" to flat.unitType,
        "vacant" to flat.vacant
    )
}
