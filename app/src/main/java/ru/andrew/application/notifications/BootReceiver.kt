package ru.andrew.application.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Системный BroadcastReceiver для обработки перезагрузки устройства.
 * Восстанавливает все запланированные алармы для активных заявок после старта ОС.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Device boot completed. Re-scheduling active request reminders...")
        ReminderRescheduler.rescheduleFromReceiver(context, goAsync())
    }
}
