package ru.andrew.application.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Системный BroadcastReceiver, который заново ставит напоминания для всех активных заявок:
 * - после перезагрузки устройства (будильники AlarmManager её не переживают);
 * - после обновления приложения: будильники, поставленные до появления USE_EXACT_ALARM неточными,
 *   переставляются точными.
 * Выдачу разрешения на точные будильники (Android 12–12L) обрабатывает [ExactAlarmPermissionReceiver].
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"

        /**
         * События, после которых нужно перепланировать напоминания.
         * Должны совпадать с intent-filter ресивера в AndroidManifest.xml (проверяет ExactAlarmManifestPolicyTest).
         */
        internal val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return

        Log.d(TAG, "Received ${intent.action}. Re-scheduling active request reminders...")
        ReminderRescheduler.rescheduleFromReceiver(context, goAsync())
    }
}
