package ru.andrew.application.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.andrew.application.di.DependencyProvider
import ru.andrew.application.domain.RequestStatus

/**
 * Системный BroadcastReceiver для приема событий от AlarmManager.
 * Срабатывает точно в запланированное время напоминания (nextActionDateTime).
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getLongExtra("requestId", -1L)
        Log.d(TAG, "onReceive triggered for request ID: $requestId")
        
        if (requestId == -1L) {
            Log.e(TAG, "onReceive: Invalid requestId (-1). Skipping.")
            return
        }

        // goAsync() позволяет безопасно выполнять асинхронные операции в BroadcastReceiver,
        // удерживая процесс активным до завершения корутины.
        val pendingResult = goAsync()
        
        receiverScope.launch {
            try {
                val repository = DependencyProvider.provideRequestRepository(context)
                val request = repository.getRequestByIdOneShot(requestId)
                
                if (request != null) {
                    if (request.status == RequestStatus.ACTIVE) {
                        Log.d(TAG, "Request #$requestId is ACTIVE. Displaying notification...")
                        NotificationHelper.showNotification(context, request)
                    } else {
                        Log.d(TAG, "Request #$requestId has status ${request.status} (not ACTIVE). Skipping notification.")
                    }
                } else {
                    Log.e(TAG, "Request #$requestId not found in database.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm for request #$requestId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
