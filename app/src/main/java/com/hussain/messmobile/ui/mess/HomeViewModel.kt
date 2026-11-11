package com.hussain.messmobile.ui.mess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.messmobile.data.repository.MessRepository
import com.hussain.messmobile.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Everything the home screen shows. */
data class HomeUiState(
    val mealsToday: List<MealSummary> = emptyList(),
    val memberCount: Int = 0,
    val duesTotal: Double = 0.0,
    val paidTotal: Double = 0.0,
    val outstanding: Double = 0.0,
    val unpaidMembers: Int = 0,
    val announcements: List<AnnouncementSummary> = emptyList(),
    val pendingShoppingItems: Int = 0,
)

data class MealSummary(
    val id: Long,
    val type: String,
    val description: String,
    val cookName: String?,
    val totalCost: Double,
)

data class AnnouncementSummary(
    val id: Long,
    val title: String,
    val body: String,
    val createdAt: Long,
    val pinned: Boolean,
)

/** The dashboard of the app. */
class HomeViewModel(repository: MessRepository) : ViewModel() {

    private val period = DateUtils.currentPeriod()

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            repository.mealsForDate(DateUtils.startOfToday()),
            repository.members,
        ) { meals, members ->
            val names = members.associate { it.id to it.name }
            meals.map { meal ->
                MealSummary(
                    id = meal.id,
                    type = meal.type.label,
                    description = meal.description,
                    cookName = meal.cookId?.let { names[it] },
                    totalCost = meal.totalCost,
                )
            }
        },
        combine(
            repository.activeMemberCount,
            repository.duesForPeriod(period),
            repository.paymentsForPeriod(period),
        ) { memberCount, dues, payments ->
            val paid = payments.sumOf { it.amount }
            val total = dues.sumOf { it.amount }
            val paidByMember = payments
                .groupBy { it.memberId }
                .mapValues { (_, payments) -> payments.sumOf { it.amount } }
            val unpaid = dues.count { duesEntity ->
                (paidByMember[duesEntity.memberId] ?: 0.0) < duesEntity.amount
            }
            DuesSummary(memberCount, total, paid, unpaid)
        },
        combine(
            repository.announcements,
            repository.pendingShoppingCount,
        ) { announcements, pendingItems -> announcements to pendingItems },
    ) { meals, dues, extras ->
        val (announcements, pendingItems) = extras
        HomeUiState(
            mealsToday = meals,
            memberCount = dues.memberCount,
            duesTotal = dues.total,
            paidTotal = dues.paid,
            outstanding = (dues.total - dues.paid).coerceAtLeast(0.0),
            unpaidMembers = dues.unpaid,
            announcements = announcements.take(3).map {
                AnnouncementSummary(
                    id = it.id,
                    title = it.title,
                    body = it.body,
                    createdAt = it.createdAt,
                    pinned = it.pinned,
                )
            },
            pendingShoppingItems = pendingItems,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}

private data class DuesSummary(
    val memberCount: Int,
    val total: Double,
    val paid: Double,
    val unpaid: Int,
)
