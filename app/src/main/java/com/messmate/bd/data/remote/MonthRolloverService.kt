package com.messmate.bd.data.remote

import com.messmate.bd.domain.model.ActionType
import com.messmate.bd.domain.model.BlueprintMember
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.MessBlueprint
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.model.ProvisionedMess
import com.messmate.bd.domain.model.SheetSchema
import com.messmate.bd.domain.model.SheetSchema.Tabs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Closes a month and opens the next one.
 *
 * The procedure is strictly defined so that nothing is lost:
 *
 * 1. Read the closing `NetBalance` of every member from the `Summary` tab.
 * 2. Write `Config!LockStatus = LOCKED` – the month becomes read-only.
 * 3. Append a `MONTH_LOCKED` entry to the `AuditLog`.
 * 4. Create the spreadsheet of the next month with the **same members, the
 *    same seat rents** and the closing balance as `PrevBalance`.
 * 5. Rename the old sheet to `… (Closed)` so the archive stays readable.
 *
 * Because the new sheet is a **new file**, the old month keeps its complete
 * history – nothing is ever overwritten.
 */
@Singleton
class MonthRolloverService @Inject constructor(
    private val provisioner: SpreadsheetProvisioner,
    private val sheetsGateway: SheetsGateway,
    private val driveGateway: DriveGateway,
    private val clock: Clock,
) {

    /**
     * @param closingBalances member id → closing `NetBalance` of the month that
     *        is being closed. A positive balance means the manager owes the
     *        member; it is carried over as `PrevBalance`.
     */
    suspend fun rollover(
        spreadsheetId: String,
        config: MessConfig,
        members: List<Member>,
        closingBalances: Map<String, Double>,
        nextMonth: YearMonth = config.monthYear.plusMonths(1),
    ): ProvisionedMess = withContext(Dispatchers.IO) {
        val manager = members.firstOrNull { it.isManager }
        val managerEmail = manager?.gmail ?: config.managerEmail

        // ------------------------------------------------------------------
        // 1. Lock the current sheet
        // ------------------------------------------------------------------
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange(
                    range = "${Tabs.CONFIG}!B${SheetSchema.Config.ROW_LOCK_STATUS}",
                    values = listOf(listOf("LOCKED")),
                ),
            ),
            inputOption = SheetInputOption.RAW,
        )

        // ------------------------------------------------------------------
        // 2. Audit trail
        // ------------------------------------------------------------------
        sheetsGateway.appendRows(
            spreadsheetId = spreadsheetId,
            tab = Tabs.AUDIT,
            rows = listOf(
                listOf(
                    UUID.randomUUID().toString(),
                    java.time.OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    managerEmail,
                    ActionType.MONTH_LOCKED.sheetValue,
                    "Month ${config.monthYear} closed and locked.",
                ),
            ),
        )

        // ------------------------------------------------------------------
        // 3. Build the blueprint of the next month
        // ------------------------------------------------------------------
        val carriedMembers = members
            .filter { it.isActive }
            .sortedBy { it.memberId }
            .map { member ->
                BlueprintMember(
                    memberId = member.memberId,
                    name = member.name,
                    gmail = member.gmail,
                    role = member.role,
                    seatRent = member.seatRent,
                    prevBalance = closingBalances[member.memberId] ?: 0.0,
                    activeStatus = member.activeStatus,
                )
            }

        val blueprint = MessBlueprint(
            messName = config.messName,
            monthYear = nextMonth,
            managerName = manager?.name ?: config.messName,
            managerEmail = managerEmail,
            members = carriedMembers,
            breakfastWeight = config.breakfastWeight,
            breakfastCutoff = config.breakfastCutoff,
            lunchCutoff = config.lunchCutoff,
            dinnerCutoff = config.dinnerCutoff,
        )

        // ------------------------------------------------------------------
        // 4. Create the new sheet
        // ------------------------------------------------------------------
        val provisioned = provisioner.provision(blueprint)

        // ------------------------------------------------------------------
        // 5. Rename the old sheet (archive)
        // ------------------------------------------------------------------
        runCatching {
            driveGateway.renameFile(
                spreadsheetId,
                "${config.messName} – ${config.monthYear.format(DateTimeFormatter.ofPattern("yyyy-MM"))} (Closed)",
            )
        }

        provisioned
    }
}
