package ru.andrew.application.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ru.andrew.application.MainActivity
import ru.andrew.application.R
import ru.andrew.application.data.entity.Request
import ru.andrew.application.domain.ActionType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Вспомогательный класс для работы с системными уведомлениями Android.
 * Отвечает за создание канала уведомлений и отправку локальных пушей в стиле Material 3.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "request_actions"

    /**
     * Создает высокоприоритетный канал уведомлений (для Android 8.0+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Отображает локальное пуш-уведомление для конкретной заявки.
     */
    fun showNotification(context: Context, request: Request) {
        // Гарантируем, что канал уведомлений создан
        createNotificationChannel(context)

        // Локализация типа действия для заголовка
        val actionPrefix = when (request.actionType) {
            ActionType.CALL -> context.getString(R.string.action_call)
            ActionType.VISIT -> context.getString(R.string.action_visit)
            ActionType.DIAGNOSTICS -> context.getString(R.string.action_diagnostics)
            ActionType.REPAIR -> context.getString(R.string.action_repair)
            ActionType.AWAITING_PARTS -> context.getString(R.string.action_awaiting_parts)
            ActionType.CLARIFY_INFO -> context.getString(R.string.action_clarify_info)
            ActionType.OTHER, null -> context.getString(R.string.action_other)
        }

        // Преобразуем первую букву действия в заглавную
        val capitalizedAction = actionPrefix.replaceFirstChar { it.uppercase() }
        val titleText = "$capitalizedAction: ${request.title}"

        // Форматируем время выполнения напоминания
        val formattedTime = request.nextActionDateTime?.let { formatNotificationDateTime(context, it) } ?: context.getString(R.string.not_assigned)
        
        // Маскируем номер телефона клиента для защиты персональных данных (PII)
        val maskedPhone = maskPhoneNumber(request.phone)
        val contentText = "$formattedTime\n$maskedPhone"

        // Генерируем одноразовый криптографический токен безопасности для защиты от Intent Redirection
        val token = SecureTokenManager.generateAndSaveToken(request.id)

        // Настраиваем Intent для запуска MainActivity с передачей ID заявки и защитного токена
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("requestId", request.id)
            putExtra("secure_token", token)
        }
        
        // Создаем PendingIntent для обработки тапа по уведомлению
        val pendingIntent = PendingIntent.getActivity(
            context,
            request.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Строим уведомление в премиальном дизайне Material 3
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Использование стандартной иконки будильника
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // Скрывать подробности на экране блокировки
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(request.id.toInt(), notification)
    }

    /**
     * Маскирует номер телефона (например, +7 (999) 123-45-67 -> +7 (999) ***-45-67) для защиты PII.
     */
    private fun maskPhoneNumber(phone: String): String {
        val trimmed = phone.trim()
        return if (trimmed.length >= 7) {
            val start = trimmed.length - 7
            val end = trimmed.length - 4
            trimmed.substring(0, start) + "***" + trimmed.substring(end)
        } else {
            trimmed
        }
    }

    /**
     * Форматирует дату напоминания в понятную относительную форму (Сегодня, Завтра или DD.MM.YYYY).
     */
    private fun formatNotificationDateTime(context: Context, dateTime: LocalDateTime): String {
        val today = LocalDateTime.now().toLocalDate()
        val targetDate = dateTime.toLocalDate()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val formattedTime = dateTime.format(timeFormatter)

        return when (targetDate) {
            today -> {
                val todayLabel = context.getString(R.string.notification_today)
                "$todayLabel $formattedTime"
            }
            today.plusDays(1) -> {
                val tomorrowLabel = context.getString(R.string.notification_tomorrow)
                "$tomorrowLabel $formattedTime"
            }
            else -> {
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                "${dateTime.format(dateFormatter)} $formattedTime"
            }
        }
    }
}
