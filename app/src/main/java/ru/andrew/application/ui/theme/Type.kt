package ru.andrew.application.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultTypography = Typography()

internal val AppTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(
        fontSize = 60.sp,
        lineHeight = 68.sp,
        fontWeight = FontWeight.Bold
    ),
    displayMedium = DefaultTypography.displayMedium.copy(
        fontSize = 50.sp,
        lineHeight = 58.sp,
        fontWeight = FontWeight.Bold
    ),
    displaySmall = DefaultTypography.displaySmall.copy(
        fontSize = 42.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineLarge = DefaultTypography.headlineLarge.copy(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = DefaultTypography.titleSmall.copy(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    bodySmall = DefaultTypography.bodySmall.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = DefaultTypography.labelLarge.copy(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    )
)
