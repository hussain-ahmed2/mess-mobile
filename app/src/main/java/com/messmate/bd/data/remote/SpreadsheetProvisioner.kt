package com.messmate.bd.data.remote

import com.messmate.bd.domain.model.ActionType
import com.messmate.bd.domain.model.BlueprintMember
import com.messmate.bd.domain.model.MealGrid
import com.messmate.bd.domain.model.MessBlueprint
import com.messmate.bd.domain.model.ProvisionedMess
import com.messmate.bd.domain.model.SheetSchema
import com.messmate.bd.domain.model.SheetSchema.Tabs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the mess spreadsheet from scratch.
 *
 * The whole sheet is generated programmatically so that the app only ever needs
 * the **restricted** `drive.file` scope: a file the app creates is fully
 * accessible to the app, so no `spreadsheets` or full `drive` scope is needed.
 *
 * ### What gets created
 *
 * | Tab | Content |
 * |-----|---------|
 * | `Config` | 8 key/value rows (mess name, month, cut-offs, lock state, …) |
 * | `Members` | one row per member (id, name, gmail, role, seat rent, …) |
 * | `Meals` | the full grid – one row per (member, day) – pre-filled with `0` |
 * | `Bazar` | header only – rows are appended while the month runs |
 * | `UtilityBills` | header only – appended |
 * | `Deposits` | header only – appended |
 * | `AuditLog` | header only – appended (plus the creation entry) |
 * | `Summary` | **100 % formulas** – metrics on top, one formula row per member |
 *
 * ### Write strategy
 *
 * 1. `ensureTabs` – create the 8 tabs, delete the default `Sheet1`, freeze row 1.
 * 2. `batchWrite(RAW)` – headers and static content in **one** request.
 * 3. `batchWrite(USER_ENTERED)` – the `Summary` formulas in **one** request
 *    (USER_ENTERED is required so `=SUM(…)` becomes a real formula).
 * 4. `permissions.create` – share the file with every member.
 * 5. `values.append` – the first `AuditLog` entry.
 */
