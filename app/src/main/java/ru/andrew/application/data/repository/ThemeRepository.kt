package ru.andrew.application.data.repository

import kotlinx.coroutines.flow.Flow
import ru.andrew.application.ui.theme.AppTheme

interface ThemeRepository {
    val themeFlow: Flow<AppTheme>
    fun getTheme(): AppTheme
    suspend fun setTheme(theme: AppTheme)
}
