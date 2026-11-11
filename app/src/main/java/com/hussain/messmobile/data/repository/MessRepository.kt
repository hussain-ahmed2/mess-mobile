package com.hussain.messmobile.data.repository

import com.hussain.messmobile.data.local.MessDatabase
import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import com.hussain.messmobile.data.local.entity.DuesEntity
import com.hussain.messmobile.data.local.entity.ExpenseEntity
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.data.local.entity.MealEntryEntity
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.local.entity.PaymentEntity
import com.hussain.messmobile.data.local.entity.ShoppingItemEntity
import com.hussain.messmobile.data.model.ExpenseCategory
import com.hussain.messmobile.data.model.MealType
import com.hussain.messmobile.data.model.MemberRole
import kotlinx.coroutines.flow.Flow

/**
 * The single place where the app reads from and writes to the database.
 *
 * Everything is stored on the device, there is no server involved.
 */
class MessRepository(database: MessDatabase) {

    private val memberDao = database.memberDao()
    private val mealDao = database.mealDao()
    private val mealEntryDao = database.mealEntryDao()
    private val duesDao = database.duesDao()
    private val paymentDao = database.paymentDao()
    private val expenseDao = database.expenseDao()
    private val shoppingDao = database.shoppingDao()
    private val announcementDao = database.announcementDao()

    val members: Flow<List<MemberEntity>> = memberDao.observeAll()
    val activeMembers: Flow<List<MemberEntity>> = memberDao.observeActive()
    val activeMemberCount: Flow<Int> = memberDao.observeActiveCount()
    val announcements: Flow<List<AnnouncementEntity>> = announcementDao.observeAll()
    val shoppingItems: Flow<List<ShoppingItemEntity>> = shoppingDao.observeAll()
    val pendingShoppingCount: Flow<Int> = shoppingDao.observePendingCount()
    val expenses: Flow<List<ExpenseEntity>> = expenseDao.observeAll()

    fun mealsForDate(date: Long): Flow<List<MealEntity>> = mealDao.observeForDate(date)

    fun entriesForMeal(mealId: Long): Flow<List<MealEntryEntity>> = mealEntryDao.observeForMeal(mealId)

    fun mealEntriesForDate(date: Long): Flow<List<MealEntryEntity>> = mealEntryDao.observeForDate(date)

    fun duesForPeriod(period: String): Flow<List<DuesEntity>> = duesDao.observeForPeriod(period)

    fun paymentsForPeriod(period: String): Flow<List<PaymentEntity>> = paymentDao.observeForPeriod(period)

    // ---------------------------------------------------------------- members

    suspend fun addMember(name: String, phone: String?, role: MemberRole): Long = memberDao.insert(
        MemberEntity(
            name = name.trim(),
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            role = role,
            joinedAt = System.currentTimeMillis(),
        ),
    )

    suspend fun updateMember(member: MemberEntity) = memberDao.update(member)

    suspend fun deleteMember(member: MemberEntity) = memberDao.delete(member)

    suspend fun setMemberActive(member: MemberEntity, active: Boolean) =
        memberDao.update(member.copy(active = active))

    // ------------------------------------------------------------------ meals

    suspend fun addMeal(
        date: Long,
        type: MealType,
        description: String,
        cookId: Long?,
        totalCost: Double,
    ): Long = mealDao.insert(
        MealEntity(
            date = date,
            type = type,
            description = description.trim(),
            cookId = cookId,
            totalCost = totalCost,
        ),
    )

    suspend fun updateMeal(meal: MealEntity) = mealDao.update(meal)

    suspend fun deleteMeal(meal: MealEntity) = mealDao.deleteById(meal.id)

    /** Opts a member in or out of a meal. */
    suspend fun setOptIn(mealId: Long, memberId: Long, optedIn: Boolean) {
        updateEntry(mealId, memberId) { it.copy(optedIn = optedIn) }
    }

    /** Marks whether a member actually ate. */
    suspend fun setEaten(mealId: Long, memberId: Long, eaten: Boolean) {
        updateEntry(mealId, memberId) { it.copy(eaten = eaten) }
    }

