package com.hussain.messmobile.ui.mess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.repository.MessRepository
import com.hussain.messmobile.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What one member owes and paid for a month. */
data class MemberDues(
    val member: MemberEntity,
    val amountDue: Double,
    val amountPaid: Double,
) {
    val outstanding: Double get() = (amountDue - amountPaid).coerceAtLeast(0.0)
    val settled: Boolean get() = outstanding <= 0.0 && amountDue > 0.0
}

/** The dues of the mess for a month. */
class DuesViewModel(private val repository: MessRepository) : ViewModel() {

    private val period = MutableStateFlow(DateUtils.currentPeriod())

    val selectedPeriod: StateFlow<String> = period

    val members: StateFlow<List<MemberEntity>> = repository.activeMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val memberDues: StateFlow<List<MemberDues>> = combine(
        period.flatMapLatest { repository.duesForPeriod(it) },
        period.flatMapLatest { repository.paymentsForPeriod(it) },
        repository.activeMembers,
    ) { dues, payments, members ->
        val paidByMember = payments.groupBy { it.memberId }.mapValues { entry -> entry.value.sumOf { it.amount } }
        members.map { member ->
            MemberDues(
                member = member,
                amountDue = dues.filter { it.memberId == member.id }.sumOf { it.amount },
                amountPaid = paidByMember[member.id] ?: 0.0,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun selectPeriod(period: String) {
        this.period.value = period
    }

    fun shiftPeriod(months: Long) {
        val current = DateUtils.parsePeriod(period.value)
        this.period.value = current.plusMonths(months).toString()
    }

    fun setDues(memberId: Long, amount: Double) {
        viewModelScope.launch { repository.setDues(memberId, period.value, amount) }
    }

    fun addPayment(memberId: Long, amount: Double, paidAt: Long, note: String) {
        viewModelScope.launch { repository.addPayment(memberId, period.value, amount, paidAt, note) }
    }
}
