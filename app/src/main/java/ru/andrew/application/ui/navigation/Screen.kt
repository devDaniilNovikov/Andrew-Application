package ru.andrew.application.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import ru.andrew.application.R

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    object Create : Screen("create?requestId={requestId}", R.string.nav_create, Icons.Default.Add)
    object Active : Screen("active", R.string.nav_active, Icons.Default.List)
    object History : Screen("history", R.string.nav_history, Icons.Default.Refresh)
}
