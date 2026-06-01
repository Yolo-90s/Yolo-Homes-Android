package com.example.yolo_homes.data.model

/**
 * Maps to the `maintenanceReceipts` collection. `paidDate` is epoch millis (Long) to
 * match existing data; mapping is done manually in the repository (see MaintenanceRepository).
 */
data class MaintenanceReceipt(
    val id: String = "",
    val flatId: String = "",
    val amount: Double = 0.0,
    val period: String = "",
    val paymentMethod: String = "",
    val paidDate: Long = 0L,
    val capturedBy: String = "",
    val edited: Boolean = false
)