    /** Sets what a member has to pay for a meal. */
    suspend fun setEntryAmount(mealId: Long, memberId: Long, amount: Double) {
        updateEntry(mealId, memberId) { it.copy(amount = amount) }
    }

    /** Shares the cost of a meal equally between everybody who opted in. */
    suspend fun splitMealCost(mealId: Long) {
        val meal = mealDao.findById(mealId) ?: return
        val entries = mealEntryDao.getForMeal(mealId)
        val participants = entries.filter { it.optedIn }
        if (participants.isEmpty() || meal.totalCost <= 0.0) return

        val share = meal.totalCost / participants.size
        participants.forEach { entry ->
            mealEntryDao.update(entry.copy(amount = share))
        }
    }

    private suspend fun updateEntry(
        mealId: Long,
        memberId: Long,
        transform: (MealEntryEntity) -> MealEntryEntity,
    ) {
        val existing = mealEntryDao.find(mealId, memberId)
        if (existing == null) {
            mealEntryDao.insert(transform(MealEntryEntity(mealId = mealId, memberId = memberId)))
        } else {
            mealEntryDao.update(transform(existing))
        }
    }

    // ------------------------------------------------------------------- dues

    /** Sets what a member owes for a month, creates the entry when it does not exist yet. */
    suspend fun setDues(memberId: Long, period: String, amount: Double) {
        val existing = duesDao.find(memberId, period)
        if (existing == null) {
            duesDao.insert(DuesEntity(memberId = memberId, period = period, amount = amount))
        } else {
            duesDao.update(existing.copy(amount = amount))
        }
    }

    suspend fun deleteDues(dues: DuesEntity) = duesDao.deleteById(dues.id)

    suspend fun addPayment(
        memberId: Long,
        period: String,
        amount: Double,
        paidAt: Long,
        note: String,
    ): Long = paymentDao.insert(
        PaymentEntity(
            memberId = memberId,
            period = period,
            amount = amount,
            paidAt = paidAt,
            note = note.trim(),
        ),
    )

    suspend fun deletePayment(payment: PaymentEntity) = paymentDao.delete(payment)

    suspend fun deletePaymentById(paymentId: Long) = paymentDao.deleteById(paymentId)

    // --------------------------------------------------------------- expenses

    suspend fun addExpense(
        description: String,
        amount: Double,
        category: ExpenseCategory,
        date: Long,
        paidByMemberId: Long?,
        shared: Boolean,
    ): Long = expenseDao.insert(
        ExpenseEntity(
            description = description.trim(),
            amount = amount,
            category = category,
            date = date,
            paidByMemberId = paidByMemberId,
            shared = shared,
        ),
    )

    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteById(expense.id)

    // --------------------------------------------------------------- shopping

    suspend fun addShoppingItem(name: String, quantity: String, estimatedCost: Double): Long =
        shoppingDao.insert(
            ShoppingItemEntity(
                name = name.trim(),
                quantity = quantity.trim(),
                estimatedCost = estimatedCost,
                createdAt = System.currentTimeMillis(),
            ),
        )

    suspend fun updateShoppingItem(item: ShoppingItemEntity) = shoppingDao.update(item)

    suspend fun deleteShoppingItem(item: ShoppingItemEntity) = shoppingDao.deleteById(item.id)

    suspend fun setShoppingItemPurchased(
        item: ShoppingItemEntity,
        purchased: Boolean,
        purchasedByMemberId: Long? = null,
    ) = shoppingDao.update(
        item.copy(
            purchased = purchased,
            purchasedByMemberId = if (purchased) purchasedByMemberId else null,
            purchasedAt = if (purchased) System.currentTimeMillis() else null,
        ),
    )

    suspend fun clearPurchasedShoppingItems() = shoppingDao.deletePurchased()

    // ----------------------------------------------------------- announcements

    suspend fun addAnnouncement(title: String, body: String): Long = announcementDao.insert(
        AnnouncementEntity(
            title = title.trim(),
            body = body.trim(),
            createdAt = System.currentTimeMillis(),
        ),
    )

    suspend fun updateAnnouncement(announcement: AnnouncementEntity) = announcementDao.update(announcement)

    suspend fun deleteAnnouncement(announcement: AnnouncementEntity) = announcementDao.deleteById(announcement.id)
}
