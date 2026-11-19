package ru.andrew.application.notifications

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Момент срабатывания напоминания в epoch millis.
 *
 * Возвращает null, если будильник ставить не нужно: дата не задана или уже наступила.
 * В этом случае ранее поставленный будильник нужно снять, иначе он сработает по старому времени.
 */
internal fun reminderTriggerAtMillis(
    nextAction: LocalDateTime?,
    nowMillis: Long,
    zone: ZoneId
): Long? {
    val triggerAtMillis = nextAction?.atZone(zone)?.toInstant()?.toEpochMilli() ?: return null
    return triggerAtMillis.takeIf { it > nowMillis }
}
