package ru.andrew.application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.domain.ActionType
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.di.DependencyProvider
import java.time.LocalDateTime
import ru.andrew.application.ui.utils.UiText
import ru.andrew.application.R
import android.util.Log

/**
 * Состояние формы создания заявки.
 */
data class CreateRequestUiState(
    val title: String = "",
    val phone: String = "",
    val clientName: String = "",
    val address: String = "",
    val equipmentType: EquipmentType = EquipmentType.CABIN,
    val actionType: ActionType = ActionType.OTHER,
    val nextActionDateTime: LocalDateTime? = null,
    val comment: String = "",
    val error: UiText? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel для управления формой создания новой заявки с валидацией и сохранением в Room.
 */
class CreateRequestViewModel(
    private val requestRepository: RequestRepository
) : ViewModel() {

    sealed interface CreateRequestEvent {
        data class NavigationSuccess(val isEdit: Boolean) : CreateRequestEvent
    }

    private val _eventChannel = Channel<CreateRequestEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()

    var editingRequestId: Long? = null
        private set

    fun loadRequestForEditing(id: Long) {
        if (editingRequestId == id) return
        editingRequestId = id
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val request = requestRepository.getRequestByIdOneShot(id)
                if (request != null) {
                    _uiState.value = CreateRequestUiState(
                        title = request.title,
                        phone = request.phone,
                        clientName = request.clientName ?: "",
                        address = request.address ?: "",
                        equipmentType = request.equipmentType ?: EquipmentType.CABIN,
                        actionType = request.actionType ?: ActionType.OTHER,
                        nextActionDateTime = request.nextActionDateTime,
                        comment = request.comment ?: "",
                        error = null,
                        isLoading = false
                    )
                } else {
                    _uiState.update { it.copy(isLoading = false, error = UiText.DynamicString("Заявка не найдена")) }
                }
            } catch (e: Exception) {
                Log.e("CreateRequestViewModel", "Failed to load request for editing", e)
                _uiState.update { it.copy(isLoading = false, error = UiText.DynamicString("Ошибка при загрузке заявки")) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, error = null) }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone, error = null) }
    }

    fun updateClientName(clientName: String) {
        _uiState.update { it.copy(clientName = clientName) }
    }

    fun updateAddress(address: String) {
        _uiState.update { it.copy(address = address) }
    }

    fun updateEquipmentType(type: EquipmentType) {
        _uiState.update { it.copy(equipmentType = type) }
    }

    fun updateActionType(type: ActionType) {
        _uiState.update { it.copy(actionType = type) }
    }

    fun updateNextActionDateTime(dateTime: LocalDateTime?) {
        _uiState.update { it.copy(nextActionDateTime = dateTime, error = null) }
    }

    fun updateComment(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }

    /**
     * Сбросить все поля формы в исходное состояние.
     */
    fun clearForm() {
        editingRequestId = null
        _uiState.value = CreateRequestUiState()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        if (phone.trim().isEmpty()) return true
        val cleanPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        val phoneRegex = "^\\+?\\d{7,15}$".toRegex()
        return phoneRegex.matches(cleanPhone)
    }

    /**
     * Попытаться сохранить заявку в базу данных.
     * Проверяет обязательные поля: название, телефон и дата действия.
     */
    fun saveRequest() {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        
        // Валидация полей согласно PRD (название, дата следующего действия)
        if (currentState.title.trim().isEmpty() || 
            !isValidPhoneNumber(currentState.phone) ||
            currentState.nextActionDateTime == null
        ) {
            _uiState.update { 
                it.copy(error = UiText.StringResource(R.string.create_validation_error)) 
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        if (editingRequestId != null) {
            viewModelScope.launch {
                try {
                    val existingRequest = requestRepository.getRequestByIdOneShot(editingRequestId!!)
                    if (existingRequest != null) {
                        val updatedRequest = existingRequest.copy(
                            title = currentState.title.trim(),
                            clientName = currentState.clientName.trim().takeIf { it.isNotEmpty() },
                            phone = currentState.phone.trim(),
                            address = currentState.address.trim().takeIf { it.isNotEmpty() },
                            equipmentType = currentState.equipmentType,
                            actionType = currentState.actionType,
                            nextActionDateTime = currentState.nextActionDateTime,
                            comment = currentState.comment.trim().takeIf { it.isNotEmpty() }
                        )
                        requestRepository.updateRequest(updatedRequest)
                        clearForm()
                        _eventChannel.send(CreateRequestEvent.NavigationSuccess(isEdit = true))
                    } else {
                        _uiState.update { it.copy(error = UiText.DynamicString("Заявка для редактирования не найдена")) }
                    }
                } catch (e: Exception) {
                    Log.e("CreateRequestViewModel", "Failed to update request", e)
                    _uiState.update { it.copy(error = UiText.StringResource(R.string.create_db_error)) }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            return
        }

        viewModelScope.launch {
            try {
                val now = LocalDateTime.now()
                val newRequest = Request(
                    id = 0L, // Автогенерация ID в БД
                    title = currentState.title.trim(),
                    clientName = currentState.clientName.trim().takeIf { it.isNotEmpty() },
                    phone = currentState.phone.trim(),
                    address = currentState.address.trim().takeIf { it.isNotEmpty() },
                    equipmentType = currentState.equipmentType,
                    actionType = currentState.actionType,
                    nextActionDateTime = currentState.nextActionDateTime,
                    comment = currentState.comment.trim().takeIf { it.isNotEmpty() },
                    status = RequestStatus.ACTIVE,
                    createdAt = now,
                    updatedAt = now
                )
                
                requestRepository.createRequest(newRequest)
                clearForm()
                _eventChannel.send(CreateRequestEvent.NavigationSuccess(isEdit = false))
                _uiState.update { 
                    it.copy(
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("CreateRequestViewModel", "Failed to save request", e)
                _uiState.update { 
                    it.copy(error = UiText.StringResource(R.string.create_db_error)) 
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val repository = DependencyProvider.provideRequestRepository(application)
                return CreateRequestViewModel(repository) as T
            }
        }
    }
}

