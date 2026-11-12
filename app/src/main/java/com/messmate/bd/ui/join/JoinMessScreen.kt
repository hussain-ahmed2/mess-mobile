package com.messmate.bd.ui.join

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.messmate.bd.R
import com.messmate.bd.ui.components.ErrorBanner
import com.messmate.bd.ui.components.LabeledField
import com.messmate.bd.ui.components.SectionCard

/**
 * The "join an existing mess" screen.
 *
 * Three ways to identify the mess:
 * * paste the sheet id,
 * * pick the sheet from Drive (the system file picker – this also grants the
 *   app the `drive.file` access to that file),
 * * scan the QR code of the manager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinMessScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinMessViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // `stringResource` must be read in the composable scope, not in the click
    // listener.
    val scanPrompt = stringResource(R.string.scan_qr_hint)

    LaunchedEffect(state.joined) {
        if (state.joined) onBack()
    }

    // The system file picker – the returned uri contains the spreadsheet id.
    val pickDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onSpreadsheetIdDetected(it.toString()) }
    }

    // The QR scanner of the manager.
    val scanQr = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.onSpreadsheetIdDetected(it) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_join_mess)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.error != null) {
                ErrorBanner(message = state.error.orEmpty())
            }

            SectionCard(title = stringResource(R.string.join_sheet_id)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(
                        value = state.sheetId,
                        onValueChange = viewModel::onSheetIdChange,
                        label = stringResource(R.string.join_sheet_id),
                        placeholder = stringResource(R.string.join_sheet_id_hint),
                    )

                    OutlinedButton(
                        onClick = {
                            pickDocument.launch(
                                arrayOf("application/vnd.google-apps.spreadsheet"),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Text(
                            text = stringResource(R.string.join_pick_from_drive),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val options = ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt(scanPrompt)
                                .setBeepEnabled(false)
                                .setOrientationLocked(false)
                                .setBarcodeImageEnabled(false)
                            scanQr.launch(options)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                        Text(
                            text = stringResource(R.string.join_scan_qr),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::join,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.join_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
