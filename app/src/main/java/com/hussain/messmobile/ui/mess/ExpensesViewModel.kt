package com.hussain.messmobile.ui.mess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.messmobile.data.local.entity.ExpenseEntity
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.local.entity.ShoppingItemEntity
import com.hussain.messmobile.data.model.ExpenseCategory
import com.hussain.messmobile.data.repository.MessRepository
import com.hussain.messmobile.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The shared expenses of the mess. */
class ExpensesViewModel(private val repository: MessRepository) : ViewModel() {

    val expenses: StateFlow<List<ExpenseEntity>> = repository.expenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val members: StateFlow<List<MemberEntity>> = repository.activeMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val shoppingItems: StateFlow<List<ShoppingItemEntity>> = repository.shoppingItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val totalThisMonth: StateFlow<Double> = repository.expenses
        .map { expenses ->
            val now = DateUtils.toLocalDate(System.currentTimeMillis())
            val start = DateUtils.startOfDay(now.withDayOfMonth(1))
            val end = DateUtils.startOfDay(now.withDayOfMonth(now.lengthOfMonth()))
            expenses.filter { it.date in start..end }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun addExpense(
        description: String,
        amount: Double,
        category: ExpenseCategory,
        date: Long,
        paidByMemberId: Long?,
        shared: Boolean,
    ) {
        if (description.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            repository.addExpense(description, amount, category, date, paidByMemberId, shared)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun addShoppingItem(name: String, quantity: String, estimatedCost: Double) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addShoppingItem(name, quantity, estimatedCost) }
    }

    fun setPurchased(item: ShoppingItemEntity, purchased: Boolean, purchasedByMemberId: Long? = null) {
        viewModelScope.launch { repository.setShoppingItemPurchased(item, purchased, purchasedByMemberId) }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch { repository.deleteShoppingItem(item) }
    }

    fun clearPurchased() {
        viewModelScope.launch { repository.clearPurchasedShoppingItems() }
    }
}
