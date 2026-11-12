package com.messmate.bd.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.messmate.bd.domain.model.ActiveStatus
import com.messmate.bd.domain.model.Role
import java.time.YearMonth

/**
 * The mess this device is part of.
 *
 * There is exactly one **active** row at a time (the one the user is currently
 * looking at); closed months stay in the table as history.
 */
@Entity(tableName = "mess")
data class MessEntity(
    @PrimaryKey val spreadsheetId: String,
    val name: String,
    val spreadsheetUrl: String,
    val role: Role,
    val managerEmail: String,
    val monthYear: YearMonth,
    /** The `MemberID` of the signed-in user – `null` for a not-yet-joined manager. */
    val myMemberId: String?,
    val schemaVersion: Int,
    val lastSyncedAt: Long,
    val isActive: Boolean,
)

/**
 * The `Config` tab of a mess (one row per sheet).
 */
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val spreadsheetId: String,
    val messName: String,
    val monthYear: YearMonth,
    val managerEmail: String,
    val breakfastWeight: Double,
    val breakfastCutoff: String,
    val lunchCutoff: String,
    val dinnerCutoff: String,
    val lockStatus: String,
)

/**
 * A member of the mess (one row of the `Members` tab).
 *
 * [sheetRow] is the 1-based row of the member inside the sheet – it lets the
 * repository update a single cell (seat rent, status) instead of rewriting the
 * whole tab.
 */
@Entity(
    tableName = "members",
    primaryKeys = ["spreadsheetId", "memberId"],
    indices = [Index("spreadsheetId")],
)
data class MemberEntity(
    val spreadsheetId: String,
    val memberId: String,
    val name: String,
    val gmail: String,
    val role: Role,
    val seatRent: Double,
    val activeStatus: ActiveStatus,
    val prevBalance: Double,
    val sheetRow: Int,
)

/**
 * One row of the `Meals` tab – a single (member, day) pair.
 */
@Entity(
    tableName = "meals",
    primaryKeys = ["spreadsheetId", "date", "memberId"],
    indices = [Index(value = ["spreadsheetId", "memberId"]), Index(value = ["spreadsheetId", "date"])],
)
data class MealEntity(
    val spreadsheetId: String,
    /** ISO date (`2026-11-12`) – sorts and compares lexicographically. */
    val date: String,
    val memberId: String,
    val breakfast: Int,
    val lunch: Int,
    val dinner: Int,
    val guestMeals: Int,
    val updatedBy: String,
    val updatedAt: String,
)
