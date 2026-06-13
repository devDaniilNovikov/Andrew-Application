package ru.andrew.application.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.andrew.application.data.repository.ThemeRepository
import ru.andrew.application.data.repository.ThemeRepositoryImpl
import ru.andrew.application.ui.theme.AppTheme

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ThemeRepository = ThemeRepositoryImpl(application)

    val themeState: StateFlow<AppTheme> = repository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getTheme()
        )

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setTheme(theme)
        }
    }
}
