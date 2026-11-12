package com.messmate.bd.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.messmate.bd.data.local.entity.ConfigEntity
import com.messmate.bd.data.local.entity.MemberEntity
import com.messmate.bd.data.local.entity.MessEntity
import kotlinx.coroutines.flow.Flow

/** Access to the `mess` and `config` tables. */
@Dao
interface MessDao {

    // -----------------------------------------------------------------------
    // Mess
    // -----------------------------------------------------------------------

    @Query("SELECT * FROM mess WHERE isActive = 1 LIMIT 1")
    fun observeActiveMess(): Flow<MessEntity?>

    @Query("SELECT * FROM mess WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveMess(): MessEntity?

    @Query("SELECT * FROM mess WHERE spreadsheetId = :spreadsheetId")
    suspend fun getMess(spreadsheetId: String): MessEntity?

    @Upsert
    suspend fun upsertMess(mess: MessEntity)

    /** Marks every mess as inactive – used when the user leaves a mess. */
    @Query("UPDATE mess SET isActive = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM mess WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteMess(spreadsheetId: String)

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    @Query("SELECT * FROM config WHERE spreadsheetId = :spreadsheetId")
    fun observeConfig(spreadsheetId: String): Flow<ConfigEntity?>

    @Query("SELECT * FROM config WHERE spreadsheetId = :spreadsheetId")
    suspend fun getConfig(spreadsheetId: String): ConfigEntity?

    @Upsert
    suspend fun upsertConfig(config: ConfigEntity)

    @Query("DELETE FROM config WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteConfig(spreadsheetId: String)
}

/** Access to the `members` table. */
@Dao
interface MemberDao {

    @Query("SELECT * FROM members WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    fun observeMembers(spreadsheetId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    suspend fun getMembers(spreadsheetId: String): List<MemberEntity>

    @Query(
        "SELECT * FROM members WHERE spreadsheetId = :spreadsheetId AND memberId = :memberId LIMIT 1",
    )
    suspend fun getMember(spreadsheetId: String, memberId: String): MemberEntity?

    @Upsert
    suspend fun upsertMembers(members: List<MemberEntity>)

    @Query("DELETE FROM members WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteAll(spreadsheetId: String)

    @Query(
        "UPDATE members SET seatRent = :seatRent " +
            "WHERE spreadsheetId = :spreadsheetId AND memberId = :memberId",
    )
    suspend fun updateSeatRent(spreadsheetId: String, memberId: String, seatRent: Double)

    @Query(
        "UPDATE members SET activeStatus = :activeStatus " +
            "WHERE spreadsheetId = :spreadsheetId AND memberId = :memberId",
    )
    suspend fun updateActiveStatus(
        spreadsheetId: String,
        memberId: String,
        activeStatus: String,
    )

    @Query("SELECT memberId FROM members WHERE spreadsheetId = :spreadsheetId ORDER BY sheetRow ASC")
    suspend fun memberIds(spreadsheetId: String): List<String>

    @Query("SELECT COUNT(*) FROM members WHERE spreadsheetId = :spreadsheetId")
    suspend fun memberCount(spreadsheetId: String): Int
}
