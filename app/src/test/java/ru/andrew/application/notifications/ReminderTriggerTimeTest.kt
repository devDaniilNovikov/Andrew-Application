package ru.andrew.application.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Решение «ставить будильник или снять старый» для NotificationSchedulerImpl.scheduleNotification.
 * null означает, что ранее поставленный будильник нужно отменить.
 */
class ReminderTriggerTimeTest {

    private val zone = ZoneId.of("Europe/Moscow")
    private val now = LocalDateTime.of(2026, 11, 19, 12, 0)
    private val nowMillis = now.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun futureTimeReturnsTriggerMoment() {
        val nextAction = now.plusHours(3)

        assertEquals(
            nextAction.atZone(zone).toInstant().toEpochMilli(),
            reminderTriggerAtMillis(nextAction, nowMillis, zone)
        )
    }

    @Test
    fun rescheduleToPastReturnsNullSoOldAlarmIsCancelled() {
        assertNull(reminderTriggerAtMillis(now.minusMinutes(1), nowMillis, zone))
    }

    @Test
    fun currentMomentIsNotScheduled() {
        assertNull(reminderTriggerAtMillis(now, nowMillis, zone))
    }

    @Test
    fun missingDateReturnsNull() {
        assertNull(reminderTriggerAtMillis(null, nowMillis, zone))
    }
}
