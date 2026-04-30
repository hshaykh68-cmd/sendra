package com.sendra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sendra.domain.model.FileInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.sendra.ui.screens.home.HomeScreen
import com.sendra.ui.screens.radar.RadarScreen
import com.sendra.ui.screens.transfer.TransferScreen

@Composable
fun SendraNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToRadar = { files ->
                    navController.navigate(Screen.Radar.createRoute(files))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.Radar.route,
            arguments = listOf(
                navArgument("files") {
                    type = NavType.StringType
                    defaultValue = "[]"
                }
            )
        ) { backStackEntry ->
            RadarScreen(
                onNavigateToTransfer = { sessionId ->
                    navController.navigate(Screen.Transfer.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Transfer.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            TransferScreen(
                onNavigateBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
        
        composable(Screen.History.route) {
            // HistoryScreen implementation
        }
        
        composable(Screen.Settings.route) {
            // SettingsScreen implementation
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Radar : Screen("radar?files={files}") {
        fun createRoute(files: List<FileInfo>): String {
            val json = Json.encodeToString(files)
            return "radar?files=$json"
        }
    }
    object Transfer : Screen("transfer/{sessionId}") {
        fun createRoute(sessionId: String): String = "transfer/$sessionId"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
}
