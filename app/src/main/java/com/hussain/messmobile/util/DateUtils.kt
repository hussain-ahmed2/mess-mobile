package com.hussain.messmobile.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Helpers around dates, days and the monthly periods the dues are collected for. */
object DateUtils {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val longDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    /** Local midnight of the given day, in epoch milliseconds. */
    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Local midnight of today, in epoch milliseconds. */
    fun startOfToday(): Long = startOfDay(LocalDate.now())

    /** The day the given timestamp belongs to. */
    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    /** The month the given timestamp belongs to, formatted as `yyyy-MM`. */
    fun periodOf(millis: Long): String = YearMonth.from(toLocalDate(millis)).toString()

    /** The current month, formatted as `yyyy-MM`. */
    fun currentPeriod(): String = YearMonth.now().toString()

    /** The month before the current one, formatted as `yyyy-MM`. */
    fun previousPeriod(): String = YearMonth.now().minusMonths(1).toString()

    /** Parses a `yyyy-MM` period. */
    fun parsePeriod(period: String): YearMonth = YearMonth.parse(period)

    /** Formats a `yyyy-MM` period as `November 2026`. */
    fun formatPeriod(period: String): String = monthFormatter.format(parsePeriod(period))

    /** Formats a timestamp as `Tue, 10 Nov`. */
    fun formatShortDate(millis: Long): String = shortDateFormatter.format(toLocalDate(millis))

    /** Formats a timestamp as `10 November 2026`. */
    fun formatLongDate(millis: Long): String = longDateFormatter.format(toLocalDate(millis))

    /** Formats an amount with the currency of the device. */
    fun formatMoney(amount: Double): String = NumberFormat.getCurrencyInstance().format(amount)

    /** Turns the UTC midnight of the date picker into the local midnight of that day. */
    fun fromUtcMillis(utcMillis: Long): Long =
        startOfDay(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate())

    /** Turns the local midnight of a day into the UTC midnight the date picker expects. */
    fun toUtcMillis(localMidnight: Long): Long =
        toLocalDate(localMidnight).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
