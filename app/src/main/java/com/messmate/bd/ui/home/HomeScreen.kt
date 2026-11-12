package com.messmate.bd.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messmate.bd.R
import com.messmate.bd.domain.model.MealType
import com.messmate.bd.ui.components.InfoBanner
import com.messmate.bd.ui.components.SectionCard
import com.messmate.bd.ui.components.StatCard
import com.messmate.bd.ui.components.TopBarActions
import com.messmate.bd.ui.util.Formatters
import java.time.LocalDate

/**
 * The dashboard: the live meal rate, the personal balance and the meal
 * switches with their cut-off countdowns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAudit: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Failed writes are reported as a snackbar.
    val event by viewModel.events.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(event) {
        (event as? HomeEvent.Message)?.let { message ->
            snackbarHostState.showSnackbar(message.text)
            viewModel.consumeEvent()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = state.messName.ifEmpty { stringResource(R.string.app_name) })
                        Text(
                            text = Formatters.month(state.month),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null)
                    }
                    TopBarActions(onOpenAudit = onOpenAudit, onOpenSettings = onOpenSettings)
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLocked) {
                item { InfoBanner(message = stringResource(R.string.month_locked_banner)) }
            }

            if (state.pendingSync > 0) {
                item {
                    InfoBanner(
                        message = stringResource(R.string.label_pending_sync, state.pendingSync),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = stringResource(R.string.dashboard_live_meal_rate),
                        value = Formatters.money(state.totals.liveMealRate),
                        hint = stringResource(
                            R.string.dashboard_meal_rate_hint,
                            state.totals.totalMeals.toInt().toString(),
                        ),
                        icon = Icons.Outlined.Fastfood,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = stringResource(R.string.dashboard_total_bazar),
                        value = Formatters.moneyShort(state.totals.totalBazar),
                        icon = Icons.Outlined.ShoppingBasket,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                BalanceCard(state = state)
            }

            item {
                MealSection(
                    title = stringResource(R.string.dashboard_today_meals),
                    date = LocalDate.now(),
                    toggles = state.today,
                    isManager = state.isManager,
                    isLocked = state.isLocked,
                    onToggle = { type, enabled -> viewModel.toggleMeal(LocalDate.now(), type, enabled) },
                    onGuestMealsChange = { count ->
                        viewModel.setGuestMeals(LocalDate.now(), count)
                    },
                )
            }

            item {
                MealSection(
                    title = stringResource(R.string.dashboard_tomorrow_meals),
                    date = LocalDate.now().plusDays(1),
                    toggles = state.tomorrow,
                    isManager = state.isManager,
                    isLocked = state.isLocked,
                    onToggle = { type, enabled ->
                        viewModel.toggleMeal(LocalDate.now().plusDays(1), type, enabled)
                    },
                    onGuestMealsChange = { count ->
                        viewModel.setGuestMeals(LocalDate.now().plusDays(1), count)
                    },
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/**
 * The personal balance – positive means the manager owes the member.
 */
@Composable
private fun BalanceCard(state: HomeUiState) {
    val statement = state.myStatement
    val balance = statement?.netBalance ?: 0.0
    val isCredit = balance > 0.0
    val isSettled = kotlin.math.abs(balance) < 0.01

    val (headline, color) = when {
        isSettled -> stringResource(R.string.dashboard_balance_settled) to MaterialTheme.colorScheme.onSurface
        isCredit -> stringResource(R.string.dashboard_balance_credit, Formatters.money(balance)) to Color(0xFF2E7D32)
        else -> stringResource(R.string.dashboard_balance_due, Formatters.money(-balance)) to MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.dashboard_my_balance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (statement != null) {
                Text(
                    text = "${stringResource(R.string.statement_meals)}: " +
                        "${statement.totalMeals}   ·   " +
                        "${stringResource(R.string.statement_total_cost)}: " +
                        Formatters.money(statement.totalCost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * The three meal switches of one day.
 */
@Composable
private fun MealSection(
    title: String,
    date: LocalDate,
    toggles: List<MealToggleUi>,
    isManager: Boolean,
    isLocked: Boolean,
    onToggle: (MealType, Boolean) -> Unit,
    onGuestMealsChange: (Int) -> Unit,
) {
    SectionCard(
        title = title,
        subtitle = Formatters.date(date),
    ) {
        Column {
            toggles.forEach { toggle ->
                MealRow(
                    toggle = toggle,
                    isManager = isManager,
                    onToggle = { onToggle(toggle.type, it) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Guest meals are charged to the member that hosts them – they
            // count as full meals in the meal rate.
            GuestMealsRow(
                count = toggles.firstOrNull()?.guestMeals ?: 0,
                enabled = !isLocked,
                onCountChange = onGuestMealsChange,
            )
        }
    }
}

/**
 * The stepper that sets the number of guest meals.
 */
@Composable
private fun GuestMealsRow(
    count: Int,
    enabled: Boolean,
    onCountChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.guest_meals),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onCountChange((count - 1).coerceAtLeast(0)) },
            enabled = enabled && count > 0,
        ) {
            Icon(Icons.Outlined.Remove, contentDescription = null)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(
            onClick = { onCountChange(count + 1) },
            enabled = enabled,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
        }
    }
}

/**
 * One meal switch with the cut-off countdown.
 */
@Composable
private fun MealRow(
    toggle: MealToggleUi,
    isManager: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val label = when (toggle.type) {
        MealType.BREAKFAST -> stringResource(R.string.meal_breakfast)
        MealType.LUNCH -> stringResource(R.string.meal_lunch)
        MealType.DINNER -> stringResource(R.string.meal_dinner)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            when {
                toggle.remaining != null -> Text(
                    text = stringResource(R.string.meal_countdown, Formatters.duration(toggle.remaining)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                toggle.locked -> Text(
                    text = stringResource(R.string.meal_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = toggle.enabled,
            onCheckedChange = onToggle,
            // A member cannot change a meal after the cut-off – the manager can.
            enabled = !toggle.locked || isManager,
        )
    }
}
