package ru.andrew.application.domain

enum class EquipmentType(val displayName: String) {
    DOMESTIC_REFRIGERATOR("бытовой холодильник"),
    COMMERCIAL_EQUIPMENT("торговое оборудование"),
    INDUSTRIAL_EQUIPMENT("промышленное оборудование"),
    ICE_GENERATOR("ледогенератор"),
    FREEZER_CHAMBER("морозильная камера"),
    REFRIGERATED_SHOWCASE("холодильная витрина"),
    SPLIT_SYSTEM("сплит-система / кондиционер"),
    OTHER("другое")
}
