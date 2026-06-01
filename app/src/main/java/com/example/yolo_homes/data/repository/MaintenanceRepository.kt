package com.example.yolo_homes.data.repository

import com.example.yolo_homes.core.FirestoreCollections
import com.example.yolo_homes.data.asFlow
import com.example.yolo_homes.data.model.MaintenanceReceipt
import com.example.yolo_homes.data.toReceipt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val collection get() = db.collection(FirestoreCollections.MAINTENANCE_RECEIPTS)

    /** Newest receipts first; [limit] keeps reads bounded for dashboards/lists. */
    fun observeReceipts(limit: Long = 200): Flow<List<MaintenanceReceipt>> =
        collection.orderBy("paidDate", Query.Direction.DESCENDING)
            .limit(limit)
            .asFlow { it.toReceipt() }
            .catch { emit(emptyList()) }

    fun observeReceiptsForPeriod(period: String): Flow<List<MaintenanceReceipt>> =
        collection.whereEqualTo("period", period).asFlow { it.toReceipt() }
            .catch { emit(emptyList()) }

    suspend fun getReceipts(): List<MaintenanceReceipt> =
        collection.orderBy("paidDate", Query.Direction.DESCENDING)
            .get().await().documents.map { it.toReceipt() }

    suspend fun addReceipt(receipt: MaintenanceReceipt) {
        val data = mapOf(
            "flatId" to receipt.flatId,
            "amount" to receipt.amount,
            "period" to receipt.period,
            "paymentMethod" to receipt.paymentMethod,
            "paidDate" to System.currentTimeMillis(),
            "capturedBy" to receipt.capturedBy,
            "edited" to false
        )
        collection.add(data).await()
    }

    suspend fun updateReceipt(receipt: MaintenanceReceipt) {
        require(receipt.id.isNotBlank()) { "Receipt id required for update" }
        val data = mapOf(
            "flatId" to receipt.flatId,
            "amount" to receipt.amount,
            "period" to receipt.period,
            "paymentMethod" to receipt.paymentMethod,
            "paidDate" to receipt.paidDate,
            "capturedBy" to receipt.capturedBy,
            "edited" to true
        )
        collection.document(receipt.id).set(data).await()
    }

    suspend fun deleteReceipt(id: String) {
        collection.document(id).delete().await()
    }
}
