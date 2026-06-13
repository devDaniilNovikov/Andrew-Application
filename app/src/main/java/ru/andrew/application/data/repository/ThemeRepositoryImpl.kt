package ru.andrew.application.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.ui.theme.ThemePreferences

class ThemeRepositoryImpl(context: Context) : ThemeRepository {
    private val themePreferences = ThemePreferences(context)
    private val _themeFlow = MutableStateFlow(themePreferences.getTheme())
    
    override val themeFlow: Flow<AppTheme> = _themeFlow.asStateFlow()

    override fun getTheme(): AppTheme {
        return themePreferences.getTheme()
    }

    override fun setTheme(theme: AppTheme) {
        themePreferences.setTheme(theme)
        _themeFlow.value = theme
    }
}
