package ru.andrew.application.domain

import androidx.annotation.StringRes
import ru.andrew.application.R

enum class ActionType(@StringRes val displayNameResId: Int) {
    CALL(R.string.action_call),
    VISIT(R.string.action_visit),
    DIAGNOSTICS(R.string.action_diagnostics),
    REPAIR(R.string.action_repair),
    AWAITING_PARTS(R.string.action_awaiting_parts),
    CLARIFY_INFO(R.string.action_clarify_info),
    OTHER(R.string.action_other)
}
