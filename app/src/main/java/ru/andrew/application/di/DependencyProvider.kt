package ru.andrew.application.di

import android.content.Context
import ru.andrew.application.data.repository.ThemeRepository
import ru.andrew.application.data.repository.ThemeRepositoryImpl

object DependencyProvider {
    @Volatile
    private var themeRepository: ThemeRepository? = null

    fun provideThemeRepository(context: Context): ThemeRepository {
        return themeRepository ?: synchronized(this) {
            themeRepository ?: ThemeRepositoryImpl(context.applicationContext).also {
                themeRepository = it
            }
        }
    }
}
