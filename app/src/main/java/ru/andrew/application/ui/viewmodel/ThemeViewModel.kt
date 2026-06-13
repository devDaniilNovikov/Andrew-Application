package ru.andrew.application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.andrew.application.data.repository.ThemeRepository
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.di.DependencyProvider

class ThemeViewModel(private val repository: ThemeRepository) : ViewModel() {

    private val _themeState = MutableStateFlow(AppTheme.SYSTEM)
    val themeState: StateFlow<AppTheme> = _themeState.asStateFlow()

    init {
        viewModelScope.launch {
            _themeState.value = repository.getTheme()
        }
    }

    fun selectTheme(theme: AppTheme) {
        repository.setTheme(theme)
        _themeState.value = theme
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
