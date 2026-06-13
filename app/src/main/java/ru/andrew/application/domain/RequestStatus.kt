package ru.andrew.application.domain

import androidx.annotation.Keep

@Keep
enum class RequestStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
