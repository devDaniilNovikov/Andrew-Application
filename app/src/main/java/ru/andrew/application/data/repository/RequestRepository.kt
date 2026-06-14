package ru.andrew.application.data.repository

import kotlinx.coroutines.flow.Flow
import ru.andrew.application.data.entity.Request

/**
 * Интерфейс репозитория для управления заявками.
 * Изолирует источник данных (Room Database) от ViewModel и UI.
 */
interface RequestRepository {
    fun getRequestById(id: Long): Flow<Request?>
    suspend fun getRequestByIdOneShot(id: Long): Request?
    fun getActiveRequests(): Flow<List<Request>>
    fun getHistoryRequests(sortByStatus: Boolean): Flow<List<Request>>
    
    suspend fun createRequest(request: Request): Long
    suspend fun updateRequest(request: Request)
    suspend fun completeRequest(id: Long, finalPrice: Double?, finalComment: String?)
    suspend fun cancelRequest(id: Long, cancelReason: String, finalComment: String?)
    suspend fun restoreToActive(id: Long)
    suspend fun updateRequestResults(id: Long, finalPrice: Double?, finalComment: String?, cancelReason: String?)
    
    fun getClosedRequestsInPeriod(start: java.time.LocalDateTime, end: java.time.LocalDateTime): Flow<List<Request>>
    fun getCreatedRequestsInPeriod(start: java.time.LocalDateTime, end: java.time.LocalDateTime): Flow<List<Request>>
}

