package com.messmate.bd.domain.model

/**
 * The contract of the auto-generated Google Spreadsheet.
 *
 * Every tab name, header, column order and row index used by the app is
 * declared **once** here. The provisioner uses it to create the sheet, the
 * repository uses it to target single cells, and the calculation engine uses it
 * to mirror the sheet formulas locally.
 *
 * ⚠️ Changing anything in this file changes the on-disk format of every mess
 * sheet in the wild – bump [SCHEMA_VERSION] and write a migration when you do.
 */
object SheetSchema {

    /** Version of the sheet layout. Stored in the local cache for migrations. */
    const val SCHEMA_VERSION = 1

    // -----------------------------------------------------------------------
    // Tab names
    // -----------------------------------------------------------------------

    object Tabs {
        const val CONFIG = "Config"
        const val MEMBERS = "Members"
        const val MEALS = "Meals"
        const val BAZAR = "Bazar"
        const val UTILITY = "UtilityBills"
        const val DEPOSITS = "Deposits"
        const val AUDIT = "AuditLog"
        const val SUMMARY = "Summary"

        /** Creation order – also the order of the tabs in the sheet UI. */
        val ALL = listOf(CONFIG, MEMBERS, MEALS, BAZAR, UTILITY, DEPOSITS, AUDIT, SUMMARY)
    }

    // -----------------------------------------------------------------------
    // Column headers (index 0 == column A)
    // -----------------------------------------------------------------------

    object Headers {
        val CONFIG = listOf("Key", "Value")

        val MEMBERS = listOf("MemberID", "Name", "Gmail", "Role", "SeatRent", "ActiveStatus", "PrevBalance")

        val MEALS = listOf("Date", "MemberID", "Breakfast", "Lunch", "Dinner", "GuestMeals", "UpdatedBy", "UpdatedAt")

        val BAZAR = listOf("EntryID", "Date", "ShopperName", "Items", "Amount", "LoggedBy", "Timestamp")

        val UTILITY = listOf("BillID", "Date", "Category", "Amount", "SplitType", "TargetMemberID", "Notes")

        val DEPOSITS = listOf("DepositID", "Date", "MemberID", "Amount", "Method", "TrxID", "Status")

        val AUDIT = listOf("LogID", "Timestamp", "ActorEmail", "ActionType", "Details")

        val SUMMARY = listOf(
            "MemberID", "Name", "TotalMeals", "MealCost", "SeatRent",
            "EqualUtility", "CustomUtility", "TotalCost", "PrevBalance",
            "TotalDeposit", "NetBalance",
        )

        fun of(tab: String): List<String> = when (tab) {
            Tabs.CONFIG -> CONFIG
            Tabs.MEMBERS -> MEMBERS
            Tabs.MEALS -> MEALS
            Tabs.BAZAR -> BAZAR
            Tabs.UTILITY -> UTILITY
            Tabs.DEPOSITS -> DEPOSITS
            Tabs.AUDIT -> AUDIT
            Tabs.SUMMARY -> SUMMARY
            else -> emptyList()
        }
    }

    // -----------------------------------------------------------------------
    // Zero based column indexes per tab
    // -----------------------------------------------------------------------

    object Cols {
        object CONFIG {
            const val KEY = 0
            const val VALUE = 1
        }

        object MEMBERS {
            const val MEMBER_ID = 0
            const val NAME = 1
            const val GMAIL = 2
            const val ROLE = 3
            const val SEAT_RENT = 4
            const val ACTIVE_STATUS = 5
            const val PREV_BALANCE = 6
        }

        object MEALS {
            const val DATE = 0
            const val MEMBER_ID = 1
            const val BREAKFAST = 2
            const val LUNCH = 3
            const val DINNER = 4
            const val GUEST = 5
            const val UPDATED_BY = 6
            const val UPDATED_AT = 7
        }

        object BAZAR {
            const val ENTRY_ID = 0
            const val DATE = 1
            const val SHOPPER = 2
            const val ITEMS = 3
            const val AMOUNT = 4
            const val LOGGED_BY = 5
            const val TIMESTAMP = 6
        }

        object UTILITY {
            const val BILL_ID = 0
            const val DATE = 1
            const val CATEGORY = 2
            const val AMOUNT = 3
            const val SPLIT_TYPE = 4
            const val TARGET_MEMBER_ID = 5
            const val NOTES = 6
        }

        object DEPOSITS {
            const val DEPOSIT_ID = 0
            const val DATE = 1
            const val MEMBER_ID = 2
            const val AMOUNT = 3
            const val METHOD = 4
            const val TRX_ID = 5
            const val STATUS = 6
        }

        object AUDIT {
            const val LOG_ID = 0
            const val TIMESTAMP = 1
            const val ACTOR_EMAIL = 2
            const val ACTION_TYPE = 3
            const val DETAILS = 4
        }

        object SUMMARY {
            const val MEMBER_ID = 0
            const val NAME = 1
            const val TOTAL_MEALS = 2
            const val MEAL_COST = 3
            const val SEAT_RENT = 4
            const val EQUAL_UTILITY = 5
            const val CUSTOM_UTILITY = 6
            const val TOTAL_COST = 7
            const val PREV_BALANCE = 8
            const val TOTAL_DEPOSIT = 9
            const val NET_BALANCE = 10
        }
    }

    // -----------------------------------------------------------------------
    // Config tab – key/value pairs
    // -----------------------------------------------------------------------

