package ru.andrew.application.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.andrew.application.di.DependencyProvider

/**
 * Системный BroadcastReceiver, который заново планирует напоминания для всех активных заявок:
 * - после перезагрузки устройства (будильники AlarmManager не переживают перезагрузку);
 * - после обновления приложения (будильники, поставленные неточными, переставляются точными);
 * - после выдачи разрешения на точные будильники (Android 12–12L).
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"

        /**
         * События, после которых нужно перепланировать напоминания.
         * Должны совпадать с intent-filter ресивера в AndroidManifest.xml (проверяется ExactAlarmManifestPolicyTest).
         * Строковые константы подставляются при компиляции, поэтому безопасны и на API ниже 31.
         */
        @SuppressLint("InlinedApi")
        internal val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }

    private val bootScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESCHEDULE_ACTIONS) {
            Log.d(TAG, "Received ${intent.action}. Re-scheduling active request reminders...")
            
            val pendingResult = goAsync()
            
            bootScope.launch {
                try {
                    val repository = DependencyProvider.provideRequestRepository(context)
                    
                    // Собираем первый снимок списка активных заявок
                    val activeRequests = repository.getActiveRequests().first()
                    val scheduler = DependencyProvider.provideNotificationScheduler(context)
                    
                    Log.d(TAG, "Found ${activeRequests.size} active requests to re-schedule.")
                    
                    var rescheduledCount = 0
                    for (request in activeRequests) {
                        scheduler.scheduleNotification(request)
                        rescheduledCount++
                    }
                    
                    Log.d(TAG, "Successfully re-scheduled $rescheduledCount active reminders.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to re-schedule reminders after ${intent.action}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
