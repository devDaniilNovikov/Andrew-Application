package ru.andrew.application.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Переход на вкладку нижнего меню с сохранением состояния вкладок.
 *
 * Незавершённое редактирование при этом отбрасывается без сохранения: исходная заявка не меняется,
 * а сессия редактирования не всплывёт позже на другой вкладке поверх неё.
 */
fun NavController.navigateToTab(route: String) {
    popBackStack(EditRequestRoute.PATTERN, inclusive = true, saveState = false)
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
