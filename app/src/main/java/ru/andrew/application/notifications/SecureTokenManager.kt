package ru.andrew.application.notifications

import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Менеджер одноразовых токенов для deep link из уведомлений.
 *
 * MainActivity экспортирована (лаунчер), поэтому любое приложение может запустить её
 * с произвольным `requestId`. Токен подтверждает, что Intent пришёл из нашего уведомления.
 *
 * Токены хранятся в приватных SharedPreferences, а не в памяти: уведомление публикуется
 * из короткоживущего процесса [NotificationReceiver], и к моменту тапа процесс обычно уже убит.
 */
class SecureTokenManager(
    private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis
) {

    /**
     * Генерирует новый токен для заявки и синхронно сохраняет его на диск
     * (до публикации уведомления). Предыдущий токен этой заявки заменяется.
     */
    @Synchronized
    fun generateAndSaveToken(requestId: Long): String {
        val token = UUID.randomUUID().toString()
        val issuedAt = now()
        val editor = prefs.edit()
        pruneExpired(editor, issuedAt)
        editor.putString(key(requestId), "$token$SEPARATOR$issuedAt")
        editor.commit()
        return token
    }

    /**
     * Проверяет токен и удаляет его при любом исходе, кроме отсутствия записи:
     * токен строго одноразовый, перебор по одной записи невозможен.
     */
    @Synchronized
    fun validateAndConsumeToken(requestId: Long, token: String?): Boolean {
        if (token == null) return false
        val stored = prefs.getString(key(requestId), null) ?: return false
        prefs.edit().remove(key(requestId)).apply()

        val separatorIndex = stored.lastIndexOf(SEPARATOR)
        if (separatorIndex <= 0) return false
        val savedToken = stored.substring(0, separatorIndex)
        val issuedAt = stored.substring(separatorIndex + 1).toLongOrNull() ?: return false
        if (now() - issuedAt > TOKEN_TTL_MILLIS) return false

        return MessageDigest.isEqual(savedToken.toByteArray(), token.toByteArray())
    }

    private fun pruneExpired(editor: SharedPreferences.Editor, nowMillis: Long) {
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX)) return@forEach
            val issuedAt = (value as? String)
                ?.substringAfterLast(SEPARATOR, missingDelimiterValue = "")
                ?.toLongOrNull()
            if (issuedAt == null || nowMillis - issuedAt > TOKEN_TTL_MILLIS) {
                editor.remove(key)
            }
        }
    }

    private fun key(requestId: Long) = "$KEY_PREFIX$requestId"

    companion object {
        const val PREFS_NAME = "notification_tokens"
        private const val KEY_PREFIX = "token_"
        private const val SEPARATOR = '|'
        val TOKEN_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
