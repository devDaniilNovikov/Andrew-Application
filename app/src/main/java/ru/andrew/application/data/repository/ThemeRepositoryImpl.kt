package ru.andrew.application.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.ui.theme.ThemePreferences

class ThemeRepositoryImpl(
    context: Context,
    private val externalScope: CoroutineScope
) : ThemeRepository {
    private val themePreferences = ThemePreferences(context)
    private val _themeFlow = MutableStateFlow(AppTheme.SYSTEM)
    
    override val themeFlow: StateFlow<AppTheme> = _themeFlow.asStateFlow()

    init {
        externalScope.launch {
            val savedTheme = withContext(Dispatchers.IO) {
                themePreferences.getTheme()
            }
            _themeFlow.value = savedTheme
        }
    }

    override fun setTheme(theme: AppTheme) {
        themePreferences.setTheme(theme)
        _themeFlow.value = theme
    }
}
