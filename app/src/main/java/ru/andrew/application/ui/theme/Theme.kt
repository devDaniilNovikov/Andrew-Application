package ru.andrew.application.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    tertiary = AppTertiary,
    background = AppBackground,
    surface = AppBackground,
    surfaceVariant = AppSurfaceVariant
)

@Composable
fun AndrewApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}

