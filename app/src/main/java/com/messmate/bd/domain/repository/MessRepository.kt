package com.messmate.bd.domain.repository

import com.messmate.bd.domain.model.AuditRecord
import com.messmate.bd.domain.model.BazarEntry
import com.messmate.bd.domain.model.Deposit
import com.messmate.bd.domain.model.MealEntry
import com.messmate.bd.domain.model.MealType
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.domain.model.Mess
import com.messmate.bd.domain.model.MessBlueprint
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.model.MessTotals
import com.messmate.bd.domain.model.UtilityBill
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * The single entry point to every mess related data operation.
 *
 * ## Architecture
 *
 * ```
 *  ViewModel ──▶ MessRepository ──▶ Room (source of truth for the UI)
 *                      │                 ▲
 *                      │                 │ writes are mirrored
 *                      ▼                 │
 *              Sheets/Drive REST  ──▶ outbox ──▶ WorkManager (retry when offline)
 * ```
 *
 * * The UI **always** reads from Room, so the app starts instantly and works
 *   offline.
 * * Every command is written to Room first (optimistic) and then pushed to the
 *   sheet. When the device is offline the command is queued in the outbox and
 *   replayed by the sync worker.
 * * The Google Sheet stays the **master** of the data – every device syncs
 *   from it.
 */
interface MessRepository {

    // -----------------------------------------------------------------------
    // Observers (Room backed – they keep emitting while offline)
    // -----------------------------------------------------------------------

    /** The mess this device belongs to, or `null` when the user has not joined one. */
    fun observeMess(): Flow<Mess?>

    /** Live `Config` tab. */
    fun observeConfig(): Flow<MessConfig?>

    /** Live `Members` tab, ordered like the sheet. */
    fun observeMembers(): Flow<List<Member>>

    /** Live `Meals` tab. */
    fun observeMeals(): Flow<List<MealEntry>>

    /** Live `Bazar` tab. */
    fun observeBazar(): Flow<List<BazarEntry>>

    /** Live `UtilityBills` tab. */
    fun observeUtilityBills(): Flow<List<UtilityBill>>

    /** Live `Deposits` tab. */
    fun observeDeposits(): Flow<List<Deposit>>

    /** Live `AuditLog` tab (newest first). */
    fun observeAuditLog(): Flow<List<AuditRecord>>

    /** Statements of every member, recomputed locally from the cached tabs. */
    fun observeStatements(): Flow<List<MemberStatement>>

    /**
     * The headline numbers of the `Summary` tab – total bazar, total meals,
     * the live meal rate and the total of the equal utilities.
     */
    fun observeTotals(): Flow<MessTotals>

    /** Number of changes that still wait to be pushed to the sheet. */
    fun observePendingSyncCount(): Flow<Int>

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Creates a brand new mess: provisions the spreadsheet, writes every tab,
     * shares the file with the members and caches everything locally.
     */
    suspend fun createMess(blueprint: MessBlueprint): Result<Mess>

    /**
     * Joins an existing mess.
     *
     * The sheet has to be accessible with the `drive.file` scope – either because
     * the manager shared it with the user's Gmail address, or because the user
     * picked it through the system file picker.
     */
    suspend fun joinMess(spreadsheetId: String): Result<Mess>

    /** Pulls every tab from the sheet into the local cache. */
    suspend fun refresh(): Result<Unit>

    /** Removes the mess from this device (the sheet itself is untouched). */
    suspend fun leaveMess(): Result<Unit>

    // -----------------------------------------------------------------------
    // Meals
    // -----------------------------------------------------------------------

    /** Turns a single meal (breakfast / lunch / dinner) on or off. */
    suspend fun setMeal(
        memberId: String,
        date: LocalDate,
        mealType: MealType,
        enabled: Boolean,
    ): Result<Unit>

    /** Sets the number of guest meals of a member for a day. */
    suspend fun setGuestMeals(memberId: String, date: LocalDate, count: Int): Result<Unit>

    // -----------------------------------------------------------------------
    // Bazar / utilities / deposits
    // -----------------------------------------------------------------------

    /** Appends a market trip to the `Bazar` tab. */
    suspend fun addBazarEntry(entry: BazarEntry): Result<Unit>

    /** Appends a fixed cost to the `UtilityBills` tab. */
    suspend fun addUtilityBill(bill: UtilityBill): Result<Unit>

    /** Appends a deposit to the `Deposits` tab. */
    suspend fun addDeposit(deposit: Deposit): Result<Unit>

    /** Verifies or rejects a pending deposit (manager only). */
    suspend fun resolveDeposit(depositId: String, approve: Boolean): Result<Unit>

    // -----------------------------------------------------------------------
    // Members
    // -----------------------------------------------------------------------

    /** Appends a new member (Members + Meals grid + Summary row). */
    suspend fun addMember(member: Member): Result<Unit>

    /** Updates the seat rent of a member. */
    suspend fun updateSeatRent(memberId: String, seatRent: Double): Result<Unit>

    /** Activates / deactivates a member. */
    suspend fun updateMemberStatus(memberId: String, active: Boolean): Result<Unit>

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    /** Updates the three daily cut-off times. */
    suspend fun updateCutoffs(
        breakfast: LocalTime,
        lunch: LocalTime,
        dinner: LocalTime,
    ): Result<Unit>

    /** Updates the breakfast weight (0.5 or 1.0). */
    suspend fun updateBreakfastWeight(weight: Double): Result<Unit>

    // -----------------------------------------------------------------------
    // Month end
    // -----------------------------------------------------------------------

    /**
     * Locks the current sheet, creates the sheet for the next month, copies the
     * members and seat rents and carries the closing balances forward.
     */
    suspend fun closeMonthAndRollover(): Result<Mess>

    /** The month the current sheet belongs to. */
    suspend fun currentMonth(): YearMonth?
}
