package ru.andrew.application.ui.viewmodel

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val revenue: Double = 0.0,
    val acceptedCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0,
    val successRate: Float = 0f,
    val revenueChartPoints: List<Pair<String, Float>> = emptyList(),
    val cancelReasons: List<Pair<String, Int>> = emptyList()
)
