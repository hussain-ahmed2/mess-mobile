package com.hussain.messmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hussain.messmobile.data.local.entity.ExpenseEntity
import com.hussain.messmobile.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<ExpenseEntity>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<ExpenseEntity>)
}

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items ORDER BY purchased ASC, createdAt DESC")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items")
    suspend fun getAll(): List<ShoppingItemEntity>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE purchased = 0")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shopping_items WHERE purchased = 1")
    suspend fun deletePurchased()





    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<ShoppingItemEntity>)
}
