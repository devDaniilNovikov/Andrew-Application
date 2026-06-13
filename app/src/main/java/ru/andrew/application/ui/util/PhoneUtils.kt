package ru.andrew.application.ui.util

/**
 * Вспомогательная функция для красивого форматирования номера телефона.
 */
fun formatPhoneNumber(phone: String): String {
    val clean = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
    return when {
        clean.length == 11 && (clean.startsWith("7") || clean.startsWith("8")) -> {
            val prefix = if (clean.startsWith("7")) "+7" else "8"
            "$prefix (${clean.substring(1, 4)}) ${clean.substring(4, 7)}-${clean.substring(7, 9)}-${clean.substring(9, 11)}"
        }
        clean.length == 12 && clean.startsWith("+7") -> {
            "+7 (${clean.substring(2, 5)}) ${clean.substring(5, 8)}-${clean.substring(8, 10)}-${clean.substring(10, 12)}"
        }
        else -> phone
    }
}

/**
 * Очищает номер телефона от пробелов, скобок, дефисов.
 */
fun cleanPhoneNumber(phone: String): String {
    return phone.replace(Regex("[\\s\\-\\(\\)]"), "")
}
