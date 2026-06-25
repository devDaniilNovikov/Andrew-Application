package ru.andrew.application.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import ru.andrew.application.ui.navigation.Screen
import ru.andrew.application.ui.theme.AppTheme

@Composable
fun MainScreen(
    currentTheme: AppTheme,
    deepLinkRequestId: Long = -1L,
    onThemeSelected: (AppTheme) -> Unit,
    onDeepLinkHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { _ -> }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        Screen.Create,
        Screen.Active,
        Screen.History
    )

    LaunchedEffect(deepLinkRequestId) {
        if (deepLinkRequestId != -1L) {
            navController.navigate("active?deepLinkRequestId=$deepLinkRequestId") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onDeepLinkHandled()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navigationItems.forEach { screen ->
                    val isSelected = currentRoute?.substringBefore('?') == screen.route.substringBefore('?')
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            val baseRoute = screen.route.substringBefore('?')
                            val currentBaseRoute = currentRoute?.substringBefore('?')
                            if (currentBaseRoute != baseRoute) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(id = screen.titleResId)
                            )
                        },
                        label = {
                            Text(text = stringResource(id = screen.titleResId))
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Active.route, // Task 2.3: "Список" is the start tab destination
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(
                route = Screen.Create.route,
                arguments = listOf(
                    navArgument("requestId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getLong("requestId") ?: -1L
                CreateRequestScreen(navController = navController, requestId = requestId)
            }
            composable(
                route = "active?deepLinkRequestId={deepLinkRequestId}",
                arguments = listOf(
                    navArgument("deepLinkRequestId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val deepLinkRequestIdArg = backStackEntry.arguments?.getLong("deepLinkRequestId") ?: -1L
                ActiveRequestsScreen(
                    navController = navController,
                    deepLinkRequestId = deepLinkRequestIdArg,
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected
                )
            }
        }
    }
}
