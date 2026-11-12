package com.messmate.bd.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.messmate.bd.data.local.dao.AuditLogDao
import com.messmate.bd.data.local.dao.BazarDao
import com.messmate.bd.data.local.dao.DepositDao
import com.messmate.bd.data.local.dao.MealDao
import com.messmate.bd.data.local.dao.MemberDao
import com.messmate.bd.data.local.dao.MessDao
import com.messmate.bd.data.local.dao.PendingOperationDao
import com.messmate.bd.data.local.dao.UtilityBillDao
import com.messmate.bd.data.local.entity.AuditRecordEntity
import com.messmate.bd.data.local.entity.BazarEntity
import com.messmate.bd.data.local.entity.ConfigEntity
import com.messmate.bd.data.local.entity.DepositEntity
import com.messmate.bd.data.local.entity.MealEntity
import com.messmate.bd.data.local.entity.MemberEntity
import com.messmate.bd.data.local.entity.MessEntity
import com.messmate.bd.data.local.entity.PendingOperationEntity
import com.messmate.bd.data.local.entity.UtilityBillEntity

/**
 * The local cache of the mess data.
 *
 * Room is the **single source of truth for the UI** – the app renders from the
 * database, never directly from the network. That is what makes the start-up
 * instant and the app usable without a connection.
 */
@Database(
    entities = [
        MessEntity::class,
        ConfigEntity::class,
        MemberEntity::class,
        MealEntity::class,
        BazarEntity::class,
        UtilityBillEntity::class,
        DepositEntity::class,
        AuditRecordEntity::class,
        PendingOperationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MessDatabase : RoomDatabase() {

    abstract fun messDao(): MessDao
    abstract fun memberDao(): MemberDao
    abstract fun mealDao(): MealDao
    abstract fun bazarDao(): BazarDao
    abstract fun utilityBillDao(): UtilityBillDao
    abstract fun depositDao(): DepositDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        const val NAME = "messmate.db"
    }
}
