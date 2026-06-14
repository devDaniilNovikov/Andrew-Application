package ru.andrew.application.notifications

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Безопасный потокобезопасный менеджер одноразовых криптографических токенов.
 * Используется для предотвращения уязвимостей типа Intent Redirection (подмены входящего Интента)
 * при запуске приложения из внешних источников.
 */
object SecureTokenManager {
    private val tokens = ConcurrentHashMap<Long, String>()

    /**
     * Генерирует уникальный токен для переданного ID запроса и сохраняет его в памяти.
     */
    fun generateAndSaveToken(requestId: Long): String {
        val token = UUID.randomUUID().toString()
        tokens[requestId] = token
        return token
    }

    /**
     * Верифицирует токен для переданного ID запроса.
     * Если токен валиден, он удаляется (токен является строго одноразовым).
     */
    fun validateAndConsumeToken(requestId: Long, token: String?): Boolean {
        if (token == null) return false
        val savedToken = tokens[requestId]
        if (savedToken != null && savedToken == token) {
            tokens.remove(requestId) // Одноразовое использование
            return true
        }
        return false
    }
}
