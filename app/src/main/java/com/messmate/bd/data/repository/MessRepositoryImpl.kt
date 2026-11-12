package com.messmate.bd.data.repository

import androidx.room.withTransaction
import com.messmate.bd.core.network.NetworkMonitor
import com.messmate.bd.data.local.MessDatabase
import com.messmate.bd.data.local.dao.AuditLogDao
import com.messmate.bd.data.local.dao.BazarDao
import com.messmate.bd.data.local.dao.DepositDao
import com.messmate.bd.data.local.dao.MealDao
import com.messmate.bd.data.local.dao.MemberDao
import com.messmate.bd.data.local.dao.MessDao
import com.messmate.bd.data.local.dao.PendingOperationDao
import com.messmate.bd.data.local.dao.UtilityBillDao
import com.messmate.bd.data.local.entity.AuditRecordEntity
import com.messmate.bd.data.local.entity.BazarEntity
import com.messmate.bd.data.local.entity.ConfigEntity
import com.messmate.bd.data.local.entity.DepositEntity
import com.messmate.bd.data.local.entity.MealEntity
import com.messmate.bd.data.local.entity.MemberEntity
import com.messmate.bd.data.local.entity.MessEntity
import com.messmate.bd.data.local.entity.OutboxPayload
import com.messmate.bd.data.local.entity.OutboxStatus
import com.messmate.bd.data.local.entity.PendingOperationEntity
import com.messmate.bd.data.local.entity.UtilityBillEntity
import com.messmate.bd.data.mapper.SheetMappers
import com.messmate.bd.data.mapper.SheetMappers.toDomain
import com.messmate.bd.data.remote.DriveGateway
import com.messmate.bd.data.remote.MonthRolloverService
import com.messmate.bd.data.remote.SheetsGateway
import com.messmate.bd.data.remote.SpreadsheetProvisioner
import com.messmate.bd.domain.calculation.CalculationEngine
import com.messmate.bd.domain.calculation.CutoffPolicy
import com.messmate.bd.domain.model.ActionType
import com.messmate.bd.domain.model.ActiveStatus
import com.messmate.bd.domain.model.AuditRecord
import com.messmate.bd.domain.model.BazarEntry
import com.messmate.bd.domain.model.Deposit
import com.messmate.bd.domain.model.MealEntry
import com.messmate.bd.domain.model.MealGrid
import com.messmate.bd.domain.model.MealType
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.MemberIdGenerator
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.domain.model.Mess
import com.messmate.bd.domain.model.MessBlueprint
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.model.MessTotals
import com.messmate.bd.domain.model.Role
import com.messmate.bd.domain.model.SheetSchema
import com.messmate.bd.domain.model.SheetSchema.Tabs
import com.messmate.bd.domain.model.UtilityBill
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.MessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one implementation of [MessRepository].
 *
 * ### Write path
 *
 * ```
 *  command ──▶ Room (optimistic, instant UI)
 *          └─▶ outbox ──▶ sheet (immediately when online)
 *                       └─▶ WorkManager (retried when offline)
 * ```
 *
 * ### Read path
 *
 * ```
 *  UI ──▶ Room flow ──▶ (nothing else)
 *                 ▲
 *                 │ refresh()
 *           sheet (batchGet of every tab in one request)
 * ```
 */
