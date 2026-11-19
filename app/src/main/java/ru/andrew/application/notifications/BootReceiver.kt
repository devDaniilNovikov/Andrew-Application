package ru.andrew.application.notifications

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
 * Системный BroadcastReceiver для обработки перезагрузки устройства.
 * Восстанавливает все запланированные алармы для активных заявок после старта ОС.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    private val bootScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed. Re-scheduling active request reminders...")
            
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
                    Log.e(TAG, "Failed to re-schedule reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
