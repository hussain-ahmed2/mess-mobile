package com.messmate.bd.ui.statement

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.domain.model.MessConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders the monthly statement as a PDF.
 *
 * The PDF is written into the cache directory and shared through the Android
 * share sheet – the usual way to send a bill to the other members on WhatsApp.
 */
@Singleton
class StatementPdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Creates the PDF and returns a `content://` uri that can be shared.
     *
     * @throws IllegalStateException when the file could not be written.
     */
    fun export(
        messName: String,
        month: YearMonth,
        config: MessConfig,
        statements: List<MemberStatement>,
    ): Uri {
        val document = PdfDocument()
        try {
            drawSummaryPage(document, messName, month, config, statements)
            statements.forEach { statement ->
                drawMemberPage(document, messName, month, statement)
            }

            val file = File(context.cacheDir, "messmate_${messName}_${month}.pdf".sanitize())
            file.outputStream().use { document.writeTo(it) }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } finally {
            document.close()
        }
    }

    // -----------------------------------------------------------------------
    // Pages
    // -----------------------------------------------------------------------

    private fun drawSummaryPage(
        document: PdfDocument,
        messName: String,
        month: YearMonth,
        config: MessConfig,
        statements: List<MemberStatement>,
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)

        var y = drawHeader(page, messName, month.toString())

        y = drawRow(page, y, "Total bazar", "")
        y = drawRow(page, y, "Total meals", "")
        y = drawRow(page, y, "Breakfast weight", config.breakfastWeight.toString())
        y = drawRow(page, y, "Members", statements.size.toString())
        y += 16f

        val headerPaint = boldPaint(11f)
        page.canvas.drawText("MemberID", 40f, y, headerPaint)
        page.canvas.drawText("Name", 120f, y, headerPaint)
        page.canvas.drawText("Meals", 250f, y, headerPaint)
        page.canvas.drawText("Cost", 310f, y, headerPaint)
        page.canvas.drawText("Deposit", 400f, y, headerPaint)
        page.canvas.drawText("Balance", 480f, y, headerPaint)
        y += 16f

        statements.forEach { statement ->
            page.canvas.drawText(statement.member.memberId, 40f, y, bodyPaint)
            page.canvas.drawText(statement.member.name.take(16), 120f, y, bodyPaint)
            page.canvas.drawText(statement.totalMeals.toInt().toString(), 250f, y, bodyPaint)
            page.canvas.drawText(statement.totalCost.toString(), 310f, y, bodyPaint)
            page.canvas.drawText(statement.totalDeposit.toString(), 400f, y, bodyPaint)
            page.canvas.drawText(statement.netBalance.toString(), 480f, y, bodyPaint)
            y += 16f
        }

        document.finishPage(page)
    }

    private fun drawMemberPage(
        document: PdfDocument,
        messName: String,
        month: YearMonth,
        statement: MemberStatement,
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
        val page = document.startPage(pageInfo)

        var y = drawHeader(page, messName, month.toString())
        y = drawRow(page, y, "Member", "${statement.member.memberId} · ${statement.member.name}")
        y += 8f

        val rows = listOf(
            "Total meals" to statement.totalMeals.toString(),
            "Meal cost" to statement.mealCost.toString(),
            "Seat rent" to statement.seatRent.toString(),
            "Shared utilities" to statement.equalUtility.toString(),
            "Personal charges" to statement.customUtility.toString(),
            "Total cost" to statement.totalCost.toString(),
            "Previous balance" to statement.prevBalance.toString(),
            "Deposits" to statement.totalDeposit.toString(),
        )

        rows.forEach { (label, value) -> y = drawRow(page, y, label, value) }

        y += 12f
        val balancePaint = boldPaint(14f)
        page.canvas.drawText(
            "Net balance: ${statement.netBalance}",
            40f,
            y,
            balancePaint,
        )
        y += 24f
        page.canvas.drawText(
            "Positive balance = the mess manager owes the member.",
            40f,
            y,
            bodyPaint,
        )

        document.finishPage(page)
    }

    private fun drawHeader(page: PdfDocument.Page, messName: String, month: String): Float {
        page.canvas.drawText(messName, 40f, 60f, titlePaint)
        page.canvas.drawText(month, 40f, 80f, bodyPaint)
        page.canvas.drawLine(40f, 92f, PAGE_WIDTH - 40f, 92f, linePaint)
        return 120f
    }

    private fun drawRow(page: PdfDocument.Page, y: Float, label: String, value: String): Float {
        page.canvas.drawText(label, 40f, y, bodyPaint)
        page.canvas.drawText(value, 300f, y, bodyPaint)
        return y + 18f
    }

    // -----------------------------------------------------------------------
    // Paints
    // -----------------------------------------------------------------------

    private val titlePaint = Paint().apply {
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val bodyPaint = Paint().apply {
        textSize = 11f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        strokeWidth = 1f
        color = android.graphics.Color.LTGRAY
    }

    private fun boldPaint(size: Float) = Paint().apply {
        textSize = size
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private fun String.sanitize(): String = replace(Regex("[^a-zA-Z0-9_-]"), "_")

    private companion object {
        // A4 at 72 dpi.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
    }
}
