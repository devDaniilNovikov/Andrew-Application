package ru.andrew.application.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val _themeFlow = MutableStateFlow(themePreferences.getTheme())
    
    override val themeFlow: StateFlow<AppTheme> = _themeFlow.asStateFlow()

    override fun setTheme(theme: AppTheme) {
        externalScope.launch {
            withContext(Dispatchers.IO) {
                themePreferences.setTheme(theme)
            }
            _themeFlow.value = theme
        }
    }
}
