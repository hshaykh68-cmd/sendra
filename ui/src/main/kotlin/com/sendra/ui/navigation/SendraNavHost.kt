package com.sendra.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sendra.domain.model.FileInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.sendra.ui.screens.splash.SplashScreen
import com.sendra.ui.screens.home.HomeScreen
import com.sendra.ui.screens.radar.EnhancedRadarScreen
import com.sendra.ui.screens.transfer.EnhancedTransferScreen
import com.sendra.ui.screens.receiver.ReceiverScreen
import com.sendra.ui.screens.history.HistoryScreen
import com.sendra.ui.screens.settings.SettingsScreen

@Composable
fun SendraNavHost(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?") ?: "home"
    
    // Show bottom nav only on main screens (not splash, receiver, transfer)
    val showBottomNav = currentRoute in listOf("home", "radar", "history", "settings")
    
    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                // Pop up to the start destination to avoid building a large back stack
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(padding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToRadar = { files ->
                        navController.navigate(Screen.Radar.createRoute(files))
                    },
                    onNavigateToReceiver = {
                        navController.navigate(Screen.Receiver.route)
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
                EnhancedRadarScreen(
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
                EnhancedTransferScreen(
                    onNavigateBack = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                )
            }
            
            composable(Screen.Receiver.route) {
                ReceiverScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
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
    object Receiver : Screen("receiver")
    object History : Screen("history")
    object Settings : Screen("settings")
}
