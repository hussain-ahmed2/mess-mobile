package com.messmate.bd.domain.calculation

import com.messmate.bd.domain.model.MealType
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.model.Role
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Cut-off rules for the daily meal toggles.
 *
 * Bangladeshi messes close the "meal register" some hours before each meal so
 * that the cook knows the exact quantity. This class encodes that rule:
 *
 * | Actor     | Today before cut-off | Today after cut-off | Yesterday | Tomorrow |
 * |-----------|-----------------------|---------------------|-----------|----------|
 * | Member    | ✅ change allowed     | ❌ locked            | ❌ locked  | ✅ allowed|
 * | Manager   | ✅ change allowed     | ✅ override allowed  | ✅ override| ✅ allowed|
 *
 * When the whole month is closed (`Config!LockStatus = LOCKED`) nobody may
 * change anything any more.
 */
class CutoffPolicy(private val clock: Clock = Clock.systemDefaultZone()) {

    /** The current wall clock time – injectable for tests. */
    fun now(): LocalDateTime = LocalDateTime.now(clock)

    /** Today's date in the device's time zone. */
    fun today(): LocalDate = LocalDate.now(clock)

    /**
     * `true` when the cut-off time of [mealType] has already passed for [date].
     *
     * Only *today* can be locked by a cut-off; past days are always "closed"
     * (see [canMemberEdit]) and future days are always open.
     */
    fun isCutoffPassed(config: MessConfig, mealType: MealType, date: LocalDate): Boolean {
        val today = today()
        if (date.isBefore(today)) return true
        if (date.isAfter(today)) return false
        return now().toLocalTime() >= config.cutoffFor(mealType)
    }

    /** `true` when the cut-off of [mealType] has not passed yet for today. */
    fun isOpenForToday(config: MessConfig, mealType: MealType): Boolean =
        !isCutoffPassed(config, mealType, today())

    /**
     * Whether a **member** may change their own meal selection.
     *
     * @param date the day the meal belongs to.
     */
    fun canMemberEdit(config: MessConfig, mealType: MealType, date: LocalDate): Boolean {
        if (config.isLocked) return false
        val today = today()
        return when {
            date.isAfter(today) -> true      // pre-declaring tomorrow is fine
            date.isBefore(today) -> false    // history is immutable for members
            else -> now().toLocalTime() < config.cutoffFor(mealType)
        }
    }

    /**
     * Whether the **manager** may override a (locked) meal.
     *
     * The manager can still edit after the cut-off, but never on a closed month.
     */
    fun canManagerEdit(config: MessConfig, date: LocalDate): Boolean {
        if (config.isLocked) return false
        val today = today()
        // The manager may fix the past (typos, guest corrections) but not the future.
        return !date.isAfter(today)
    }

    /** Convenience overload combining the actor role with the rules above. */
    fun canEdit(config: MessConfig, role: Role, mealType: MealType, date: LocalDate): Boolean =
        if (role == Role.MANAGER) canManagerEdit(config, date)
        else canMemberEdit(config, mealType, date)

    /**
     * Time left until the cut-off of [mealType] (for the live countdown card).
     *
     * @return `null` when the cut-off is not applicable (different day / passed).
     */
    fun remainingUntilCutoff(config: MessConfig, mealType: MealType, date: LocalDate): Duration? {
        if (date != today()) return null
        val cutoff = config.cutoffFor(mealType)
        val now = now()
        val cutoffDateTime = LocalDateTime.of(date, cutoff)
        if (!now.isBefore(cutoffDateTime)) return null
        return Duration.between(now, cutoffDateTime)
    }

    /**
     * Next cut-off that is still ahead – used for the "next deadline" hint on
     * the dashboard.
     */
    fun nextCutoff(config: MessConfig): Pair<MealType, LocalTime>? {
        val current = now().toLocalTime()
        val today = today()
        val remainingToday = MealType.entries
            .map { it to config.cutoffFor(it) }
            .filter { it.second > current }
            .minByOrNull { it.second }
        if (remainingToday != null) return remainingToday

        // Everything for today has passed -> the next deadline is tomorrow's breakfast.
        return MealType.BREAKFAST to config.breakfastCutoff
    }

    /** `true` when [date] belongs to the month the sheet was created for. */
    fun isInsideMonth(config: MessConfig, date: LocalDate): Boolean =
        date.year == config.monthYear.year && date.month == config.monthYear.month

    /** Convenience for the UI: the cut-off time of a meal as a `LocalTime`. */
    fun cutoffTime(config: MessConfig, mealType: MealType): LocalTime = config.cutoffFor(mealType)
}
