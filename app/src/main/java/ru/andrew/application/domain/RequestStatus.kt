package ru.andrew.application.domain

import androidx.annotation.StringRes
import ru.andrew.application.R

enum class RequestStatus(@StringRes val displayNameResId: Int) {
    ACTIVE(R.string.status_active),
    COMPLETED(R.string.status_completed),
    CANCELLED(R.string.status_cancelled)
}
