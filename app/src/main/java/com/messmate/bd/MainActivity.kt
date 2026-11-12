package com.messmate.bd

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.messmate.bd.auth.GoogleAuthManager
import com.messmate.bd.core.locale.LocaleController
import com.messmate.bd.ui.navigation.MessMateNavHost
import com.messmate.bd.ui.theme.MessMateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single activity of the app.
 *
 * It hosts the whole navigation graph (onboarding → dashboard → bazar →
 * utilities → members → statement → audit → settings).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var googleAuthManager: GoogleAuthManager

    /**
     * The base context carries the selected language – the whole app is then
     * rendered in English or বাংলা.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Keep the platform locale list in sync with the stored preference.
        LocaleController(this).applyToPlatform()

        setContent {
            MessMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MessMateNavHost()
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
