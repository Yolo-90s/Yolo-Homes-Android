package com.example.yolo_homes.data.model

/**
 * Maps to the `readings` collection — one water meter reading per flat per period.
 * `date` is epoch millis (Long) to match existing data; mapping is done manually
 * in the repository (see ReadingRepository).
 */
data class Reading(
    val id: String = "",
    val flatId: String = "",
    val previousReading: Double = 0.0,
    val currentReading: Double = 0.0,
    val usageLiters: Double = 0.0,
    val excessLiters: Double = 0.0,
    val amount: Double = 0.0,
    val date: Long = 0L,
    val capturedBy: String = "",
    val edited: Boolean = false,
    val hasImage: Boolean = false
)
