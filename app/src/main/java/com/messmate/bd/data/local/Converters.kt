package com.messmate.bd.data.local

import androidx.room.TypeConverter
import com.messmate.bd.domain.model.ActiveStatus
import com.messmate.bd.domain.model.ActionType
import com.messmate.bd.domain.model.DepositMethod
import com.messmate.bd.domain.model.DepositStatus
import com.messmate.bd.domain.model.Role
import com.messmate.bd.domain.model.SplitType
import com.messmate.bd.domain.model.UtilityCategory
import java.time.YearMonth

/**
 * Room type converters.
 *
 * Enums are stored with their **sheet representation** (`MANAGER`, `EQUAL_ALL`,
 * `bKash`, …). That way the local cache and the spreadsheet always speak the
 * same language and the values stay readable when someone exports the DB.
 */
class Converters {

    @TypeConverter
    fun fromRole(value: Role): String = value.sheetValue

    @TypeConverter
    fun toRole(value: String): Role = Role.fromSheetValue(value)

    @TypeConverter
    fun fromActiveStatus(value: ActiveStatus): String = value.sheetValue

    @TypeConverter
    fun toActiveStatus(value: String): ActiveStatus = ActiveStatus.fromSheetValue(value)

    @TypeConverter
    fun fromSplitType(value: SplitType): String = value.sheetValue

    @TypeConverter
    fun toSplitType(value: String): SplitType = SplitType.fromSheetValue(value)

    @TypeConverter
    fun fromUtilityCategory(value: UtilityCategory): String = value.sheetValue

    @TypeConverter
    fun toUtilityCategory(value: String): UtilityCategory = UtilityCategory.fromSheetValue(value)

    @TypeConverter
    fun fromDepositMethod(value: DepositMethod): String = value.sheetValue

    @TypeConverter
    fun toDepositMethod(value: String): DepositMethod = DepositMethod.fromSheetValue(value)

    @TypeConverter
    fun fromDepositStatus(value: DepositStatus): String = value.sheetValue

    @TypeConverter
    fun toDepositStatus(value: String): DepositStatus = DepositStatus.fromSheetValue(value)

    @TypeConverter
    fun fromActionType(value: ActionType): String = value.sheetValue

    @TypeConverter
    fun toActionType(value: String): ActionType =
        ActionType.fromSheetValue(value) ?: ActionType.MEMBER_UPDATED

    /** `2026-11` */
    @TypeConverter
    fun fromYearMonth(value: YearMonth): String = value.toString()

    @TypeConverter
    fun toYearMonth(value: String): YearMonth = YearMonth.parse(value)
}
