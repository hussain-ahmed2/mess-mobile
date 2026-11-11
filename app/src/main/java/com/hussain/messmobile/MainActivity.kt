package com.hussain.messmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hussain.messmobile.ui.components.SplashOverlay
import com.hussain.messmobile.ui.navigation.MessMobileApp
import com.hussain.messmobile.ui.theme.MessMobileTheme

class MainActivity : ComponentActivity() {

    /**
     * The system splash screen stays on screen until the Compose splash overlay is ready.
     */
    private val contentReady = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !contentReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MessMobileTheme {
                var showSplashOverlay by rememberSaveable { mutableStateOf(true) }

                // Releases the system splash screen as soon as the first frame is drawn. This
                // also runs when the activity is recreated and the overlay is not shown again.
                LaunchedEffect(Unit) { contentReady.value = true }

                Box(modifier = Modifier.fillMaxSize()) {
                    MessMobileApp()

                    if (showSplashOverlay) {
                        SplashOverlay(
                            onReady = { contentReady.value = true },
                            onFinished = { showSplashOverlay = false },
                        )
                    }
                }
            }
        }
    }
}
