package ru.andrew.application.ui.viewmodel

/**
 * Состояние пользовательского интерфейса для экрана статистики.
 *
 * @property isLoading Флаг загрузки данных.
 * @property revenue Общая выручка (сумма финальных цен выполненных заявок).
 * @property acceptedCount Количество принятых (созданных) заявок за период.
 * @property completedCount Количество выполненных заявок за период.
 * @property cancelledCount Количество отмененных заявок за период.
 * @property successRate Процент успешности выполненных заявок от общего числа закрытых (выполненные + отмененные).
 * @property revenueChartPoints Список точек для графика выручки, где первый элемент пары — подпись (X), а второй — значение (Y).
 * @property cancelReasons Список причин отмен с количеством заявок по каждой причине, отсортированный по убыванию.
 */
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
