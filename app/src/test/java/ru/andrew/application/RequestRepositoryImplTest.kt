package ru.andrew.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.andrew.application.data.dao.RequestDao
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepositoryImpl
import ru.andrew.application.data.util.TimeProvider
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.notifications.NotificationScheduler
import java.time.LocalDateTime

class RequestRepositoryImplTest {

    @Test
    fun deleteRequest_removesRequestAndCancelsNotification() = runTest {
        val request = Request(
            id = 42L,
            title = "Ремонт холодильника",
            phone = "89991112233",
            nextActionDateTime = LocalDateTime.of(2026, 7, 1, 12, 0)
        )
        val requestDao = FakeRequestDao(listOf(request))
        val notificationScheduler = FakeNotificationScheduler()
        val repository = RequestRepositoryImpl(
            requestDao = requestDao,
            notificationScheduler = notificationScheduler,
            timeProvider = FixedTimeProvider(LocalDateTime.of(2026, 7, 1, 10, 0))
        )

        repository.deleteRequest(42L)

        assertNull(requestDao.getRequestByIdOneShot(42L))
        assertEquals(listOf(42L), notificationScheduler.cancelledRequestIds)
    }

    private class FakeRequestDao(initialRequests: List<Request>) : RequestDao {
        private val requests = MutableStateFlow(initialRequests)

        override suspend fun insertRequest(request: Request): Long {
            val id = if (request.id == 0L) {
                (requests.value.maxOfOrNull { it.id } ?: 0L) + 1L
            } else {
                request.id
            }
            requests.value = requests.value + request.copy(id = id)
            return id
        }

        override suspend fun updateRequest(request: Request) {
            requests.value = requests.value.map { existing ->
                if (existing.id == request.id) request else existing
            }
        }

        override suspend fun deleteRequest(request: Request) {
            deleteRequestById(request.id)
        }

        override suspend fun deleteRequestById(id: Long): Int {
            val before = requests.value.size
            requests.value = requests.value.filterNot { it.id == id }
            return before - requests.value.size
        }

        override fun getRequestById(id: Long): Flow<Request?> {
            return requests.map { currentRequests ->
                currentRequests.firstOrNull { it.id == id }
            }
        }

        override suspend fun getRequestByIdOneShot(id: Long): Request? {
            return requests.value.firstOrNull { it.id == id }
        }

        override fun getActiveRequests(status: RequestStatus): Flow<List<Request>> {
            return requests.map { currentRequests ->
                currentRequests
                    .filter { it.status == status }
                    .sortedBy { it.nextActionDateTime }
            }
        }

        override fun getHistoryRequestsByClosedAt(activeStatus: RequestStatus): Flow<List<Request>> {
            return requests.map { currentRequests ->
                currentRequests
                    .filter { it.status != activeStatus }
                    .sortedByDescending { it.closedAt }
            }
        }

        override fun getHistoryRequestsByStatusAndClosedAt(
            activeStatus: RequestStatus,
            completedStatus: RequestStatus,
            cancelledStatus: RequestStatus
        ): Flow<List<Request>> {
            return requests.map { currentRequests ->
                currentRequests
                    .filter { it.status != activeStatus }
                    .sortedWith(
                        compareBy<Request> {
                            when (it.status) {
                                completedStatus -> 1
                                cancelledStatus -> 2
                                else -> 3
                            }
                        }.thenByDescending { it.closedAt }
                    )
            }
        }

        override suspend fun updateRequestStatusAndResults(
            id: Long,
            status: RequestStatus,
            finalPrice: Double?,
            finalComment: String?,
            closedAt: LocalDateTime?,
            cancelReason: String?,
            updatedAt: LocalDateTime
        ) {
            requests.value = requests.value.map { request ->
                if (request.id == id) {
                    request.copy(
                        status = status,
                        finalPrice = finalPrice,
                        finalComment = finalComment,
                        closedAt = closedAt,
                        cancelReason = cancelReason,
                        updatedAt = updatedAt
                    )
                } else {
                    request
                }
            }
        }

        override suspend fun updateRequestResultsOnly(
            id: Long,
            finalPrice: Double?,
            finalComment: String?,
            cancelReason: String?,
            updatedAt: LocalDateTime
        ) {
            requests.value = requests.value.map { request ->
                if (request.id == id) {
                    request.copy(
                        finalPrice = finalPrice,
                        finalComment = finalComment,
                        cancelReason = cancelReason,
                        updatedAt = updatedAt
                    )
                } else {
                    request
                }
            }
        }
    }

    private class FakeNotificationScheduler : NotificationScheduler {
        val cancelledRequestIds = mutableListOf<Long>()

        override fun scheduleNotification(request: Request) = Unit

        override fun cancelNotification(requestId: Long) {
            cancelledRequestIds += requestId
        }
    }

    private class FixedTimeProvider(private val now: LocalDateTime) : TimeProvider {
        override fun getNow(): LocalDateTime = now
    }
}
