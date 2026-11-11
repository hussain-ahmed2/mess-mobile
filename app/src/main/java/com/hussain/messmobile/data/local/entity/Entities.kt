package com.hussain.messmobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hussain.messmobile.data.model.ExpenseCategory
import com.hussain.messmobile.data.model.MealType
import com.hussain.messmobile.data.model.MemberRole
import kotlinx.serialization.Serializable

/** Somebody who lives in the mess. */
@Entity(tableName = "members")
@Serializable
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val role: MemberRole = MemberRole.Member,
    val joinedAt: Long = 0L,
    val active: Boolean = true,
)

/** A meal that was cooked on a specific day. */
@Entity(tableName = "meals")
@Serializable
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Local midnight of the day the meal belongs to, in epoch milliseconds. */
    val date: Long,
    val type: MealType,
    val description: String = "",
    val cookId: Long? = null,
    /** What the whole meal cost, shared between everybody who ate. */
    val totalCost: Double = 0.0,
)

/**
 * One member on one meal: whether they opted in and whether they actually ate.
 *
 * The amount is what this member has to pay for this meal.
 */
@Entity(
    tableName = "meal_entries",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["mealId"]),
        Index(value = ["memberId"]),
        Index(value = ["mealId", "memberId"], unique = true),
    ],
)
@Serializable
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val memberId: Long,
    val optedIn: Boolean = true,
    val eaten: Boolean = false,
    val amount: Double = 0.0,
)

/** What a member owes for a month, for example `2026-11`. */
@Entity(
    tableName = "dues",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["memberId"]),
        Index(value = ["memberId", "period"], unique = true),
    ],
)
@Serializable
data class DuesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    /** The month the dues belong to, formatted as `yyyy-MM`. */
    val period: String,
    val amount: Double,
)

/** A payment a member made towards their dues. */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["memberId"])],
)
@Serializable
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val period: String,
    val amount: Double,
    val paidAt: Long,
    val note: String = "",
)

/** A shared expense of the mess. */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidByMemberId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["paidByMemberId"])],
)
@Serializable
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: ExpenseCategory = ExpenseCategory.Other,
    val date: Long,
    val paidByMemberId: Long? = null,
    /** Whether the amount is split between all active members. */
    val shared: Boolean = true,
)

/** An item on the shopping list. */
@Entity(tableName = "shopping_items")
@Serializable
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String = "",
    val estimatedCost: Double = 0.0,
    val purchased: Boolean = false,
    val purchasedByMemberId: Long? = null,
    val purchasedAt: Long? = null,
    val createdAt: Long = 0L,
)

/** A message for every member of the mess. */
@Entity(tableName = "announcements")
@Serializable
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String = "",
    val createdAt: Long = 0L,
    val pinned: Boolean = false,
)
