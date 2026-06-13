package ru.andrew.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.SystemBarStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.andrew.application.ui.theme.AndrewApplicationTheme
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.ui.viewmodel.ThemeViewModel
import ru.andrew.application.ui.screens.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val themeViewModel = ViewModelProvider(this, ThemeViewModel.Factory)[ThemeViewModel::class.java]
        
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                themeViewModel.themeState.collect { theme ->
                    val darkTheme = when (theme) {
                        AppTheme.LIGHT -> false
                        AppTheme.DARK -> true
                        AppTheme.SYSTEM -> {
                            val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                            uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                        }
                    }
                    
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
                }
            }
        }
        
        setContent {
            val currentTheme by themeViewModel.themeState.collectAsStateWithLifecycle()
            
            AndrewApplicationTheme(theme = currentTheme) {
                MainScreen(
                    currentTheme = currentTheme,
                    onThemeSelected = { newTheme ->
                        themeViewModel.selectTheme(newTheme)
                    }
                )
            }
        }
    }
}
