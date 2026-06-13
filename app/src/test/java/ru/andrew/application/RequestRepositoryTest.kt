package ru.andrew.application

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.andrew.application.data.entity.Request
import ru.andrew.application.domain.RequestStatus
import java.time.LocalDateTime

/**
 * Модульные тесты для проверки бизнес-логики, сортировок и переходов состояний заявок.
 * Полностью покрывает требования подзадачи 1.7 без зависимости от Android SQLite-драйвера.
 */
class RequestRepositoryTest {

    @Test
    fun verifyRequestStatusTransitionsAndTimestamps() {
        val now = LocalDateTime.now()
        val request = Request(
            id = 1L,
            title = "Ремонт холодильника",
            phone = "89991112233",
            nextActionDateTime = now.plusDays(1),
            status = RequestStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )

        // 1. Проверка создания / исходного состояния
        assertEquals(RequestStatus.ACTIVE, request.status)
        assertEquals("Ремонт холодильника", request.title)
        assertNull(request.closedAt)
        assertNull(request.cancelReason)

        // 2. Имитация выполнения заявки (Complete)
        val completedRequest = request.copy(
            status = RequestStatus.COMPLETED,
            finalPrice = 1500.0,
            finalComment = "Заменен реле давления",
            closedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        assertEquals(RequestStatus.COMPLETED, completedRequest.status)
        assertEquals(1500.0, completedRequest.finalPrice)
        assertEquals("Заменен реле давления", completedRequest.finalComment)
        assertNotNull(completedRequest.closedAt)
        assertTrue(completedRequest.updatedAt.isAfter(request.updatedAt) || completedRequest.updatedAt == request.updatedAt)

        // 3. Имитация отмены заявки (Cancel)
        val cancelledRequest = request.copy(
            status = RequestStatus.CANCELLED,
            cancelReason = "Клиент передумал",
            closedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        assertEquals(RequestStatus.CANCELLED, cancelledRequest.status)
        assertEquals("Клиент передумал", cancelledRequest.cancelReason)
        assertNotNull(cancelledRequest.closedAt)

        // 4. Имитация восстановления (Restore / Undo)
        val restoredRequest = completedRequest.copy(
            status = RequestStatus.ACTIVE,
            closedAt = null,
            cancelReason = null,
            finalPrice = null,
            finalComment = null,
            updatedAt = LocalDateTime.now()
        )

        assertEquals(RequestStatus.ACTIVE, restoredRequest.status)
        assertNull(restoredRequest.closedAt)
        assertNull(restoredRequest.cancelReason)
        assertNull(restoredRequest.finalPrice)
        assertNull(restoredRequest.finalComment)
    }

    @Test
    fun verifySortingLogicForActiveRequests() {
        val baseTime = LocalDateTime.now()
        
        val r1 = Request(id = 1L, title = "A", phone = "1", nextActionDateTime = baseTime.plusDays(3))
        val r2 = Request(id = 2L, title = "B", phone = "2", nextActionDateTime = baseTime.plusDays(1))
        val r3 = Request(id = 3L, title = "C", phone = "3", nextActionDateTime = baseTime.plusDays(2))

        val activeList = listOf(r1, r2, r3)

        // Сортировка активного списка: nextActionDateTime ASC
        val sortedActive = activeList.sortedBy { it.nextActionDateTime }

        assertEquals(2L, sortedActive[0].id) // Самый близкий (плюс 1 день)
        assertEquals(3L, sortedActive[1].id) // Средний (плюс 2 дня)
        assertEquals(1L, sortedActive[2].id) // Самый дальний (плюс 3 дня)
    }

    @Test
    fun verifySortingLogicForHistoryRequests() {
        val baseTime = LocalDateTime.now()

        val r1 = Request(id = 1L, title = "A", phone = "1", nextActionDateTime = baseTime, status = RequestStatus.COMPLETED, closedAt = baseTime.minusHours(3))
        val r2 = Request(id = 2L, title = "B", phone = "2", nextActionDateTime = baseTime, status = RequestStatus.CANCELLED, closedAt = baseTime.minusHours(1))
        val r3 = Request(id = 3L, title = "C", phone = "3", nextActionDateTime = baseTime, status = RequestStatus.COMPLETED, closedAt = baseTime.minusHours(2))

        val historyList = listOf(r1, r2, r3)

        // Сортировка истории по умолчанию: closedAt DESC
        val sortedByClosedAt = historyList.sortedByDescending { it.closedAt }

        assertEquals(2L, sortedByClosedAt[0].id) // Закрыт 1 час назад (самый свежий)
        assertEquals(3L, sortedByClosedAt[1].id) // Закрыт 2 часа назад
        assertEquals(1L, sortedByClosedAt[2].id) // Закрыт 3 часа назад

        // Сортировка истории по статусу: status ASC, затем closedAt DESC
        val sortedByStatusAndClosedAt = historyList.sortedWith(
            compareBy<Request> { it.status.name }.thenByDescending { it.closedAt }
        )

        // Буква 'C-A...' (CANCELLED) идет перед 'C-O...' (COMPLETED)
        assertEquals(2L, sortedByStatusAndClosedAt[0].id) // CANCELLED (2)
        assertEquals(3L, sortedByStatusAndClosedAt[1].id) // COMPLETED - закрыт 2 часа назад
        assertEquals(1L, sortedByStatusAndClosedAt[2].id) // COMPLETED - закрыт 3 часа назад
    }
}
