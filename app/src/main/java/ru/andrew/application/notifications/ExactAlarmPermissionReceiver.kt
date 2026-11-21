package ru.andrew.application.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Срабатывает, когда пользователь выдаёт разрешение на точные будильники (Android 12+).
 * Будильники, поставленные до этого в неточном режиме, переставляются точными.
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ExactAlarmPermission"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        if (!ExactAlarmPermission.isGranted(context)) return

        Log.d(TAG, "Exact alarm permission granted. Re-scheduling reminders as exact alarms...")
        ReminderRescheduler.rescheduleFromReceiver(context, goAsync())
    }
}
