package com.messmate.bd.ui.statement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messmate.bd.R
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.ui.components.EmptyState
import com.messmate.bd.ui.components.SectionCard
import com.messmate.bd.ui.util.Formatters

/**
 * The monthly statement of every member.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    modifier: Modifier = Modifier,
    viewModel: StatementViewModel = hiltViewModel(),
) {
    val statements by viewModel.statements.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.statement_title))
                        if (config != null) {
                            Text(
                                text = Formatters.month(config!!.monthYear),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportPdf(context) }) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = stringResource(R.string.action_export_pdf))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (statements.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.ReceiptLong,
                title = stringResource(R.string.label_empty),
                message = stringResource(R.string.statement_title),
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(statements, key = { it.member.memberId }) { statement ->
                StatementCard(statement = statement)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/**
 * The full breakdown of one member.
 */
@Composable
private fun StatementCard(statement: MemberStatement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${statement.member.name} (${statement.member.memberId})",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (statement.member.isManager) {
                    stringResource(R.string.role_manager_short)
                } else {
                    stringResource(R.string.role_member_short)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            StatementRow(stringResource(R.string.statement_meals), statement.totalMeals.toString())
            StatementRow(stringResource(R.string.statement_meal_cost), Formatters.money(statement.mealCost))
            StatementRow(stringResource(R.string.statement_seat_rent), Formatters.money(statement.seatRent))
            StatementRow(stringResource(R.string.statement_equal_utility), Formatters.money(statement.equalUtility))
            StatementRow(stringResource(R.string.statement_custom_utility), Formatters.money(statement.customUtility))
            StatementRow(stringResource(R.string.statement_total_cost), Formatters.money(statement.totalCost))
            StatementRow(stringResource(R.string.statement_prev_balance), Formatters.money(statement.prevBalance))
            StatementRow(stringResource(R.string.statement_deposits), Formatters.money(statement.totalDeposit))

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            val isCredit = statement.netBalance > 0.0
            val isSettled = kotlin.math.abs(statement.netBalance) < 0.01
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.statement_net_balance),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (isSettled) {
                        Formatters.money(0.0)
                    } else {
                        Formatters.money(statement.netBalance)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isSettled -> MaterialTheme.colorScheme.onSurface
                        isCredit -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

/**
 * A label/value line of the statement.
 */
@Composable
private fun StatementRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
