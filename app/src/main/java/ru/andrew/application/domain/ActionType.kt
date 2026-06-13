package ru.andrew.application.domain

enum class ActionType(val displayName: String) {
    CALL("позвонить"),
    VISIT("приехать"),
    DIAGNOSTICS("диагностика"),
    REPAIR("ремонт"),
    AWAITING_PARTS("ожидание детали"),
    CLARIFY_INFO("уточнить информацию"),
    OTHER("другое")
}
