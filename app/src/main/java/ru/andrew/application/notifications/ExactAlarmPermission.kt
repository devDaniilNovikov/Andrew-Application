package ru.andrew.application.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Разрешение на точные будильники. Без него AlarmManager доставляет напоминания неточно —
 * с опозданием на минуты и больше.
 *
 * На Android 13+ приложение объявляет USE_EXACT_ALARM: оно выдаётся при установке, и проверка
 * всегда проходит. На Android 12–12L действует SCHEDULE_EXACT_ALARM, который пользователь может
 * отозвать, — тогда экран списка предлагает выдать его снова.
 */
object ExactAlarmPermission {

    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Системный экран, где пользователь выдаёт разрешение, или null на Android < 12,
     * где разрешение не требуется.
     */
    fun settingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.fromParts("package", context.packageName, null)
        )
    }
}
