package com.hussain.messmobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hussain.messmobile.data.local.dao.AnnouncementDao
import com.hussain.messmobile.data.local.dao.DuesDao
import com.hussain.messmobile.data.local.dao.ExpenseDao
import com.hussain.messmobile.data.local.dao.MealDao
import com.hussain.messmobile.data.local.dao.MealEntryDao
import com.hussain.messmobile.data.local.dao.MemberDao
import com.hussain.messmobile.data.local.dao.PaymentDao
import com.hussain.messmobile.data.local.dao.ShoppingDao
import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import com.hussain.messmobile.data.local.entity.DuesEntity
import com.hussain.messmobile.data.local.entity.ExpenseEntity
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.data.local.entity.MealEntryEntity
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.local.entity.PaymentEntity
import com.hussain.messmobile.data.local.entity.ShoppingItemEntity

/** The local database of the app, everything lives on the device. */
@Database(
    entities = [
        MemberEntity::class,
        MealEntity::class,
        MealEntryEntity::class,
        DuesEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        ShoppingItemEntity::class,
        AnnouncementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MessDatabase : RoomDatabase() {

    abstract fun memberDao(): MemberDao

    abstract fun mealDao(): MealDao

    abstract fun mealEntryDao(): MealEntryDao

    abstract fun duesDao(): DuesDao

    abstract fun paymentDao(): PaymentDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun announcementDao(): AnnouncementDao

    companion object {
        const val NAME = "mess.db"

        fun build(context: Context): MessDatabase =
            Room.databaseBuilder(context, MessDatabase::class.java, NAME)
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
