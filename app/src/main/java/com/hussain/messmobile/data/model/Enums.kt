package com.hussain.messmobile.data.model

import kotlinx.serialization.Serializable

/** The role a member has inside the mess. */
@Serializable
enum class MemberRole {
    /** Can manage everything, including members and the mess settings. */
    Admin,

    /** Cooks regularly, shown next to the meals they prepared. */
    Cook,

    /** A regular member of the mess. */
    Member,
    ;

    val label: String
        get() = when (this) {
            Admin -> "Admin"
            Cook -> "Cook"
            Member -> "Member"
        }
}

/** The meal of the day. */
@Serializable
enum class MealType {
    Breakfast,
    Lunch,
    Dinner,
    ;

    val label: String
        get() = when (this) {
            Breakfast -> "Breakfast"
            Lunch -> "Lunch"
            Dinner -> "Dinner"
        }
}

/** The category of a shared expense. */
@Serializable
enum class ExpenseCategory {
    Grocery,
    Utility,
    Maintenance,
    Other,
    ;

    val label: String
        get() = when (this) {
            Grocery -> "Grocery"
            Utility -> "Utility"
            Maintenance -> "Maintenance"
            Other -> "Other"
        }
}
