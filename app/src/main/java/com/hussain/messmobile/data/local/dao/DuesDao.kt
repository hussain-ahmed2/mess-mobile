package com.hussain.messmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hussain.messmobile.data.local.entity.DuesEntity
import com.hussain.messmobile.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DuesDao {

    @Query("SELECT * FROM dues WHERE period = :period ORDER BY memberId ASC")
    fun observeForPeriod(period: String): Flow<List<DuesEntity>>

    @Query("SELECT * FROM dues")
    suspend fun getAll(): List<DuesEntity>

    @Query("SELECT * FROM dues WHERE memberId = :memberId AND period = :period")
    suspend fun find(memberId: Long, period: String): DuesEntity?

    @Insert
    suspend fun insert(dues: DuesEntity): Long

    @Update
    suspend fun update(dues: DuesEntity)

    @Query("DELETE FROM dues WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments")
    suspend fun getAll(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE period = :period ORDER BY paidAt DESC")
    fun observeForPeriod(period: String): Flow<List<PaymentEntity>>

    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Delete
    suspend fun delete(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM dues")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<DuesEntity>)


    @Query("DELETE FROM payments")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<PaymentEntity>)

}
