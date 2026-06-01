package com.example.yolo_homes.data.model

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

/**
 * Maps to `appSettings/main`. Drives water billing math and global preferences.
 *
 * [freeLiters] is the **absolute exclude baseline** (default 200 L): water meters ship
 * showing ~100+ L, so any meter reading at or below this value is excluded from billing —
 * charging only counts cumulative liters above it. There is no separate per-month free
 * allowance; every liter above the baseline is billed at [ratePerExcessLiter].
 */
data class AppSettings(
    val apartmentName: String = "Sri Manjunatha Residency",
    val address: String = "",
    val currency: String = "₹",
    val freeLiters: Double = 200.0,
    val ratePerExcessLiter: Double = 0.0,
    val readingFrequency: String = "Monthly",
    val decimalPrecision: Int = 2,
    val roundingRule: String = "nearest",
    val sendBillMessage: Boolean = false,
    val sendReminder: Boolean = false,
    val unit: String = "Liters",
    val waterSource: String = ""
) {
    /**
     * Computes usage / billable / amount for a meter reading.
     *
     * - `usage`   = raw consumption this period (current − previous).
     * - `excess`  = billable liters = the portion above the absolute [freeLiters] baseline,
     *               i.e. max(current, baseline) − max(previous, baseline).
     * - `amount`  = billable × [ratePerExcessLiter], rounded per [roundingRule].
     */
    fun computeBill(previous: Double, current: Double): WaterBill {
        val baseline = freeLiters
        val usage = (current - previous).coerceAtLeast(0.0)
        val billable = (max(current, baseline) - max(previous, baseline)).coerceAtLeast(0.0)
        val raw = billable * ratePerExcessLiter
        val amount = when (roundingRule.lowercase()) {
            "up", "ceil" -> ceil(raw)
            "down", "floor" -> floor(raw)
            "none", "off", "" -> raw
            else -> round(raw)
        }
        return WaterBill(usage = usage, excess = billable, amount = amount)
    }
}

data class WaterBill(
    val usage: Double,
    val excess: Double,
    val amount: Double
)
