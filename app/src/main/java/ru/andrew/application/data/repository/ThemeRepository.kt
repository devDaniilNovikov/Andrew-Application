package ru.andrew.application.data.repository

import kotlinx.coroutines.flow.StateFlow
import ru.andrew.application.ui.theme.AppTheme

interface ThemeRepository {
    val themeFlow: StateFlow<AppTheme>
    fun setTheme(theme: AppTheme)
}
