package ru.andrew.application.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppOnPrimaryContainer,
    inversePrimary = AppInversePrimary,
    primaryFixed = AppPrimaryFixed,
    primaryFixedDim = AppPrimaryFixedDim,
    onPrimaryFixed = AppOnPrimaryFixed,
    onPrimaryFixedVariant = AppOnPrimaryFixedVariant,
    secondary = AppSecondary,
    onSecondary = AppOnSecondary,
    secondaryContainer = AppSecondaryContainer,
    onSecondaryContainer = AppOnSecondaryContainer,
    secondaryFixed = AppSecondaryFixed,
    secondaryFixedDim = AppSecondaryFixedDim,
    onSecondaryFixed = AppOnSecondaryFixed,
    onSecondaryFixedVariant = AppOnSecondaryFixedVariant,
    tertiary = AppTertiary,
    onTertiary = AppOnTertiary,
    tertiaryContainer = AppTertiaryContainer,
    onTertiaryContainer = AppOnTertiaryContainer,
    tertiaryFixed = AppTertiaryFixed,
    tertiaryFixedDim = AppTertiaryFixedDim,
    onTertiaryFixed = AppOnTertiaryFixed,
    onTertiaryFixedVariant = AppOnTertiaryFixedVariant,
    error = AppError,
    onError = AppOnError,
    errorContainer = AppErrorContainer,
    onErrorContainer = AppOnErrorContainer,
    background = AppBackground,
    onBackground = AppOnBackground,
    surface = AppBackground,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppOnSurfaceVariant,
    inverseSurface = AppInverseSurface,
    inverseOnSurface = AppInverseOnSurface,
    outline = AppOutline,
    outlineVariant = AppOutlineVariant,
    scrim = AppScrim,
    surfaceTint = AppSurfaceTint,
    surfaceContainerLowest = AppSurfaceContainerLowest,
    surfaceContainerLow = AppSurfaceContainerLow,
    surfaceContainer = AppSurfaceContainer,
    surfaceContainerHigh = AppSurfaceContainerHigh,
    surfaceContainerHighest = AppSurfaceContainerHighest,
    surfaceDim = AppSurfaceDim,
    surfaceBright = AppSurfaceBright
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAppPrimary,
    onPrimary = DarkAppOnPrimary,
    primaryContainer = DarkAppPrimaryContainer,
    onPrimaryContainer = DarkAppOnPrimaryContainer,
    inversePrimary = AppInversePrimary,
    primaryFixed = DarkAppPrimary,
    primaryFixedDim = DarkAppPrimary,
    onPrimaryFixed = DarkAppOnPrimary,
    onPrimaryFixedVariant = DarkAppPrimary,
    secondary = DarkAppSecondary,
    onSecondary = DarkAppOnSecondary,
    secondaryContainer = DarkAppSecondaryContainer,
    onSecondaryContainer = DarkAppOnSecondaryContainer,
    secondaryFixed = DarkAppSecondary,
    secondaryFixedDim = DarkAppSecondary,
    onSecondaryFixed = DarkAppOnSecondary,
    onSecondaryFixedVariant = DarkAppSecondary,
    tertiary = DarkAppTertiary,
    onTertiary = DarkAppOnTertiary,
    tertiaryContainer = DarkAppTertiaryContainer,
    onTertiaryContainer = DarkAppOnTertiaryContainer,
    tertiaryFixed = DarkAppTertiary,
    tertiaryFixedDim = DarkAppTertiary,
    onTertiaryFixed = DarkAppOnTertiary,
    onTertiaryFixedVariant = DarkAppTertiary,
    error = DarkAppError,
    onError = DarkAppOnError,
    errorContainer = DarkAppErrorContainer,
    onErrorContainer = DarkAppOnErrorContainer,
    background = DarkAppBackground,
    onBackground = DarkAppOnBackground,
    surface = DarkAppBackground,
    onSurface = DarkAppOnSurface,
    surfaceVariant = DarkAppSurfaceVariant,
    onSurfaceVariant = DarkAppOnSurfaceVariant,
    inverseSurface = DarkAppInverseSurface,
    inverseOnSurface = DarkAppInverseOnSurface,
    outline = DarkAppOutline,
    outlineVariant = DarkAppOutlineVariant,
    scrim = DarkAppScrim,
    surfaceTint = DarkAppSurfaceTint,
    surfaceContainerLowest = DarkAppSurfaceContainerLowest,
    surfaceContainerLow = DarkAppSurfaceContainerLow,
    surfaceContainer = DarkAppSurfaceContainer,
    surfaceContainerHigh = DarkAppSurfaceContainerHigh,
    surfaceContainerHighest = DarkAppSurfaceContainerHighest,
    surfaceDim = DarkAppSurfaceDim,
    surfaceBright = DarkAppSurfaceBright
)

@Composable
fun AndrewApplicationTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}
