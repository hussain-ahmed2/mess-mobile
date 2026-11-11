package com.hussain.messmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hussain.messmobile.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Query("SELECT * FROM members ORDER BY active DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE active = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<MemberEntity>>

    @Query("SELECT COUNT(*) FROM members WHERE active = 1")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM members")
    suspend fun getAll(): List<MemberEntity>

    @Insert
    suspend fun insert(member: MemberEntity): Long

    @Update
    suspend fun update(member: MemberEntity)

    @Delete
    suspend fun delete(member: MemberEntity)

    @Query("DELETE FROM members")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<MemberEntity>)

}
