package com.example.yolo_homes.data

import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.model.MaintenanceReceipt
import com.example.yolo_homes.data.model.Reading
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/** Reads a temporal field stored as Long, Double, Timestamp, Date, or a date String → epoch millis. */
fun Any?.asMillis(): Long = when (this) {
    is Long -> this
    is Double -> toLong()
    is Number -> toLong()
    is Timestamp -> toDate().time
    is Date -> time
    is String -> parseDateString(this)
    else -> 0L
}

/** Parses prior-app date strings like "2026-04-01", ISO timestamps, or "2026-04". */
private fun parseDateString(value: String): Long {
    if (value.isBlank()) return 0L
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd", "yyyy-MM")
    for (p in patterns) {
        try {
            val fmt = java.text.SimpleDateFormat(p, java.util.Locale.getDefault())
            fmt.isLenient = false
            return fmt.parse(value)?.time ?: continue
        } catch (_: Exception) { /* try next */ }
    }
    return 0L
}

private fun DocumentSnapshot.double(field: String): Double = when (val v = get(field)) {
    is Number -> v.toDouble()
    is String -> v.toDoubleOrNull() ?: 0.0
    else -> 0.0
}

/** Type-safe string read: returns null if the field is missing or not actually a String. */
fun DocumentSnapshot.strOrNull(field: String): String? = get(field) as? String

private fun DocumentSnapshot.str(field: String): String = strOrNull(field) ?: ""

/** Coerces any scalar (incl. numbers, e.g. a phone stored as Long) to its String form. */
private fun DocumentSnapshot.text(field: String): String = when (val v = get(field)) {
    null -> ""
    is Double -> if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    else -> v.toString()
}

private fun DocumentSnapshot.int(field: String, default: Int): Int =
    (get(field) as? Number)?.toInt() ?: default

private fun DocumentSnapshot.bool(field: String, default: Boolean): Boolean =
    (get(field) as? Boolean) ?: default

/** Maps appSettings/main tolerantly, falling back to AppSettings() defaults per field. */
fun DocumentSnapshot.toSettings(): AppSettings {
    val d = AppSettings()
    return AppSettings(
        apartmentName = strOrNull("apartmentName") ?: d.apartmentName,
        address = strOrNull("address") ?: d.address,
        currency = strOrNull("currency") ?: d.currency,
        freeLiters = if (contains("freeLiters")) double("freeLiters") else d.freeLiters,
        ratePerExcessLiter = if (contains("ratePerExcessLiter")) double("ratePerExcessLiter") else d.ratePerExcessLiter,
        readingFrequency = strOrNull("readingFrequency") ?: d.readingFrequency,
        decimalPrecision = int("decimalPrecision", d.decimalPrecision),
        roundingRule = strOrNull("roundingRule") ?: d.roundingRule,
        sendBillMessage = bool("sendBillMessage", d.sendBillMessage),
        sendReminder = bool("sendReminder", d.sendReminder),
        unit = strOrNull("unit") ?: d.unit,
        waterSource = strOrNull("waterSource") ?: d.waterSource
    )
}

/**
 * Maps a flat doc tolerantly. Handles both the `flats` collection (`flatNumber`, auto-id) and
 * `masterFlats` (`flatNo`, id = flatNo). Phones may be stored as numbers.
 */
fun DocumentSnapshot.toFlat(): Flat = Flat(
    id = id,
    flatNo = text("flatNo").ifBlank { text("flatNumber") },
    block = text("block"),
    ownerName = text("ownerName"),
    ownerPhone = text("ownerPhone"),
    tenantName = text("tenantName"),
    tenantPhone = text("tenantPhone"),
    role = strOrNull("role") ?: "resident",
    email = strOrNull("email") ?: ""
)

/** Maps a readings doc tolerantly (handles legacy Long vs Timestamp dates). */
fun DocumentSnapshot.toReading(): Reading = Reading(
    id = id,
    flatId = str("flatId"),
    previousReading = double("previousReading"),
    currentReading = double("currentReading"),
    usageLiters = double("usageLiters"),
    excessLiters = double("excessLiters"),
    amount = double("amount"),
    date = get("date").asMillis(),
    capturedBy = str("capturedBy"),
    edited = (get("edited") as? Boolean) ?: false,
    hasImage = (get("hasImage") as? Boolean) ?: false
)

/** Maps a maintenanceReceipts doc tolerantly. */
fun DocumentSnapshot.toReceipt(): MaintenanceReceipt = MaintenanceReceipt(
    id = id,
    flatId = str("flatId"),
    amount = double("amount"),
    period = str("period"),
    paymentMethod = str("paymentMethod"),
    paidDate = get("paidDate").asMillis(),
    capturedBy = str("capturedBy"),
    edited = (get("edited") as? Boolean) ?: false
)
