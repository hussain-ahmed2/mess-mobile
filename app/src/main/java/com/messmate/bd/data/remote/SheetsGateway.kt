package com.messmate.bd.data.remote

import com.google.api.services.sheets.Sheets
import com.google.api.services.sheets.model.AppendValuesRequest
import com.google.api.services.sheets.model.BatchGetValuesRequest
import com.google.api.services.sheets.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.model.GridProperties
import com.google.api.services.sheets.model.Request
import com.google.api.services.sheets.model.Spreadsheet
import com.google.api.services.sheets.model.SpreadsheetProperties
import com.google.api.services.sheets.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, coroutine friendly wrapper around the Google Sheets v4 REST API.
 *
 * ### Why these specific calls?
 *
 * | Operation | Sheets call | Reason |
 * |-----------|-------------|--------|
 * | Read every tab | `values.batchGet` | **one** HTTP request for all tabs → saves quota |
 * | Write many cells | `values.batchUpdate` | atomic, avoids read-modify-write races |
 * | Append a ledger row | `values.append` | never overwrites a concurrent write |
 * | Create / reshape tabs | `spreadsheets.batchUpdate` | add & delete sheets, freeze header |
 */
@Singleton
class SheetsGateway @Inject constructor(
    private val apiFactory: GoogleApiFactory,
) {

    /** Lazily created client – building it is expensive (TLS + JSON factory). */
    private val service: Sheets by lazy { apiFactory.createSheetsService() }

    // -----------------------------------------------------------------------
    // Reading
    // -----------------------------------------------------------------------

    /**
     * Reads **all** requested ranges with a single HTTP request.
     *
     * @param ranges AAR1 notation ranges such as `Members!A1:G200`.
     * @return map of tab name → rows (each row is a list of cell strings).
     */
    suspend fun batchRead(
        spreadsheetId: String,
        ranges: List<String>,
    ): Map<String, List<List<String>>> = withContext(Dispatchers.IO) {
        if (ranges.isEmpty()) return@withContext emptyMap()

        val request = BatchGetValuesRequest()
            .setRanges(ranges)
            .setValueRenderOption(com.google.api.services.sheets.model.ValueRenderOption.FORMATTED_VALUE)
            .setDateTimeRenderOption(com.google.api.services.sheets.model.DateTimeRenderOption.SERIAL_NUMBER)

        val response = service.spreadsheets().values().batchGet(spreadsheetId, request).execute()

        val result = LinkedHashMap<String, List<List<String>>>(ranges.size)
        response.getValueRanges()?.forEachIndexed { index, range ->
            // The API returns the ranges in the order they were requested.
            val requested = ranges.getOrNull(index)
            val tab = tabNameOf(range.getRange()).ifBlank { tabNameOf(requested) }
            val key = tab.ifBlank { requested.orEmpty() }
            result[key] = range.getValues()?.map { row -> row.map { cell -> cell?.toString().orEmpty() } }
                ?: emptyList()
        }
        result
    }

    /** Reads a single range (convenience wrapper around [batchRead]). */
    suspend fun read(spreadsheetId: String, range: String): List<List<String>> =
        batchRead(spreadsheetId, listOf(range))[tabNameOf(range)] ?: emptyList()

    // -----------------------------------------------------------------------
    // Writing
    // -----------------------------------------------------------------------

    /**
     * Writes several ranges with one HTTP request.
     *
     * @param inputOption `RAW` keeps the values exactly as given (numbers stay
     *        numbers, `2026-11-01` stays text). `USER_ENTERED` interprets the
     *        content – required for formulas.
     */
    suspend fun batchWrite(
        spreadsheetId: String,
        ranges: List<SheetValueRange>,
        inputOption: SheetInputOption = SheetInputOption.RAW,
    ): Unit = withContext(Dispatchers.IO) {
        if (ranges.isEmpty()) return@withContext

        val data = ranges.map { range ->
            BatchUpdateValuesRequest.Data()
                .setRange(range.range)
                .setValues(range.values.toApiValues())
        }

        val request = BatchUpdateValuesRequest()
            .setValueInputOption(inputOption.apiValue)
            .setData(data)

        service.spreadsheets().values().batchUpdate(spreadsheetId, request).execute()
    }

    /**
     * Appends rows at the end of a tab.
     *
     * Used for the append-only ledgers (`Bazar`, `UtilityBills`, `Deposits`,
     * `AuditLog`) so that two members logging an entry at the same time never
     * overwrite each other.
     */
    suspend fun appendRows(
        spreadsheetId: String,
        tab: String,
        rows: List<List<Any?>>,
        inputOption: SheetInputOption = SheetInputOption.RAW,
    ): Unit = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) return@withContext

        val request = AppendValuesRequest()
            .setValueInputOption(inputOption.apiValue)
            .setInsertDataOption(INSERT_DATA_OPTION_INSERT_ROWS)
            .setValues(rows.toApiValues())

        // The range decides where the rows are appended and how wide they are
        // ("Bazar!A:G" = the full width of the tab).
        val lastColumn = com.messmate.bd.domain.model.SheetSchema.lastColumnLetter(tab)
        service.spreadsheets().values()
            .append(spreadsheetId, "$tab!A:$lastColumn", request)
            .execute()
    }

    /** Clears a range (used when a member is removed from the grid). */
    suspend fun clear(spreadsheetId: String, range: String): Unit = withContext(Dispatchers.IO) {
        service.spreadsheets().values()
            .clear(spreadsheetId, range, com.google.api.services.sheets.model.ClearValuesRequest())
            .execute()
    }

    // -----------------------------------------------------------------------
    // Sheet (tab) management
    // -----------------------------------------------------------------------

    /** Loads the spreadsheet metadata (tab names, ids, order). */
    suspend fun getSpreadsheet(spreadsheetId: String): Spreadsheet = withContext(Dispatchers.IO) {
        service.spreadsheets().get(spreadsheetId).execute()
    }

    /**
     * Makes sure the spreadsheet contains exactly the given tabs.
     *
     * Missing tabs are created, foreign tabs (e.g. the default `Sheet1`) are
     * removed and the header row of every tab is frozen so the sheet stays
     * readable on a phone.
     */
    suspend fun ensureTabs(spreadsheetId: String, desiredTabs: List<String>) {
        withContext(Dispatchers.IO) {
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val existing = spreadsheet.getSheets()?.map { it.properties.title }.orEmpty()

            val requests = mutableListOf<Request>()

            // 1. Create the missing tabs (in the requested order).
            desiredTabs.filter { it !in existing }.forEach { title ->
                requests += Request().setAddSheet(
                    com.google.api.services.sheets.model.AddSheetRequest()
                        .setProperties(SpreadsheetProperties().setTitle(title)),
                )
            }

            // 2. Remove tabs that are not part of the schema (e.g. "Sheet1").
            spreadsheet.getSheets()
                ?.filter { it.properties.title !in desiredTabs }
                ?.forEach { sheet ->
                    requests += Request().setDeleteSheet(
                        com.google.api.services.sheets.model.DeleteSheetRequest()
                            .setSheetId(sheet.properties.sheetId),
                    )
                }

            if (requests.isNotEmpty()) {
                service.spreadsheets().batchUpdate(
                    spreadsheetId,
                    BatchUpdateSpreadsheetRequest().setRequests(requests),
                ).execute()
            }

            // 3. Freeze the header row of every schema tab.
            val updated = service.spreadsheets().get(spreadsheetId).execute()
            val freezeRequests = updated.getSheets()
                ?.filter { it.properties.title in desiredTabs }
                ?.map { sheet ->
                    Request().setUpdateSheetProperties(
                        com.google.api.services.sheets.model.UpdateSheetPropertiesRequest()
                            .setSheetId(sheet.properties.sheetId)
                            .setProperties(
                                SpreadsheetProperties()
                                    .setGridProperties(GridProperties().setFrozenRowCount(1)),
                            ),
                    )
                }
                .orEmpty()

            if (freezeRequests.isNotEmpty()) {
                service.spreadsheets().batchUpdate(
                    spreadsheetId,
                    BatchUpdateSpreadsheetRequest().setRequests(freezeRequests),
                ).execute()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Extracts the tab name out of an A1 range (`Members!A1:G200` → `Members`).
     */
    private fun tabNameOf(range: String?): String =
        range?.substringBefore('!')?.trim().orEmpty()

    /** Converts the generic cell values into the list type the API expects. */
    private fun List<List<Any?>>.toApiValues(): MutableList<MutableList<Any>> =
        map { row ->
            row.map { cell ->
                when (cell) {
                    null -> ""
                    is String -> cell
                    is Number -> cell
                    is Boolean -> cell
                    else -> cell.toString()
                }
            }.toMutableList()
        }.toMutableList()

    private companion object {
        const val INSERT_DATA_OPTION_INSERT_ROWS = "INSERT_ROWS"
    }
}

/**
 * A range plus its values – the unit of work for [SheetsGateway.batchWrite].
 */
data class SheetValueRange(
    /** A1 notation, e.g. `Meals!C5` or `Members!A1:G10`. */
    val range: String,
    val values: List<List<Any?>>,
)

/**
 * How the Sheets API should interpret the written values.
 */
enum class SheetInputOption(val apiValue: String) {
    /** Keep the value as-is – numbers stay numbers, ISO dates stay text. */
    RAW("RAW"),

    /** Interpret the value as if a human typed it – required for formulas. */
    USER_ENTERED("USER_ENTERED"),
}
