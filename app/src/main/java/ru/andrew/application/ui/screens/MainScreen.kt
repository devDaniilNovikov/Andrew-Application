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
import ru.andrew.application.ui.navigation.EditRequestRoute
import ru.andrew.application.ui.navigation.Screen
import ru.andrew.application.ui.navigation.navigateToTab
import ru.andrew.application.ui.theme.AppTheme

@Composable
fun MainScreen(
    currentTheme: AppTheme,
    deepLinkRequestId: Long? = null,
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
    // Экран редактирования открыт поверх вкладки — подсвечиваем вкладку, с которой его открыли
    val selectedTabRoute = if (currentRoute == EditRequestRoute.PATTERN) {
        navController.previousBackStackEntry?.destination?.route
    } else {
        currentRoute
    }

    val navigationItems = listOf(
        Screen.Create,
        Screen.Active,
        Screen.History
    )

    // Тап по уведомлению: переходим на «Список». Карточку откроет сам экран списка, когда загрузит
    // заявки, и сразу сбросит запрос — иначе она всплывала бы при каждом изменении списка.
    LaunchedEffect(deepLinkRequestId) {
        if (deepLinkRequestId != null &&
            navController.currentDestination?.route != Screen.Active.route
        ) {
            navController.navigateToTab(Screen.Active.route)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navigationItems.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedTabRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigateToTab(screen.route)
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
            composable(Screen.Create.route) {
                CreateRequestScreen(navController = navController)
            }
            composable(
                route = EditRequestRoute.PATTERN,
                arguments = listOf(
                    navArgument(EditRequestRoute.ARG_REQUEST_ID) {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getLong(EditRequestRoute.ARG_REQUEST_ID) ?: -1L
                CreateRequestScreen(navController = navController, requestId = requestId)
            }
            composable(Screen.Active.route) {
                ActiveRequestsScreen(
                    navController = navController,
                    deepLinkRequestId = deepLinkRequestId,
                    onDeepLinkConsumed = onDeepLinkHandled,
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    navController = navController,
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected
                )
            }
        }
    }
}
