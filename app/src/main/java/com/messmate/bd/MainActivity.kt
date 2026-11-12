package com.messmate.bd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.messmate.bd.auth.GoogleAuthManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single activity of the app.
 *
 * The real navigation graph (onboarding → dashboard → bazar → utilities →
 * members → statement → audit) is implemented in the UI layer; this shell only
 * makes sure the project compiles and runs with the data layer in place.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var googleAuthManager: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppShell()
                }
            }
        }
    }

    /**
     * The Google scope consent screen is started with
     * `startIntentSenderForResult` – its result is forwarded to the auth
     * manager, which completes the pending access token request.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googleAuthManager.onActivityResult(requestCode, data)
    }
}

/**
 * Placeholder screen – replaced by the navigation graph in the UI layer.
 */
@Composable
private fun AppShell() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "MessMate BD",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Data layer ready – UI layer comes next.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppShellPreview() {
    MaterialTheme {
        AppShell()
    }
}
