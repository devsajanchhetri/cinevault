package com.example.core.utilities

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MoneyUtils {
    private val PAISA_MULTIPLIER = BigDecimal(100)

    /**
     * Parses a user-entered NPR string (e.g., "45000", "45,000.50") into exact paisa (Long).
     * Returns null if invalid or negative.
     */
    fun parseNprToPaisa(input: String): Long? {
        val cleaned = input.replace(",", "").replace("NPR", "", ignoreCase = true).trim()
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            if (bd < BigDecimal.ZERO) return null
            bd.multiply(PAISA_MULTIPLIER)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts paisa (Long) to BigDecimal NPR with 2 decimal places.
     */
    fun paisaToBigDecimal(paisa: Long): BigDecimal {
        return BigDecimal.valueOf(paisa).divide(PAISA_MULTIPLIER, 2, RoundingMode.HALF_UP)
    }

    /**
     * Formats paisa as "NPR 45,000" or "NPR 45,000.50" using decimal-safe BigDecimal math.
     */
    fun formatNpr(paisa: Long, alwaysShowDecimals: Boolean = false): String {
        val isNegative = paisa < 0
        val absPaisa = kotlin.math.abs(paisa)
        val bd = paisaToBigDecimal(absPaisa)
        val symbols = DecimalFormatSymbols(Locale.US)
        val hasFraction = absPaisa % 100L != 0L
        val pattern = if (alwaysShowDecimals || hasFraction) "#,##0.00" else "#,##0"
        val formatter = DecimalFormat(pattern, symbols)
        val formatted = formatter.format(bd)
        return if (isNegative) "-NPR $formatted" else "NPR $formatted"
    }

    /**
     * Formats paisa to plain input string for edit forms (e.g., "45000" or "45000.50").
     */
    fun paisaToInputString(paisa: Long): String {
        if (paisa == 0L) return ""
        val bd = paisaToBigDecimal(paisa)
        return if (paisa % 100L == 0L) {
            (paisa / 100L).toString()
        } else {
            bd.toPlainString()
        }
    }
}

object DateUtils {
    fun formatShortDate(epochMs: Long): String {
        if (epochMs <= 0L) return "Not set"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun formatMonthYear(epochMs: Long): String {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun formatIsoDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun parseIsoDateOrToday(isoString: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.parse(isoString.trim())?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun startOfDayMillis(epochMs: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(epochMs: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun isSameCalendarDay(epochA: Long, epochB: Long): Boolean {
        if (epochA <= 0L || epochB <= 0L) return false
        return startOfDayMillis(epochA) == startOfDayMillis(epochB)
    }
}
