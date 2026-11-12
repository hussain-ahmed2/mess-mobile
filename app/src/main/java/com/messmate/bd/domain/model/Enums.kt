package com.messmate.bd.domain.model

/**
 * Role of a participant inside a mess.
 *
 * The value is persisted verbatim in the `Members` sheet (`Role` column) so the
 * sheet stays human readable and can be audited by hand.
 */
enum class Role(val sheetValue: String) {
    MANAGER("MANAGER"),
    MEMBER("MEMBER");

    companion object {
        fun fromSheetValue(raw: String?): Role =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) } ?: MEMBER
    }
}

/**
 * The three daily meals of a Bangladeshi mess plus the "guest meal" counter.
 *
 * [weight] is how many *meals* a single unit of this type consumes. Breakfast
 * is configurable (`0.5` or `1.0`) because most messes serve a light breakfast.
 */
enum class MealType(val sheetColumn: String, val defaultWeight: Double) {
    BREAKFAST("Breakfast", 1.0),
    LUNCH("Lunch", 1.0),
    DINNER("Dinner", 1.0);

    companion object {
        fun fromSheetColumn(raw: String?): MealType? =
            entries.firstOrNull { it.sheetColumn.equals(raw?.trim(), ignoreCase = true) }
    }
}

/**
 * How a utility bill is distributed between members.
 *
 * `EQUAL_ALL`      -> split equally between every ACTIVE member of the month.
 * `CUSTOM_MEMBER`  -> charged 100% to one specific member (e.g. AC sub-meter).
 */
enum class SplitType(val sheetValue: String) {
    EQUAL_ALL("EQUAL_ALL"),
    CUSTOM_MEMBER("CUSTOM_MEMBER");

    companion object {
        fun fromSheetValue(raw: String?): SplitType =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) } ?: EQUAL_ALL
    }
}

/** Fixed / utility cost categories supported by the `UtilityBills` tab. */
enum class UtilityCategory(val sheetValue: String) {
    COOK("Cook"),
    ELECTRICITY("Electricity"),
    GAS("Gas"),
    WIFI("WiFi"),
    WATER("Water"),
    GARBAGE("Garbage"),
    OTHER("Other");

    companion object {
        fun fromSheetValue(raw: String?): UtilityCategory =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) } ?: OTHER
    }
}

/** Payment instrument used for a deposit. */
enum class DepositMethod(val sheetValue: String) {
    CASH("Cash"),
    BKASH("bKash"),
    NAGAD("Nagad"),
    ROCKET("Rocket"),
    BANK("Bank");

    companion object {
        fun fromSheetValue(raw: String?): DepositMethod =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) } ?: CASH
    }
}

/**
 * Lifecycle of a deposit.
 *
 * Only `VERIFIED` deposits are counted towards the monthly `NetBalance`
 * (see the `Summary` sheet formulas).
 */
enum class DepositStatus(val sheetValue: String) {
    PENDING("PENDING"),
    VERIFIED("VERIFIED"),
    REJECTED("REJECTED");

    companion object {
        fun fromSheetValue(raw: String?): DepositStatus =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) } ?: PENDING
    }
}

/** Whether a member currently occupies a seat (drives equal-split maths). */
enum class ActiveStatus(val sheetValue: String) {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    companion object {
        fun fromSheetValue(raw: String?): ActiveStatus =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) } ?: ACTIVE
    }
}

/** Lock state of a monthly sheet. `LOCKED` sheets are read-only for everyone. */
enum class LockStatus(val sheetValue: String) {
    OPEN("OPEN"),
    LOCKED("LOCKED");

    companion object {
        fun fromSheetValue(raw: String?): LockStatus =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) } ?: OPEN
    }
}

/**
 * Every mutation that is written to the immutable `AuditLog` tab.
 *
 * The value is written verbatim to the sheet so a member reading the sheet by
 * hand understands what happened.
 */
enum class ActionType(val sheetValue: String) {
    MESS_CREATED("MESS_CREATED"),
    MEMBER_ADDED("MEMBER_ADDED"),
    MEMBER_UPDATED("MEMBER_UPDATED"),
    MEAL_UPDATED("MEAL_UPDATED"),
    MEAL_OVERRIDDEN("MEAL_OVERRIDDEN"),
    BAZAR_ADDED("BAZAR_ADDED"),
    UTILITY_ADDED("UTILITY_ADDED"),
    DEPOSIT_LOGGED("DEPOSIT_LOGGED"),
    DEPOSIT_VERIFIED("DEPOSIT_VERIFIED"),
    DEPOSIT_REJECTED("DEPOSIT_REJECTED"),
    CONFIG_UPDATED("CONFIG_UPDATED"),
    MONTH_LOCKED("MONTH_LOCKED"),
    MONTH_ROLLED_OVER("MONTH_ROLLED_OVER"),
    SHEET_SHARED("SHEET_SHARED");

    companion object {
        fun fromSheetValue(raw: String?): ActionType? =
            entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) }
    }
}
