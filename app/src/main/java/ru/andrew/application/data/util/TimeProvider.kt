package ru.andrew.application.data.util

import java.time.LocalDateTime

/**
 * Интерфейс для предоставления текущего системного времени.
 * Позволяет абстрагировать получение времени для улучшения тестируемости (Unit-тесты).
 */
interface TimeProvider {
    fun getNow(): LocalDateTime
}

/**
 * Стандартная реализация TimeProvider, возвращающая текущее локальное время устройства.
 */
class SystemTimeProvider : TimeProvider {
    override fun getNow(): LocalDateTime {
        return LocalDateTime.now()
    }
}
