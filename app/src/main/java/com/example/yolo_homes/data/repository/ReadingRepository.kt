package com.example.yolo_homes.data.repository

import com.example.yolo_homes.core.FirestoreCollections
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.asFlow
import com.example.yolo_homes.data.model.Reading
import com.example.yolo_homes.data.toReading
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val collection get() = db.collection(FirestoreCollections.READINGS)

    fun observeReadings(limit: Long = 200): Flow<List<Reading>> =
        collection.orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .asFlow { it.toReading() }
            .catch { emit(emptyList()) }

    /** All readings for one flat (Flat Detail's multi-year history) — no artificial limit. */
    fun observeReadingsForFlat(flatId: String): Flow<List<Reading>> =
        collection.whereEqualTo("flatId", flatId).asFlow { it.toReading() }
            .map { it.sortedByDescending { r -> r.date } }
            .catch { emit(emptyList()) }

    suspend fun getReadings(): List<Reading> =
        collection.orderBy("date", Query.Direction.DESCENDING)
            .get().await().documents.map { it.toReading() }

    /** Latest reading for a flat — used to auto-fill the previous reading on a new entry. */
    suspend fun getLatestForFlat(flatId: String): Reading? =
        collection.whereEqualTo("flatId", flatId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get().await()
            .documents.map { it.toReading() }
            .firstOrNull()

    suspend fun addReading(reading: Reading) {
        val data = mapOf(
            "flatId" to reading.flatId,
            "previousReading" to reading.previousReading,
            "currentReading" to reading.currentReading,
            "usageLiters" to reading.usageLiters,
            "excessLiters" to reading.excessLiters,
            "amount" to reading.amount,
            // Readings are physically taken at month-end but entered a day or two into the
            // next month — record the period the reading actually belongs to, not today's date.
            "date" to Formatters.lastDayOfPreviousMonthMillis(),
            "capturedBy" to reading.capturedBy,
            "edited" to false,
            "hasImage" to reading.hasImage
        )
        collection.add(data).await()
    }

    /** Bulk version of [addReading] — one atomic write for every row a Bulk Upload review submits. */
    suspend fun addReadingsBatch(readings: List<Reading>) {
        val batch = db.batch()
        val sharedDate = Formatters.lastDayOfPreviousMonthMillis()
        readings.forEach { reading ->
            val data = mapOf(
                "flatId" to reading.flatId,
                "previousReading" to reading.previousReading,
                "currentReading" to reading.currentReading,
                "usageLiters" to reading.usageLiters,
                "excessLiters" to reading.excessLiters,
                "amount" to reading.amount,
                "date" to sharedDate,
                "capturedBy" to reading.capturedBy,
                "edited" to false,
                "hasImage" to reading.hasImage
            )
            batch.set(collection.document(), data)
        }
        batch.commit().await()
    }

    suspend fun updateReading(reading: Reading) {
        require(reading.id.isNotBlank()) { "Reading id required for update" }
        val data = mapOf(
            "flatId" to reading.flatId,
            "previousReading" to reading.previousReading,
            "currentReading" to reading.currentReading,
            "usageLiters" to reading.usageLiters,
            "excessLiters" to reading.excessLiters,
            "amount" to reading.amount,
            "date" to reading.date,
            "capturedBy" to reading.capturedBy,
            "edited" to true,
            "hasImage" to reading.hasImage
        )
        collection.document(reading.id).set(data).await()
    }

    suspend fun deleteReading(id: String) {
        collection.document(id).delete().await()
    }
}
