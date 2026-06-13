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

    override suspend fun createRequest(request: Request): Long = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val finalRequest = request.copy(
            status = RequestStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
        requestDao.insertRequest(finalRequest)
    }

    override suspend fun updateRequest(request: Request) = withContext(Dispatchers.IO) {
        val finalRequest = request.copy(
            updatedAt = LocalDateTime.now()
        )
        requestDao.updateRequest(finalRequest)
    }

    override suspend fun completeRequest(id: Long, finalPrice: Double?, finalComment: String?) = withContext(Dispatchers.IO) {
        val current = requestDao.getRequestByIdOneShot(id)
        if (current != null) {
            val now = LocalDateTime.now()
            val updated = current.copy(
                status = RequestStatus.COMPLETED,
                finalPrice = finalPrice,
                finalComment = finalComment,
                closedAt = now,
                updatedAt = now
            )
            requestDao.updateRequest(updated)
        }
    }

    override suspend fun cancelRequest(id: Long, cancelReason: String, finalComment: String?) = withContext(Dispatchers.IO) {
        val current = requestDao.getRequestByIdOneShot(id)
        if (current != null) {
            val now = LocalDateTime.now()
            val updated = current.copy(
                status = RequestStatus.CANCELLED,
                cancelReason = cancelReason,
                finalComment = finalComment,
                closedAt = now,
                updatedAt = now
            )
            requestDao.updateRequest(updated)
        }
    }

    override suspend fun restoreToActive(id: Long) = withContext(Dispatchers.IO) {
        val current = requestDao.getRequestByIdOneShot(id)
        if (current != null) {
            val now = LocalDateTime.now()
            val updated = current.copy(
                status = RequestStatus.ACTIVE,
                closedAt = null,
                cancelReason = null,
                finalPrice = null,
                finalComment = null,
                updatedAt = now
            )
            requestDao.updateRequest(updated)
        }
    }

    override suspend fun updateRequestResults(
        id: Long,
        finalPrice: Double?,
        finalComment: String?,
        cancelReason: String?
    ) = withContext(Dispatchers.IO) {
        val current = requestDao.getRequestByIdOneShot(id)
        if (current != null) {
            val now = LocalDateTime.now()
            val updated = current.copy(
                finalPrice = finalPrice,
                finalComment = finalComment,
                cancelReason = cancelReason,
                updatedAt = now
            )
            requestDao.updateRequest(updated)
        }
    }
}
