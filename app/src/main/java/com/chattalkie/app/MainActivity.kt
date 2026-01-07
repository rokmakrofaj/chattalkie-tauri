package com.chattalkie.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.*
import com.chattalkie.app.ui.screens.MainScreen
import com.chattalkie.app.ui.theme.ChatTalkieTheme

class MainActivity : ComponentActivity() {
    private var inviteToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ SADECE RENK VER, INSET'E KARIŞMA
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        // Handle deep link from intent
        handleIntent(intent)

        // Initialize Data Layer
        com.chattalkie.app.data.ChatRepository.initialize(applicationContext)

        setContent {
            ChatTalkieTheme {
                // ❌ BURADA ASLA padding / insets YOK
                MainScreen(inviteToken = inviteToken)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "chattalkie" && data.host == "join") {
            // Extract token from chattalkie://join/{token}
            val token = data.pathSegments.firstOrNull()
            inviteToken = token
        }
    }
}
