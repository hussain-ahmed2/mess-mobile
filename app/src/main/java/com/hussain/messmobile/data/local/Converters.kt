package com.hussain.messmobile.data.local

import androidx.room.TypeConverter
import com.hussain.messmobile.data.model.ExpenseCategory
import com.hussain.messmobile.data.model.MealType
import com.hussain.messmobile.data.model.MemberRole

/** Converts the enums of the app to and from their database representation. */
class Converters {

    @TypeConverter
    fun fromMemberRole(role: MemberRole): String = role.name

    @TypeConverter
    fun toMemberRole(value: String): MemberRole =
        enumValueOrDefault(value, MemberRole.Member)

    @TypeConverter
    fun fromMealType(type: MealType): String = type.name

    @TypeConverter
    fun toMealType(value: String): MealType = enumValueOrDefault(value, MealType.Lunch)

    @TypeConverter
    fun fromExpenseCategory(category: ExpenseCategory): String = category.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory =
        enumValueOrDefault(value, ExpenseCategory.Other)

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
