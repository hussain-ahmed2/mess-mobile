package com.messmate.bd.data.mapper

import com.messmate.bd.data.local.entity.AuditRecordEntity
import com.messmate.bd.data.local.entity.BazarEntity
import com.messmate.bd.data.local.entity.ConfigEntity
import com.messmate.bd.data.local.entity.DepositEntity
import com.messmate.bd.data.local.entity.MealEntity
import com.messmate.bd.data.local.entity.MemberEntity
import com.messmate.bd.data.local.entity.UtilityBillEntity
import com.messmate.bd.domain.model.ActionType
import com.messmate.bd.domain.model.ActiveStatus
import com.messmate.bd.domain.model.AuditRecord
import com.messmate.bd.domain.model.BazarEntry
import com.messmate.bd.domain.model.Deposit
import com.messmate.bd.domain.model.DepositMethod
import com.messmate.bd.domain.model.DepositStatus
import com.messmate.bd.domain.model.LockStatus
import com.messmate.bd.domain.model.MealEntry
import com.messmate.bd.domain.model.MealType
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.model.Role
import com.messmate.bd.domain.model.SplitType
import com.messmate.bd.domain.model.UtilityBill
import com.messmate.bd.domain.model.UtilityCategory
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Translation layer between the **raw sheet rows** (`List<List<String>>`), the
 * Room entities and the domain models.
 *
 * Everything that can go wrong with user edited data (missing cells, "৳1,200",
 * "2026/11/12") is handled here – the rest of the app only sees clean types.
 */
object SheetMappers {

    // -----------------------------------------------------------------------
    // Primitive parsing
    // -----------------------------------------------------------------------

    /**
     * Parses a number from a cell. Tolerates thousands separators and a
     * leading currency symbol because the sheet may be formatted by hand.
     */
    fun parseDouble(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val cleaned = raw
            .replace(",", "")
            .replace("৳", "")
            .replace("TK", "", ignoreCase = true)
            .trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /** Parses a cell that holds a flag (`1`/`0`, `TRUE`/`FALSE`, `Yes`/`No`). */
    fun parseFlag(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        return when (raw.trim().lowercase()) {
            "1", "true", "yes", "y", "on" -> 1
            else -> 0
        }
    }

    /** Parses an ISO date, tolerating a few common hand-written formats. */
    fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        return runCatching { LocalDate.parse(value) }.getOrNull()
            ?: runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
            ?: runCatching {
                LocalDate.parse(value, DateTimeFormatter.ofPattern("d MMM yyyy"))
            }.getOrNull()
            ?: runCatching {
                LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            }.getOrNull()
    }

