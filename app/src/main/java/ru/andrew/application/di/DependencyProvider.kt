package ru.andrew.application.di

import android.content.Context
import ru.andrew.application.data.db.AppDatabase
import ru.andrew.application.data.repository.ThemeRepository
import ru.andrew.application.data.repository.ThemeRepositoryImpl
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.data.repository.RequestRepositoryImpl

/**
 * Поставщик зависимостей (Dependency Provider / Service Locator) для внедрения репозиториев во ViewModel.
 * Все зависимости предоставляются как потокобезопасные синглтоны.
 */
object DependencyProvider {

    @Volatile
    private var themeRepository: ThemeRepository? = null

    @Volatile
    private var requestRepository: RequestRepository? = null

    fun provideThemeRepository(context: Context): ThemeRepository {
        return themeRepository ?: synchronized(this) {
            themeRepository ?: ThemeRepositoryImpl(
                context.applicationContext
            ).also {
                themeRepository = it
            }
        }
    }

    fun provideRequestRepository(context: Context): RequestRepository {
        return requestRepository ?: synchronized(this) {
            requestRepository ?: RequestRepositoryImpl(
                AppDatabase.getInstance(context.applicationContext).requestDao()
            ).also {
                requestRepository = it
            }
        }
    }
}
