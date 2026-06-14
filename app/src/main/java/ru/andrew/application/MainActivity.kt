package ru.andrew.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.activity.SystemBarStyle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import ru.andrew.application.ui.theme.AndrewApplicationTheme
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.ui.viewmodel.ThemeViewModel
import ru.andrew.application.ui.screens.MainScreen
import android.content.Intent

class MainActivity : ComponentActivity() {

    private val deepLinkRequestId = mutableStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        
        val themeViewModel = ViewModelProvider(this, ThemeViewModel.Factory)[ThemeViewModel::class.java]
        
        setContent {
            val currentTheme by themeViewModel.theme.collectAsStateWithLifecycle()
            val deepLinkId by deepLinkRequestId
            val darkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            
            DisposableEffect(darkTheme) {
                if (darkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    )
                }
                onDispose {}
            }
            
            AndrewApplicationTheme(theme = currentTheme) {
                MainScreen(
                    currentTheme = currentTheme,
                    deepLinkRequestId = deepLinkId,
                    onThemeSelected = { newTheme ->
                        themeViewModel.selectTheme(newTheme)
                    },
                    onDeepLinkHandled = {
                        deepLinkRequestId.value = -1L
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val requestId = intent?.getLongExtra("requestId", -1L) ?: -1L
        val token = intent?.getStringExtra("secure_token")
        
        if (requestId != -1L) {
            // Валидируем одноразовый токен безопасности перед раскрытием деталей заявки
            if (ru.andrew.application.notifications.SecureTokenManager.validateAndConsumeToken(requestId, token)) {
                deepLinkRequestId.value = requestId
            }
            intent?.removeExtra("requestId")
            intent?.removeExtra("secure_token")
        }
    }
}
