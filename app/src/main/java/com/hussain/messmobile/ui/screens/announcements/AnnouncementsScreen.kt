package com.hussain.messmobile.ui.screens.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.R
import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import com.hussain.messmobile.ui.components.AnnouncementDialog
import com.hussain.messmobile.ui.components.ConfirmDialog
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.IconAction
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.AnnouncementsViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** The announcements for the whole mess. */
@Composable
fun AnnouncementsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnnouncementsViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val announcements by viewModel.announcements.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AnnouncementEntity?>(null) }
    var deleting by remember { mutableStateOf<AnnouncementEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.three),
    ) {
        if (announcements.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing to announce",
                    message = "Write a message for everybody in the mess.",
                )
            }
        }

        items(announcements, key = { it.id }) { announcement ->
            SectionCard(title = announcement.title) {
                if (announcement.body.isNotBlank()) {
                    ThemedText(text = announcement.body, style = AppTextStyle.Default)
                }
                ThemedText(
                    text = DateUtils.formatLongDate(announcement.createdAt),
                    style = AppTextStyle.Small,
                    color = appColors.textSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.two),
                ) {
                    IconAction(
                        painter = painterResource(
                            if (announcement.pinned) R.drawable.ic_check else R.drawable.ic_add,
                        ),
                        contentDescription = if (announcement.pinned) {
                            "Unpin the announcement"
                        } else {
                            "Pin the announcement"
                        },
                        onClick = { viewModel.setPinned(announcement, !announcement.pinned) },
                    )
                    IconAction(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Edit the announcement",
                        onClick = {
                            editing = announcement
                            showDialog = true
                        },
                    )
                    IconAction(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Delete the announcement",
                        onClick = { deleting = announcement },
                    )
                }
            }
        }

        item {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
                modifier = Modifier.padding(Spacing.three),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add announcement",
                )
            }
        }
    }

    if (showDialog) {
        val announcement = editing
        AnnouncementDialog(
            announcement = announcement,
            onDismiss = { showDialog = false },
            onSave = { title, body ->
                if (announcement == null) {
                    viewModel.addAnnouncement(title, body)
                } else {
                    viewModel.updateAnnouncement(announcement, title, body)
                }
                showDialog = false
            },
        )
    }

    deleting?.let { announcement ->
        ConfirmDialog(
            title = "Delete the announcement?",
            text = announcement.title,
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteAnnouncement(announcement)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
