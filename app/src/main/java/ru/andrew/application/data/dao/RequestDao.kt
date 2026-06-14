package ru.andrew.application.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.andrew.application.data.entity.Request

/**
 * Data Access Object (DAO) для работы с таблицей заявок requests в Room.
 * Все тяжелые операции ввода-вывода выполняются в неблокирующем асинхронном режиме.
 */
@Dao
interface RequestDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRequest(request: Request): Long

    @Update
    suspend fun updateRequest(request: Request)

    @Delete
    suspend fun deleteRequest(request: Request)

    @Query("SELECT * FROM requests WHERE id = :id")
    fun getRequestById(id: Long): Flow<Request?>

    @Query("SELECT * FROM requests WHERE id = :id")
    suspend fun getRequestByIdOneShot(id: Long): Request?

    /**
     * Поток активных заявок. Сортировка от ближайших к дальним по времени следующего действия.
     */
    @Query("SELECT * FROM requests WHERE status = :status ORDER BY nextActionDateTime ASC")
    fun getActiveRequests(status: ru.andrew.application.domain.RequestStatus): Flow<List<Request>>

    /**
     * Поток истории заявок (выполненные и отмененные) с дефолтной сортировкой по дате закрытия (closedAt) по убыванию.
     */
    @Query("SELECT * FROM requests WHERE status != :activeStatus ORDER BY closedAt DESC")
    fun getHistoryRequestsByClosedAt(activeStatus: ru.andrew.application.domain.RequestStatus): Flow<List<Request>>

    /**
     * Поток истории заявок с сортировкой сначала по статусу, затем по дате закрытия (closedAt) по убыванию.
     */
    @Query("SELECT * FROM requests WHERE status != :activeStatus ORDER BY CASE WHEN status = :completedStatus THEN 1 WHEN status = :cancelledStatus THEN 2 ELSE 3 END ASC, closedAt DESC")
    fun getHistoryRequestsByStatusAndClosedAt(
        activeStatus: ru.andrew.application.domain.RequestStatus,
        completedStatus: ru.andrew.application.domain.RequestStatus,
        cancelledStatus: ru.andrew.application.domain.RequestStatus
    ): Flow<List<Request>>

    /**
     * Атомарный SQL-запрос для обновления статуса и результатов заявки в обход read-modify-write гонок.
     */
    @Query("UPDATE requests SET status = :status, finalPrice = :finalPrice, finalComment = :finalComment, closedAt = :closedAt, cancelReason = :cancelReason, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRequestStatusAndResults(
        id: Long,
        status: ru.andrew.application.domain.RequestStatus,
        finalPrice: Double?,
        finalComment: String?,
        closedAt: java.time.LocalDateTime?,
        cancelReason: String?,
        updatedAt: java.time.LocalDateTime
    )

    /**
     * Атомарный SQL-запрос для обновления результатов заявки (цена, комментарий, причина отмены) без изменения статуса.
     */
    @Query("UPDATE requests SET finalPrice = :finalPrice, finalComment = :finalComment, cancelReason = :cancelReason, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRequestResultsOnly(
        id: Long,
        finalPrice: Double?,
        finalComment: String?,
        cancelReason: String?,
        updatedAt: java.time.LocalDateTime
    )

    /**
     * Поток заявок, закрытых в указанный период (включая границы).
     */
    @Query("SELECT * FROM requests WHERE closedAt >= :start AND closedAt <= :end")
    fun getClosedRequestsInPeriod(start: java.time.LocalDateTime, end: java.time.LocalDateTime): Flow<List<Request>>

    /**
     * Поток заявок, созданных в указанный период (включая границы).
     */
    @Query("SELECT * FROM requests WHERE createdAt >= :start AND createdAt <= :end")
    fun getCreatedRequestsInPeriod(start: java.time.LocalDateTime, end: java.time.LocalDateTime): Flow<List<Request>>
}

