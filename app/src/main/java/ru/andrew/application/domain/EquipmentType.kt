package ru.andrew.application.domain

import androidx.annotation.StringRes
import ru.andrew.application.R

enum class EquipmentType(@StringRes val displayNameResId: Int) {
    DOMESTIC_REFRIGERATOR(R.string.equipment_domestic_refrigerator),
    COMMERCIAL_EQUIPMENT(R.string.equipment_commercial),
    INDUSTRIAL_EQUIPMENT(R.string.equipment_industrial),
    ICE_GENERATOR(R.string.equipment_ice_generator),
    FREEZER_CHAMBER(R.string.equipment_freezer_chamber),
    REFRIGERATED_SHOWCASE(R.string.equipment_refrigerated_showcase),
    SPLIT_SYSTEM(R.string.equipment_split_system),
    OTHER(R.string.equipment_other)
}
