package com.messmate.bd.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Domain models of MessMate BD.
 *
 * These types are the contract between the UI layer, the local Room cache and
 * the Google Sheet. Every field maps 1:1 to a cell of the auto-generated
 * spreadsheet so that "what you see in the app" == "what you see in the sheet".
 */

/** A participant of the mess. */
data class Member(
    val memberId: String,
    val name: String,
    val gmail: String,
    val role: Role,
    val seatRent: Double,
    val activeStatus: ActiveStatus,
    /** Balance carried over from the previous month (positive = manager owes). */
    val prevBalance: Double,
    /**
     * 1-based row of this member inside the `Members` tab.
     *
     * The row is stored so seat-rent / status edits can target a single cell
     * instead of scanning the whole column.
     */
    val sheetRow: Int = 0,
) {
    val isActive: Boolean get() = activeStatus == ActiveStatus.ACTIVE
    val isManager: Boolean get() = role == Role.MANAGER
}

/** Daily meal record for one member (one row of the `Meals` tab). */
data class MealEntry(
    val date: LocalDate,
    val memberId: String,
    /** 0 = off, 1 = on. */
    val breakfast: Int,
    val lunch: Int,
    val dinner: Int,
    /** Number of guest meals hosted by / charged to this member. */
    val guestMeals: Int,
    val updatedBy: String,
    val updatedAt: String,
) {
    fun valueOf(type: MealType): Int = when (type) {
        MealType.BREAKFAST -> breakfast
        MealType.LUNCH -> lunch
        MealType.DINNER -> dinner
    }
}

/** A market trip (bazar). */
data class BazarEntry(
    val entryId: String,
    val date: LocalDate,
    val shopperName: String,
    val items: String,
    val amount: Double,
    val loggedBy: String,
    val timestamp: String,
)

/** A fixed / utility cost line. */
data class UtilityBill(
    val billId: String,
    val date: LocalDate,
    val category: UtilityCategory,
    val amount: Double,
    val splitType: SplitType,
    /** Required when [splitType] is `CUSTOM_MEMBER`. */
    val targetMemberId: String?,
    val notes: String,
)

/** A deposit (cash / bKash / Nagad / Rocket / bank). */
data class Deposit(
    val depositId: String,
    val date: LocalDate,
    val memberId: String,
    val amount: Double,
    val method: DepositMethod,
    val trxId: String,
    val status: DepositStatus,
)

/** Immutable audit trail entry. */
data class AuditRecord(
    val logId: String,
    val timestamp: String,
    val actorEmail: String,
    val actionType: ActionType,
    val details: String,
)

/**
 * The `Config` tab – the knobs of the mess.
 *
 * @param breakfastWeight how many meals one breakfast counts as (0.5 or 1.0).
 * @param cutoffs the daily cut-off times after which members can no longer
 *        change their own meal selection.
 */
data class MessConfig(
    val messName: String,
    val monthYear: YearMonth,
    val managerEmail: String,
    val breakfastWeight: Double,
    val breakfastCutoff: LocalTime,
    val lunchCutoff: LocalTime,
    val dinnerCutoff: LocalTime,
    val lockStatus: LockStatus,
) {
    val isLocked: Boolean get() = lockStatus == LockStatus.LOCKED

    fun cutoffFor(type: MealType): LocalTime = when (type) {
        MealType.BREAKFAST -> breakfastCutoff
        MealType.LUNCH -> lunchCutoff
        MealType.DINNER -> dinnerCutoff
    }

    /** Weight applied to a meal type when counting "meals consumed". */
    fun weightOf(type: MealType): Double = when (type) {
        MealType.BREAKFAST -> breakfastWeight
        MealType.LUNCH -> 1.0
        MealType.DINNER -> 1.0
    }
}

/** Top-of-sheet aggregates exposed by the formula driven `Summary` tab. */
data class MessTotals(
    val totalBazar: Double,
    val totalMeals: Double,
    val liveMealRate: Double,
    val totalEqualUtilities: Double,
)

/**
 * Per-member monthly statement – the exact same numbers the `Summary` tab
 * computes with `SUMIFS`.
 */
data class MemberStatement(
    val member: Member,
    val totalMeals: Double,
    val mealCost: Double,
    val seatRent: Double,
    val equalUtility: Double,
    val customUtility: Double,
    val totalCost: Double,
    val prevBalance: Double,
    val totalDeposit: Double,
    val netBalance: Double,
) {
    /** Positive => the manager owes the member. Negative => the member owes. */
    val isCredit: Boolean get() = netBalance > 0.0
    val isSettled: Boolean get() = kotlin.math.abs(netBalance) < 0.01
}

/** A mess the current device participates in. */
data class Mess(
    val spreadsheetId: String,
    val spreadsheetUrl: String,
    val name: String,
    val role: Role,
    val managerEmail: String,
    val monthYear: YearMonth,
    val myMemberId: String?,
) {
    val isManager: Boolean get() = role == Role.MANAGER
}

/** Everything the UI needs for the dashboard in one immutable snapshot. */
data class MessSnapshot(
    val mess: Mess,
    val config: MessConfig,
    val members: List<Member>,
    val totals: MessTotals,
    val statements: List<MemberStatement>,
    val pendingSyncCount: Int,
    val isLocked: Boolean,
)

/** Result of a login / join attempt. */
sealed interface JoinResult {
    data class Success(val mess: Mess) : JoinResult
    data class Failure(val message: String) : JoinResult
}
