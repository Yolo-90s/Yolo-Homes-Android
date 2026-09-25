package com.example.yolo_homes.data.model

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

/**
 * Maps to `appSettings/main`. Drives water billing math and global preferences.
 *
 * Two billing methods, chosen via [billingMethod]:
 * - `"flat"`: [freeLiters] is an **absolute exclude baseline** (default 200 L): water meters
 *   ship showing ~100+ L, so any meter reading at or below this value is excluded from
 *   billing. There is no per-month free allowance — the baseline only matters on a flat's
 *   first-ever reading. Every liter above it is billed at [ratePerExcessLiter].
 * - `"tiered"`: a free allowance per billing period ([freeLitersMonthly]); only usage above
 *   it is billed, at [tieredRatePerLiter].
 */
data class AppSettings(
    val apartmentName: String = "Sri Manjunatha Residency",
    val address: String = "",
    val currency: String = "₹",
    val billingMethod: String = "flat",
    val freeLiters: Double = 200.0,
    val ratePerExcessLiter: Double = 0.0,
    val freeLitersMonthly: Double = 10000.0,
    val tieredRatePerLiter: Double = 0.02,
    val readingFrequency: String = "Monthly",
    val decimalPrecision: Int = 0,
    val roundingRule: String = "none",
    val sendBillMessage: Boolean = false,
    val sendReminder: Boolean = false,
    val unit: String = "Liters",
    val waterSource: String = "",
    // Admin-written plain-text instructions shown to residents on the
    // Maintenance screen — the app has no in-app payment flow (receipts
    // are logged after the fact), so this is how residents learn how to
    // actually pay, e.g. "Pay via UPI: office@upi, or contact the office."
    val payInstructions: String = ""
) {
    /**
     * Computes usage / billable / amount for a meter reading, per [billingMethod].
     *
     * - `usage`   = raw consumption this period (current − previous).
     * - `excess`  = billable liters — see [billingMethod] doc above for how this differs
     *               between "flat" and "tiered".
     * - `amount`  = billable × the active rate, rounded per [roundingRule].
     */
    fun computeBill(previous: Double, current: Double): WaterBill {
        val tiered = billingMethod == "tiered"
        val usage = (current - previous).coerceAtLeast(0.0)

        val billable: Double
        val rate: Double
        if (tiered) {
            rate = tieredRatePerLiter
            billable = (usage - freeLitersMonthly).coerceAtLeast(0.0)
        } else {
            val baseline = freeLiters
            rate = ratePerExcessLiter
            billable = (max(current, baseline) - max(previous, baseline)).coerceAtLeast(0.0)
        }

        val raw = billable * rate
        val amount = when (roundingRule.lowercase()) {
            "up", "ceil" -> ceil(raw)
            "down", "floor" -> floor(raw)
            "none", "off", "" -> raw
            else -> round(raw)
        }
        return WaterBill(usage = usage, excess = billable, amount = amount)
    }

    /** Resolves the rate/limit fields that actually apply under the active billing method. */
    fun billingRateInfo(): BillingRateInfo {
        val tiered = billingMethod == "tiered"
        return BillingRateInfo(
            tiered = tiered,
            rate = if (tiered) tieredRatePerLiter else ratePerExcessLiter,
            limit = if (tiered) freeLitersMonthly else freeLiters,
            limitLabel = if (tiered) "Free amount each month" else "Free water limit"
        )
    }
}

data class WaterBill(
    val usage: Double,
    val excess: Double,
    val amount: Double
)

data class BillingRateInfo(
    val tiered: Boolean,
    val rate: Double,
    val limit: Double,
    val limitLabel: String
)
