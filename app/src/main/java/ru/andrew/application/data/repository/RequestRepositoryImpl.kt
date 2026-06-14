package ru.andrew.application.data.repository

import kotlinx.coroutines.flow.Flow
import ru.andrew.application.data.dao.RequestDao
import ru.andrew.application.data.entity.Request
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.data.util.TimeProvider
import ru.andrew.application.data.util.SystemTimeProvider
import java.time.LocalDateTime

/**
 * Реализация RequestRepository с использованием Room DAO.
 * Корутины Room (suspend и Flow) автоматически и безопасно выполняются на фоновом пуле потоков.
 */
class RequestRepositoryImpl(
    private val requestDao: RequestDao,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) : RequestRepository {

    override fun getRequestById(id: Long): Flow<Request?> {
        return requestDao.getRequestById(id)
    }

    override suspend fun getRequestByIdOneShot(id: Long): Request? {
        return requestDao.getRequestByIdOneShot(id)
    }

    override fun getActiveRequests(): Flow<List<Request>> {
        return requestDao.getActiveRequests(RequestStatus.ACTIVE)
    }

    override fun getHistoryRequests(sortByStatus: Boolean): Flow<List<Request>> {
        return if (sortByStatus) {
            requestDao.getHistoryRequestsByStatusAndClosedAt(
                activeStatus = RequestStatus.ACTIVE,
                completedStatus = RequestStatus.COMPLETED,
                cancelledStatus = RequestStatus.CANCELLED
            )
        } else {
            requestDao.getHistoryRequestsByClosedAt(RequestStatus.ACTIVE)
        }
    }

    override suspend fun createRequest(request: Request): Long {
        val now = timeProvider.getNow()
        val finalRequest = request.copy(
            id = 0L, // Принудительно обнуляем id для автогенерации первичного ключа и предотвращения REPLACE перезаписи
            status = RequestStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
        return requestDao.insertRequest(finalRequest)
    }

    override suspend fun updateRequest(request: Request) {
        val finalRequest = request.copy(
            updatedAt = timeProvider.getNow()
        )
        requestDao.updateRequest(finalRequest)
    }

    override suspend fun completeRequest(id: Long, finalPrice: Double?, finalComment: String?) {
        val now = timeProvider.getNow()
        requestDao.updateRequestStatusAndResults(
            id = id,
            status = RequestStatus.COMPLETED,
            finalPrice = finalPrice,
            finalComment = finalComment,
            closedAt = now,
            cancelReason = null,
            updatedAt = now
        )
    }

    override suspend fun cancelRequest(id: Long, cancelReason: String, finalComment: String?) {
        val now = timeProvider.getNow()
        requestDao.updateRequestStatusAndResults(
            id = id,
            status = RequestStatus.CANCELLED,
            finalPrice = null,
            finalComment = finalComment,
            closedAt = now,
            cancelReason = cancelReason,
            updatedAt = now
        )
    }

    override suspend fun restoreToActive(id: Long) {
        val now = timeProvider.getNow()
        requestDao.updateRequestStatusAndResults(
            id = id,
            status = RequestStatus.ACTIVE,
            finalPrice = null,
            finalComment = null,
            closedAt = null,
            cancelReason = null,
            updatedAt = now
        )
    }

    override suspend fun updateRequestResults(
        id: Long,
        finalPrice: Double?,
        finalComment: String?,
        cancelReason: String?
    ) {
        val now = timeProvider.getNow()
        requestDao.updateRequestResultsOnly(
            id = id,
            finalPrice = finalPrice,
            finalComment = finalComment,
            cancelReason = cancelReason,
            updatedAt = now
        )
    }

    override fun getClosedRequestsInPeriod(start: LocalDateTime, end: LocalDateTime): Flow<List<Request>> {
        return requestDao.getClosedRequestsInPeriod(start, end)
    }

    override fun getCreatedRequestsInPeriod(start: LocalDateTime, end: LocalDateTime): Flow<List<Request>> {
        return requestDao.getCreatedRequestsInPeriod(start, end)
    }
}

