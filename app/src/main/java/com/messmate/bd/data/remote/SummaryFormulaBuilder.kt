package com.messmate.bd.data.remote

import com.messmate.bd.domain.model.SheetSchema
import com.messmate.bd.domain.model.SheetSchema.Tabs

/**
 * The **formula layer** of the `Summary` tab.
 *
 * Everything the app shows on the dashboard and in the monthly statement is
 * computed *inside the spreadsheet* with `SUMIFS` / `VLOOKUP` formulas, so the
 * sheet stays the single source of truth and anybody can inspect the maths in
 * the browser.
 *
 * The formulas below are the exact counterpart of
 * `com.messmate.bd.domain.calculation.CalculationEngine` – when one changes,
 * the other has to change too.
 *
 * ### Layout of the `Summary` tab
 *
 * ```
 * Row 1  | TotalBazar | =SUM(Bazar!E:E) | TotalMeals | =… | LiveMealRate | =B1/D1 | TotalEqualUtilities | =SUMIFS(…)
 * Row 2  | Mess       | =Config!B2      | Month      | =Config!B3
 * Row 3  | MemberID | Name | TotalMeals | MealCost | SeatRent | EqualUtility | CustomUtility | TotalCost | PrevBalance | TotalDeposit | NetBalance
 * Row 4+ | one formula block per member
 * ```
 */
object SummaryFormulaBuilder {

    /** First row that holds data (row 1 is the header of every tab). */
    private const val FIRST_DATA_ROW = 2

    // -----------------------------------------------------------------------
    // Ranges used by the formulas (bounded → cheap to evaluate)
    // -----------------------------------------------------------------------

    private fun columnRange(tab: String, columnLetter: String, maxRow: Int): String =
        "$tab!\$$columnLetter\$$FIRST_DATA_ROW:\$$columnLetter\$$maxRow"

    private val mealsMember = columnRange(Tabs.MEALS, "B", SheetSchema.MEALS_MAX_ROW)
    private val mealsBreakfast = columnRange(Tabs.MEALS, "C", SheetSchema.MEALS_MAX_ROW)
    private val mealsLunch = columnRange(Tabs.MEALS, "D", SheetSchema.MEALS_MAX_ROW)
    private val mealsDinner = columnRange(Tabs.MEALS, "E", SheetSchema.MEALS_MAX_ROW)
    private val mealsGuest = columnRange(Tabs.MEALS, "F", SheetSchema.MEALS_MAX_ROW)

    private val bazarAmount = columnRange(Tabs.BAZAR, "E", SheetSchema.LEDGER_MAX_ROW)

    private val utilityAmount = columnRange(Tabs.UTILITY, "D", SheetSchema.LEDGER_MAX_ROW)
    private val utilitySplitType = columnRange(Tabs.UTILITY, "E", SheetSchema.LEDGER_MAX_ROW)
    private val utilityTargetMember = columnRange(Tabs.UTILITY, "F", SheetSchema.LEDGER_MAX_ROW)

    private val depositAmount = columnRange(Tabs.DEPOSITS, "D", SheetSchema.LEDGER_MAX_ROW)
    private val depositMember = columnRange(Tabs.DEPOSITS, "C", SheetSchema.LEDGER_MAX_ROW)
    private val depositStatus = columnRange(Tabs.DEPOSITS, "G", SheetSchema.LEDGER_MAX_ROW)

    /** `Members!$A$2:$G$500` – the lookup table for every `VLOOKUP`. */
    private val membersTable =
        "${Tabs.MEMBERS}!\$A\$$FIRST_DATA_ROW:\$G\$${SheetSchema.MEMBERS_MAX_ROW}"

    /** `Members!$F$2:$F$500` – the `ActiveStatus` column. */
    private val membersActive = columnRange(Tabs.MEMBERS, "F", SheetSchema.MEMBERS_MAX_ROW)

    /** `Config!$B$5` – the breakfast weight. */
    private val breakfastWeight =
        "${Tabs.CONFIG}!\$B\$${SheetSchema.Config.ROW_BREAKFAST_WEIGHT}"

    // -----------------------------------------------------------------------
    // Row 1 – the metric block
    // -----------------------------------------------------------------------

