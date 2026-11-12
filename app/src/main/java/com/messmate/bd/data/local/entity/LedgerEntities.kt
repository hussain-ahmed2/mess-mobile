package com.messmate.bd.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.messmate.bd.domain.model.ActionType
import com.messmate.bd.domain.model.DepositMethod
import com.messmate.bd.domain.model.DepositStatus
import com.messmate.bd.domain.model.SplitType
import com.messmate.bd.domain.model.UtilityCategory

/**
 * One row of the append-only `Bazar` tab.
 */
@Entity(
    tableName = "bazar",
    primaryKeys = ["spreadsheetId", "entryId"],
    indices = [Index("spreadsheetId")],
)
data class BazarEntity(
    val spreadsheetId: String,
    val entryId: String,
    val date: String,
    val shopperName: String,
    val items: String,
    val amount: Double,
    val loggedBy: String,
    val timestamp: String,
    /** 1-based row inside the `Bazar` tab (row 1 is the header). */
    val sheetRow: Int,
)

/**
 * One row of the append-only `UtilityBills` tab.
 */
@Entity(
    tableName = "utility_bills",
    primaryKeys = ["spreadsheetId", "billId"],
    indices = [Index("spreadsheetId")],
)
data class UtilityBillEntity(
    val spreadsheetId: String,
    val billId: String,
    val date: String,
    val category: UtilityCategory,
    val amount: Double,
    val splitType: SplitType,
    /** `null` for `EQUAL_ALL` bills. */
    val targetMemberId: String?,
    val notes: String,
    /** 1-based row inside the `UtilityBills` tab (row 1 is the header). */
    val sheetRow: Int,
)

/**
 * One row of the append-only `Deposits` tab.
 */
@Entity(
    tableName = "deposits",
    primaryKeys = ["spreadsheetId", "depositId"],
    indices = [Index("spreadsheetId"), Index(value = ["spreadsheetId", "memberId"])],
)
data class DepositEntity(
    val spreadsheetId: String,
    val depositId: String,
    val date: String,
    val memberId: String,
    val amount: Double,
    val method: DepositMethod,
    val trxId: String,
    val status: DepositStatus,
    /** 1-based row inside the `Deposits` tab (row 1 is the header). */
    val sheetRow: Int,
)

/**
 * One row of the immutable `AuditLog` tab.
 */
@Entity(
    tableName = "audit_log",
    primaryKeys = ["spreadsheetId", "logId"],
    indices = [Index("spreadsheetId")],
)
data class AuditRecordEntity(
    val spreadsheetId: String,
    val logId: String,
    val timestamp: String,
    val actorEmail: String,
    val actionType: ActionType,
    val details: String,
    /** 1-based row inside the `AuditLog` tab (row 1 is the header). */
    val sheetRow: Int,
)
