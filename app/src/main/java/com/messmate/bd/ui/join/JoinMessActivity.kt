package com.messmate.bd.ui.join

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messmate.bd.MainActivity
import com.messmate.bd.ui.components.ErrorBanner
import com.messmate.bd.ui.theme.MessMateTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for the deep link `messmate://join/<spreadsheetId>`.
 *
 * The QR code of a mess contains exactly this URL, so scanning it opens the
 * app straight into the "join this mess" flow.
 */
@AndroidEntryPoint
class JoinMessActivity : ComponentActivity() {

    private val viewModel: JoinMessActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The id comes either from the deep link or from the extra of the QR
        // scanner activity.
        val sheetId = intent.dataString
            ?: intent.getStringExtra(EXTRA_SHEET_ID)
        viewModel.join(sheetId)

        setContent {
            MessMateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    // The mess was joined – continue in the app.
                    LaunchedEffect(state.joined) {
                        if (state.joined) {
                            startActivity(
                                Intent(this@JoinMessActivity, MainActivity::class.java)
                                    .addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                                    ),
                            )
                            finish()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when {
                            state.joined -> {
                                Text(
                                    text = "Joined!",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }

                            state.error != null -> {
                                ErrorBanner(message = state.error.orEmpty())
                            }

                            else -> CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SHEET_ID = "sheet_id"
    }
}
