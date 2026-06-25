package ru.andrew.application.ui.extensions

import androidx.annotation.StringRes
import ru.andrew.application.R
import ru.andrew.application.domain.ActionType
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.domain.RequestStatus

@get:StringRes
val RequestStatus.displayNameResId: Int
    get() = when (this) {
        RequestStatus.ACTIVE -> R.string.status_active
        RequestStatus.COMPLETED -> R.string.status_completed
        RequestStatus.CANCELLED -> R.string.status_cancelled
    }

@get:StringRes
val EquipmentType.displayNameResId: Int
    get() = when (this) {
        EquipmentType.CABIN -> R.string.equipment_cabin
        EquipmentType.COMMERCIAL -> R.string.equipment_commercial
        EquipmentType.INDUSTRIAL -> R.string.equipment_industrial
        EquipmentType.AIR_CONDITIONER -> R.string.equipment_air_conditioner
        EquipmentType.ICE_GENERATOR -> R.string.equipment_ice_generator
        EquipmentType.OTHER -> R.string.equipment_other
    }

@get:StringRes
val ActionType.displayNameResId: Int
    get() = when (this) {
        ActionType.CALL -> R.string.action_call
        ActionType.VISIT -> R.string.action_visit
        ActionType.DIAGNOSTICS -> R.string.action_diagnostics
        ActionType.REPAIR -> R.string.action_repair
        ActionType.AWAITING_PARTS -> R.string.action_awaiting_parts
        ActionType.CLARIFY_INFO -> R.string.action_clarify_info
        ActionType.OTHER -> R.string.action_other
    }
