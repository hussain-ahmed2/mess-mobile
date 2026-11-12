package com.messmate.bd.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.messmate.bd.data.local.entity.AuditRecordEntity
import com.messmate.bd.data.local.entity.BazarEntity
import com.messmate.bd.data.local.entity.DepositEntity
import com.messmate.bd.data.local.entity.UtilityBillEntity
import kotlinx.coroutines.flow.Flow

/** Access to the append-only `Bazar` ledger. */
@Dao
interface BazarDao {

    @Query("SELECT * FROM bazar WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    fun observeBazar(spreadsheetId: String): Flow<List<BazarEntity>>

    @Query("SELECT * FROM bazar WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    suspend fun getBazar(spreadsheetId: String): List<BazarEntity>

    @Query("SELECT * FROM bazar WHERE spreadsheetId = :spreadsheetId AND entryId = :entryId LIMIT 1")
    suspend fun getBazarEntry(spreadsheetId: String, entryId: String): BazarEntity?

    @Upsert
    suspend fun upsertBazar(entries: List<BazarEntity>)

    @Query("DELETE FROM bazar WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteAll(spreadsheetId: String)
}

/** Access to the append-only `UtilityBills` ledger. */
@Dao
interface UtilityBillDao {

    @Query("SELECT * FROM utility_bills WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    fun observeBills(spreadsheetId: String): Flow<List<UtilityBillEntity>>

    @Query("SELECT * FROM utility_bills WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    suspend fun getBills(spreadsheetId: String): List<UtilityBillEntity>

    @Query(
        "SELECT * FROM utility_bills WHERE spreadsheetId = :spreadsheetId AND billId = :billId LIMIT 1",
    )
    suspend fun getBill(spreadsheetId: String, billId: String): UtilityBillEntity?

    @Upsert
    suspend fun upsertBills(bills: List<UtilityBillEntity>)

    @Query("DELETE FROM utility_bills WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteAll(spreadsheetId: String)
}

/** Access to the append-only `Deposits` ledger. */
@Dao
interface DepositDao {

    @Query("SELECT * FROM deposits WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    fun observeDeposits(spreadsheetId: String): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    suspend fun getDeposits(spreadsheetId: String): List<DepositEntity>

    @Query(
        "SELECT * FROM deposits WHERE spreadsheetId = :spreadsheetId AND depositId = :depositId LIMIT 1",
    )
    suspend fun getDeposit(spreadsheetId: String, depositId: String): DepositEntity?

    @Upsert
    suspend fun upsertDeposits(deposits: List<DepositEntity>)

    @Query(
        "UPDATE deposits SET status = :status " +
            "WHERE spreadsheetId = :spreadsheetId AND depositId = :depositId",
    )
    suspend fun updateStatus(spreadsheetId: String, depositId: String, status: String)

    @Query("DELETE FROM deposits WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteAll(spreadsheetId: String)
}

/** Access to the immutable `AuditLog`. */
@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_log WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow DESC")
    fun observeAuditLog(spreadsheetId: String): Flow<List<AuditRecordEntity>>

    @Query("SELECT * FROM audit_log WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow DESC")
    suspend fun getAuditLog(spreadsheetId: String): List<AuditRecordEntity>

    @Upsert
    suspend fun upsertAuditLog(records: List<AuditRecordEntity>)

    @Query("DELETE FROM audit_log WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteAll(spreadsheetId: String)
}
