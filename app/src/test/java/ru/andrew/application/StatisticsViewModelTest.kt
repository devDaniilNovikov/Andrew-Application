package ru.andrew.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.data.util.TimeProvider
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.ui.viewmodel.StatisticsPeriod
import ru.andrew.application.ui.viewmodel.StatisticsViewModel
import java.time.LocalDateTime

/**
 * Модульные тесты для проверки реактивного расчета статистики,
 * разделения периодов и группировки выручки во вьюмодели StatisticsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRequestRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var viewModel: StatisticsViewModel

    // Воскресенье, 14 июня 2026 года, 12:00:00
    private val baseDateTime = LocalDateTime.of(2026, 6, 14, 12, 0, 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTimeProvider = FakeTimeProvider(baseDateTime)
        fakeRepository = FakeRequestRepository()
        viewModel = StatisticsViewModel(fakeRepository, fakeTimeProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultInitialization() = runTest {
        // Запускаем сбор у StateFlow, чтобы активировать WhileSubscribed
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        // По умолчанию должен быть выбран период WEEK
        assertEquals(StatisticsPeriod.WEEK, viewModel.selectedPeriod.value)
        
        // Начальное состояние isLoading должно смениться на false, когда данные репозитория пустые
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0.0, state.revenue, 0.0)
        assertEquals(0, state.acceptedCount)
        assertEquals(0, state.completedCount)
        assertEquals(0, state.cancelledCount)
        assertEquals(0f, state.successRate)
        assertTrue(state.revenueChartPoints.all { it.second == 0f })

        collectJob.cancel()
    }

    @Test
    fun testSelectPeriod() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.selectPeriod(StatisticsPeriod.MONTH)
        runCurrent()
        assertEquals(StatisticsPeriod.MONTH, viewModel.selectedPeriod.value)

        viewModel.selectPeriod(StatisticsPeriod.YEAR)
        runCurrent()
        assertEquals(StatisticsPeriod.YEAR, viewModel.selectedPeriod.value)

        viewModel.selectPeriod(StatisticsPeriod.DAY)
        runCurrent()
        assertEquals(StatisticsPeriod.DAY, viewModel.selectedPeriod.value)

        collectJob.cancel()
    }

    @Test
    fun testCalculateStatistics_withDiverseRequests() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        // Создаем набор тестовых заявок
        val created = listOf(
            createRequest(id = 1, createdAt = baseDateTime.minusHours(5)),
            createRequest(id = 2, createdAt = baseDateTime.minusHours(2)),
            createRequest(id = 3, createdAt = baseDateTime.minusHours(1))
        )

        val closed = listOf(
            // Выполненная заявка с выручкой 2500
            createRequest(
                id = 1,
                status = RequestStatus.COMPLETED,
                finalPrice = 2500.0,
                closedAt = baseDateTime.minusHours(4)
            ),
            // Выполненная заявка с выручкой 1500
            createRequest(
                id = 2,
                status = RequestStatus.COMPLETED,
                finalPrice = 1500.0,
                closedAt = baseDateTime.minusHours(2)
            ),
            // Отмененная заявка с причиной "Клиент отказался"
            createRequest(
                id = 3,
                status = RequestStatus.CANCELLED,
                cancelReason = "Клиент отказался",
                closedAt = baseDateTime.minusHours(1)
            ),
            // Активная заявка (должна игнорироваться при расчете выручки и закрытых заявок)
            createRequest(
                id = 4,
                status = RequestStatus.ACTIVE,
                closedAt = null
            )
        )

        fakeRepository.updateData(created, closed)
        runCurrent()

        val state = viewModel.uiState.value

        // Выручка: 2500 + 1500 = 4000
        assertEquals(4000.0, state.revenue, 0.0)
        
        // Принято/создано: 3
        assertEquals(3, state.acceptedCount)
        
        // Выполнено: 2
        assertEquals(2, state.completedCount)
        
        // Отменено: 1
        assertEquals(1, state.cancelledCount)

        // Процент успешности: 2 / (2 + 1) * 100% = 66.666...%
        assertEquals(66.66667f, state.successRate, 0.001f)

        collectJob.cancel()
    }

    @Test
    fun testCalculateStatistics_zeroClosedRequests_successRateIsZero() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        // Нет закрытых заявок за период
        fakeRepository.updateData(emptyList(), emptyList())
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(0f, state.successRate)

        collectJob.cancel()
    }

    @Test
    fun testCancelReasonsGroupingAndSorting() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val closed = listOf(
            createRequest(id = 1, status = RequestStatus.CANCELLED, cancelReason = "Дорого", closedAt = baseDateTime),
            createRequest(id = 2, status = RequestStatus.CANCELLED, cancelReason = "Дорого", closedAt = baseDateTime),
            createRequest(id = 3, status = RequestStatus.CANCELLED, cancelReason = "Дорого", closedAt = baseDateTime),
            createRequest(id = 4, status = RequestStatus.CANCELLED, cancelReason = "Долго ждать", closedAt = baseDateTime),
            createRequest(id = 5, status = RequestStatus.CANCELLED, cancelReason = "Долго ждать", closedAt = baseDateTime),
            createRequest(id = 6, status = RequestStatus.CANCELLED, cancelReason = "Мастер не приехал", closedAt = baseDateTime),
            // Пустая или пустая причина (должна игнорироваться)
            createRequest(id = 7, status = RequestStatus.CANCELLED, cancelReason = "", closedAt = baseDateTime),
            createRequest(id = 8, status = RequestStatus.CANCELLED, cancelReason = null, closedAt = baseDateTime)
        )

        fakeRepository.updateData(emptyList(), closed)
        runCurrent()

        val state = viewModel.uiState.value
        val reasons = state.cancelReasons

        // Должно быть ровно 3 валидных причины отмен, отсортированных по убыванию
        assertEquals(3, reasons.size)
        
        assertEquals("Дорого", reasons[0].first)
        assertEquals(3, reasons[0].second)

        assertEquals("Долго ждать", reasons[1].first)
        assertEquals(2, reasons[1].second)

        assertEquals("Мастер не приехал", reasons[2].first)
        assertEquals(1, reasons[2].second)

        collectJob.cancel()
    }

    @Test
    fun testRevenueGrouping_DAY() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.selectPeriod(StatisticsPeriod.DAY)
        runCurrent()

        val closed = listOf(
            // Ночь (00:00 - 05:59) -> интервал "00:00"
            createRequest(id = 1, status = RequestStatus.COMPLETED, finalPrice = 100.0, closedAt = baseDateTime.withHour(3)),
            // Утро (06:00 - 11:59) -> интервал "06:00"
            createRequest(id = 2, status = RequestStatus.COMPLETED, finalPrice = 200.0, closedAt = baseDateTime.withHour(7)),
            // День (12:00 - 17:59) -> интервал "12:00"
            createRequest(id = 3, status = RequestStatus.COMPLETED, finalPrice = 300.0, closedAt = baseDateTime.withHour(14)),
            // Вечер (18:00 - 23:59) -> интервал "18:00"
            createRequest(id = 4, status = RequestStatus.COMPLETED, finalPrice = 400.0, closedAt = baseDateTime.withHour(20))
        )

        fakeRepository.updateData(emptyList(), closed)
        runCurrent()

        val state = viewModel.uiState.value
        val points = state.revenueChartPoints

        assertEquals(4, points.size)
        assertEquals("00:00" to 100f, points[0])
        assertEquals("06:00" to 200f, points[1])
        assertEquals("12:00" to 300f, points[2])
        assertEquals("18:00" to 400f, points[3])

        collectJob.cancel()
    }

    @Test
    fun testRevenueGrouping_WEEK() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.selectPeriod(StatisticsPeriod.WEEK)
        runCurrent()

        // Наша базовая дата — воскресенье, 14 июня 2026 года
        val closed = listOf(
            // Понедельник
            createRequest(id = 1, status = RequestStatus.COMPLETED, finalPrice = 500.0, closedAt = baseDateTime.withDayOfMonth(8)),
            // Среда
            createRequest(id = 2, status = RequestStatus.COMPLETED, finalPrice = 1000.0, closedAt = baseDateTime.withDayOfMonth(10)),
            // Воскресенье (базовый день)
            createRequest(id = 3, status = RequestStatus.COMPLETED, finalPrice = 1500.0, closedAt = baseDateTime)
        )

        fakeRepository.updateData(emptyList(), closed)
        runCurrent()

        val state = viewModel.uiState.value
        val points = state.revenueChartPoints

        // Дни недели: Пн, Вт, Ср, Чт, Пт, Сб, Вс
        assertEquals(7, points.size)
        assertEquals("Пн" to 500f, points[0])
        assertEquals("Вт" to 0f, points[1])
        assertEquals("Ср" to 1000f, points[2])
        assertEquals("Чт" to 0f, points[3])
        assertEquals("Пт" to 0f, points[4])
        assertEquals("Сб" to 0f, points[5])
        assertEquals("Вс" to 1500f, points[6])

        collectJob.cancel()
    }

    @Test
    fun testRevenueGrouping_MONTH() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.selectPeriod(StatisticsPeriod.MONTH)
        runCurrent()

        val closed = listOf(
            // Неделя 1 (1-7 дни)
            createRequest(id = 1, status = RequestStatus.COMPLETED, finalPrice = 100.0, closedAt = baseDateTime.withDayOfMonth(3)),
            // Неделя 2 (8-14 дни)
            createRequest(id = 2, status = RequestStatus.COMPLETED, finalPrice = 200.0, closedAt = baseDateTime.withDayOfMonth(10)),
            // Неделя 3 (15-21 дни)
            createRequest(id = 3, status = RequestStatus.COMPLETED, finalPrice = 300.0, closedAt = baseDateTime.withDayOfMonth(17)),
            // Неделя 4 (22+ дни)
            createRequest(id = 4, status = RequestStatus.COMPLETED, finalPrice = 400.0, closedAt = baseDateTime.withDayOfMonth(25))
        )

        fakeRepository.updateData(emptyList(), closed)
        runCurrent()

        val state = viewModel.uiState.value
        val points = state.revenueChartPoints

        assertEquals(4, points.size)
        assertEquals("Нед 1" to 100f, points[0])
        assertEquals("Нед 2" to 200f, points[1])
        assertEquals("Нед 3" to 300f, points[2])
        assertEquals("Нед 4" to 400f, points[3])

        collectJob.cancel()
    }

    @Test
    fun testRevenueGrouping_YEAR() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.selectPeriod(StatisticsPeriod.YEAR)
        runCurrent()

        val closed = listOf(
            // Январь
            createRequest(id = 1, status = RequestStatus.COMPLETED, finalPrice = 1000.0, closedAt = baseDateTime.withMonth(1)),
            // Июнь
            createRequest(id = 2, status = RequestStatus.COMPLETED, finalPrice = 2000.0, closedAt = baseDateTime.withMonth(6)),
            // Декабрь
            createRequest(id = 3, status = RequestStatus.COMPLETED, finalPrice = 3000.0, closedAt = baseDateTime.withMonth(12))
        )

        fakeRepository.updateData(emptyList(), closed)
        runCurrent()

        val state = viewModel.uiState.value
        val points = state.revenueChartPoints

        assertEquals(12, points.size)
        assertEquals("Янв" to 1000f, points[0])
        assertEquals("Июн" to 2000f, points[5])
        assertEquals("Дек" to 3000f, points[11])

        // Остальные месяцы должны быть по нулям
        assertEquals("Фев" to 0f, points[1])
        assertEquals("Мар" to 0f, points[2])

        collectJob.cancel()
    }

    private fun createRequest(
        id: Long,
        status: RequestStatus = RequestStatus.ACTIVE,
        finalPrice: Double? = null,
        cancelReason: String? = null,
        createdAt: LocalDateTime = baseDateTime,
        closedAt: LocalDateTime? = null
    ): Request {
        return Request(
            id = id,
            title = "Заявка $id",
            phone = "123456789",
            nextActionDateTime = baseDateTime,
            status = status,
            finalPrice = finalPrice,
            cancelReason = cancelReason,
            createdAt = createdAt,
            updatedAt = baseDateTime,
            closedAt = closedAt
        )
    }
}

/**
 * Вспомогательный класс для фейкового TimeProvider.
 */
