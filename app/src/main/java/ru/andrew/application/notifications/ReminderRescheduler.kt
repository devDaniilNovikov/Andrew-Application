package ru.andrew.application.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.andrew.application.di.DependencyProvider

/**
 * Сверяет будильники AlarmManager с активными заявками в базе.
 *
 * Будильники пропадают после перезагрузки и после force stop (на части прошивок — после «очистки»
 * недавних приложений), а после выдачи разрешения на точные будильники их нужно поставить заново
 * в точном режиме. Повторная постановка безопасна: PendingIntent одной заявки просто обновляется.
 */
object ReminderRescheduler {

    private const val TAG = "ReminderRescheduler"

    suspend fun rescheduleAll(context: Context): Int {
        val appContext = context.applicationContext
        val activeRequests = DependencyProvider.provideRequestRepository(appContext)
            .getActiveRequests()
            .first()
        val scheduler = DependencyProvider.provideNotificationScheduler(appContext)
        activeRequests.forEach { scheduler.scheduleNotification(it) }
        Log.d(TAG, "Re-scheduled reminders for ${activeRequests.size} active requests.")
        return activeRequests.size
    }

    /**
     * Запускает сверку из BroadcastReceiver: процесс удерживается через goAsync()
     * до завершения работы с базой.
     */
    fun rescheduleFromReceiver(context: Context, pendingResult: BroadcastReceiver.PendingResult) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                rescheduleAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-schedule reminders", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
