package com.hussain.messmobile

import com.hussain.messmobile.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateUtilsTest {

    @Test
    fun theCurrentPeriodIsTheCurrentMonth() {
        assertEquals(YearMonth.now().toString(), DateUtils.currentPeriod())
    }

    @Test
    fun theStartOfADayIsAtMidnight() {
        val date = LocalDate.of(2026, 11, 10)

        val start = DateUtils.startOfDay(date)

        assertEquals(date, DateUtils.toLocalDate(start))
        assertEquals(0, java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.systemDefault()).hour)
    }

    @Test
    fun aPeriodIsTheMonthOfADay() {
        val date = LocalDate.of(2026, 11, 10)

        assertEquals("2026-11", DateUtils.periodOf(DateUtils.startOfDay(date)))
    }

    @Test
    fun aPeriodIsFormattedForHumans() {
        assertEquals("November 2026", DateUtils.formatPeriod("2026-11"))
    }

    @Test
    fun theUtcMillisOfTheDatePickerRoundTrip() {
        val local = DateUtils.startOfDay(LocalDate.of(2026, 11, 10))

        val utc = DateUtils.toUtcMillis(local)

        assertEquals(local, DateUtils.fromUtcMillis(utc))
    }

    @Test
    fun moneyIsFormattedWithTheCurrencyOfTheDevice() {
        assertTrue(DateUtils.formatMoney(1234.5).isNotBlank())
    }
}
