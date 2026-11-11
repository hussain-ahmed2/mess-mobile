package com.hussain.messmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnouncementDao {

    @Query("SELECT * FROM announcements ORDER BY pinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements")
    suspend fun getAll(): List<AnnouncementEntity>

    @Insert
    suspend fun insert(announcement: AnnouncementEntity): Long

    @Update
    suspend fun update(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM announcements")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<AnnouncementEntity>)

}
