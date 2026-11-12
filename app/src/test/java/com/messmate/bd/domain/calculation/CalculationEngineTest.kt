package com.messmate.bd.domain.calculation

import com.messmate.bd.domain.model.ActiveStatus
import com.messmate.bd.domain.model.BazarEntry
import com.messmate.bd.domain.model.Deposit
import com.messmate.bd.domain.model.DepositMethod
import com.messmate.bd.domain.model.DepositStatus
import com.messmate.bd.domain.model.MealEntry
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.Role
import com.messmate.bd.domain.model.SplitType
import com.messmate.bd.domain.model.UtilityBill
import com.messmate.bd.domain.model.UtilityCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The local calculation engine must produce exactly the numbers of the
 * `Summary` tab – these tests lock that contract down.
 */
class CalculationEngineTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 12)

    private fun member(
        id: String,
        role: Role = Role.MEMBER,
        seatRent: Double = 0.0,
        active: ActiveStatus = ActiveStatus.ACTIVE,
        prevBalance: Double = 0.0,
    ) = Member(
        memberId = id,
        name = "Member $id",
        gmail = "$id@example.com",
        role = role,
        seatRent = seatRent,
        activeStatus = active,
        prevBalance = prevBalance,
    )

    private fun meal(
        memberId: String,
        breakfast: Int = 0,
        lunch: Int = 0,
        dinner: Int = 0,
        guest: Int = 0,
    ) = MealEntry(
        date = today,
        memberId = memberId,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        guestMeals = guest,
        updatedBy = "test",
        updatedAt = "",
    )

    @Test
    fun `breakfast is weighted with the configured weight`() {
        val meals = listOf(meal("M001", breakfast = 1))

        assertEquals(1.0, CalculationEngine.totalMealsOf(meals, "M001", 1.0), 0.001)
        assertEquals(0.5, CalculationEngine.totalMealsOf(meals, "M001", 0.5), 0.001)
    }

    @Test
    fun `guest meals count as full meals`() {
        val meals = listOf(meal("M001", lunch = 1, guest = 2))

        assertEquals(3.0, CalculationEngine.totalMealsOf(meals, "M001", 1.0), 0.001)
    }

    @Test
    fun `meal rate is bazar divided by meals`() {
        val bazar = listOf(
            BazarEntry("B1", today, "Ali", "Rice", 1000.0, "ali", ""),
            BazarEntry("B2", today, "Karim", "Fish", 500.0, "karim", ""),
        )
        val meals = listOf(
            meal("M001", lunch = 1),
            meal("M002", lunch = 1),
            meal("M003", lunch = 1),
            meal("M004", lunch = 1),
        )

        val rate = CalculationEngine.mealRate(
            CalculationEngine.totalBazar(bazar),
            CalculationEngine.totalMeals(meals, 1.0),
        )

        assertEquals(375.0, rate, 0.001)
    }

    @Test
    fun `equal utility is shared between active members only`() {
        val members = listOf(
            member("M001"),
            member("M002"),
            member("M003", active = ActiveStatus.INACTIVE),
        )
        val bills = listOf(
            UtilityBill("U1", today, UtilityCategory.COOK, 3000.0, SplitType.EQUAL_ALL, null, ""),
        )

        // 3000 / 2 active members
        assertEquals(1500.0, CalculationEngine.equalUtilityShare(bills, 2), 0.001)

        val statements = CalculationEngine.statements(
            members = members,
            meals = emptyList(),
            bills = bills,
            deposits = emptyList(),
            breakfastWeight = 1.0,
            totalBazar = 0.0,
        )

        assertEquals(1500.0, statements.first { it.member.memberId == "M001" }.equalUtility, 0.001)
        assertEquals(1500.0, statements.first { it.member.memberId == "M002" }.equalUtility, 0.001)
        // An inactive member does not take part in the split.
        assertEquals(0.0, statements.first { it.member.memberId == "M003" }.equalUtility, 0.001)
    }

    @Test
    fun `custom utility is charged to a single member`() {
        val bills = listOf(
            UtilityBill("U1", today, UtilityCategory.ELECTRICITY, 500.0, SplitType.CUSTOM_MEMBER, "M002", "AC"),
        )

        assertEquals(500.0, CalculationEngine.customUtilitiesOf(bills, "M002"), 0.001)
        assertEquals(0.0, CalculationEngine.customUtilitiesOf(bills, "M001"), 0.001)
    }

    @Test
    fun `only verified deposits count towards the balance`() {
        val deposits = listOf(
            Deposit("D1", today, "M001", 500.0, DepositMethod.BKASH, "TX1", DepositStatus.PENDING),
            Deposit("D2", today, "M001", 1000.0, DepositMethod.CASH, "", DepositStatus.VERIFIED),
            Deposit("D3", today, "M001", 700.0, DepositMethod.NAGAD, "TX3", DepositStatus.REJECTED),
        )

        assertEquals(1000.0, CalculationEngine.verifiedDepositsOf(deposits, "M001"), 0.001)
    }

    @Test
    fun `net balance is deposits minus cost`() {
        val members = listOf(member("M001", seatRent = 1000.0, prevBalance = 200.0))
        val meals = listOf(meal("M001", lunch = 1))
        val deposits = listOf(
            Deposit("D1", today, "M001", 1000.0, DepositMethod.CASH, "", DepositStatus.VERIFIED),
        )
        val bazar = listOf(BazarEntry("B1", today, "Ali", "Rice", 600.0, "ali", ""))

        val statement = CalculationEngine.statements(
            members = members,
            meals = meals,
            bills = emptyList(),
            deposits = deposits,
            breakfastWeight = 1.0,
            totalBazar = CalculationEngine.totalBazar(bazar),
        ).single()

        // rate = 600 / 1 meal -> meal cost 600; seat rent 1000; cost = 1600
        assertEquals(600.0, statement.mealCost, 0.001)
        assertEquals(1600.0, statement.totalCost, 0.001)
        // (200 + 1000) - 1600 = -400
        assertEquals(-400.0, statement.netBalance, 0.001)
    }
}