    /**
     * `[["TotalBazar", "=SUM(...)"], ["TotalMeals", "=..."], …]`
     *
     * The layout is `label | value | label | value | label | value | label | value`
     * so that the block reads nicely on a phone.
     */
    fun metricBlock(): List<List<String>> = listOf(
        listOf(
            "TotalBazar",
            "=SUM($bazarAmount)",
        ),
        listOf(
            "TotalMeals",
            "=SUM($mealsBreakfast)*$breakfastWeight" +
                "+SUM($mealsLunch)" +
                "+SUM($mealsDinner)" +
                "+SUM($mealsGuest)",
        ),
        listOf(
            "LiveMealRate",
            "=IFERROR(${SheetSchema.Summary.CELL_TOTAL_BAZAR}/${SheetSchema.Summary.CELL_TOTAL_MEALS},0)",
        ),
        listOf(
            "TotalEqualUtilities",
            "=SUMIFS($utilityAmount,$utilitySplitType,\"EQUAL_ALL\")",
        ),
    )

    /**
     * Row 2 – echoes the mess name and the month so the sheet is self
     * describing when it is opened in the browser.
     */
    fun contextBlock(): List<List<String>> = listOf(
        listOf(
            "Mess",
            "=${Tabs.CONFIG}!\$B\$${SheetSchema.Config.ROW_MESS_NAME}",
        ),
        listOf(
            "Month",
            "=${Tabs.CONFIG}!\$B\$${SheetSchema.Config.ROW_MONTH_YEAR}",
        ),
        listOf(
            "Meal rate = TotalBazar / TotalMeals",
            "",
        ),
    )

    // -----------------------------------------------------------------------
    // Member rows
    // -----------------------------------------------------------------------

    /**
     * The formula block of a single member.
     *
     * @param memberId the member id written into column A.
     * @param row      the 1-based sheet row of this member (4, 5, 6 …).
     */
    fun memberRow(memberId: String, row: Int): List<String> {
        val key = "\$A$row"
        return listOf(
            // A – MemberID (literal value, every other formula references it)
            memberId,

            // B – Name (looked up from the Members tab)
            "=IFERROR(VLOOKUP($key,$membersTable,2,FALSE),\"\")",

            // C – TotalMeals (breakfast is weighted, guests count as full meals)
            "=SUMIFS($mealsBreakfast,$mealsMember,$key)*$breakfastWeight" +
                "+SUMIFS($mealsLunch,$mealsMember,$key)" +
                "+SUMIFS($mealsDinner,$mealsMember,$key)" +
                "+SUMIFS($mealsGuest,$mealsMember,$key)",

            // D – MealCost = own meals × live meal rate
            "=ROUND(\$C$row*IFERROR(${SheetSchema.Summary.CELL_MEAL_RATE},0),2)",

            // E – SeatRent (individual, e.g. single room vs. 3-seat room)
            "=IFERROR(VLOOKUP($key,$membersTable,5,FALSE),0)",

            // F – EqualUtility (only ACTIVE members take part in the split)
            "=IFERROR(IF(VLOOKUP($key,$membersTable,6,FALSE)=\"ACTIVE\"," +
                "${SheetSchema.Summary.CELL_TOTAL_EQUAL_UTILITY}/COUNTIFS($membersActive,\"ACTIVE\"),0),0)",

            // G – CustomUtility (bills charged to this member only)
            "=SUMIFS($utilityAmount,$utilityTargetMember,$key)",

            // H – TotalCost
            "=ROUND(\$D$row+\$E$row+\$F$row+\$G$row,2)",

            // I – PrevBalance (carried over from the previous month)
            "=IFERROR(VLOOKUP($key,$membersTable,7,FALSE),0)",

            // J – TotalDeposit (only verified deposits count)
            "=SUMIFS($depositAmount,$depositMember,$key,$depositStatus,\"VERIFIED\")",

            // K – NetBalance = (carry forward + deposits) − cost
            "=ROUND((\$I$row+\$J$row)-\$H$row,2)",
        )
    }

    /**
     * The whole member block of the `Summary` tab – one row per member.
     *
     * @param memberIds member ids in the order of the `Members` tab.
     */
    fun memberRows(memberIds: List<String>): List<List<String>> =
        memberIds.mapIndexed { index, memberId ->
            memberRow(memberId, SheetSchema.summaryRow(index))
        }
}
