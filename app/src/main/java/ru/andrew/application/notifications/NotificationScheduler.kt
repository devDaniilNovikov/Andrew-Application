package ru.andrew.application.notifications

import ru.andrew.application.data.entity.Request

/**
 * Интерфейс планировщика локальных уведомлений.
 * Изолирует работу с AlarmManager от репозиториев и бизнес-логики.
 */
interface NotificationScheduler {
    /**
     * Планирует точное напоминание в AlarmManager для указанной активной заявки.
     */
    fun scheduleNotification(request: Request)

    /**
     * Отменяет запланированное напоминание в AlarmManager для указанной заявки.
     */
    fun cancelNotification(requestId: Long)
}
