package ru.andrew.application.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import ru.andrew.application.data.dao.RequestDao
import ru.andrew.application.data.entity.Request
import ru.andrew.application.domain.RequestStatus
import java.time.LocalDateTime

/**
 * Реализация RequestRepository с использованием Room DAO.
 * Все тяжелые операции ввода-вывода (IO) принудительно переключаются на Dispatchers.IO.
 */
class RequestRepositoryImpl(
    private val requestDao: RequestDao
) : RequestRepository {

    override fun getRequestById(id: Long): Flow<Request?> {
        return requestDao.getRequestById(id)
    }

    override suspend fun getRequestByIdOneShot(id: Long): Request? = withContext(Dispatchers.IO) {
        requestDao.getRequestByIdOneShot(id)
    }

    override fun getActiveRequests(): Flow<List<Request>> {
        return requestDao.getActiveRequests()
    }

    override fun getHistoryRequests(sortByStatus: Boolean): Flow<List<Request>> {
        return if (sortByStatus) {
            requestDao.getHistoryRequestsByStatusAndClosedAt()
        } else {
            requestDao.getHistoryRequestsByClosedAt()
        }
    }

    override suspend fun createRequest(request: Request): Long {
        val now = LocalDateTime.now()
        val finalRequest = request.copy(
            status = RequestStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
        return requestDao.insertRequest(finalRequest)
    }

    override suspend fun updateRequest(request: Request) {
        val finalRequest = request.copy(
            updatedAt = LocalDateTime.now()
        )
        requestDao.updateRequest(finalRequest)
    }

    override suspend fun completeRequest(id: Long, finalPrice: Double?, finalComment: String?) {
        val now = LocalDateTime.now()
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
        val now = LocalDateTime.now()
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
        val now = LocalDateTime.now()
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
        val now = LocalDateTime.now()
        requestDao.updateRequestResultsOnly(
            id = id,
            finalPrice = finalPrice,
            finalComment = finalComment,
            cancelReason = cancelReason,
            updatedAt = now
        )
    }
}