@Singleton
class MessRepositoryImpl @Inject constructor(
    private val database: MessDatabase,
    private val messDao: MessDao,
    private val memberDao: MemberDao,
    private val mealDao: MealDao,
    private val bazarDao: BazarDao,
    private val utilityBillDao: UtilityBillDao,
    private val depositDao: DepositDao,
    private val auditLogDao: AuditLogDao,
    private val pendingOperationDao: PendingOperationDao,
    private val sheetsGateway: SheetsGateway,
    private val driveGateway: DriveGateway,
    private val provisioner: SpreadsheetProvisioner,
    private val rolloverService: MonthRolloverService,
    private val sheetWriter: SheetWriter,
    private val accountRepository: AccountRepository,
    private val networkMonitor: NetworkMonitor,
    private val cutoffPolicy: CutoffPolicy,
    private val clock: Clock,
    private val json: Json,
) : MessRepository {

    // =======================================================================
    // Observers
    // =======================================================================

    override fun observeMess(): Flow<Mess?> =
        messDao.observeActiveMess().map { it?.toDomain() }

    override fun observeConfig(): Flow<MessConfig?> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(null)
            } else {
                messDao.observeConfig(mess.spreadsheetId).map { it?.toDomain() }
            }
        }

    override fun observeMembers(): Flow<List<Member>> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(emptyList())
            } else {
                memberDao.observeMembers(mess.spreadsheetId).map { list -> list.map { it.toDomain() } }
            }
        }

    override fun observeMeals(): Flow<List<MealEntry>> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(emptyList())
            } else {
                mealDao.observeMeals(mess.spreadsheetId).map { list -> list.map { it.toDomain() } }
            }
        }

    override fun observeBazar(): Flow<List<BazarEntry>> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(emptyList())
            } else {
                bazarDao.observeBazar(mess.spreadsheetId).map { list -> list.map { it.toDomain() } }
            }
        }

    override fun observeUtilityBills(): Flow<List<UtilityBill>> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(emptyList())
            } else {
                utilityBillDao.observeBills(mess.spreadsheetId).map { list -> list.map { it.toDomain() } }
            }
        }

    override fun observeDeposits(): Flow<List<Deposit>> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(emptyList())
            } else {
                depositDao.observeDeposits(mess.spreadsheetId).map { list -> list.map { it.toDomain() } }
            }
        }

    override fun observeAuditLog(): Flow<List<AuditRecord>> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(emptyList())
            } else {
                auditLogDao.observeAuditLog(mess.spreadsheetId).map { list -> list.map { it.toDomain() } }
            }
        }

    /**
     * The monthly statements of every member.
     *
     * The numbers are computed locally with the very same rules the `Summary`
     * tab uses, so the app shows the sheet's numbers even while offline.
     */
    override fun observeStatements(): Flow<List<MemberStatement>> =
        combine(
            combine(
                observeMembers(),
                observeMeals(),
                observeUtilityBills(),
                observeDeposits(),
            ) { members, meals, bills, deposits -> StatementSources(members, meals, bills, deposits) },
            observeBazar(),
            observeConfig(),
        ) { sources, bazar, config ->
            if (config == null) {
                emptyList()
            } else {
                CalculationEngine.statements(
                    members = sources.members,
                    meals = sources.meals,
                    bills = sources.bills,
                    deposits = sources.deposits,
                    breakfastWeight = config.breakfastWeight,
                    totalBazar = CalculationEngine.totalBazar(bazar),
                )
            }
        }

    /**
     * The headline numbers of the `Summary` tab.
     *
     * They are computed locally with the same rules the sheet uses, so the
     * dashboard shows the correct numbers even while offline.
     */
    override fun observeTotals(): Flow<MessTotals> = combine(
        observeBazar(),
        observeMeals(),
        observeUtilityBills(),
        observeConfig(),
    ) { bazar, meals, bills, config ->
        if (config == null) {
            MessTotals(totalBazar = 0.0, totalMeals = 0.0, liveMealRate = 0.0, totalEqualUtilities = 0.0)
        } else {
            CalculationEngine.totals(
                bazar = bazar,
                meals = meals,
                bills = bills,
                breakfastWeight = config.breakfastWeight,
            )
        }
    }

    override fun observePendingSyncCount(): Flow<Int> =
        messDao.observeActiveMess().flatMapLatest { mess ->
            if (mess == null) {
                flowOf(0)
            } else {
                pendingOperationDao.observePendingCount(mess.spreadsheetId)
            }
        }

    // =======================================================================
    // Lifecycle
    // =======================================================================

    override suspend fun createMess(blueprint: MessBlueprint): Result<Mess> = resultOf {
        val provisioned = provisioner.provision(blueprint)

        // The new mess becomes the active one.
        messDao.deactivateAll()
        messDao.upsertMess(
            MessEntity(
                spreadsheetId = provisioned.spreadsheetId,
                name = blueprint.messName,
                spreadsheetUrl = provisioned.spreadsheetUrl,
                role = Role.MANAGER,
                managerEmail = blueprint.managerEmail,
                monthYear = blueprint.monthYear,
                myMemberId = provisioned.members.firstOrNull { it.role == Role.MANAGER }?.memberId,
                schemaVersion = SheetSchema.SCHEMA_VERSION,
                lastSyncedAt = clock.millis(),
                isActive = true,
            ),
        )

        // Pull everything the sheet contains into the cache.
        refresh().getOrThrow()

        requireMessEntity().toDomain()
    }

    override suspend fun joinMess(spreadsheetId: String): Result<Mess> = resultOf {
        // 1. The app must have access to the file (drive.file).
        val file = driveGateway.getFile(spreadsheetId)
            ?: throw IllegalStateException(
                "No access to this sheet. Ask the manager to share it with your Gmail address.",
            )

        // 2. Read the header of the sheet to find out who we are.
        val header = sheetsGateway.batchRead(
            spreadsheetId,
            listOf("${Tabs.CONFIG}!A1:B20", "${Tabs.MEMBERS}!A1:G500"),
        )
        val configEntity = SheetMappers.configToEntity(
            spreadsheetId,
            header[Tabs.CONFIG].orEmpty(),
        ) ?: throw IllegalStateException("This spreadsheet does not look like a MessMate sheet.")

        val memberEntities = SheetMappers.membersToEntities(
            spreadsheetId,
            header[Tabs.MEMBERS].orEmpty(),
        )
        val email = accountRepository.observeEmail().first().orEmpty()
        val myMemberId = memberEntities.firstOrNull { it.gmail.equals(email, ignoreCase = true) }
            ?.memberId

        // 3. Register the mess locally and sync everything.
        messDao.deactivateAll()
        messDao.upsertMess(
            MessEntity(
                spreadsheetId = spreadsheetId,
                name = configEntity.messName.ifBlank { file.name },
                spreadsheetUrl = file.webViewLink,
                role = if (myMemberId != null && memberEntities.first { it.memberId == myMemberId }.role == Role.MANAGER) {
                    Role.MANAGER
                } else {
                    Role.MEMBER
                },
                managerEmail = configEntity.managerEmail,
                monthYear = configEntity.monthYear,
                myMemberId = myMemberId,
                schemaVersion = SheetSchema.SCHEMA_VERSION,
                lastSyncedAt = clock.millis(),
                isActive = true,
            ),
        )
        messDao.upsertConfig(configEntity)
        memberDao.upsertMembers(memberEntities)

        refresh().getOrThrow()

        // 4. Not a member yet? Join on the spot – the manager can deactivate
        //    the account later if it was not approved.
        if (myMemberId == null && email.isNotBlank()) {
            addMember(
                Member(
                    memberId = MemberIdGenerator.next(memberEntities.map { it.memberId }),
                    name = email.substringBefore('@'),
                    gmail = email,
                    role = Role.MEMBER,
                    seatRent = 0.0,
                    activeStatus = ActiveStatus.ACTIVE,
                    prevBalance = 0.0,
                ),
            ).getOrThrow()
            refresh().getOrThrow()
        }

        requireMessEntity().toDomain()
    }

    /**
     * Pulls every tab of the sheet into the local cache.
     *
     * All tabs are read with **one** `values.batchGet` request – that keeps
     * the API quota low and the sync fast.
     */
    override suspend fun refresh(): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val ranges = listOf(
            "${Tabs.CONFIG}!A1:B20",
            "${Tabs.MEMBERS}!A1:G${SheetSchema.MEMBERS_MAX_ROW}",
            "${Tabs.MEALS}!A1:H${SheetSchema.MEALS_MAX_ROW}",
            "${Tabs.BAZAR}!A1:G${SheetSchema.LEDGER_MAX_ROW}",
            "${Tabs.UTILITY}!A1:G${SheetSchema.LEDGER_MAX_ROW}",
            "${Tabs.DEPOSITS}!A1:G${SheetSchema.LEDGER_MAX_ROW}",
            "${Tabs.AUDIT}!A1:E${SheetSchema.LEDGER_MAX_ROW}",
        )

        val data = sheetsGateway.batchRead(sheetId, ranges)
        val email = accountRepository.observeEmail().first().orEmpty()

        database.withTransaction {
            SheetMappers.configToEntity(sheetId, data[Tabs.CONFIG].orEmpty())
                ?.let { messDao.upsertConfig(it) }

            val members = SheetMappers.membersToEntities(sheetId, data[Tabs.MEMBERS].orEmpty())
            memberDao.upsertMembers(members)

            mealDao.upsertMeals(
                SheetMappers.mealsToEntities(sheetId, data[Tabs.MEALS].orEmpty()),
            )
            bazarDao.upsertBazar(
                SheetMappers.bazarToEntities(sheetId, data[Tabs.BAZAR].orEmpty()),
            )
            utilityBillDao.upsertBills(
                SheetMappers.utilitiesToEntities(sheetId, data[Tabs.UTILITY].orEmpty()),
            )
            depositDao.upsertDeposits(
                SheetMappers.depositsToEntities(sheetId, data[Tabs.DEPOSITS].orEmpty()),
            )
            auditLogDao.upsertAuditLog(
                SheetMappers.auditToEntities(sheetId, data[Tabs.AUDIT].orEmpty()),
            )

            // Keep the "my member id" and the sync timestamp up to date.
            messDao.getMess(sheetId)?.let { mess ->
                val myMemberId = members.firstOrNull { it.gmail.equals(email, ignoreCase = true) }
                    ?.memberId ?: mess.myMemberId
                messDao.upsertMess(mess.copy(myMemberId = myMemberId, lastSyncedAt = clock.millis()))
            }
        }
    }

    override suspend fun leaveMess(): Result<Unit> = resultOf {
        val mess = requireMessEntity()
        database.withTransaction {
            messDao.deactivateAll()
            messDao.deleteMess(mess.spreadsheetId)
            messDao.deleteConfig(mess.spreadsheetId)
            memberDao.deleteAll(mess.spreadsheetId)
            mealDao.deleteAll(mess.spreadsheetId)
            bazarDao.deleteAll(mess.spreadsheetId)
            utilityBillDao.deleteAll(mess.spreadsheetId)
            depositDao.deleteAll(mess.spreadsheetId)
            auditLogDao.deleteAll(mess.spreadsheetId)
            pendingOperationDao.deleteAll(mess.spreadsheetId)
        }
    }

    // =======================================================================
    // Meals
    // =======================================================================

    override suspend fun setMeal(
        memberId: String,
        date: LocalDate,
        mealType: MealType,
        enabled: Boolean,
    ): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val config = requireConfig()
        val role = currentRole()

        check(config.isLocked) { "This month is closed – no changes possible." }
        check(cutoffPolicy.canEdit(config, role, mealType, date)) {
            "The cut-off for ${mealType.name.lowercase()} has passed – only the manager can change it."
        }
        // Members may only change their own meals.
        check(role == Role.MANAGER || memberId == requireMessEntity().myMemberId) {
            "You can only change your own meals."
        }

        val stamp = nowIso()
        val actor = currentEmail()

        // 1. Optimistic update of the cache.
        val existing = mealDao.getMeal(sheetId, date.toString(), memberId)
        if (existing == null) {
            mealDao.upsertMeals(
                listOf(
                    MealEntity(
                        spreadsheetId = sheetId,
                        date = date.toString(),
                        memberId = memberId,
                        breakfast = 0,
                        lunch = 0,
                        dinner = 0,
                        guestMeals = 0,
                        updatedBy = actor,
                        updatedAt = stamp,
                    ),
                ),
            )
        }
        val value = if (enabled) 1 else 0
        when (mealType) {
            MealType.BREAKFAST -> mealDao.updateBreakfast(sheetId, date.toString(), memberId, value, actor, stamp)
            MealType.LUNCH -> mealDao.updateLunch(sheetId, date.toString(), memberId, value, actor, stamp)
            MealType.DINNER -> mealDao.updateDinner(sheetId, date.toString(), memberId, value, actor, stamp)
        }

        // 2. Audit entry (visible immediately, appended to the sheet on flush).
        writeAuditLocally(
            sheetId = sheetId,
            action = if (role == Role.MANAGER) ActionType.MEAL_OVERRIDDEN else ActionType.MEAL_UPDATED,
            details = "$memberId · ${mealType.name.lowercase()} · ${if (enabled) "on" else "off"} · $date",
            actor = actor,
            stamp = stamp,
        )

        // 3. Queue + push.
        enqueue(
            OutboxPayload.SetMeal(
                memberId = memberId,
                date = date.toString(),
                mealType = mealType.name,
                enabled = enabled,
            ),
        )
        flushQueue()
    }

    override suspend fun setGuestMeals(
        memberId: String,
        date: LocalDate,
        count: Int,
    ): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val config = requireConfig()
        val role = currentRole()

        check(config.isLocked) { "This month is closed – no changes possible." }
        check(role == Role.MANAGER || memberId == requireMessEntity().myMemberId) {
            "You can only change your own meals."
        }

        val stamp = nowIso()
        val actor = currentEmail()

        mealDao.getMeal(sheetId, date.toString(), memberId)?.let {
            mealDao.updateGuestMeals(sheetId, date.toString(), memberId, count.coerceAtLeast(0), actor, stamp)
        } ?: mealDao.upsertMeals(
            listOf(
                MealEntity(
                    spreadsheetId = sheetId,
                    date = date.toString(),
                    memberId = memberId,
                    breakfast = 0,
                    lunch = 0,
                    dinner = 0,
                    guestMeals = count.coerceAtLeast(0),
                    updatedBy = actor,
                    updatedAt = stamp,
                ),
            ),
        )

        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.MEAL_UPDATED,
            details = "$memberId · $count guest meal(s) · $date",
            actor = actor,
            stamp = stamp,
        )

        enqueue(
            OutboxPayload.SetGuestMeals(
                memberId = memberId,
                date = date.toString(),
                count = count.coerceAtLeast(0),
            ),
        )
        flushQueue()
    }

    // =======================================================================
    // Ledgers
    // =======================================================================

    override suspend fun addBazarEntry(entry: BazarEntry): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val stamp = nowIso()
        val actor = currentEmail()

        bazarDao.upsertBazar(
            listOf(
                BazarEntity(
                    spreadsheetId = sheetId,
                    entryId = entry.entryId,
                    date = entry.date.toString(),
                    shopperName = entry.shopperName,
                    items = entry.items,
                    amount = entry.amount,
                    loggedBy = entry.loggedBy.ifBlank { actor },
                    timestamp = entry.timestamp.ifBlank { stamp },
                    sheetRow = 0, // filled in by the next sync
                ),
            ),
        )

        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.BAZAR_ADDED,
            details = "৳${entry.amount} · ${entry.items}",
            actor = actor,
            stamp = stamp,
        )

        enqueue(
            OutboxPayload.AppendBazar(
                entryId = entry.entryId,
                date = entry.date.toString(),
                shopperName = entry.shopperName,
                items = entry.items,
                amount = entry.amount,
                loggedBy = entry.loggedBy.ifBlank { actor },
                timestamp = entry.timestamp.ifBlank { stamp },
            ),
        )
        flushQueue()
    }

    override suspend fun addUtilityBill(bill: UtilityBill): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val stamp = nowIso()
        val actor = currentEmail()

        utilityBillDao.upsertBills(
            listOf(
                UtilityBillEntity(
                    spreadsheetId = sheetId,
                    billId = bill.billId,
                    date = bill.date.toString(),
                    category = bill.category,
                    amount = bill.amount,
                    splitType = bill.splitType,
                    targetMemberId = bill.targetMemberId,
                    notes = bill.notes,
                    sheetRow = 0,
                ),
            ),
        )

        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.UTILITY_ADDED,
            details = "${bill.category.sheetValue} · ৳${bill.amount} · ${bill.splitType.sheetValue}",
            actor = actor,
            stamp = stamp,
        )

        enqueue(
            OutboxPayload.AppendUtility(
                billId = bill.billId,
                date = bill.date.toString(),
                category = bill.category.sheetValue,
                amount = bill.amount,
                splitType = bill.splitType.sheetValue,
                targetMemberId = bill.targetMemberId,
                notes = bill.notes,
            ),
        )
        flushQueue()
    }

    override suspend fun addDeposit(deposit: Deposit): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val stamp = nowIso()
        val actor = currentEmail()

        depositDao.upsertDeposits(
            listOf(
                DepositEntity(
                    spreadsheetId = sheetId,
                    depositId = deposit.depositId,
                    date = deposit.date.toString(),
                    memberId = deposit.memberId,
                    amount = deposit.amount,
                    method = deposit.method,
                    trxId = deposit.trxId,
                    status = deposit.status,
                    sheetRow = 0,
                ),
            ),
        )

        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.DEPOSIT_LOGGED,
            details = "$deposit.memberId · ৳${deposit.amount} · ${deposit.method.sheetValue}",
            actor = actor,
            stamp = stamp,
        )

        enqueue(
            OutboxPayload.AppendDeposit(
                depositId = deposit.depositId,
                date = deposit.date.toString(),
                memberId = deposit.memberId,
                amount = deposit.amount,
                method = deposit.method.sheetValue,
                trxId = deposit.trxId,
                status = deposit.status.sheetValue,
            ),
        )
        flushQueue()
    }

    override suspend fun resolveDeposit(depositId: String, approve: Boolean): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val role = currentRole()
        check(role == Role.MANAGER) { "Only the manager can verify deposits." }

        val status = if (approve) "VERIFIED" else "REJECTED"
        depositDao.updateStatus(sheetId, depositId, status)

        val stamp = nowIso()
        val actor = currentEmail()
        writeAuditLocally(
            sheetId = sheetId,
            action = if (approve) ActionType.DEPOSIT_VERIFIED else ActionType.DEPOSIT_REJECTED,
            details = "$depositId · $status",
            actor = actor,
            stamp = stamp,
        )

        enqueue(OutboxPayload.SetDepositStatus(depositId = depositId, status = status))
        flushQueue()
    }

    // =======================================================================
    // Members
    // =======================================================================

    override suspend fun addMember(member: Member): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        val config = requireConfig()
        check(!config.isLocked) { "This month is closed – no new members possible." }

        val existing = memberDao.getMembers(sheetId)
        val memberId = member.memberId.ifBlank {
            MemberIdGenerator.next(existing.map { it.memberId })
        }
        val sheetRow = existing.size + 2 // the row the member will get when appended

        // 1. Member row.
        memberDao.upsertMembers(
            listOf(
                MemberEntity(
                    spreadsheetId = sheetId,
                    memberId = memberId,
                    name = member.name,
                    gmail = member.gmail,
                    role = member.role,
                    seatRent = member.seatRent,
                    activeStatus = member.activeStatus,
                    prevBalance = member.prevBalance,
                    sheetRow = sheetRow,
                ),
            ),
        )

        // 2. The empty meal grid of the new member (so the dashboard can show it).
        val days = config.monthYear.lengthOfMonth()
        val gridEntities = MealGrid.rowsFor(listOf(memberId), config.monthYear).map { gridRow ->
            MealEntity(
                spreadsheetId = sheetId,
                date = gridRow.date.toString(),
                memberId = memberId,
                breakfast = 0,
                lunch = 0,
                dinner = 0,
                guestMeals = 0,
                updatedBy = "",
                updatedAt = "",
            )
        }
        mealDao.upsertMeals(gridEntities)

        val stamp = nowIso()
        val actor = currentEmail()
        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.MEMBER_ADDED,
            details = "$memberId · ${member.name} · ${member.gmail}",
            actor = actor,
            stamp = stamp,
        )

        enqueue(
            OutboxPayload.AppendMember(
                memberId = memberId,
                name = member.name,
                gmail = member.gmail,
                role = member.role.sheetValue,
                seatRent = member.seatRent,
                prevBalance = member.prevBalance,
            ),
        )
        flushQueue()
    }

    override suspend fun updateSeatRent(memberId: String, seatRent: Double): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        check(currentRole() == Role.MANAGER) { "Only the manager can change the seat rent." }
        check(!requireConfig().isLocked) { "This month is closed." }

        memberDao.updateSeatRent(sheetId, memberId, seatRent)

        val stamp = nowIso()
        val actor = currentEmail()
        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.MEMBER_UPDATED,
            details = "$memberId · seat rent ৳$seatRent",
            actor = actor,
            stamp = stamp,
        )

        enqueue(OutboxPayload.SetSeatRent(memberId = memberId, seatRent = seatRent))
        flushQueue()
    }

    override suspend fun updateMemberStatus(memberId: String, active: Boolean): Result<Unit> =
        resultOf {
            val sheetId = requireSheetId()
            check(currentRole() == Role.MANAGER) { "Only the manager can activate or deactivate members." }
            check(!requireConfig().isLocked) { "This month is closed." }

            memberDao.updateActiveStatus(
                sheetId,
                memberId,
                if (active) ActiveStatus.ACTIVE.sheetValue else ActiveStatus.INACTIVE.sheetValue,
            )

            val stamp = nowIso()
            val actor = currentEmail()
            writeAuditLocally(
                sheetId = sheetId,
                action = ActionType.MEMBER_UPDATED,
                details = "$memberId · ${if (active) "activated" else "deactivated"}",
                actor = actor,
                stamp = stamp,
            )

            enqueue(OutboxPayload.SetMemberActive(memberId = memberId, active = active))
            flushQueue()
        }

    // =======================================================================
    // Configuration
    // =======================================================================

    override suspend fun updateCutoffs(
        breakfast: LocalTime,
        lunch: LocalTime,
        dinner: LocalTime,
    ): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        check(currentRole() == Role.MANAGER) { "Only the manager can change the cut-off times." }
        check(!requireConfig().isLocked) { "This month is closed." }

        val stamp = nowIso()
        val actor = currentEmail()

        enqueue(
            OutboxPayload.SetConfigValue(
                SheetSchema.Config.KEY_BREAKFAST_CUTOFF,
                breakfast.format(DateTimeFormatter.ofPattern("HH:mm")),
            ),
        )
        enqueue(
            OutboxPayload.SetConfigValue(
                SheetSchema.Config.KEY_LUNCH_CUTOFF,
                lunch.format(DateTimeFormatter.ofPattern("HH:mm")),
            ),
        )
        enqueue(
            OutboxPayload.SetConfigValue(
                SheetSchema.Config.KEY_DINNER_CUTOFF,
                dinner.format(DateTimeFormatter.ofPattern("HH:mm")),
            ),
        )

        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.CONFIG_UPDATED,
            details = "Cut-offs: $breakfast / $lunch / $dinner",
            actor = actor,
            stamp = stamp,
        )

        flushQueue()
    }

    override suspend fun updateBreakfastWeight(weight: Double): Result<Unit> = resultOf {
        val sheetId = requireSheetId()
        check(currentRole() == Role.MANAGER) { "Only the manager can change the breakfast weight." }
        check(!requireConfig().isLocked) { "This month is closed." }
        check(weight == 0.5 || weight == 1.0) { "The breakfast weight must be 0.5 or 1.0." }

        val stamp = nowIso()
        val actor = currentEmail()

        enqueue(
            OutboxPayload.SetConfigValue(
                SheetSchema.Config.KEY_BREAKFAST_WEIGHT,
                weight.toString(),
            ),
        )

        writeAuditLocally(
            sheetId = sheetId,
            action = ActionType.CONFIG_UPDATED,
            details = "Breakfast weight: $weight",
            actor = actor,
            stamp = stamp,
        )

        flushQueue()
    }

    // =======================================================================
    // Month end
    // =======================================================================

    override suspend fun closeMonthAndRollover(): Result<Mess> = resultOf {
        val sheetId = requireSheetId()
        val config = requireConfig()
        val members = memberDao.getMembers(sheetId).map { it.toDomain() }
        check(currentRole() == Role.MANAGER) { "Only the manager can close the month." }
        check(!config.isLocked) { "This month is already closed." }

        // Closing balances come from the local calculation (identical to the sheet).
        val statements = calculateStatements()
        val closingBalances = statements.associate { it.member.memberId to it.netBalance }

        val provisioned = rolloverService.rollover(
            spreadsheetId = sheetId,
            config = config,
            members = members,
            closingBalances = closingBalances,
        )

        // Switch the device over to the new sheet.
        messDao.deactivateAll()
        messDao.upsertMess(
            MessEntity(
                spreadsheetId = provisioned.spreadsheetId,
                name = config.messName,
                spreadsheetUrl = provisioned.spreadsheetUrl,
                role = Role.MANAGER,
                managerEmail = config.managerEmail,
                monthYear = config.monthYear.plusMonths(1),
                myMemberId = provisioned.members.firstOrNull { it.role == Role.MANAGER }?.memberId,
                schemaVersion = SheetSchema.SCHEMA_VERSION,
                lastSyncedAt = clock.millis(),
                isActive = true,
            ),
        )

        // Wipe the cache of the new sheet (it is empty anyway) and sync it.
        database.withTransaction {
            memberDao.deleteAll(provisioned.spreadsheetId)
            mealDao.deleteAll(provisioned.spreadsheetId)
            bazarDao.deleteAll(provisioned.spreadsheetId)
            utilityBillDao.deleteAll(provisioned.spreadsheetId)
            depositDao.deleteAll(provisioned.spreadsheetId)
            auditLogDao.deleteAll(provisioned.spreadsheetId)
        }

        refresh().getOrThrow()
        requireMessEntity().toDomain()
    }

    override suspend fun currentMonth(): YearMonth? = requireConfig().monthYear

    // =======================================================================
    // Outbox
    // =======================================================================

    /** Puts an operation into the durable queue. */
    private suspend fun enqueue(payload: OutboxPayload) {
        val sheetId = activeSheetId() ?: return
        pendingOperationDao.upsert(
            PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                spreadsheetId = sheetId,
                type = payload::class.simpleName ?: "Operation",
                payload = json.encodeToString(payload),
                createdAt = clock.millis(),
                status = OutboxStatus.PENDING.name,
                attempts = 0,
                lastError = null,
            ),
        )
    }

    /**
     * Pushes every queued operation to the sheet.
     *
     * The order of the queue is preserved (some operations depend on their
     * predecessor, e.g. "add member" before "set seat rent"). When one
     * operation fails the rest waits – the sync worker tries again later.
     */
    suspend fun flushQueue(): Result<Unit> = resultOf {
        val sheetId = activeSheetId() ?: return@resultOf

        // No connection → keep the queue and wait for the sync worker.
        if (!networkMonitor.isOnline()) return@resultOf

        val operations = pendingOperationDao.getPending(sheetId)
        if (operations.isEmpty()) return@resultOf

        val actor = currentEmail()

        operations.forEach { operation ->
            val payload = runCatching {
                json.decodeFromString<OutboxPayload>(operation.payload)
            }.getOrNull()

            if (payload == null) {
                // Broken payload – drop it instead of blocking the queue forever.
                pendingOperationDao.delete(operation.id)
                return@forEach
            }

            try {
                sheetWriter.apply(sheetId, payload)
                sheetWriter.appendAudit(
                    spreadsheetId = sheetId,
                    actorEmail = actor,
                    actionType = auditActionFor(payload),
                    details = auditDetailsFor(payload),
                )
                pendingOperationDao.delete(operation.id)
            } catch (error: Exception) {
                val attempts = operation.attempts + 1
                if (attempts >= MAX_ATTEMPTS) {
                    // Give up on this operation, keep the rest of the queue alive.
                    pendingOperationDao.delete(operation.id)
                } else {
                    pendingOperationDao.updateStatus(
                        id = operation.id,
                        status = OutboxStatus.FAILED.name,
                        attempts = attempts,
                        error = error.message,
                    )
                }
                return@resultOf // stop here – preserve the order
            }
        }
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    private suspend fun requireSheetId(): String = requireMessId()

    private suspend fun requireMessId(): String =
        activeSheetId() ?: throw IllegalStateException("No mess joined yet.")

    private suspend fun activeSheetId(): String? = messDao.getActiveMess()?.spreadsheetId

    private suspend fun requireMessEntity(): MessEntity =
        messDao.getActiveMess() ?: throw IllegalStateException("No mess joined yet.")

    private suspend fun requireConfig(): MessConfig =
        messDao.getActiveMess()
            ?.let { messDao.getConfig(it.spreadsheetId) }
            ?.toDomain()
            ?: throw IllegalStateException("Configuration not loaded yet.")

    private suspend fun currentRole(): Role = requireMessEntity().role

    private suspend fun currentEmail(): String = accountRepository.observeEmail().first().orEmpty()

    /** Computes the statements locally (same maths as the `Summary` tab). */
    private suspend fun calculateStatements(): List<MemberStatement> {
        val sheetId = requireSheetId()
        val config = requireConfig()
        val members = memberDao.getMembers(sheetId).map { it.toDomain() }
        val meals = mealDao.getMeals(sheetId).map { it.toDomain() }
        val bills = utilityBillDao.getBills(sheetId).map { it.toDomain() }
        val deposits = depositDao.getDeposits(sheetId).map { it.toDomain() }
        val bazar = bazarDao.getBazar(sheetId).map { it.toDomain() }

        return CalculationEngine.statements(
            members = members,
            meals = meals,
            bills = bills,
            deposits = deposits,
            breakfastWeight = config.breakfastWeight,
            totalBazar = CalculationEngine.totalBazar(bazar),
        )
    }

    /** Writes an audit entry into the local cache (it is pushed on flush). */
    private suspend fun writeAuditLocally(
        sheetId: String,
        action: ActionType,
        details: String,
        actor: String,
        stamp: String,
    ) {
        val nextRow = (auditLogDao.getAuditLog(sheetId).maxOfOrNull { it.sheetRow } ?: 1) + 1
        auditLogDao.upsertAuditLog(
            listOf(
                AuditRecordEntity(
                    spreadsheetId = sheetId,
                    logId = UUID.randomUUID().toString(),
                    timestamp = stamp,
                    actorEmail = actor,
                    actionType = action,
                    details = details,
                    sheetRow = nextRow,
                ),
            ),
        )
    }

    private fun auditActionFor(payload: OutboxPayload): ActionType = when (payload) {
        is OutboxPayload.SetMeal -> ActionType.MEAL_UPDATED
        is OutboxPayload.SetGuestMeals -> ActionType.MEAL_UPDATED
        is OutboxPayload.AppendBazar -> ActionType.BAZAR_ADDED
        is OutboxPayload.AppendUtility -> ActionType.UTILITY_ADDED
        is OutboxPayload.AppendDeposit -> ActionType.DEPOSIT_LOGGED
        is OutboxPayload.SetDepositStatus -> ActionType.DEPOSIT_VERIFIED
        is OutboxPayload.SetSeatRent -> ActionType.MEMBER_UPDATED
        is OutboxPayload.SetMemberActive -> ActionType.MEMBER_UPDATED
        is OutboxPayload.AppendMember -> ActionType.MEMBER_ADDED
        is OutboxPayload.SetConfigValue -> ActionType.CONFIG_UPDATED
    }

    private fun auditDetailsFor(payload: OutboxPayload): String = when (payload) {
        is OutboxPayload.SetMeal ->
            "${payload.memberId} · ${payload.mealType.lowercase()} · ${if (payload.enabled) "on" else "off"} · ${payload.date}"

        is OutboxPayload.SetGuestMeals -> "${payload.memberId} · ${payload.count} guest meal(s) · ${payload.date}"
        is OutboxPayload.AppendBazar -> "৳${payload.amount} · ${payload.items}"
        is OutboxPayload.AppendUtility -> "${payload.category} · ৳${payload.amount} · ${payload.splitType}"
        is OutboxPayload.AppendDeposit -> "${payload.memberId} · ৳${payload.amount} · ${payload.method}"
        is OutboxPayload.SetDepositStatus -> "${payload.depositId} · ${payload.status}"
        is OutboxPayload.SetSeatRent -> "${payload.memberId} · seat rent ৳${payload.seatRent}"
        is OutboxPayload.SetMemberActive -> "${payload.memberId} · ${if (payload.active) "activated" else "deactivated"}"
        is OutboxPayload.AppendMember -> "${payload.memberId} · ${payload.name} · ${payload.gmail}"
        is OutboxPayload.SetConfigValue -> "${payload.key} = ${payload.value}"
    }

    private fun nowIso(): String =
        OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    /**
     * Runs [block] and wraps the outcome in a [Result].
     *
     * Unlike `runCatching` a `CancellationException` is rethrown – cancelling a
     * coroutine must never be swallowed.
     */
    private suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }

    // -----------------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------------

    private fun MessEntity.toDomain(): Mess = Mess(
        spreadsheetId = spreadsheetId,
        spreadsheetUrl = spreadsheetUrl,
        name = name,
        role = role,
        managerEmail = managerEmail,
        monthYear = monthYear,
        myMemberId = myMemberId,
    )

    private companion object {
        /** How often a failing operation is retried before it is dropped. */
        const val MAX_ATTEMPTS = 5
    }
}

/**
 * The four data sources the statement calculation needs – bundled so the
 * `combine` chain stays type safe.
 */
private data class StatementSources(
    val members: List<Member>,
    val meals: List<MealEntry>,
    val bills: List<UtilityBill>,
    val deposits: List<Deposit>,
)
