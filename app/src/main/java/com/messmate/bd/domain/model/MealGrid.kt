package com.messmate.bd.domain.model

import java.time.YearMonth

/**
 * Layout of the `Meals` tab.
 *
 * The tab is pre-populated with **one row per (member, day)** so that toggling
 * a meal is a single-cell write – no append, no row shifting, no collisions
 * when two members toggle at the same time.
 *
 * Rows are grouped **member first, then day**:
 *
 * ```
 * M001 → day 1, day 2, … day 31      (rows 2 … 32)
 * M002 → day 1, day 2, … day 31      (rows 33 … 63)
 * M003 → day 1, day 2, … day 31      (rows 64 … 94)
 * ```
 *
 * Member blocks are appended in the order the members were created (their
 * `MemberID` is strictly increasing and members are never deleted – only
 * deactivated), therefore the row of a member never shifts when somebody joins
 * later. Every device derives the exact same coordinates from the `Members` tab
 * + the month length, which is what makes the sheet safe to edit concurrently.
 */
object MealGrid {

    /**
     * 1-based sheet row of a meal cell.
     *
     * @param memberIndex zero based position of the member in the `Members` tab.
     * @param dayOfMonth 1..31
     */
    fun rowOf(memberIndex: Int, dayOfMonth: Int, daysInMonth: Int): Int {
        require(memberIndex >= 0) { "memberIndex must be >= 0" }
        require(dayOfMonth in 1..daysInMonth) { "dayOfMonth $dayOfMonth outside 1..$daysInMonth" }
        return 2 + (memberIndex * daysInMonth) + (dayOfMonth - 1)
    }

    /** Number of rows a single member occupies (one per day of the month). */
    fun rowsPerMember(month: YearMonth): Int = month.lengthOfMonth()

    /**
     * Zero based member index that owns [row], or `null` when the row does not
     * belong to any member block.
     */
    fun memberIndexOfRow(row: Int, daysInMonth: Int): Int? {
        if (row < 2) return null
        val offset = row - 2
        return offset / daysInMonth
    }

    /**
     * Day of month (1..31) that owns [row], or `null` for a row outside the grid.
     */
    fun dayOfMonthOfRow(row: Int, daysInMonth: Int): Int? {
        if (row < 2) return null
        val offset = row - 2
        return (offset % daysInMonth) + 1
    }

    /**
     * The full grid for [memberIds] – used by the provisioner to pre-populate
     * the tab and by the repository to detect rows that still have to be added
     * when a member joins mid-month.
     */
    fun rowsFor(memberIds: List<String>, month: YearMonth): List<MealGridRow> {
        val days = month.lengthOfMonth()
        val rows = ArrayList<MealGridRow>(memberIds.size * days)
        memberIds.forEachIndexed { memberIndex, memberId ->
            for (day in 1..days) {
                rows += MealGridRow(
                    row = rowOf(memberIndex, day, days),
                    date = month.atDay(day),
                    memberId = memberId,
                )
            }
        }
        return rows
    }
}

/** One pre-populated row of the `Meals` tab. */
data class MealGridRow(
    val row: Int,
    val date: java.time.LocalDate,
    val memberId: String,
)
