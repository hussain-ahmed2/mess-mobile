package com.messmate.bd.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.model.ActiveStatus
import com.messmate.bd.domain.model.Deposit
import com.messmate.bd.domain.model.DepositMethod
import com.messmate.bd.domain.model.DepositStatus
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.domain.model.Mess
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
 * The members, their seat rents and the deposits.
 */
@HiltViewModel
class MembersViewModel @Inject constructor(
    private val messRepository: MessRepository,
) : ViewModel() {

    val members: StateFlow<List<Member>> = messRepository.observeMembers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val statements: StateFlow<List<MemberStatement>> = messRepository.observeStatements().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val deposits: StateFlow<List<Deposit>> = messRepository.observeDeposits().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val mess: StateFlow<Mess?> = messRepository.observeMess().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    /** Adds a member and shares the sheet with them. */
    fun addMember(name: String, gmail: String, seatRent: Double) {
        viewModelScope.launch {
            val member = Member(
                memberId = "", // the repository assigns the next free id
                name = name.trim(),
                gmail = gmail.trim(),
                role = com.messmate.bd.domain.model.Role.MEMBER,
                seatRent = seatRent,
                activeStatus = ActiveStatus.ACTIVE,
                prevBalance = 0.0,
            )
            messRepository.addMember(member).onFailure { error ->
                _events.value = error.message ?: "Could not add the member."
            }
        }
    }

    /** Changes the seat rent of a member. */
    fun updateSeatRent(memberId: String, seatRent: Double) {
        viewModelScope.launch {
            messRepository.updateSeatRent(memberId, seatRent).onFailure { error ->
                _events.value = error.message ?: "Could not change the seat rent."
            }
        }
    }

    /** Logs a deposit. */
    fun addDeposit(memberId: String, amount: Double, method: DepositMethod, trxId: String) {
        viewModelScope.launch {
            val deposit = Deposit(
                depositId = UUID.randomUUID().toString(),
                date = LocalDate.now(),
                memberId = memberId,
                amount = amount,
                method = method,
                trxId = trxId.trim(),
                status = DepositStatus.PENDING,
            )
            messRepository.addDeposit(deposit).onFailure { error ->
                _events.value = error.message ?: "Could not save the deposit."
            }
        }
    }

    /** Verifies or rejects a pending deposit (manager only). */
    fun resolveDeposit(depositId: String, approve: Boolean) {
        viewModelScope.launch {
            messRepository.resolveDeposit(depositId, approve).onFailure { error ->
                _events.value = error.message ?: "Could not update the deposit."
            }
        }
    }

    /** The statement of a single member – `null` when there is none yet. */
    fun statementOf(memberId: String): MemberStatement? =
        statements.value.firstOrNull { it.member.memberId == memberId }

    fun consumeEvent() {
        _events.value = null
    }
}
