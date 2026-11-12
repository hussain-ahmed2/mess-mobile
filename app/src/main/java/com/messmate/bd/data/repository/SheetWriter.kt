package com.messmate.bd.data.repository

import com.messmate.bd.data.local.dao.DepositDao
import com.messmate.bd.data.local.dao.MemberDao
import com.messmate.bd.data.local.dao.MessDao
import com.messmate.bd.data.local.entity.OutboxPayload
import com.messmate.bd.data.remote.DriveGateway
import com.messmate.bd.data.remote.SheetInputOption
import com.messmate.bd.data.remote.SheetValueRange
import com.messmate.bd.data.remote.SheetsGateway
import com.messmate.bd.data.remote.SummaryFormulaBuilder
import com.messmate.bd.domain.model.ActionType
import com.messmate.bd.domain.model.MealGrid
import com.messmate.bd.domain.model.SheetSchema
import com.messmate.bd.domain.model.SheetSchema.Tabs
import com.messmate.bd.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates a queued [OutboxPayload] into the concrete Google Sheets calls.
 *
 * Every write targets **one** cell or **appends** one row – never a
 * read-modify-write cycle – so two members acting at the same time can never
 * overwrite each other.
 */
@Singleton
class SheetWriter @Inject constructor(
    private val sheetsGateway: SheetsGateway,
    private val driveGateway: DriveGateway,
    private val messDao: MessDao,
    private val memberDao: MemberDao,
    private val depositDao: DepositDao,
    private val accountRepository: AccountRepository,
    private val clock: Clock,
) {

    /**
     * Applies a single queued operation to the spreadsheet.
     *
     * @throws Exception when the sheet rejected the write – the operation stays
     *         in the outbox and is retried later.
     */
    suspend fun apply(spreadsheetId: String, payload: OutboxPayload) {
        when (payload) {
            is OutboxPayload.SetMeal -> writeMeal(spreadsheetId, payload)
            is OutboxPayload.SetGuestMeals -> writeGuestMeals(spreadsheetId, payload)
            is OutboxPayload.AppendBazar -> appendBazar(spreadsheetId, payload)
            is OutboxPayload.AppendUtility -> appendUtility(spreadsheetId, payload)
            is OutboxPayload.AppendDeposit -> appendDeposit(spreadsheetId, payload)
            is OutboxPayload.SetDepositStatus -> writeDepositStatus(spreadsheetId, payload)
            is OutboxPayload.SetSeatRent -> writeSeatRent(spreadsheetId, payload)
            is OutboxPayload.SetMemberActive -> writeMemberStatus(spreadsheetId, payload)
            is OutboxPayload.AppendMember -> appendMember(spreadsheetId, payload)
            is OutboxPayload.SetConfigValue -> writeConfigValue(spreadsheetId, payload)
        }
    }

    /** Appends an entry to the immutable `AuditLog` tab. */
    suspend fun appendAudit(
        spreadsheetId: String,
        actorEmail: String,
        actionType: ActionType,
        details: String,
    ) {
        sheetsGateway.appendRows(
            spreadsheetId = spreadsheetId,
            tab = Tabs.AUDIT,
            rows = listOf(
                listOf(
                    UUID.randomUUID().toString(),
                    OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    actorEmail,
                    actionType.sheetValue,
                    details,
                ),
            ),
        )
    }

    // -----------------------------------------------------------------------
    // Meals
    // -----------------------------------------------------------------------

    /** Writes a single meal flag (breakfast / lunch / dinner) of one member. */
    private suspend fun writeMeal(spreadsheetId: String, payload: OutboxPayload.SetMeal) {
        val row = mealRowOf(spreadsheetId, payload.memberId, payload.date) ?: return
        val column = columnOf(payload.mealType)
        val value = if (payload.enabled) 1 else 0
        val stamp = nowIso()

        // One request → the flag and the "who changed it" stamp stay in sync.
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange("${Tabs.MEALS}!$column$row", listOf(listOf(value))),
                SheetValueRange(
                    "${Tabs.MEALS}!G$row:H$row",
                    listOf(listOf(actorOf(spreadsheetId), stamp)),
                ),
            ),
            inputOption = SheetInputOption.RAW,
        )
    }

    /** Writes the number of guest meals of one member. */
    private suspend fun writeGuestMeals(spreadsheetId: String, payload: OutboxPayload.SetGuestMeals) {
        val row = mealRowOf(spreadsheetId, payload.memberId, payload.date) ?: return
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange("${Tabs.MEALS}!F$row", listOf(listOf(payload.count))),
                SheetValueRange(
                    "${Tabs.MEALS}!G$row:H$row",
                    listOf(listOf(actorOf(spreadsheetId), nowIso())),
                ),
            ),
            inputOption = SheetInputOption.RAW,
        )
    }

    /**
     * Row of the meal cell of a member.
     *
     * Derived from the member's position inside the `Members` tab and the day of
     * the month – every device computes the same coordinate.
     */
    private suspend fun mealRowOf(
        spreadsheetId: String,
        memberId: String,
        isoDate: String,
    ): Int? {
        val member = memberDao.getMember(spreadsheetId, memberId) ?: return null
        val config = messDao.getConfig(spreadsheetId) ?: return null
        val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return null
        val memberIndex = (member.sheetRow - 2).coerceAtLeast(0)
        val days = config.monthYear.lengthOfMonth()
        if (date.dayOfMonth > days) return null
        return MealGrid.rowOf(memberIndex, date.dayOfMonth, days)
    }

    private fun columnOf(mealType: String): String = when (mealType.uppercase()) {
        "BREAKFAST" -> "C"
        "LUNCH" -> "D"
        "DINNER" -> "E"
        else -> "C"
    }

    // -----------------------------------------------------------------------
    // Ledgers (append only)
    // -----------------------------------------------------------------------

    private suspend fun appendBazar(spreadsheetId: String, payload: OutboxPayload.AppendBazar) {
        sheetsGateway.appendRows(
            spreadsheetId = spreadsheetId,
            tab = Tabs.BAZAR,
            rows = listOf(
                listOf(
                    payload.entryId,
                    payload.date,
                    payload.shopperName,
                    payload.items,
                    payload.amount,
                    payload.loggedBy,
                    payload.timestamp,
                ),
            ),
        )
    }

    private suspend fun appendUtility(spreadsheetId: String, payload: OutboxPayload.AppendUtility) {
        sheetsGateway.appendRows(
            spreadsheetId = spreadsheetId,
            tab = Tabs.UTILITY,
            rows = listOf(
                listOf(
                    payload.billId,
                    payload.date,
                    payload.category,
                    payload.amount,
                    payload.splitType,
                    payload.targetMemberId.orEmpty(),
                    payload.notes,
                ),
            ),
        )
    }

    private suspend fun appendDeposit(spreadsheetId: String, payload: OutboxPayload.AppendDeposit) {
        sheetsGateway.appendRows(
            spreadsheetId = spreadsheetId,
            tab = Tabs.DEPOSITS,
            rows = listOf(
                listOf(
                    payload.depositId,
                    payload.date,
                    payload.memberId,
                    payload.amount,
                    payload.method,
                    payload.trxId,
                    payload.status,
                ),
            ),
        )
    }

    // -----------------------------------------------------------------------
    // Single cell updates
    // -----------------------------------------------------------------------

    private suspend fun writeDepositStatus(
        spreadsheetId: String,
        payload: OutboxPayload.SetDepositStatus,
    ) {
        val deposit = depositDao.getDeposit(spreadsheetId, payload.depositId) ?: return
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange(
                    "${Tabs.DEPOSITS}!G${deposit.sheetRow}",
                    listOf(listOf(payload.status)),
                ),
            ),
            inputOption = SheetInputOption.RAW,
        )
    }

    private suspend fun writeSeatRent(spreadsheetId: String, payload: OutboxPayload.SetSeatRent) {
        val member = memberDao.getMember(spreadsheetId, payload.memberId) ?: return
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange(
                    "${Tabs.MEMBERS}!E${member.sheetRow}",
                    listOf(listOf(payload.seatRent)),
                ),
            ),
            inputOption = SheetInputOption.RAW,
        )
    }

    private suspend fun writeMemberStatus(
        spreadsheetId: String,
        payload: OutboxPayload.SetMemberActive,
    ) {
        val member = memberDao.getMember(spreadsheetId, payload.memberId) ?: return
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange(
                    "${Tabs.MEMBERS}!F${member.sheetRow}",
                    listOf(listOf(if (payload.active) "ACTIVE" else "INACTIVE")),
                ),
            ),
            inputOption = SheetInputOption.RAW,
        )
    }

    private suspend fun writeConfigValue(
        spreadsheetId: String,
        payload: OutboxPayload.SetConfigValue,
    ) {
        val row = SheetSchema.Config.rowOf(payload.key)
        if (row <= 0) return
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange(
                    "${Tabs.CONFIG}!B$row",
                    listOf(listOf(payload.value)),
                ),
            ),
            inputOption = SheetInputOption.RAW,
        )
    }

    // -----------------------------------------------------------------------
    // New member
    // -----------------------------------------------------------------------

    /**
     * Appends a member to **three** places:
     *
     * 1. `Members`  – the person themselves.
     * 2. `Meals`    – the pre-populated day grid of that member.
     * 3. `Summary`  – the formula row of that member.
     *
     * Finally the file is shared with the new member so they get access.
     */
    private suspend fun appendMember(spreadsheetId: String, payload: OutboxPayload.AppendMember) {
        val config = messDao.getConfig(spreadsheetId)
        val month = config?.monthYear ?: java.time.YearMonth.now()
        val days = month.lengthOfMonth()
        val members = memberDao.getMembers(spreadsheetId)
        val memberIndex = members.size

        // 1. The member row (appended → every existing row keeps its position).
        sheetsGateway.appendRows(
            spreadsheetId = spreadsheetId,
            tab = Tabs.MEMBERS,
            rows = listOf(
                listOf(
                    payload.memberId,
                    payload.name,
                    payload.gmail,
                    payload.role,
                    payload.seatRent,
                    "ACTIVE",
                    payload.prevBalance,
                ),
            ),
        )

        // 2. The meal grid of the new member.
        val gridRows = (1..days).map { day ->
            listOf(month.atDay(day).toString(), payload.memberId, 0, 0, 0, 0, "", "")
        }
        sheetsGateway.appendRows(
            spreadsheetId = spreadsheetId,
            tab = Tabs.MEALS,
            rows = gridRows,
        )

        // 3. The formula row inside the `Summary` tab.
        val summaryRow = SheetSchema.summaryRow(memberIndex)
        val lastColumn = SheetSchema.lastColumnLetter(Tabs.SUMMARY)
        sheetsGateway.batchWrite(
            spreadsheetId = spreadsheetId,
            ranges = listOf(
                SheetValueRange(
                    "${Tabs.SUMMARY}!A$summaryRow:$lastColumn$summaryRow",
                    listOf(SummaryFormulaBuilder.memberRow(payload.memberId, summaryRow)),
                ),
            ),
            inputOption = SheetInputOption.USER_ENTERED,
        )

        // 4. Give the member access to the file.
        if (payload.gmail.isNotBlank()) {
            driveGateway.shareWithUsers(
                fileId = spreadsheetId,
                emails = listOf(payload.gmail),
                sendNotification = true,
            )
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** The e-mail of the signed-in user – written into `Meals!UpdatedBy`. */
    private suspend fun actorOf(spreadsheetId: String): String =
        accountRepository.observeEmail().first().orEmpty()

    private fun nowIso(): String =
        OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
