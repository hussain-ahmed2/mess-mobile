package com.messmate.bd.ui.join

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for the deep link `messmate://join/<spreadsheetId>`.
 *
 * The QR code of a mess contains exactly this URL, so scanning it opens the
 * app straight into the "join this mess" flow. The real implementation (sheet
 * picker, member matching, sheet read access) is part of the UI layer.
 */
@AndroidEntryPoint
class JoinMessActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    Text("Join Mess")
                }
            }
        }
    }
}
