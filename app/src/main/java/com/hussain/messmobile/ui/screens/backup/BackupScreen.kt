package com.hussain.messmobile.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.BuildConfig
import com.hussain.messmobile.R
import com.hussain.messmobile.ui.components.ConfirmDialog
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.BackupViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Saves the data of the app to a file and restores it again. */
@Composable
fun BackupScreen(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val name = defaultFileName()
            viewModel.export(uri, BuildConfig.VERSION_NAME, name)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.inspect(uri, uri.lastPathSegment ?: "the backup file")
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.three),
    ) {
        item {
            SectionCard(title = "Backup") {
                ThemedText(
                    text = "A backup is a single file with everybody, the meals, the dues, the " +
                        "expenses and the announcements of the mess. Store it wherever you want, " +
                        "for example on your cloud drive.",
                    style = AppTextStyle.Default,
                    color = appColors.textSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.three))
                BackupAction(
                    iconRes = R.drawable.ic_backup,
                    title = "Save a backup",
                    subtitle = "Write the data of the app into a file",
                    onClick = { exportLauncher.launch(defaultFileName()) },
                )
                BackupAction(
                    iconRes = R.drawable.ic_restore,
                    title = "Restore a backup",
                    subtitle = "Load a file and continue where you stopped",
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                )
            }
        }

        item {
            SectionCard(title = "Status") {
                if (state.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        Spacer(modifier = Modifier.width(Spacing.two))
                        ThemedText(text = "Working on it", style = AppTextStyle.Default)
                    }
                }

                state.message?.let { message ->
                    ThemedText(text = message, style = AppTextStyle.Default)
                }

                state.error?.let { error ->
                    ThemedText(text = error, style = AppTextStyle.Default)
                }

                state.lastExportAt?.let { exportedAt ->
                    ThemedText(
                        text = "Last backup: ${formatTimestamp(exportedAt)}",
                        style = AppTextStyle.Small,
                        color = appColors.textSecondary,
                    )
                }

                if (!state.busy && state.message == null && state.error == null && state.lastExportAt == null) {
                    ThemedText(
                        text = "No backup was saved yet.",
                        style = AppTextStyle.Default,
                        color = appColors.textSecondary,
                    )
                }
            }
        }

        item {
            SectionCard(title = "Good to know") {
                ThemedText(
                    text = "• Uninstalling the app does not delete a backup file, it stays in " +
                        "your files.\n" +
                        "• Install the app again, open this screen and pick the backup file to " +
                        "get all your data back.\n" +
                        "• Restoring replaces everything that is in the app right now.",
                    style = AppTextStyle.Default,
                    color = appColors.textSecondary,
                )
            }
        }
    }

    state.pendingRestore?.let { backup ->
        ConfirmDialog(
            title = "Restore this backup?",
            text = "The backup from ${state.pendingSource ?: "the file"} holds " +
                "${backup.members.size} members, ${backup.meals.size} meals, " +
                "${backup.expenses.size} expenses and ${backup.announcements.size} announcements. " +
                "Everything that is in the app right now is replaced with it.",
            confirmLabel = "Restore",
            onConfirm = { viewModel.restore() },
            onDismiss = { viewModel.cancelRestore() },
        )
    }
}

/** A row that starts an action. */
@Composable
private fun BackupAction(
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

/** The name of a new backup file. */
private fun defaultFileName(): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.getDefault()).format(Date())
    return "mess-backup-$stamp.json"
}

/** Formats a timestamp for the backup screen. */
private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
