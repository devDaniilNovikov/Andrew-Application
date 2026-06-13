package ru.andrew.application.domain

enum class RequestStatus(val displayName: String) {
    ACTIVE("Активная"),
    COMPLETED("Выполнена"),
    CANCELLED("Отменена")
}
