package ru.andrew.application.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.andrew.application.data.repository.ThemeRepository
import ru.andrew.application.data.repository.ThemeRepositoryImpl

object DependencyProvider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var themeRepository: ThemeRepository? = null

    fun provideThemeRepository(context: Context): ThemeRepository {
        return themeRepository ?: synchronized(this) {
            themeRepository ?: ThemeRepositoryImpl(
                context.applicationContext,
                applicationScope
            ).also {
                themeRepository = it
            }
        }
    }
}
