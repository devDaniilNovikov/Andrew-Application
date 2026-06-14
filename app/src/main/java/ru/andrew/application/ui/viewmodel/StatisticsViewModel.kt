package ru.andrew.application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.data.util.TimeProvider
import ru.andrew.application.di.DependencyProvider
import ru.andrew.application.domain.RequestStatus
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * ViewModel для расчета реактивной статистики по выполненным и созданным заявкам.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val requestRepository: RequestRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatisticsPeriod.WEEK)
    val selectedPeriod: StateFlow<StatisticsPeriod> = _selectedPeriod.asStateFlow()

    val uiState: StateFlow<StatisticsUiState> = _selectedPeriod
        .flatMapLatest { period ->
            val now = timeProvider.getNow()
            val start = getStartOfPeriod(period, now)
            val end = getEndOfPeriod(period, now)

            val createdRequestsFlow = requestRepository.getCreatedRequestsInPeriod(start, end)
            val closedRequestsFlow = requestRepository.getClosedRequestsInPeriod(start, end)

            createdRequestsFlow.combine(closedRequestsFlow) { createdRequests, closedRequests ->
                calculateStatistics(period, createdRequests, closedRequests)
            }
        }
        .catch {
            emit(StatisticsUiState(isLoading = false))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsUiState(isLoading = true)
        )

    fun selectPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
    }

    private fun getStartOfPeriod(period: StatisticsPeriod, now: LocalDateTime): LocalDateTime {
        return when (period) {
            StatisticsPeriod.DAY -> now.toLocalDate().atStartOfDay()
            StatisticsPeriod.WEEK -> {
                val dayOfWeek = now.dayOfWeek.value // 1 (Пн) - 7 (Вс)
                now.toLocalDate().minusDays((dayOfWeek - 1).toLong()).atStartOfDay()
            }
            StatisticsPeriod.MONTH -> now.toLocalDate().withDayOfMonth(1).atStartOfDay()
            StatisticsPeriod.YEAR -> now.toLocalDate().withDayOfYear(1).atStartOfDay()
        }
    }

    private fun getEndOfPeriod(period: StatisticsPeriod, now: LocalDateTime): LocalDateTime {
        return when (period) {
            StatisticsPeriod.DAY -> now.toLocalDate().atTime(LocalTime.MAX)
            StatisticsPeriod.WEEK -> {
                val dayOfWeek = now.dayOfWeek.value
                now.toLocalDate().plusDays((7 - dayOfWeek).toLong()).atTime(LocalTime.MAX)
            }
            StatisticsPeriod.MONTH -> now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(LocalTime.MAX)
            StatisticsPeriod.YEAR -> now.toLocalDate().withMonth(12).withDayOfMonth(31).atTime(LocalTime.MAX)
        }
    }

    private fun calculateStatistics(
        period: StatisticsPeriod,
        createdRequests: List<Request>,
        closedRequests: List<Request>
    ): StatisticsUiState {
        val completedRequests = closedRequests.filter { it.status == RequestStatus.COMPLETED }
        val cancelledRequests = closedRequests.filter { it.status == RequestStatus.CANCELLED }

        val revenue = completedRequests.sumOf { it.finalPrice ?: 0.0 }
        val acceptedCount = createdRequests.size
        val completedCount = completedRequests.size
        val cancelledCount = cancelledRequests.size

        val totalClosed = completedCount + cancelledCount
        val successRate = if (totalClosed > 0) {
            (completedCount.toFloat() / totalClosed.toFloat()) * 100f
        } else {
            0f
        }

        val revenueChartPoints = getRevenueChartPoints(period, completedRequests)

        val cancelReasons = cancelledRequests
            .filter { !it.cancelReason.isNullOrBlank() }
            .groupBy { it.cancelReason!! }
            .map { Pair(it.key, it.value.size) }
            .sortedByDescending { it.second }

        return StatisticsUiState(
            isLoading = false,
            revenue = revenue,
            acceptedCount = acceptedCount,
            completedCount = completedCount,
            cancelledCount = cancelledCount,
            successRate = successRate,
            revenueChartPoints = revenueChartPoints,
            cancelReasons = cancelReasons
        )
    }

    private fun getRevenueChartPoints(
        period: StatisticsPeriod,
        completedRequests: List<Request>
    ): List<Pair<String, Float>> {
        return when (period) {
            StatisticsPeriod.DAY -> {
                val intervals = listOf("00:00", "06:00", "12:00", "18:00")
                val points = intervals.associateWith { 0f }.toMutableMap()
                completedRequests.forEach { req ->
                    val closedAt = req.closedAt ?: return@forEach
                    val hour = closedAt.hour
                    val interval = when {
                        hour < 6 -> "00:00"
                        hour < 12 -> "06:00"
                        hour < 18 -> "12:00"
                        else -> "18:00"
                    }
                    points[interval] = (points[interval] ?: 0f) + (req.finalPrice?.toFloat() ?: 0f)
                }
                intervals.map { Pair(it, points[it] ?: 0f) }
            }
            StatisticsPeriod.WEEK -> {
                val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                val points = days.associateWith { 0f }.toMutableMap()
                completedRequests.forEach { req ->
                    val closedAt = req.closedAt ?: return@forEach
                    val dayIndex = closedAt.dayOfWeek.value - 1
                    if (dayIndex in days.indices) {
                        val dayName = days[dayIndex]
                        points[dayName] = (points[dayName] ?: 0f) + (req.finalPrice?.toFloat() ?: 0f)
                    }
                }
                days.map { Pair(it, points[it] ?: 0f) }
            }
            StatisticsPeriod.MONTH -> {
                val weeks = listOf("Нед 1", "Нед 2", "Нед 3", "Нед 4")
                val points = weeks.associateWith { 0f }.toMutableMap()
                completedRequests.forEach { req ->
                    val closedAt = req.closedAt ?: return@forEach
                    val day = closedAt.dayOfMonth
                    val weekName = when {
                        day <= 7 -> "Нед 1"
                        day <= 14 -> "Нед 2"
                        day <= 21 -> "Нед 3"
                        else -> "Нед 4"
                    }
                    points[weekName] = (points[weekName] ?: 0f) + (req.finalPrice?.toFloat() ?: 0f)
                }
                weeks.map { Pair(it, points[it] ?: 0f) }
            }
            StatisticsPeriod.YEAR -> {
                val months = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
                val points = months.associateWith { 0f }.toMutableMap()
                completedRequests.forEach { req ->
                    val closedAt = req.closedAt ?: return@forEach
                    val monthIndex = closedAt.monthValue - 1
                    if (monthIndex in months.indices) {
                        val monthName = months[monthIndex]
                        points[monthName] = (points[monthName] ?: 0f) + (req.finalPrice?.toFloat() ?: 0f)
                    }
                }
                months.map { Pair(it, points[it] ?: 0f) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val repository = DependencyProvider.provideRequestRepository(application)
                val timeProvider = DependencyProvider.provideTimeProvider()
                return StatisticsViewModel(repository, timeProvider) as T
            }
        }
    }
}
