package ru.andrew.application.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.andrew.application.data.db.AppDatabase
import ru.andrew.application.data.repository.ThemeRepository
import ru.andrew.application.data.repository.ThemeRepositoryImpl
import ru.andrew.application.data.repository.RequestRepository
import ru.andrew.application.data.repository.RequestRepositoryImpl
import ru.andrew.application.data.util.TimeProvider
import ru.andrew.application.data.util.SystemTimeProvider
import ru.andrew.application.notifications.NotificationScheduler
import ru.andrew.application.notifications.NotificationSchedulerImpl

/**
 * Поставщик зависимостей (Dependency Provider / Service Locator) для внедрения репозиториев во ViewModel.
 * Все зависимости предоставляются как потокобезопасные синглтоны.
 */
object DependencyProvider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val timeProvider: TimeProvider = SystemTimeProvider()

    @Volatile
    private var themeRepository: ThemeRepository? = null

    @Volatile
    private var requestRepository: RequestRepository? = null

    @Volatile
    private var notificationScheduler: NotificationScheduler? = null

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

    fun provideNotificationScheduler(context: Context): NotificationScheduler {
        return notificationScheduler ?: synchronized(this) {
            notificationScheduler ?: NotificationSchedulerImpl(
                context.applicationContext
            ).also {
                notificationScheduler = it
            }
        }
    }

    fun provideRequestRepository(context: Context): RequestRepository {
        return requestRepository ?: synchronized(this) {
            requestRepository ?: RequestRepositoryImpl(
                requestDao = AppDatabase.getInstance(context.applicationContext).requestDao(),
                notificationScheduler = provideNotificationScheduler(context),
                timeProvider = timeProvider
            ).also {
                requestRepository = it
            }
        }
    }
}