    /** Parses a `HH:mm` cut-off time. */
    fun parseTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        return runCatching { LocalTime.parse(value) }.getOrNull()
            ?: runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull()
            ?: runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("hh:mm a")) }.getOrNull()
    }

    /** Parses a `yyyy-MM` month. */
    fun parseYearMonth(raw: String?): YearMonth? {
        if (raw.isNullOrBlank()) return null
        return runCatching { YearMonth.parse(raw.trim()) }.getOrNull()
            ?: runCatching {
                YearMonth.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM"))
            }.getOrNull()
    }

    // -----------------------------------------------------------------------
    // Row -> entity
    // -----------------------------------------------------------------------

    /** `Config` tab -> entity. */
    fun configToEntity(spreadsheetId: String, rows: List<List<String>>): ConfigEntity? {
        val values = rows.asSequence()
            .drop(1) // skip the header
            .mapNotNull { row ->
                val key = row.getOrNull(0)?.trim().orEmpty()
                if (key.isEmpty()) null else key to row.getOrNull(1).orEmpty()
            }
            .toMap()

        if (values.isEmpty()) return null

        return ConfigEntity(
            spreadsheetId = spreadsheetId,
            messName = values["MessName"].orEmpty(),
            monthYear = parseYearMonth(values["MonthYear"]) ?: YearMonth.now(),
            managerEmail = values["ManagerEmail"].orEmpty(),
            breakfastWeight = parseDouble(values["BreakfastWeight"]).takeIf { it > 0 } ?: 1.0,
            breakfastCutoff = values["BreakfastCutoff"]?.trim().orEmpty().ifEmpty { "07:00" },
            lunchCutoff = values["LunchCutoff"]?.trim().orEmpty().ifEmpty { "10:00" },
            dinnerCutoff = values["DinnerCutoff"]?.trim().orEmpty().ifEmpty { "17:00" },
            lockStatus = values["LockStatus"]?.trim()?.uppercase() ?: LockStatus.OPEN.sheetValue,
        )
    }

    /** `Members` tab -> entities (keeping the sheet order via `sheetRow`). */
    fun membersToEntities(spreadsheetId: String, rows: List<List<String>>): List<MemberEntity> =
        rows.asSequence()
            .drop(1) // header
            .mapIndexedNotNull { index, row ->
                val memberId = row.getOrNull(0)?.trim().orEmpty()
                if (memberId.isEmpty()) return@mapIndexedNotNull null
                MemberEntity(
                    spreadsheetId = spreadsheetId,
                    memberId = memberId,
                    name = row.getOrNull(1)?.trim().orEmpty(),
                    gmail = row.getOrNull(2)?.trim().orEmpty(),
                    role = Role.fromSheetValue(row.getOrNull(3)),
                    seatRent = parseDouble(row.getOrNull(4)),
                    activeStatus = ActiveStatus.fromSheetValue(row.getOrNull(5)),
                    prevBalance = parseDouble(row.getOrNull(6)),
                    sheetRow = index + 2, // row 1 is the header
                )
            }
            .toList()

    /** `Meals` tab -> entities. */
    fun mealsToEntities(spreadsheetId: String, rows: List<List<String>>): List<MealEntity> =
        rows.asSequence()
            .drop(1) // header
            .mapNotNull { row ->
                val date = parseDate(row.getOrNull(0)) ?: return@mapNotNull null
                val memberId = row.getOrNull(1)?.trim().orEmpty()
                if (memberId.isEmpty()) return@mapNotNull null
                MealEntity(
                    spreadsheetId = spreadsheetId,
                    date = date.toString(),
                    memberId = memberId,
                    breakfast = parseFlag(row.getOrNull(2)),
                    lunch = parseFlag(row.getOrNull(3)),
                    dinner = parseFlag(row.getOrNull(4)),
                    guestMeals = parseDouble(row.getOrNull(5)).toInt(),
                    updatedBy = row.getOrNull(6)?.trim().orEmpty(),
                    updatedAt = row.getOrNull(7)?.trim().orEmpty(),
                )
            }
            .toList()

    /** `Bazar` tab -> entities. */
    fun bazarToEntities(spreadsheetId: String, rows: List<List<String>>): List<BazarEntity> =
        rows.asSequence()
            .drop(1)
            .mapIndexedNotNull { index, row ->
                val entryId = row.getOrNull(0)?.trim().orEmpty()
                if (entryId.isEmpty()) return@mapIndexedNotNull null
                BazarEntity(
                    spreadsheetId = spreadsheetId,
                    entryId = entryId,
                    date = parseDate(row.getOrNull(1))?.toString().orEmpty(),
                    shopperName = row.getOrNull(2)?.trim().orEmpty(),
                    items = row.getOrNull(3)?.trim().orEmpty(),
                    amount = parseDouble(row.getOrNull(4)),
                    loggedBy = row.getOrNull(5)?.trim().orEmpty(),
                    timestamp = row.getOrNull(6)?.trim().orEmpty(),
                    sheetRow = index + 2,
                )
            }
            .toList()

    /** `UtilityBills` tab -> entities. */
    fun utilitiesToEntities(spreadsheetId: String, rows: List<List<String>>): List<UtilityBillEntity> =
        rows.asSequence()
            .drop(1)
            .mapIndexedNotNull { index, row ->
                val billId = row.getOrNull(0)?.trim().orEmpty()
                if (billId.isEmpty()) return@mapIndexedNotNull null
                val splitType = SplitType.fromSheetValue(row.getOrNull(4))
                UtilityBillEntity(
                    spreadsheetId = spreadsheetId,
                    billId = billId,
                    date = parseDate(row.getOrNull(1))?.toString().orEmpty(),
                    category = UtilityCategory.fromSheetValue(row.getOrNull(2)),
                    amount = parseDouble(row.getOrNull(3)),
                    splitType = splitType,
                    targetMemberId = row.getOrNull(5)?.trim()?.takeIf { it.isNotEmpty() },
                    notes = row.getOrNull(6)?.trim().orEmpty(),
                    sheetRow = index + 2,
                )
            }
            .toList()

    /** `Deposits` tab -> entities. */
    fun depositsToEntities(spreadsheetId: String, rows: List<List<String>>): List<DepositEntity> =
        rows.asSequence()
            .drop(1)
            .mapIndexedNotNull { index, row ->
                val depositId = row.getOrNull(0)?.trim().orEmpty()
                if (depositId.isEmpty()) return@mapIndexedNotNull null
                DepositEntity(
                    spreadsheetId = spreadsheetId,
                    depositId = depositId,
                    date = parseDate(row.getOrNull(1))?.toString().orEmpty(),
                    memberId = row.getOrNull(2)?.trim().orEmpty(),
                    amount = parseDouble(row.getOrNull(3)),
                    method = DepositMethod.fromSheetValue(row.getOrNull(4)),
                    trxId = row.getOrNull(5)?.trim().orEmpty(),
                    status = DepositStatus.fromSheetValue(row.getOrNull(6)),
                    sheetRow = index + 2,
                )
            }
            .toList()

    /** `AuditLog` tab -> entities. */
    fun auditToEntities(spreadsheetId: String, rows: List<List<String>>): List<AuditRecordEntity> =
        rows.asSequence()
            .drop(1)
            .mapIndexedNotNull { index, row ->
                val logId = row.getOrNull(0)?.trim().orEmpty()
                if (logId.isEmpty()) return@mapIndexedNotNull null
                AuditRecordEntity(
                    spreadsheetId = spreadsheetId,
                    logId = logId,
                    timestamp = row.getOrNull(1)?.trim().orEmpty(),
                    actorEmail = row.getOrNull(2)?.trim().orEmpty(),
                    actionType = ActionType.fromSheetValue(row.getOrNull(3))
                        ?: ActionType.MEMBER_UPDATED,
                    details = row.getOrNull(4)?.trim().orEmpty(),
                    sheetRow = index + 2,
                )
            }
            .toList()

    // -----------------------------------------------------------------------
    // Entity -> domain
    // -----------------------------------------------------------------------

    fun MemberEntity.toDomain(): Member = Member(
        memberId = memberId,
        name = name,
        gmail = gmail,
        role = role,
        seatRent = seatRent,
        activeStatus = activeStatus,
        prevBalance = prevBalance,
        sheetRow = sheetRow,
    )

    fun MealEntity.toDomain(): MealEntry = MealEntry(
        date = LocalDate.parse(date),
        memberId = memberId,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        guestMeals = guestMeals,
        updatedBy = updatedBy,
        updatedAt = updatedAt,
    )

    fun BazarEntity.toDomain(): BazarEntry = BazarEntry(
        entryId = entryId,
        date = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
        shopperName = shopperName,
        items = items,
        amount = amount,
        loggedBy = loggedBy,
        timestamp = timestamp,
    )

    fun UtilityBillEntity.toDomain(): UtilityBill = UtilityBill(
        billId = billId,
        date = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
        category = category,
        amount = amount,
        splitType = splitType,
        targetMemberId = targetMemberId,
        notes = notes,
    )

    fun DepositEntity.toDomain(): Deposit = Deposit(
        depositId = depositId,
        date = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
        memberId = memberId,
        amount = amount,
        method = method,
        trxId = trxId,
        status = status,
    )

    fun AuditRecordEntity.toDomain(): AuditRecord = AuditRecord(
        logId = logId,
        timestamp = timestamp,
        actorEmail = actorEmail,
        actionType = actionType,
        details = details,
    )

    fun ConfigEntity.toDomain(): MessConfig = MessConfig(
        messName = messName,
        monthYear = monthYear,
        managerEmail = managerEmail,
        breakfastWeight = breakfastWeight,
        breakfastCutoff = parseTime(breakfastCutoff) ?: LocalTime.of(7, 0),
        lunchCutoff = parseTime(lunchCutoff) ?: LocalTime.of(10, 0),
        dinnerCutoff = parseTime(dinnerCutoff) ?: LocalTime.of(17, 0),
        lockStatus = LockStatus.fromSheetValue(lockStatus),
    )

    // -----------------------------------------------------------------------
    // Domain -> sheet row
    // -----------------------------------------------------------------------

    /** The `Meals` tab column of a meal type. */
    fun mealColumn(type: MealType): Int = when (type) {
        MealType.BREAKFAST -> 2
        MealType.LUNCH -> 3
        MealType.DINNER -> 4
    }
}
