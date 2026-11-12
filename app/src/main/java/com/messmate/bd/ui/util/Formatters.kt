package com.messmate.bd.ui.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatting helpers shared by all screens.
 *
 * The Bangla locale is not used for the *numbers* on purpose – a bill that
 * mixes Bengali digits with a Latin member id is hard to read. The currency
 * symbol is the Bengali "taka" sign in both languages.
 */
object Formatters {

    private const val TAKA = "৳"

    /** `৳1,234.50` */
    fun money(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return TAKA + format.format(amount)
    }

    /** `৳1,234` – no decimals, for headline numbers. */
    fun moneyShort(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            isGroupingUsed = true
        }
        return TAKA + format.format(amount)
    }

    /** `12 Nov 2026` */
    fun date(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

    /** `12 Nov` */
    fun dateShort(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))

    /** `07:00` */
    fun time(time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("HH:mm"))

    /** `November 2026` */
    fun month(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

    /** `2026-11` – the format used inside the `Config` tab. */
    fun monthKey(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    /** `3h 12m` – the cut-off countdown. */
    fun duration(duration: java.time.Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
