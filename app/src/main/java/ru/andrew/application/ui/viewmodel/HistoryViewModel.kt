package ru.andrew.application.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.andrew.application.data.entity.Request
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.di.DependencyProvider
import ru.andrew.application.domain.RequestStatus

enum class HistoryFilter {
    ALL, COMPLETED, CANCELLED
}

class HistoryViewModel(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _sortByStatus = MutableStateFlow(false)
    val sortByStatus: StateFlow<Boolean> = _sortByStatus.asStateFlow()

    private val _filterMode = MutableStateFlow(HistoryFilter.ALL)
    val filterMode: StateFlow<HistoryFilter> = _filterMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyRequests: StateFlow<List<Request>> = combine(
        _sortByStatus,
        _filterMode
    ) { sortBy, filter ->
        Pair(sortBy, filter)
    }.flatMapLatest { (sortBy, filter) ->
        requestRepository.getHistoryRequests(sortBy).map { requests ->
            when (filter) {
                HistoryFilter.ALL -> requests
                HistoryFilter.COMPLETED -> requests.filter { it.status == RequestStatus.COMPLETED }
                HistoryFilter.CANCELLED -> requests.filter { it.status == RequestStatus.CANCELLED }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleSortByStatus(sortByStatus: Boolean) {
        _sortByStatus.value = sortByStatus
    }

    fun setFilterMode(filter: HistoryFilter) {
        _filterMode.value = filter
    }

    fun updateRequestResults(
        id: Long,
        finalPrice: Double?,
        finalComment: String?,
        cancelReason: String?
    ) {
        viewModelScope.launch {
            requestRepository.updateRequestResults(id, finalPrice, finalComment, cancelReason)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val context = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Context
                return HistoryViewModel(DependencyProvider.provideRequestRepository(context)) as T
            }
        }
    }
}