@Singleton
class SpreadsheetProvisioner @Inject constructor(
    private val driveGateway: DriveGateway,
    private val sheetsGateway: SheetsGateway,
    private val clock: Clock,
) {

    /**
     * Provisions a complete mess spreadsheet.
     *
     * @throws Exception when the sheet could not be created – the caller is
     *         responsible for cleaning up (the repository deletes the orphaned
     *         file).
     */
    suspend fun provision(blueprint: MessBlueprint): ProvisionedMess = withContext(Dispatchers.IO) {
        val month = blueprint.monthYear
        val members = blueprint.members.sortedBy { it.memberId } // stable order!
        val memberIds = members.map { it.memberId }

        // ------------------------------------------------------------------
        // 1. Create the file
        // ------------------------------------------------------------------
        val fileName = buildFileName(blueprint.messName, month)
        val file = driveGateway.createSpreadsheet(
            name = fileName,
            description = "Mess \"${blueprint.messName}\" managed with MessMate BD",
        )

        // ------------------------------------------------------------------
        // 2. Shape the tabs
        // ------------------------------------------------------------------
        sheetsGateway.ensureTabs(file.id, SheetSchema.Tabs.ALL)

        // ------------------------------------------------------------------
        // 3. Static content (RAW → numbers stay numbers, dates stay text)
        // ------------------------------------------------------------------
        sheetsGateway.batchWrite(
            spreadsheetId = file.id,
            ranges = staticRanges(blueprint, members),
            inputOption = SheetInputOption.RAW,
        )

        // ------------------------------------------------------------------
        // 4. Formulas (USER_ENTERED → "=SUM(...)" becomes a real formula)
        // ------------------------------------------------------------------
        sheetsGateway.batchWrite(
            spreadsheetId = file.id,
            ranges = formulaRanges(memberIds),
            inputOption = SheetInputOption.USER_ENTERED,
        )

        // ------------------------------------------------------------------
        // 5. Share the file with the members
        // ------------------------------------------------------------------
        val shareOutcome = driveGateway.shareWithUsers(
            fileId = file.id,
            emails = members.map { it.gmail }.filter { it.isNotBlank() },
            sendNotification = true,
        )

        // ------------------------------------------------------------------
        // 6. First audit entry
        // ------------------------------------------------------------------
        sheetsGateway.appendRows(
            spreadsheetId = file.id,
            tab = Tabs.AUDIT,
            rows = listOf(
                listOf(
                    newId(),
                    nowIso(),
                    blueprint.managerEmail,
                    ActionType.MESS_CREATED.sheetValue,
                    "Mess \"${blueprint.messName}\" created for $monthLabel(month) " +
                        "with ${members.size} member(s); sheet shared with " +
                        "${shareOutcome.granted.size} account(s).",
                ),
            ),
        )

        ProvisionedMess(
            spreadsheetId = file.id,
            spreadsheetUrl = file.webViewLink,
            fileName = fileName,
            members = members,
        )
    }

    /**
     * Deletes a sheet that could not be provisioned completely (orphan cleanup).
     */
    suspend fun deleteSheet(spreadsheetId: String) {
        driveGateway.deleteFile(spreadsheetId)
    }

    // -----------------------------------------------------------------------
    // Static content
    // -----------------------------------------------------------------------

    /**
     * All non-formula writes of a freshly created sheet – collected into a
     * single `values.batchUpdate` request.
     */
    private fun staticRanges(
        blueprint: MessBlueprint,
        members: List<BlueprintMember>,
    ): List<SheetValueRange> {
        val month = blueprint.monthYear
        val memberIds = members.map { it.memberId }
        val gridRows = MealGrid.rowsFor(memberIds, month)

        return listOf(
            // --- Config ---------------------------------------------------
            SheetValueRange(
                range = "${Tabs.CONFIG}!A1:B${SheetSchema.Config.ROW_LOCK_STATUS}",
                values = configRows(blueprint),
            ),

            // --- Members --------------------------------------------------
            SheetValueRange(
                range = "${Tabs.MEMBERS}!A1:G${members.size + 1}",
                values = memberRows(members),
            ),

            // --- Meals (the full grid, pre-filled with zeros) ------------
            SheetValueRange(
                range = "${Tabs.MEALS}!A1:H${gridRows.size + 1}",
                values = mealRows(gridRows),
            ),

            // --- Append-only ledgers (headers only) ----------------------
            SheetValueRange(
                range = "${Tabs.BAZAR}!A1:G1",
                values = listOf(SheetSchema.Headers.BAZAR),
            ),
            SheetValueRange(
                range = "${Tabs.UTILITY}!A1:G1",
                values = listOf(SheetSchema.Headers.UTILITY),
            ),
            SheetValueRange(
                range = "${Tabs.DEPOSITS}!A1:G1",
                values = listOf(SheetSchema.Headers.DEPOSITS),
            ),
            SheetValueRange(
                range = "${Tabs.AUDIT}!A1:E1",
                values = listOf(SheetSchema.Headers.AUDIT),
            ),

            // --- Summary header row (row 3) ------------------------------
            SheetValueRange(
                range = "${Tabs.SUMMARY}!A${SheetSchema.Summary.ROW_HEADERS}:" +
                    "${SheetSchema.lastColumnLetter(Tabs.SUMMARY)}${SheetSchema.Summary.ROW_HEADERS}",
                values = listOf(SheetSchema.Headers.SUMMARY),
            ),
        )
    }

    /**
     * All formula writes – one request, `USER_ENTERED`.
     */
    private fun formulaRanges(memberIds: List<String>): List<SheetValueRange> {
        val lastColumn = SheetSchema.lastColumnLetter(Tabs.SUMMARY)
        val ranges = mutableListOf<SheetValueRange>()

        // Row 1 – metric block (label | value | label | value | …)
        ranges += SheetValueRange(
            range = "${Tabs.SUMMARY}!A${SheetSchema.Summary.ROW_METRICS}:" +
                "${lastColumn}${SheetSchema.Summary.ROW_METRICS}",
            values = SummaryFormulaBuilder.metricBlock(),
        )

        // Row 2 – mess name / month
        ranges += SheetValueRange(
            range = "${Tabs.SUMMARY}!A${SheetSchema.Summary.ROW_CONTEXT}:" +
                "${lastColumn}${SheetSchema.Summary.ROW_CONTEXT}",
            values = SummaryFormulaBuilder.contextBlock(),
        )

        // Row 4+ – one formula block per member
        if (memberIds.isNotEmpty()) {
            val firstMemberRow = SheetSchema.Summary.ROW_FIRST_MEMBER
            val lastMemberRow = SheetSchema.Summary.ROW_FIRST_MEMBER + memberIds.size - 1
            ranges += SheetValueRange(
                range = "${Tabs.SUMMARY}!A$firstMemberRow:${lastColumn}$lastMemberRow",
                values = SummaryFormulaBuilder.memberRows(memberIds),
            )
        }

        return ranges
    }

    // -----------------------------------------------------------------------
    // Row builders
    // -----------------------------------------------------------------------

    /** The `Config` tab: header + the eight key/value pairs. */
    private fun configRows(blueprint: MessBlueprint): List<List<Any?>> {
        val month = blueprint.monthYear
        val values = mapOf(
            SheetSchema.Config.KEY_MESS_NAME to blueprint.messName,
            SheetSchema.Config.KEY_MONTH_YEAR to monthLabel(month),
            SheetSchema.Config.KEY_MANAGER_EMAIL to blueprint.managerEmail,
            SheetSchema.Config.KEY_BREAKFAST_WEIGHT to blueprint.breakfastWeight,
            SheetSchema.Config.KEY_BREAKFAST_CUTOFF to formatTime(blueprint.breakfastCutoff),
            SheetSchema.Config.KEY_LUNCH_CUTOFF to formatTime(blueprint.lunchCutoff),
            SheetSchema.Config.KEY_DINNER_CUTOFF to formatTime(blueprint.dinnerCutoff),
            SheetSchema.Config.KEY_LOCK_STATUS to "OPEN",
        )

        val rows = mutableListOf<List<Any?>>(listOf(SheetSchema.Headers.CONFIG))
        SheetSchema.Config.KEYS.forEach { key ->
            rows += listOf(key, values[key] ?: "")
        }
        return rows
    }

    /** The `Members` tab: header + one row per member. */
    private fun memberRows(members: List<BlueprintMember>): List<List<Any?>> {
        val rows = mutableListOf<List<Any?>>(listOf(SheetSchema.Headers.MEMBERS))
        members.forEach { member ->
            rows += listOf(
                member.memberId,
                member.name,
                member.gmail,
                member.role.sheetValue,
                member.seatRent,
                member.activeStatus.sheetValue,
                member.prevBalance,
            )
        }
        return rows
    }

    /**
     * The `Meals` tab: header + the pre-populated grid.
     *
     * Every row belongs to exactly one (member, day) pair, therefore toggling a
     * meal later on is a single cell write – no append, no collisions.
     */
    private fun mealRows(gridRows: List<com.messmate.bd.domain.model.MealGridRow>): List<List<Any?>> {
        val rows = mutableListOf<List<Any?>>(listOf(SheetSchema.Headers.MEALS))
        gridRows.forEach { gridRow ->
            rows += listOf(
                gridRow.date.toString(), // ISO date → sorts correctly and parses everywhere
                gridRow.memberId,
                0, // breakfast
                0, // lunch
                0, // dinner
                0, // guest meals
                "", // updated by
                "", // updated at
            )
        }
        return rows
    }

    // -----------------------------------------------------------------------
    // Small helpers
    // -----------------------------------------------------------------------

    private fun buildFileName(messName: String, month: YearMonth): String =
        "$messName – ${monthLabel(month)} (MessMate)"

    /** `2026-11` – the canonical month label used in `Config` and in file names. */
    private fun monthLabel(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    /** `07:00` – the canonical cut-off format. */
    private fun formatTime(time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("HH:mm"))

    /** ISO-8601 timestamp with offset, e.g. `2026-11-12T18:02:11+06:00`. */
    private fun nowIso(): String =
        java.time.OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun newId(): String = UUID.randomUUID().toString()
}
