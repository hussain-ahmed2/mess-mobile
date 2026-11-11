package com.hussain.messmobile.ui.mess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.model.MemberRole
import com.hussain.messmobile.data.repository.MessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The members of the mess. */
class MembersViewModel(private val repository: MessRepository) : ViewModel() {

    val members: StateFlow<List<MemberEntity>> = repository.members.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun addMember(name: String, phone: String?, role: MemberRole, onDone: () -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addMember(name, phone, role)
            onDone()
        }
    }

    fun updateMember(member: MemberEntity, name: String, phone: String?, role: MemberRole) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateMember(
                member.copy(
                    name = name.trim(),
                    phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                    role = role,
                ),
            )
        }
    }

    fun setActive(member: MemberEntity, active: Boolean) {
        viewModelScope.launch { repository.setMemberActive(member, active) }
    }

    fun deleteMember(member: MemberEntity) {
        viewModelScope.launch { repository.deleteMember(member) }
    }
}
