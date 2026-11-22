package ru.andrew.application.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import ru.andrew.application.data.entity.Request
import java.time.ZoneId

/**
 * Реализация NotificationScheduler с использованием системного AlarmManager.
 * Обеспечивает точный запуск будильников для своевременного показа уведомлений.
 */
class NotificationSchedulerImpl(
    private val context: Context
) : NotificationScheduler {

    companion object {
        private const val TAG = "NotificationScheduler"
    }

    override fun scheduleNotification(request: Request) {
        val triggerAtMillis = reminderTriggerAtMillis(
            nextAction = request.nextActionDateTime,
            nowMillis = System.currentTimeMillis(),
            zone = ZoneId.systemDefault()
        )

        // Даты нет или она уже прошла (например, заявку перенесли на прошедшее время):
        // снимаем ранее поставленный будильник, иначе он сработает по старому времени.
        if (triggerAtMillis == null) {
            cancelNotification(request.id)
            Log.d(TAG, "Request #${request.id} has no future nextActionDateTime. Pending alarm cancelled.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Настраиваем намерение для отправки в NotificationReceiver
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("requestId", request.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            request.id.toInt(), // Уникальный requestCode на основе ID заявки предотвращает затирание других алармов
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Проверяем разрешение на точные будильники на устройствах Android 12+.
        // Без него напоминание неточное; экран списка предлагает выдать разрешение.
        val canScheduleExact = ExactAlarmPermission.isGranted(context)

        try {
            if (canScheduleExact) {
                // Точный будильник с возможностью пробуждения устройства в Doze Mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Successfully scheduled EXACT alarm for request #${request.id} at $triggerAtMillis")
            } else {
                // Энергоэффективный будильник при отсутствии разрешения
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Successfully scheduled INEXACT alarm for request #${request.id} at $triggerAtMillis")
            }
        } catch (e: SecurityException) {
            // Резервный вызов на случай неожиданных ограничений безопасности операционной системы
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.e(TAG, "SecurityException while scheduling exact alarm for request #${request.id}. Fallback to inexact.", e)
        }
    }

    override fun cancelNotification(requestId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, NotificationReceiver::class.java)
        
        // Получаем существующий PendingIntent без создания нового
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Successfully cancelled scheduled alarm for request #$requestId")
        } else {
            Log.d(TAG, "No scheduled alarm found for request #$requestId to cancel.")
        }
    }
}
