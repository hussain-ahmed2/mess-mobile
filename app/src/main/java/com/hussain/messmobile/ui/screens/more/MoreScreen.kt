package com.hussain.messmobile.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hussain.messmobile.R
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.navigation.Routes
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors

/** Everything that does not fit into the bottom navigation. */
@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.three),
    ) {
        item {
            SectionCard(title = "The mess") {
                MoreRow(
                    iconRes = R.drawable.ic_person,
                    title = "Members",
                    subtitle = "Everybody who lives in the mess",
                    onClick = { onNavigate(Routes.MEMBERS) },
                )
                MoreRow(
                    iconRes = R.drawable.ic_expense,
                    title = "Expenses",
                    subtitle = "What the mess spends",
                    onClick = { onNavigate(Routes.EXPENSES) },
                )
                MoreRow(
                    iconRes = R.drawable.ic_shopping_cart,
                    title = "Shopping list",
                    subtitle = "Everything that has to be bought",
                    onClick = { onNavigate(Routes.SHOPPING) },
                )
                MoreRow(
                    iconRes = R.drawable.ic_announcement,
                    title = "Announcements",
                    subtitle = "Messages for the whole mess",
                    onClick = { onNavigate(Routes.ANNOUNCEMENTS) },
                )
            }
        }

        item {
            SectionCard(title = "Data") {
                MoreRow(
                    iconRes = R.drawable.ic_backup,
                    title = "Backup and restore",
                    subtitle = "Save your data and bring it back after an uninstall",
                    onClick = { onNavigate(Routes.BACKUP) },
                )
            }
        }

        item {
            SectionCard(title = "About") {
                ThemedText(
                    text = "mess-mobile keeps every bit of data on this phone. Nothing is sent " +
                        "anywhere, and a backup file is the only way to move your data to another " +
                        "phone.",
                    style = AppTextStyle.Default,
                    color = appColors.textSecondary,
                )
            }
        }
    }
}

/** A row that opens another part of the app. */
@Composable
private fun MoreRow(
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
