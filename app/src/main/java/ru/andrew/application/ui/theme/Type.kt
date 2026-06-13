package ru.andrew.application.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultTypography = Typography()

internal val AppTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(
        fontSize = 58.sp,
        lineHeight = 66.sp,
        fontWeight = FontWeight.SemiBold
    ),
    displayMedium = DefaultTypography.displayMedium.copy(
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.SemiBold
    ),
    displaySmall = DefaultTypography.displaySmall.copy(
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = DefaultTypography.headlineLarge.copy(
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = DefaultTypography.titleSmall.copy(
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = DefaultTypography.bodySmall.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelLarge = DefaultTypography.labelLarge.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    )
)
