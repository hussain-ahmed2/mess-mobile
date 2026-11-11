package com.hussain.messmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.data.local.entity.MealEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Query(
        "SELECT * FROM meals WHERE date = :date " +
            "ORDER BY CASE type WHEN 'Breakfast' THEN 0 WHEN 'Lunch' THEN 1 WHEN 'Dinner' THEN 2 ELSE 3 END",
    )
    fun observeForDate(date: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals")
    suspend fun getAll(): List<MealEntity>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun findById(id: Long): MealEntity?

    @Insert
    suspend fun insert(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM meals")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<MealEntity>)
}

@Dao
interface MealEntryDao {

    @Query("SELECT * FROM meal_entries WHERE mealId = :mealId ORDER BY memberId ASC")
    fun observeForMeal(mealId: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries")
    suspend fun getAll(): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entries WHERE mealId = :mealId")
    suspend fun getForMeal(mealId: Long): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entries WHERE mealId IN (SELECT id FROM meals WHERE date = :date)")
    fun observeForDate(date: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE mealId = :mealId AND memberId = :memberId")
    suspend fun find(mealId: Long, memberId: Long): MealEntryEntity?

    @Insert
    suspend fun insert(entry: MealEntryEntity): Long

    @Update
    suspend fun update(entry: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE mealId = :mealId AND memberId = :memberId")
    suspend fun delete(mealId: Long, memberId: Long)





    @Query("DELETE FROM meal_entries")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<MealEntryEntity>)
}
