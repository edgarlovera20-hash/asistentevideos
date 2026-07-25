package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.ui.MainScreen
import com.example.ui.theme.HeavenlyAIMeetingTheme

class MainActivity : ComponentActivity() {

    private val deepLinkUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDeepLink(intent)

        setContent {
            HeavenlyAIMeetingTheme {
                MainScreen(
                    initialDeepLinkUrl = deepLinkUrlState.value,
                    onDeepLinkConsumed = { deepLinkUrlState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val dataStr = intent?.dataString
        if (!dataStr.isNullOrEmpty()) {
            deepLinkUrlState.value = dataStr
        }
    }
}

