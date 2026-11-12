package com.messmate.bd.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.messmate.bd.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

/** Access to the `meals` table. */
@Dao
interface MealDao {

    @Query("SELECT * FROM meals WHERE spreadsheetId = :spreadsheetId")
    fun observeMeals(spreadsheetId: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE spreadsheetId = :spreadsheetId")
    suspend fun getMeals(spreadsheetId: String): List<MealEntity>

    @Query(
        "SELECT * FROM meals WHERE spreadsheetId = :spreadsheetId " +
            "AND date = :date AND memberId = :memberId LIMIT 1",
    )
    suspend fun getMeal(spreadsheetId: String, date: String, memberId: String): MealEntity?

    @Upsert
    suspend fun upsertMeals(meals: List<MealEntity>)

    @Query("DELETE FROM meals WHERE spreadsheetId = :spreadsheetId")
    suspend fun deleteAll(spreadsheetId: String)

    @Query(
        "UPDATE meals SET breakfast = :value, updatedBy = :updatedBy, updatedAt = :updatedAt " +
            "WHERE spreadsheetId = :spreadsheetId AND date = :date AND memberId = :memberId",
    )
    suspend fun updateBreakfast(
        spreadsheetId: String,
        date: String,
        memberId: String,
        value: Int,
        updatedBy: String,
        updatedAt: String,
    )

    @Query(
        "UPDATE meals SET lunch = :value, updatedBy = :updatedBy, updatedAt = :updatedAt " +
            "WHERE spreadsheetId = :spreadsheetId AND date = :date AND memberId = :memberId",
    )
    suspend fun updateLunch(
        spreadsheetId: String,
        date: String,
        memberId: String,
        value: Int,
        updatedBy: String,
        updatedAt: String,
    )

    @Query(
        "UPDATE meals SET dinner = :value, updatedBy = :updatedBy, updatedAt = :updatedAt " +
            "WHERE spreadsheetId = :spreadsheetId AND date = :date AND memberId = :memberId",
    )
    suspend fun updateDinner(
        spreadsheetId: String,
        date: String,
        memberId: String,
        value: Int,
        updatedBy: String,
        updatedAt: String,
    )

    @Query(
        "UPDATE meals SET guestMeals = :value, updatedBy = :updatedBy, updatedAt = :updatedAt " +
            "WHERE spreadsheetId = :spreadsheetId AND date = :date AND memberId = :memberId",
    )
    suspend fun updateGuestMeals(
        spreadsheetId: String,
        date: String,
        memberId: String,
        value: Int,
        updatedBy: String,
        updatedAt: String,
    )
}
