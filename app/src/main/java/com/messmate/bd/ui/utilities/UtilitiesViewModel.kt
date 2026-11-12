package com.messmate.bd.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.SplitType
import com.messmate.bd.domain.model.UtilityBill
import com.messmate.bd.domain.model.UtilityCategory
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * The fixed costs: cook salary, gas, electricity, Wi-Fi, water, garbage.
 */
@HiltViewModel
class UtilitiesViewModel @Inject constructor(
    private val messRepository: MessRepository,
) : ViewModel() {

    val bills: StateFlow<List<UtilityBill>> = messRepository.observeUtilityBills().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val members: StateFlow<List<Member>> = messRepository.observeMembers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** The part of the bills that is shared between all active members. */
    val equalTotal: StateFlow<Double> = bills
        .map { list -> list.filter { it.splitType == SplitType.EQUAL_ALL }.sumOf { it.amount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0,
        )

    /** The part that is charged to a single member. */
    val customTotal: StateFlow<Double> = bills
        .map { list -> list.filter { it.splitType == SplitType.CUSTOM_MEMBER }.sumOf { it.amount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0,
        )

    /** `true` when the signed-in user manages the mess. */
    val isManager: StateFlow<Boolean> = messRepository.observeMess()
        .map { it?.isManager == true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    /** Adds a bill to the `UtilityBills` tab. */
    fun add(
        category: UtilityCategory,
        amount: Double,
        splitType: SplitType,
        targetMemberId: String?,
        notes: String,
    ) {
        viewModelScope.launch {
            val bill = UtilityBill(
                billId = UUID.randomUUID().toString(),
                date = LocalDate.now(),
                category = category,
                amount = amount,
                splitType = splitType,
                targetMemberId = if (splitType == SplitType.CUSTOM_MEMBER) targetMemberId else null,
                notes = notes.trim(),
            )
            messRepository.addUtilityBill(bill).onFailure { error ->
                _events.value = error.message ?: "Could not save the bill."
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
