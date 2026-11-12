package com.messmate.bd.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.messmate.bd.data.local.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Access to the outbox – the durable queue of writes that still have to reach
 * the spreadsheet.
 */
@Dao
interface PendingOperationDao {

    @Query(
        "SELECT * FROM pending_operations WHERE spreadsheetId = :spreadsheetId " +
            "AND status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC",
    )
    suspend fun getPending(spreadsheetId: String): List<PendingOperationEntity>

    @Query(
        "SELECT COUNT(*) FROM pending_operations WHERE spreadsheetId = :spreadsheetId " +
            "AND status IN ('PENDING', 'FAILED')",
    )
    fun observePendingCount(spreadsheetId: String): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM pending_operations WHERE spreadsheetId = :spreadsheetId " +
            "AND status IN ('PENDING', 'FAILED')",
    )
    suspend fun pendingCount(spreadsheetId: String): Int

    @Upsert
    suspend fun upsert(operation: PendingOperationEntity)

    @Query("UPDATE pending_operations SET status = :status, attempts = :attempts, lastError = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, attempts: Int, error: String?)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_operations WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteAll(spreadsheetId: String)
}