class FakeTimeProvider(private var now: LocalDateTime) : TimeProvider {
    override fun getNow(): LocalDateTime = now
    
    fun setNow(dateTime: LocalDateTime) {
        now = dateTime
    }
}

/**
 * Вспомогательный класс для фейкового RequestRepository.
 */
class FakeRequestRepository : RequestRepository {
    private val createdRequestsFlow = MutableStateFlow<List<Request>>(emptyList())
    private val closedRequestsFlow = MutableStateFlow<List<Request>>(emptyList())

    fun updateData(created: List<Request>, closed: List<Request>) {
        createdRequestsFlow.value = created
        closedRequestsFlow.value = closed
    }

    override fun getRequestById(id: Long): Flow<Request?> = flowOf(null)
    override suspend fun getRequestByIdOneShot(id: Long): Request? = null
    override fun getActiveRequests(): Flow<List<Request>> = flowOf(emptyList())
    override fun getHistoryRequests(sortByStatus: Boolean): Flow<List<Request>> = flowOf(emptyList())
    
    override suspend fun createRequest(request: Request): Long = 0L
    override suspend fun updateRequest(request: Request) {}
    override suspend fun completeRequest(id: Long, finalPrice: Double?, finalComment: String?) {}
    override suspend fun cancelRequest(id: Long, cancelReason: String, finalComment: String?) {}
    override suspend fun restoreToActive(id: Long) {}
    override suspend fun updateRequestResults(id: Long, finalPrice: Double?, finalComment: String?, cancelReason: String?) {}

    override fun getClosedRequestsInPeriod(start: LocalDateTime, end: LocalDateTime): Flow<List<Request>> {
        return closedRequestsFlow
    }

    override fun getCreatedRequestsInPeriod(start: LocalDateTime, end: LocalDateTime): Flow<List<Request>> {
        return createdRequestsFlow
    }
}
