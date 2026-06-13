package ru.andrew.application.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun AndrewApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