    object Config {
        const val KEY_MESS_NAME = "MessName"
        const val KEY_MONTH_YEAR = "MonthYear"
        const val KEY_MANAGER_EMAIL = "ManagerEmail"
        const val KEY_BREAKFAST_WEIGHT = "BreakfastWeight"
        const val KEY_BREAKFAST_CUTOFF = "BreakfastCutoff"
        const val KEY_LUNCH_CUTOFF = "LunchCutoff"
        const val KEY_DINNER_CUTOFF = "DinnerCutoff"
        const val KEY_LOCK_STATUS = "LockStatus"

        /** All keys in their canonical (sheet) order. */
        val KEYS = listOf(
            KEY_MESS_NAME,
            KEY_MONTH_YEAR,
            KEY_MANAGER_EMAIL,
            KEY_BREAKFAST_WEIGHT,
            KEY_BREAKFAST_CUTOFF,
            KEY_LUNCH_CUTOFF,
            KEY_DINNER_CUTOFF,
            KEY_LOCK_STATUS,
        )

        /**
         * 1-based sheet row of every config key.
         *
         * Row 1 holds the `Key | Value` header, therefore the first key lives on
         * row 2. These constants are referenced by the `Summary` formulas, so
         * they must never change silently.
         */
        const val ROW_MESS_NAME = 2
        const val ROW_MONTH_YEAR = 3
        const val ROW_MANAGER_EMAIL = 4
        const val ROW_BREAKFAST_WEIGHT = 5
        const val ROW_BREAKFAST_CUTOFF = 6
        const val ROW_LUNCH_CUTOFF = 7
        const val ROW_DINNER_CUTOFF = 8
        const val ROW_LOCK_STATUS = 9

        fun rowOf(key: String): Int = when (key) {
            KEY_MESS_NAME -> ROW_MESS_NAME
            KEY_MONTH_YEAR -> ROW_MONTH_YEAR
            KEY_MANAGER_EMAIL -> ROW_MANAGER_EMAIL
            KEY_BREAKFAST_WEIGHT -> ROW_BREAKFAST_WEIGHT
            KEY_BREAKFAST_CUTOFF -> ROW_BREAKFAST_CUTOFF
            KEY_LUNCH_CUTOFF -> ROW_LUNCH_CUTOFF
            KEY_DINNER_CUTOFF -> ROW_DINNER_CUTOFF
            KEY_LOCK_STATUS -> ROW_LOCK_STATUS
            else -> -1
        }
    }

    // -----------------------------------------------------------------------
    // Row bounds used by the SUMIFS formulas (keeps them cheap & bounded)
    // -----------------------------------------------------------------------

    /** Max row (1-based) the `Meals` tab may grow to – ~129 members × 31 days. */
    const val MEALS_MAX_ROW = 4000

    /** Max row (1-based) for the append-only ledgers (Bazar/Utility/Deposits/Audit). */
    const val LEDGER_MAX_ROW = 5000

    /** Max row (1-based) for the `Members` tab. */
    const val MEMBERS_MAX_ROW = 500

    /** Max row (1-based) for the `Summary` tab. */
    const val SUMMARY_MAX_ROW = 500

    // -----------------------------------------------------------------------
    // Summary layout
    // -----------------------------------------------------------------------

    object Summary {
        /** Row 1 – the "metric" block (label in A/C/E/G, formula in B/D/F/H). */
        const val ROW_METRICS = 1

        /** Row 2 – mess name / month echo. */
        const val ROW_CONTEXT = 2

        /** Row 3 – the column headers. */
        const val ROW_HEADERS = 3

        /** Row 4 – first member row. */
        const val ROW_FIRST_MEMBER = 4

        // Metric block cells
        const val CELL_TOTAL_BAZAR = "\$B\$1"
        const val CELL_TOTAL_MEALS = "\$D\$1"
        const val CELL_MEAL_RATE = "\$F\$1"
        const val CELL_TOTAL_EQUAL_UTILITY = "\$H\$1"
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Converts a zero based column index into its spreadsheet letter (0 -> A). */
    fun columnLetter(index: Int): String {
        require(index >= 0) { "column index must be >= 0" }
        var value = index
        val sb = StringBuilder()
        while (value >= 0) {
            sb.insert(0, ('A' + (value % 26)))
            value = value / 26 - 1
        }
        return sb.toString()
    }

    /**
     * Letter of the last column of a tab (e.g. `Members` → `G`).
     *
     * Needed for `values.append` which uses the range to determine the width
     * of the appended row.
     */
    fun lastColumnLetter(tab: String): String =
        columnLetter((Headers.of(tab).size - 1).coerceAtLeast(0))

    /**
     * Row of a member inside the `Members` tab.
     *
     * Row 1 is the header, the first member sits on row 2. Members are only ever
     * **appended**, never reordered, so this mapping is stable for every device
     * that reads the same sheet.
     */
    fun memberRow(index: Int): Int = index + 2

    /** Inverse of [memberRow] – returns the zero based member index. */
    fun memberIndexOfRow(row: Int): Int = row - 2

    /**
     * Row of a member inside the `Summary` tab.
     *
     * The `Summary` tab uses the same ordering as the `Members` tab, so the
     * mapping is identical.
     */
    fun summaryRow(index: Int): Int = Summary.ROW_FIRST_MEMBER + index
}
