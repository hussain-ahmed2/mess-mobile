package com.messmate.bd.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * The **outbox** – a durable queue of writes that still have to reach the
 * Google Sheet.
 *
 * Every mutation is written to Room *and* queued here. The sync worker drains
 * the queue whenever the device is online, so a member can log a bazar trip in
 * a basement without signal and it still ends up in the shared sheet.
 */
@Entity(
    tableName = "pending_operations",
    indices = [Index(value = ["spreadsheetId", "status"])],
)
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val spreadsheetId: String,
    /** Discriminator of [OutboxPayload] – handy for filtering & debugging. */
    val type: String,
    /** JSON encoded [OutboxPayload]. */
    val payload: String,
    val createdAt: Long,
    val status: String,
    val attempts: Int,
    val lastError: String?,
)

/** Status of a queued operation. */
enum class OutboxStatus {
    PENDING,
    SYNCING,
    FAILED,
}

/**
 * The different kinds of writes the app can push to the sheet.
 *
 * The hierarchy is `kotlinx.serialization` polymorphic – the JSON in the
 * outbox can be decoded back into the exact subtype.
 */
@Serializable
sealed interface OutboxPayload {

    /** Toggle breakfast / lunch / dinner of a member for a day. */
    @Serializable
    data class SetMeal(
        val memberId: String,
        val date: String,
        val mealType: String,
        val enabled: Boolean,
    ) : OutboxPayload

    /** Set the number of guest meals. */
    @Serializable
    data class SetGuestMeals(
        val memberId: String,
        val date: String,
        val count: Int,
    ) : OutboxPayload

    /** Append a market trip. */
    @Serializable
    data class AppendBazar(
        val entryId: String,
        val date: String,
        val shopperName: String,
        val items: String,
        val amount: Double,
        val loggedBy: String,
        val timestamp: String,
    ) : OutboxPayload

    /** Append a utility bill. */
    @Serializable
    data class AppendUtility(
        val billId: String,
        val date: String,
        val category: String,
        val amount: Double,
        val splitType: String,
        val targetMemberId: String?,
        val notes: String,
    ) : OutboxPayload

    /** Append a deposit. */
    @Serializable
    data class AppendDeposit(
        val depositId: String,
        val date: String,
        val memberId: String,
        val amount: Double,
        val method: String,
        val trxId: String,
        val status: String,
    ) : OutboxPayload

    /** Verify / reject a deposit (manager only). */
    @Serializable
    data class SetDepositStatus(
        val depositId: String,
        val status: String,
    ) : OutboxPayload

    /** Change the seat rent of a member. */
    @Serializable
    data class SetSeatRent(
        val memberId: String,
        val seatRent: Double,
    ) : OutboxPayload

    /** Activate / deactivate a member. */
    @Serializable
    data class SetMemberActive(
        val memberId: String,
        val active: Boolean,
    ) : OutboxPayload

    /** Append a brand new member (Members + meal grid + summary row). */
    @Serializable
    data class AppendMember(
        val memberId: String,
        val name: String,
        val gmail: String,
        val role: String,
        val seatRent: Double,
        val prevBalance: Double,
    ) : OutboxPayload

    /** Update a single `Config` value (cut-offs, breakfast weight, lock state). */
    @Serializable
    data class SetConfigValue(
        val key: String,
        val value: String,
    ) : OutboxPayload
}
