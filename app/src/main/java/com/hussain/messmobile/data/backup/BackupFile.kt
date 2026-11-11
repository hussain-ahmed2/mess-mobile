package com.hussain.messmobile.data.backup

import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import com.hussain.messmobile.data.local.entity.DuesEntity
import com.hussain.messmobile.data.local.entity.ExpenseEntity
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.data.local.entity.MealEntryEntity
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.local.entity.PaymentEntity
import com.hussain.messmobile.data.local.entity.ShoppingItemEntity
import kotlinx.serialization.Serializable

/**
 * The content of a backup file.
 *
 * The file is plain JSON so that it can be read on any device, inspected by the user and
 * restored after the app was uninstalled.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    val members: List<MemberEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val mealEntries: List<MealEntryEntity> = emptyList(),
    val dues: List<DuesEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val shoppingItems: List<ShoppingItemEntity> = emptyList(),
    val announcements: List<AnnouncementEntity> = emptyList(),
) {
    /** How many records the backup holds, per table. */
    val recordCount: Int
        get() = members.size + meals.size + mealEntries.size + dues.size +
            payments.size + expenses.size + shoppingItems.size + announcements.size

    companion object {
        /** Bumped whenever the format changes in an incompatible way. */
        const val CURRENT_FORMAT_VERSION = 1
    }
}
