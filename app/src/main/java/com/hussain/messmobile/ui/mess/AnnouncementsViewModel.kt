package com.hussain.messmobile.ui.mess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import com.hussain.messmobile.data.repository.MessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The announcements of the mess. */
class AnnouncementsViewModel(private val repository: MessRepository) : ViewModel() {

    val announcements: StateFlow<List<AnnouncementEntity>> = repository.announcements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun addAnnouncement(title: String, body: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addAnnouncement(title, body) }
    }

    fun updateAnnouncement(announcement: AnnouncementEntity, title: String, body: String) {
        viewModelScope.launch {
            repository.updateAnnouncement(
                announcement.copy(title = title.trim(), body = body.trim()),
            )
        }
    }

    fun setPinned(announcement: AnnouncementEntity, pinned: Boolean) {
        viewModelScope.launch { repository.updateAnnouncement(announcement.copy(pinned = pinned)) }
    }

    fun deleteAnnouncement(announcement: AnnouncementEntity) {
        viewModelScope.launch { repository.deleteAnnouncement(announcement) }
    }
}
