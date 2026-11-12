package com.messmate.bd.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

/**
 * The meal grid must stay stable – the row of a member may never change when
 * somebody joins later, otherwise every device would write into the wrong cell.
 */
class MealGridTest {

    private val january = YearMonth.of(2026, 1) // 31 days
    private val february = YearMonth.of(2026, 2) // 28 days

    @Test
    fun `rows are grouped member first`() {
        assertEquals(2, MealGrid.rowOf(memberIndex = 0, dayOfMonth = 1, daysInMonth = 31))
        assertEquals(32, MealGrid.rowOf(memberIndex = 0, dayOfMonth = 31, daysInMonth = 31))
        assertEquals(33, MealGrid.rowOf(memberIndex = 1, dayOfMonth = 1, daysInMonth = 31))
        assertEquals(63, MealGrid.rowOf(memberIndex = 1, dayOfMonth = 31, daysInMonth = 31))
    }

    @Test
    fun `a new member does not move the existing rows`() {
        val before = MealGrid.rowOf(memberIndex = 0, dayOfMonth = 5, daysInMonth = 31)
        // A second member joins – the first member keeps its rows.
        val newMemberRow = MealGrid.rowOf(memberIndex = 1, dayOfMonth = 1, daysInMonth = 31)
        assertEquals(33, newMemberRow)
        assertEquals(before, MealGrid.rowOf(memberIndex = 0, dayOfMonth = 5, daysInMonth = 31))
    }

    @Test
    fun `row can be mapped back to member and day`() {
        val row = MealGrid.rowOf(memberIndex = 2, dayOfMonth = 17, daysInMonth = 28)
        assertEquals(2, MealGrid.memberIndexOfRow(row, 28))
        assertEquals(17, MealGrid.dayOfMonthOfRow(row, 28))
    }

    @Test
    fun `header row is not part of the grid`() {
        assertNull(MealGrid.memberIndexOfRow(1, 31))
        assertNull(MealGrid.dayOfMonthOfRow(1, 31))
    }

    @Test
    fun `grid covers every member and every day`() {
        val rows = MealGrid.rowsFor(listOf("M001", "M002"), january)
        assertEquals(62, rows.size)
        assertEquals("M001", rows.first().memberId)
        assertEquals("M002", rows[31].memberId)
        assertEquals(2, rows.first().row)
    }

    @Test
    fun `february has 28 rows per member`() {
        val rows = MealGrid.rowsFor(listOf("M001"), february)
        assertEquals(28, rows.size)
        assertEquals(29, rows.first { it.date.dayOfMonth == 28 }.row)
    }
}
