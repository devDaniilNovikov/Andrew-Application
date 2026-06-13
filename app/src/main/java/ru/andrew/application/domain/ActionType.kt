package ru.andrew.application.domain

import androidx.annotation.Keep

@Keep
enum class ActionType {
    CALL,
    VISIT,
    DIAGNOSTICS,
    REPAIR,
    AWAITING_PARTS,
    CLARIFY_INFO,
    OTHER
}
