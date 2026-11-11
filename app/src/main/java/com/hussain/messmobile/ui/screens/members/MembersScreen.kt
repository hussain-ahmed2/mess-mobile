package com.hussain.messmobile.ui.screens.members

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.R
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.ui.components.ConfirmDialog
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.IconAction
import com.hussain.messmobile.ui.components.MemberAvatar
import com.hussain.messmobile.ui.components.MemberDialog
import com.hussain.messmobile.ui.components.RoleBadge
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.MembersViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors

/** Everybody who lives in the mess. */
@Composable
fun MembersScreen(
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val members by viewModel.members.collectAsState()

    var editing by remember { mutableStateOf<MemberEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<MemberEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.two),
    ) {
        if (members.isEmpty()) {
            item {
                EmptyState(
                    title = "No members yet",
                    message = "Add everybody who lives in the mess to get started.",
                )
            }
        }

        items(members, key = { it.id }) { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        editing = member
                        showDialog = true
                    }
                    .padding(vertical = Spacing.two),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.three),
            ) {
                MemberAvatar(name = member.name)

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.two),
                    ) {
                        ThemedText(text = member.name, style = AppTextStyle.Default)
                        RoleBadge(role = member.role)
                    }
                    if (!member.phone.isNullOrBlank()) {
                        ThemedText(
                            text = member.phone,
                            style = AppTextStyle.Small,
                            color = appColors.textSecondary,
                        )
                    }
                    if (!member.active) {
                        ThemedText(
                            text = "Moved out",
                            style = AppTextStyle.Small,
                            color = appColors.textSecondary,
                        )
                    }
                }

                IconAction(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Remove ${member.name}",
                    onClick = { deleting = member },
                )
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
                    contentDescription = "Add member",
                )
            }
        }
    }

    if (showDialog) {
        val member = editing
        MemberDialog(
            member = member,
            onDismiss = { showDialog = false },
            onSave = { name, phone, role ->
                if (member == null) {
                    viewModel.addMember(name, phone, role)
                } else {
                    viewModel.updateMember(member, name, phone, role)
                }
                showDialog = false
            },
        )
    }

    deleting?.let { member ->
        ConfirmDialog(
            title = "Remove ${member.name}?",
            text = "The meals, the dues and the payments of this member are removed as well.",
            confirmLabel = "Remove",
            onConfirm = {
                viewModel.deleteMember(member)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
