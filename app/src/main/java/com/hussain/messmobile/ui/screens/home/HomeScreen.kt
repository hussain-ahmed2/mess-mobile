package com.hussain.messmobile.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.R
import com.hussain.messmobile.ui.components.AnimatedIcon
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.StatTile
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.HomeViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.navigation.Routes
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** The dashboard of the mess. */
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.three),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.three),
            ) {
                AnimatedIcon()
                Column {
                    ThemedText(text = "Mess manager", style = AppTextStyle.Subtitle)
                    ThemedText(
                        text = "${state.memberCount} members",
                        style = AppTextStyle.Default,
                        color = appColors.textSecondary,
                    )
                }
            }
        }

        item {
            SectionCard(title = "Today") {
                if (state.mealsToday.isEmpty()) {
                    EmptyState(
                        title = "Nothing planned",
                        message = "No meal was added for today yet.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.two)) {
                        state.mealsToday.forEach { meal ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    ThemedText(text = meal.type, style = AppTextStyle.Small)
                                    if (meal.description.isNotBlank()) {
                                        ThemedText(
                                            text = meal.description,
                                            style = AppTextStyle.Default,
                                            color = appColors.textSecondary,
                                        )
                                    }
                                    if (meal.cookName != null) {
                                        ThemedText(
                                            text = "Cooked by ${meal.cookName}",
                                            style = AppTextStyle.Small,
                                            color = appColors.textSecondary,
                                        )
                                    }
                                }
                                if (meal.totalCost > 0.0) {
                                    ThemedText(
                                        text = DateUtils.formatMoney(meal.totalCost),
                                        style = AppTextStyle.Default,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.two))
                    TextButton(onClick = { onNavigate(Routes.MEALS) }) {
                        ThemedText(text = "Open the meals", style = AppTextStyle.LinkPrimary)
                    }
                }
            }
        }

        item {
            SectionCard(title = "Dues of ${DateUtils.formatPeriod(DateUtils.currentPeriod())}") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.two)) {
                    StatTile(
                        label = "Collected",
                        value = DateUtils.formatMoney(state.paidTotal),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Outstanding",
                        value = DateUtils.formatMoney(state.outstanding),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.three))
                ThemedText(
                    text = if (state.unpaidMembers == 0) {
                        "Everybody is settled up."
                    } else {
                        "${state.unpaidMembers} members still have to pay."
                    },
                    style = AppTextStyle.Default,
                    color = appColors.textSecondary,
                )
                TextButton(onClick = { onNavigate(Routes.DUES) }) {
                    ThemedText(text = "Open the dues", style = AppTextStyle.LinkPrimary)
                }
            }
        }

        item {
            SectionCard(title = "Announcements") {
                if (state.announcements.isEmpty()) {
                    EmptyState(
                        title = "Nothing to announce",
                        message = "Announcements for the whole mess show up here.",
                    )
                } else {
                    Column {
                        state.announcements.forEachIndexed { index, announcement ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.two))
                            ThemedText(text = announcement.title, style = AppTextStyle.Small)
                            if (announcement.body.isNotBlank()) {
                                ThemedText(
                                    text = announcement.body,
                                    style = AppTextStyle.Default,
                                    color = appColors.textSecondary,
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { onNavigate(Routes.ANNOUNCEMENTS) }) {
                    ThemedText(text = "Open the announcements", style = AppTextStyle.LinkPrimary)
                }
            }
        }

        item {
            SectionCard(title = "Shortcuts") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.one)) {
                    ShortcutRow(
                        iconRes = R.drawable.ic_person,
                        title = "Members",
                        subtitle = "Who lives in the mess",
                        onClick = { onNavigate(Routes.MEMBERS) },
                    )
                    ShortcutRow(
                        iconRes = R.drawable.ic_expense,
                        title = "Expenses",
                        subtitle = "What the mess spent",
                        onClick = { onNavigate(Routes.EXPENSES) },
                    )
                    ShortcutRow(
                        iconRes = R.drawable.ic_shopping_cart,
                        title = "Shopping list",
                        subtitle = "${state.pendingShoppingItems} items to buy",
                        onClick = { onNavigate(Routes.SHOPPING) },
                    )
                    ShortcutRow(
                        iconRes = R.drawable.ic_backup,
                        title = "Backup",
                        subtitle = "Save and restore your data",
                        onClick = { onNavigate(Routes.BACKUP) },
                    )
                }
            }
        }
    }
}

/** A shortcut in the shortcut list of the home screen. */
@Composable
private fun ShortcutRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.two),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.three),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = appColors.text,
        )
        Column(modifier = Modifier.weight(1f)) {
            ThemedText(text = title, style = AppTextStyle.Default)
            ThemedText(text = subtitle, style = AppTextStyle.Small, color = appColors.textSecondary)
        }
    }
}
