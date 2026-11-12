package com.messmate.bd.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messmate.bd.R
import com.messmate.bd.core.locale.LocaleController
import com.messmate.bd.ui.components.ErrorBanner
import com.messmate.bd.ui.components.SectionCard
import com.messmate.bd.ui.components.SettingRow
import com.messmate.bd.ui.components.TimeField
import com.messmate.bd.ui.util.Formatters

/**
 * The settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAudit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val mess by viewModel.mess.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val isManager = mess?.isManager == true

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (event != null) {
                ErrorBanner(message = event.orEmpty())
            }

            // --- Language -------------------------------------------------
            SectionCard(title = stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = viewModel.languageTag != LocaleController.TAG_BANGLA,
                        onClick = { viewModel.setLanguage(LocaleController.TAG_ENGLISH, activity) },
                        label = { Text(stringResource(R.string.settings_language_english)) },
                    )
                    FilterChip(
                        selected = viewModel.languageTag == LocaleController.TAG_BANGLA,
                        onClick = { viewModel.setLanguage(LocaleController.TAG_BANGLA, activity) },
                        label = { Text(stringResource(R.string.settings_language_bangla)) },
                    )
                }
            }

            // --- Cut-offs and breakfast weight ----------------------------
            if (config != null) {
                SectionCard(
                    title = stringResource(R.string.settings_cutoffs),
                    subtitle = if (isManager) null else stringResource(R.string.role_member_short),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField(
                            label = stringResource(R.string.meal_breakfast),
                            time = config!!.breakfastCutoff,
                            onTimeChange = { time ->
                                viewModel.updateCutoffs(time, config!!.lunchCutoff, config!!.dinnerCutoff)
                            },
                            enabled = isManager,
                        )
                        TimeField(
                            label = stringResource(R.string.meal_lunch),
                            time = config!!.lunchCutoff,
                            onTimeChange = { time ->
                                viewModel.updateCutoffs(config!!.breakfastCutoff, time, config!!.dinnerCutoff)
                            },
                            enabled = isManager,
                        )
                        TimeField(
                            label = stringResource(R.string.meal_dinner),
                            time = config!!.dinnerCutoff,
                            onTimeChange = { time ->
                                viewModel.updateCutoffs(config!!.breakfastCutoff, config!!.lunchCutoff, time)
                            },
                            enabled = isManager,
                        )

                        SettingRow(
                            label = stringResource(R.string.settings_breakfast_weight),
                            supportingText = stringResource(
                                R.string.meal_weight_hint,
                                if (config!!.breakfastWeight == 0.5) "0.5" else "1",
                            ),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0.5, 1.0).forEach { weight ->
                                    FilterChip(
                                        selected = config!!.breakfastWeight == weight,
                                        onClick = { viewModel.updateBreakfastWeight(weight) },
                                        enabled = isManager,
                                        label = { Text(if (weight == 0.5) "0.5" else "1") },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Month end -------------------------------------------------
            if (isManager) {
                SectionCard(
                    title = stringResource(R.string.month_close_title),
                    subtitle = stringResource(R.string.month_close_hint),
                ) {
                    Button(
                        onClick = viewModel::closeMonth,
                        enabled = config?.isLocked != true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                        Text(
                            text = stringResource(R.string.month_close_confirm),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            // --- Account ----------------------------------------------------
            SectionCard(title = stringResource(R.string.settings_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mess?.let { current ->
                        SettingRow(
                            label = current.name,
                            supportingText = current.spreadsheetUrl,
                        ) {}
                    }

                    OutlinedButton(
                        onClick = viewModel::signOut,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                        Text(
                            text = stringResource(R.string.settings_sign_out),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenAudit,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null)
                        Text(
                            text = stringResource(R.string.audit_title),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::leaveMess,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_leave_mess))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
