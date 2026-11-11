package com.hussain.messmobile.ui.screens.dues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
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
import com.hussain.messmobile.ui.components.DuesDialog
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.IconAction
import com.hussain.messmobile.ui.components.MemberAvatar
import com.hussain.messmobile.ui.components.PaymentDialog
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.StatTile
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.DuesViewModel
import com.hussain.messmobile.ui.mess.MemberDues
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** The monthly dues of the mess. */
@Composable
fun DuesScreen(
    modifier: Modifier = Modifier,
    viewModel: DuesViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val dues by viewModel.memberDues.collectAsState()
    val period by viewModel.selectedPeriod.collectAsState()

    var editing by remember { mutableStateOf<MemberDues?>(null) }
    var paying by remember { mutableStateOf<MemberDues?>(null) }

    val totalDue = dues.sumOf { it.amountDue }
    val totalPaid = dues.sumOf { it.amountPaid }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.three),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.two),
            ) {
                IconAction(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "Previous month",
                    onClick = { viewModel.shiftPeriod(-1) },
                )
                ThemedText(
                    text = DateUtils.formatPeriod(period),
                    style = AppTextStyle.Subtitle,
                    modifier = Modifier.weight(1f),
                )
                IconAction(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "Next month",
                    onClick = { viewModel.shiftPeriod(1) },
                )
            }
        }

        item {
            SectionCard(title = "Summary") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.two)) {
                    StatTile(
                        label = "To collect",
                        value = DateUtils.formatMoney(totalDue),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Collected",
                        value = DateUtils.formatMoney(totalPaid),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Open",
                        value = DateUtils.formatMoney((totalDue - totalPaid).coerceAtLeast(0.0)),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (dues.isEmpty()) {
            item {
                EmptyState(
                    title = "No members",
                    message = "Add the members of the mess first, then set what they owe.",
                )
            }
        }

        items(dues, key = { it.member.id }) { memberDues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editing = memberDues },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.two),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.three),
                ) {
                    MemberAvatar(name = memberDues.member.name, size = 36)

                    Column(modifier = Modifier.weight(1f)) {
                        ThemedText(text = memberDues.member.name, style = AppTextStyle.Default)
                        ThemedText(
                            text = if (memberDues.amountDue > 0.0) {
                                "Owes ${DateUtils.formatMoney(memberDues.outstanding)}" +
                                    if (memberDues.amountPaid > 0.0) {
                                        " of ${DateUtils.formatMoney(memberDues.amountDue)}"
                                    } else {
                                        ""
                                    }
                            } else {
                                "No dues set"
                            },
                            style = AppTextStyle.Small,
                            color = appColors.textSecondary,
                        )
                    }

                    if (memberDues.settled) {
                        ThemedText(text = "Paid", style = AppTextStyle.Small, color = appColors.textSecondary)
                    } else {
                        TextButton(onClick = { paying = memberDues }) {
                            ThemedText(text = "Payment", style = AppTextStyle.LinkPrimary)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(Spacing.three)) }
    }

    editing?.let { memberDues ->
        DuesDialog(
            memberName = memberDues.member.name,
            currentAmount = memberDues.amountDue,
            onDismiss = { editing = null },
            onSave = { amount ->
                viewModel.setDues(memberDues.member.id, amount)
                editing = null
            },
        )
    }

    paying?.let { memberDues ->
        PaymentDialog(
            memberName = memberDues.member.name,
            onDismiss = { paying = null },
            onSave = { amount, paidAt, note ->
                viewModel.addPayment(memberDues.member.id, amount, paidAt, note)
                paying = null
            },
        )
    }
}
