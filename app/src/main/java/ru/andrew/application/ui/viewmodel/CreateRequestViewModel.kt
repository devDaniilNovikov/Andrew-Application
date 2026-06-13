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
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.domain.ActionType
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.di.DependencyProvider
import java.time.LocalDateTime

/**
 * Обертка для поддержки локализованных строковых ресурсов и динамических строк во ViewModel.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(val resId: Int) : UiText()
}

/**
 * Состояние формы создания заявки.
 */
data class CreateRequestUiState(
    val title: String = "",
    val phone: String = "",
    val clientName: String = "",
    val address: String = "",
    val equipmentType: EquipmentType = EquipmentType.OTHER,
    val actionType: ActionType = ActionType.OTHER,
    val nextActionDateTime: LocalDateTime? = null,
    val comment: String = "",
    val error: UiText? = null,
    val isSuccess: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * ViewModel для управления формой создания новой заявки с валидацией и сохранением в Room.
 */
class CreateRequestViewModel(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()

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
        _uiState.value = CreateRequestUiState()
    }

    /**
     * Сбросить флаг успешного завершения.
     */
    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
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
        
        // Валидация полей согласно PRD (название, телефон, дата следующего действия)
        if (currentState.title.trim().isEmpty() || 
            currentState.phone.trim().isEmpty() || 
            !isValidPhoneNumber(currentState.phone) ||
            currentState.nextActionDateTime == null
        ) {
            _uiState.update { 
                it.copy(error = UiText.StringResource(R.string.create_validation_error)) 
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
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
                    status = RequestStatus.ACTIVE
                )
                
                requestRepository.createRequest(newRequest)
                _uiState.update { 
                    it.copy(
                        isSuccess = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = UiText.DynamicString(e.localizedMessage ?: "Database error")) 
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

