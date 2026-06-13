package ru.andrew.application.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.andrew.application.data.dao.RequestDao
import ru.andrew.application.data.entity.Request
import ru.andrew.application.domain.RequestStatus
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class RequestDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var requestDao: RequestDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        requestDao = database.requestDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetRequest() = runBlocking {
        val now = LocalDateTime.now()
        val request = Request(
            title = "Тестовая заявка",
            phone = "89990001122",
            nextActionDateTime = now
        )
        val id = requestDao.insertRequest(request)
        
        val retrieved = requestDao.getRequestByIdOneShot(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved!!.id)
        assertEquals("Тестовая заявка", retrieved.title)
        assertEquals("89990001122", retrieved.phone)
        assertEquals(RequestStatus.ACTIVE, retrieved.status)
    }

    @Test
    fun getActiveRequests_sortedByNextActionDateTimeAsc() = runBlocking {
        val now = LocalDateTime.now()
        
        val r1 = Request(id = 1L, title = "A", phone = "1", nextActionDateTime = now.plusDays(3), status = RequestStatus.ACTIVE)
        val r2 = Request(id = 2L, title = "B", phone = "2", nextActionDateTime = now.plusDays(1), status = RequestStatus.ACTIVE)
        val r3 = Request(id = 3L, title = "C", phone = "3", nextActionDateTime = now.plusDays(2), status = RequestStatus.ACTIVE)
        
        requestDao.insertRequest(r1)
        requestDao.insertRequest(r2)
        requestDao.insertRequest(r3)

        val activeRequests = requestDao.getActiveRequests(RequestStatus.ACTIVE).first()
        
        assertEquals(3, activeRequests.size)
        assertEquals(2L, activeRequests[0].id) // плюс 1 день (самая близкая)
        assertEquals(3L, activeRequests[1].id) // плюс 2 дня
        assertEquals(1L, activeRequests[2].id) // плюс 3 дня (самая дальняя)
    }

    @Test
    fun updateRequestStatusAndResults_isAtomicAndCorrect() = runBlocking {
        val now = LocalDateTime.now()
        val request = Request(
            id = 42L,
            title = "Ремонт кондиционера",
            phone = "12345",
            nextActionDateTime = now
        )
        requestDao.insertRequest(request)

        val closedAt = now.plusHours(2)
        requestDao.updateRequestStatusAndResults(
            id = 42L,
            status = RequestStatus.COMPLETED,
            finalPrice = 2500.0,
            finalComment = "Успешно заправлен фреон",
            closedAt = closedAt,
            cancelReason = null,
            updatedAt = closedAt
        )

        val updated = requestDao.getRequestByIdOneShot(42L)
        assertNotNull(updated)
        assertEquals(RequestStatus.COMPLETED, updated!!.status)
        assertEquals(2500.0, updated.finalPrice ?: 0.0, 0.001)
        assertEquals("Успешно заправлен фреон", updated.finalComment)
        assertNull(updated.cancelReason)
        assertEquals(closedAt, updated.closedAt)
    }

    @Test
    fun getHistoryRequests_sortedCorrectly() = runBlocking {
        val now = LocalDateTime.now()
        
        val r1 = Request(id = 10L, title = "A", phone = "1", nextActionDateTime = now, status = RequestStatus.COMPLETED, closedAt = now.minusHours(3))
        val r2 = Request(id = 20L, title = "B", phone = "2", nextActionDateTime = now, status = RequestStatus.CANCELLED, closedAt = now.minusHours(1))
        val r3 = Request(id = 30L, title = "C", phone = "3", nextActionDateTime = now, status = RequestStatus.COMPLETED, closedAt = now.minusHours(2))

        requestDao.insertRequest(r1)
        requestDao.insertRequest(r2)
        requestDao.insertRequest(r3)

        // 1. По закрытию (closedAt DESC)
        val historyByClosedAt = requestDao.getHistoryRequestsByClosedAt(RequestStatus.ACTIVE).first()
        assertEquals(3, historyByClosedAt.size)
        assertEquals(20L, historyByClosedAt[0].id) // 1 час назад (самый свежий)
        assertEquals(30L, historyByClosedAt[1].id) // 2 часа назад
        assertEquals(10L, historyByClosedAt[2].id) // 3 часа назад

        // 2. По статусу и закрытию (status ASC, затем closedAt DESC)
        val historyByStatus = requestDao.getHistoryRequestsByStatusAndClosedAt(
            activeStatus = RequestStatus.ACTIVE,
            completedStatus = RequestStatus.COMPLETED,
            cancelledStatus = RequestStatus.CANCELLED
        ).first()

        assertEquals(3, historyByStatus.size)
        // Сначала COMPLETED (потому что в запросе: WHEN status = :completedStatus THEN 1, status = :cancelledStatus THEN 2)
        // Для COMPLETED: 30L (closedAt 2 часа назад) и 10L (closedAt 3 часа назад). 30L идет первой, т.к. closedAt DESC
        assertEquals(30L, historyByStatus[0].id)
        assertEquals(10L, historyByStatus[1].id)
        // Затем CANCELLED: 20L
        assertEquals(20L, historyByStatus[2].id)
    }
}
