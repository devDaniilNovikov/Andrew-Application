package ru.andrew.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.domain.ActionType
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.ui.viewmodel.CreateRequestViewModel
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CreateRequestViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveRequestWhenEditingCompletedRequestUpdatesRequestFieldsAndResults() = runTest {
        val createdAt = LocalDateTime.of(2026, 1, 10, 9, 0)
        val closedAt = LocalDateTime.of(2026, 1, 12, 18, 30)
        val nextAction = LocalDateTime.of(2026, 1, 11, 14, 0)
        val repository = FakeRequestRepository(
            Request(
                id = 7L,
                title = "Старый заголовок",
                clientName = "Старый клиент",
                phone = "79990001122",
                address = "Старый адрес",
                equipmentType = EquipmentType.CABIN,
                actionType = ActionType.CALL,
                nextActionDateTime = nextAction,
                comment = "Старый комментарий",
                status = RequestStatus.COMPLETED,
                finalPrice = 1200.0,
                finalComment = "Старый итог",
                createdAt = createdAt,
                updatedAt = createdAt,
                closedAt = closedAt
            )
        )
        val viewModel = CreateRequestViewModel(repository)

        viewModel.loadRequestForEditing(7L)
        advanceUntilIdle()

        val updatedNextAction = nextAction.plusDays(2)
        viewModel.updateTitle("Новый заголовок")
        viewModel.updatePhone("+79993334455")
        viewModel.updateClientName("Новый клиент")
        viewModel.updateAddress("Новый адрес")
        viewModel.updateEquipmentType(EquipmentType.COMMERCIAL)
        viewModel.updateActionType(ActionType.REPAIR)
        viewModel.updateNextActionDateTime(updatedNextAction)
        viewModel.updateComment("Новый комментарий")
        viewModel.updateFinalPrice("2500,50")
        viewModel.updateFinalComment("Новый итог")

        viewModel.saveRequest()
        advanceUntilIdle()

        val savedRequest = repository.updatedRequest
        assertNotNull(savedRequest)
        assertEquals("Новый заголовок", savedRequest!!.title)
        assertEquals("Новый клиент", savedRequest.clientName)
        assertEquals("+79993334455", savedRequest.phone)
        assertEquals("Новый адрес", savedRequest.address)
        assertEquals(EquipmentType.COMMERCIAL, savedRequest.equipmentType)
        assertEquals(ActionType.REPAIR, savedRequest.actionType)
        assertEquals(updatedNextAction, savedRequest.nextActionDateTime)
        assertEquals("Новый комментарий", savedRequest.comment)
        assertEquals(RequestStatus.COMPLETED, savedRequest.status)
        assertEquals(2500.50, savedRequest.finalPrice!!, 0.0)
        assertEquals("Новый итог", savedRequest.finalComment)
        assertEquals(closedAt, savedRequest.closedAt)
    }

    private class FakeRequestRepository(initialRequest: Request) : RequestRepository {
        private var request: Request = initialRequest
        var updatedRequest: Request? = null
            private set

        override fun getRequestById(id: Long): Flow<Request?> {
            return flowOf(request.takeIf { it.id == id })
        }

        override suspend fun getRequestByIdOneShot(id: Long): Request? {
            return request.takeIf { it.id == id }
        }

        override fun getActiveRequests(): Flow<List<Request>> {
            return flowOf(emptyList())
        }

        override fun getHistoryRequests(sortByStatus: Boolean): Flow<List<Request>> {
            return flowOf(emptyList())
        }

        override suspend fun createRequest(request: Request): Long {
            error("Not used in this test")
        }

        override suspend fun updateRequest(request: Request) {
            this.request = request
            updatedRequest = request
        }

        override suspend fun completeRequest(id: Long, finalPrice: Double?, finalComment: String?) {
            error("Not used in this test")
        }

        override suspend fun cancelRequest(id: Long, cancelReason: String, finalComment: String?) {
            error("Not used in this test")
        }

        override suspend fun restoreToActive(id: Long) {
            error("Not used in this test")
        }

        override suspend fun updateRequestResults(
            id: Long,
            finalPrice: Double?,
            finalComment: String?,
            cancelReason: String?
        ) {
            error("Not used in this test")
        }
    }
}
