package com.messmate.bd.data.remote

import com.messmate.bd.domain.model.SheetSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `Summary` tab is the heart of the transparency promise – its formulas
 * must reference the right columns and rows.
 */
class SummaryFormulaBuilderTest {

    @Test
    fun `metric block contains the four headline numbers`() {
        val block = SummaryFormulaBuilder.metricBlock()

        assertEquals(4, block.size)
        assertEquals("TotalBazar", block[0][0])
        assertEquals("TotalMeals", block[1][0])
        assertEquals("LiveMealRate", block[2][0])
        assertEquals("TotalEqualUtilities", block[3][0])

        assertTrue(block[0][1].startsWith("=SUM(Bazar!"))
        assertTrue(block[1][1].contains("Config!\$B\$${SheetSchema.Config.ROW_BREAKFAST_WEIGHT}"))
        assertTrue(block[2][1].contains("\$B\$1") && block[2][1].contains("\$D\$1"))
        assertTrue(block[3][1].contains("\"EQUAL_ALL\""))
    }

    @Test
    fun `member row has eleven columns`() {
        val row = SummaryFormulaBuilder.memberRow("M001", 4)

        assertEquals(11, row.size)
        assertEquals("M001", row[0])
        assertEquals("=IFERROR(VLOOKUP(\$A4,", row[1])
        assertTrue(row[2].startsWith("=SUMIFS(Meals!"))
        assertEquals("=ROUND(\$C4*IFERROR(\$F\$1,0),2)", row[3])
        assertTrue(row[5].contains("COUNTIFS(Members!"))
        assertTrue(row[6].contains("UtilityBills!"))
        assertEquals("=ROUND(\$D4+\$E4+\$F4+\$G4,2)", row[7])
        assertTrue(row[9].contains("\"VERIFIED\""))
        assertEquals("=ROUND((\$I4+\$J4)-\$H4,2)", row[10])
    }

    @Test
    fun `member rows start at row four`() {
        val rows = SummaryFormulaBuilder.memberRows(listOf("M001", "M002"))

        assertEquals(2, rows.size)
        assertTrue(rows[0][1].contains("\$A4"))
        assertTrue(rows[1][1].contains("\$A5"))
    }

    @Test
    fun `summary row numbers follow the members tab`() {
        assertEquals(4, SheetSchema.summaryRow(0))
        assertEquals(5, SheetSchema.summaryRow(1))
        // Members start on row 2 (row 1 is the header).
        assertEquals(2, SheetSchema.memberRow(0))
    }
}
