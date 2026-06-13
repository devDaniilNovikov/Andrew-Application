package ru.andrew.application.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getTheme(): AppTheme {
        val themeName = prefs.getString(KEY_SELECTED_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit {
            putString(KEY_SELECTED_THEME, theme.name)
        }
    }

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_SELECTED_THEME = "selected_theme"
    }
}
