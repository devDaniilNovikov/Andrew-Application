package ru.andrew.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.activity.SystemBarStyle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import ru.andrew.application.ui.theme.AndrewApplicationTheme
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.ui.viewmodel.ThemeViewModel
import ru.andrew.application.ui.screens.MainScreen
import ru.andrew.application.notifications.NotificationHelper
import android.content.Intent
import android.util.Log

class MainActivity : ComponentActivity() {

    /** Id заявки из тапа по уведомлению; сбрасывается, как только экран списка его обработал. */
    private val pendingDeepLinkRequestId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // При пересоздании (поворот экрана, восстановление процесса) система отдаёт тот же Intent,
        // что и при запуске: повторно открывать по нему карточку не нужно.
        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        val themeViewModel = ViewModelProvider(this, ThemeViewModel.Factory)[ThemeViewModel::class.java]
        
        val initialTheme = themeViewModel.themeState.value
        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val initialDarkTheme = when (initialTheme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.SYSTEM -> isSystemDark
        }
        
        if (initialDarkTheme) {
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
        
        setContent {
            val currentTheme by themeViewModel.themeState.collectAsStateWithLifecycle()
            val deepLinkId by pendingDeepLinkRequestId
            val darkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            
            androidx.compose.runtime.DisposableEffect(darkTheme) {
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
                        pendingDeepLinkRequestId.value = null
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
        // Activity экспортирована (лаунчер), поэтому extras может прислать любое приложение.
        // Худшее, что даёт такой Intent, — открыть карточку внутри нашего же UI; данные наружу не уходят.
        // Повреждённые extras (например, неизвестный Parcelable на старых Android) не должны ронять приложение.
        val requestId = try {
            intent?.getLongExtra(NotificationHelper.EXTRA_REQUEST_ID, -1L) ?: -1L
        } catch (e: RuntimeException) {
            Log.w(TAG, "Ignoring intent with unreadable extras", e)
            -1L
        }
        if (requestId > 0L) {
            pendingDeepLinkRequestId.value = requestId
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
