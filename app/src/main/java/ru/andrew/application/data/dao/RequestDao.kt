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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
    @Query("SELECT * FROM requests WHERE status = 'ACTIVE' ORDER BY nextActionDateTime ASC")
    fun getActiveRequests(): Flow<List<Request>>

    /**
     * Поток истории заявок (выполненные и отмененные) с дефолтной сортировкой по дате закрытия (closedAt) по убыванию.
     */
    @Query("SELECT * FROM requests WHERE status != 'ACTIVE' ORDER BY closedAt DESC")
    fun getHistoryRequestsByClosedAt(): Flow<List<Request>>

    /**
     * Поток истории заявок с сортировкой сначала по статусу, затем по дате закрытия (closedAt) по убыванию.
     */
    @Query("SELECT * FROM requests WHERE status != 'ACTIVE' ORDER BY status ASC, closedAt DESC")
    fun getHistoryRequestsByStatusAndClosedAt(): Flow<List<Request>>
}
