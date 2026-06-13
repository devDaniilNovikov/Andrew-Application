package ru.andrew.application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.di.DependencyProvider

/**
 * Состояние экрана активных заявок.
 */
data class ActiveRequestsUiState(
    val isLoading: Boolean = false,
    val requests: List<Request> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel для управления и предоставления списка активных заявок.
 * Поток заявок автоматически обновляется при любых изменениях в БД Room.
 */
class ActiveRequestsViewModel(
    private val requestRepository: RequestRepository
) : ViewModel() {

    val uiState: StateFlow<ActiveRequestsUiState> = requestRepository.getActiveRequests()
        .map { requests ->
            ActiveRequestsUiState(requests = requests, isLoading = false)
        }
        .catch { e ->
            emit(ActiveRequestsUiState(error = e.localizedMessage ?: "Unknown error", isLoading = false))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActiveRequestsUiState(isLoading = true)
        )

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val repository = DependencyProvider.provideRequestRepository(application)
                return ActiveRequestsViewModel(repository) as T
            }
        }
    }
}
