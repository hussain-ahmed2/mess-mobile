package com.messmate.bd.domain.calculation

import com.messmate.bd.domain.model.ActiveStatus
import com.messmate.bd.domain.model.BazarEntry
import com.messmate.bd.domain.model.Deposit
import com.messmate.bd.domain.model.DepositStatus
import com.messmate.bd.domain.model.MealEntry
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.domain.model.MessTotals
import com.messmate.bd.domain.model.SplitType
import com.messmate.bd.domain.model.UtilityBill
import kotlin.math.round

/**
 * Local mirror of the `Summary` sheet formulas.
 *
 * The spreadsheet is the single source of truth, but the app must render
 * numbers instantly and work offline. This engine therefore reproduces the
 * exact `SUMIFS`/`VLOOKUP` logic of the `Summary` tab:
 *
 * ```
 * TotalMeals          = SUM(Breakfast) * BreakfastWeight + SUM(Lunch) + SUM(Dinner) + SUM(GuestMeals)
 * LiveMealRate        = TotalBazar / TotalMeals
 * MealCost            = ROUND(MemberTotalMeals * LiveMealRate, 2)
 * EqualUtility        = TotalEqualUtilities / ActiveMemberCount   (0 for inactive members)
 * CustomUtility       = SUM(bills where TargetMemberID = member)
 * TotalCost           = ROUND(MealCost + SeatRent + EqualUtility + CustomUtility, 2)
 * NetBalance          = ROUND((PrevBalance + VerifiedDeposits) - TotalCost, 2)
 * ```
 *
 * All functions are pure so they can be unit tested without Android or network.
 */
object CalculationEngine {

    /** Rounds like the spreadsheet `ROUND(x, 2)`. */
    fun money(value: Double): Double = round(value * 100.0) / 100.0

    /**
     * Total number of meals consumed by [memberId].
     *
     * Guest meals count as full meals and are attributed to the member that
     * hosts them – this keeps `SUM(member meals) == total meals`, so the bazar
     * cost is distributed without any money leaking.
     */
    fun totalMealsOf(meals: List<MealEntry>, memberId: String, breakfastWeight: Double): Double {
        var total = 0.0
        meals.forEach { meal ->
            if (meal.memberId != memberId) return@forEach
            total += meal.breakfast * breakfastWeight
            total += meal.lunch
            total += meal.dinner
            total += meal.guestMeals
        }
        return total
    }

    /** Total number of meals consumed by the whole mess. */
    fun totalMeals(meals: List<MealEntry>, breakfastWeight: Double): Double {
        var total = 0.0
        meals.forEach { meal ->
            total += meal.breakfast * breakfastWeight
            total += meal.lunch
            total += meal.dinner
            total += meal.guestMeals
        }
        return total
    }

    /** Total bazar spend of the month. */
    fun totalBazar(bazar: List<BazarEntry>): Double = bazar.sumOf { it.amount }

    /** Meal rate = total bazar cost / total meals consumed. */
    fun mealRate(totalBazar: Double, totalMeals: Double): Double =
        if (totalMeals <= 0.0) 0.0 else totalBazar / totalMeals

    /** Sum of all bills that are split equally between the members. */
    fun totalEqualUtilities(bills: List<UtilityBill>): Double =
        bills.filter { it.splitType == SplitType.EQUAL_ALL }.sumOf { it.amount }

    /** Sum of all bills charged to a single member. */
    fun customUtilitiesOf(bills: List<UtilityBill>, memberId: String): Double =
        bills.filter { it.splitType == SplitType.CUSTOM_MEMBER && it.targetMemberId == memberId }
            .sumOf { it.amount }

    /** Sum of all verified deposits of a member (pending ones do not count). */
    fun verifiedDepositsOf(deposits: List<Deposit>, memberId: String): Double =
        deposits.filter { it.memberId == memberId && it.status == DepositStatus.VERIFIED }
            .sumOf { it.amount }

    /** Number of members that are currently counted for the equal split. */
    fun activeMemberCount(members: List<Member>): Int =
        members.count { it.activeStatus == ActiveStatus.ACTIVE }

    /** The equal split share of a single active member. */
    fun equalUtilityShare(bills: List<UtilityBill>, activeMemberCount: Int): Double {
        if (activeMemberCount <= 0) return 0.0
        return totalEqualUtilities(bills) / activeMemberCount
    }

    /** Top-of-sheet aggregates shown on the dashboard. */
    fun totals(
        bazar: List<BazarEntry>,
        meals: List<MealEntry>,
        bills: List<UtilityBill>,
        breakfastWeight: Double,
    ): MessTotals {
        val bazarTotal = totalBazar(bazar)
        val mealsTotal = totalMeals(meals, breakfastWeight)
        return MessTotals(
            totalBazar = money(bazarTotal),
            totalMeals = mealsTotal,
            liveMealRate = money(mealRate(bazarTotal, mealsTotal)),
            totalEqualUtilities = money(totalEqualUtilities(bills)),
        )
    }

    /**
     * Builds the full monthly statement of a single member.
     *
     * @param mealRate        the live meal rate of the mess (already computed).
     * @param activeMemberCount number of members taking part in the equal split.
     */
    fun statement(
        member: Member,
        meals: List<MealEntry>,
        bills: List<UtilityBill>,
        deposits: List<Deposit>,
        breakfastWeight: Double,
        mealRate: Double,
        activeMemberCount: Int,
    ): MemberStatement {
        val memberMeals = totalMealsOf(meals, member.memberId, breakfastWeight)
        val mealCost = money(memberMeals * mealRate)

        // Inactive members do not take part in the equal split.
        val equalUtility = if (member.isActive) {
            money(equalUtilityShare(bills, activeMemberCount))
        } else {
            0.0
        }

        val customUtility = money(customUtilitiesOf(bills, member.memberId))
        val totalCost = money(mealCost + member.seatRent + equalUtility + customUtility)
        val depositTotal = money(verifiedDepositsOf(deposits, member.memberId))
        val netBalance = money((member.prevBalance + depositTotal) - totalCost)

        return MemberStatement(
            member = member,
            totalMeals = memberMeals,
            mealCost = mealCost,
            seatRent = member.seatRent,
            equalUtility = equalUtility,
            customUtility = customUtility,
            totalCost = totalCost,
            prevBalance = member.prevBalance,
            totalDeposit = depositTotal,
            netBalance = netBalance,
        )
    }

    /**
     * Builds the statements of **all** members of the mess.
     *
     * @param totalBazar total bazar spend of the month (used for the meal rate).
     */
    fun statements(
        members: List<Member>,
        meals: List<MealEntry>,
        bills: List<UtilityBill>,
        deposits: List<Deposit>,
        breakfastWeight: Double,
        totalBazar: Double,
    ): List<MemberStatement> {
        val rate = mealRate(totalBazar, totalMeals(meals, breakfastWeight))
        val active = activeMemberCount(members)
        return members.map { member ->
            statement(member, meals, bills, deposits, breakfastWeight, rate, active)
        }
    }
}
