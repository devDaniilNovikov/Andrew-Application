package ru.andrew.application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.andrew.application.data.repository.ThemeRepository
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.di.DependencyProvider

class ThemeViewModel(private val repository: ThemeRepository) : ViewModel() {

    val themeState: StateFlow<AppTheme> = repository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getTheme()
        )

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val repository = DependencyProvider.provideThemeRepository(application)
                return ThemeViewModel(repository) as T
            }
        }
    }
}
