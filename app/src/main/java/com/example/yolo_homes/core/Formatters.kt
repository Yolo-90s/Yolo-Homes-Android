package com.example.yolo_homes.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Lightweight formatting helpers shared across screens. */
object Formatters {

    fun currency(amount: Double, symbol: String = "₹"): String {
        return "$symbol${"%,.0f".format(amount)}"
    }

    fun currencyPrecise(amount: Double, symbol: String = "₹", decimals: Int = 2): String {
        return "$symbol${"%,.${decimals}f".format(amount)}"
    }

    fun liters(value: Double): String = "${"%,.0f".format(value)} L"

    /** Period key in the canonical "yyyy-MM" form used across receipts/readings. */
    fun periodKey(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date)

    fun periodKey(millis: Long): String =
        periodKey(if (millis > 0L) Date(millis) else Date(0))

    fun monthLabel(periodKey: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(periodKey)
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(parsed ?: Date())
        } catch (e: Exception) {
            periodKey
        }
    }

    fun shortDate(date: Date?): String {
        if (date == null) return "—"
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    }

    fun shortDate(millis: Long): String {
        if (millis <= 0L) return "—"
        return shortDate(Date(millis))
    }

    /** Returns "Good Morning" / "Good Afternoon" / "Good Evening" for the greeting header. */
    fun greeting(calendar: Calendar = Calendar.getInstance()): String {
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    /** Last [count] period keys ending with the current month, oldest first. */
    fun recentPeriods(count: Int): List<String> {
        val cal = Calendar.getInstance()
        val out = ArrayList<String>(count)
        for (i in count - 1 downTo 0) {
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -i)
            out.add(periodKey(c.time))
        }
        return out
    }
}
