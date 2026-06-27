package com.example.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.*
import com.example.viewmodel.FocusViewModel

import androidx.compose.runtime.collectAsState

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val ACHIEVEMENTS = "achievements"
    const val RANKING = "ranking"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val FOCUS_MODE = "focus_mode/{goalId}"
    const val ANALYTICS = "analytics"
    const val DUELS = "duels"
    
    fun createFocusRoute(goalId: Int) = "focus_mode/$goalId"
}

sealed class BottomNavItem(var route: String, var icon: ImageVector, var selectedIcon: ImageVector, var translationKey: String) {
    object Dashboard : BottomNavItem(Routes.DASHBOARD, Icons.Outlined.Home, Icons.Filled.Home, "tab_home")
    object Achievements : BottomNavItem(Routes.ACHIEVEMENTS, Icons.Outlined.Star, Icons.Filled.Star, "tab_achievements")
    object Ranking : BottomNavItem(Routes.RANKING, Icons.Outlined.List, Icons.Filled.List, "tab_ranking")
    object Duels : BottomNavItem(Routes.DUELS, Icons.Outlined.Shield, Icons.Filled.Shield, "tab_duels")
    object Profile : BottomNavItem(Routes.PROFILE, Icons.Outlined.Person, Icons.Filled.Person, "tab_profile")
}

@Composable
fun AppNavigation(viewModel: FocusViewModel) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val lang by viewModel.language.collectAsState()

    val isBottomBarVisible = currentDestination?.route in listOf(
        Routes.DASHBOARD,
        Routes.ACHIEVEMENTS,
        Routes.RANKING,
        Routes.DUELS,
        Routes.PROFILE
    )

    val focusSleepEnabled by viewModel.focusSleepEnabled.collectAsState()
    val forceSleepSimulation by viewModel.forceSleepSimulation.collectAsState()

    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val isSleepTime = currentHour >= 23 || currentHour < 6
    val isLocked = focusSleepEnabled && (isSleepTime || forceSleepSimulation) && currentDestination?.route != Routes.LOGIN

    if (isLocked) {
        FocusSleepLockScreen(viewModel)
    } else {
        Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    val items = listOf(
                        BottomNavItem.Dashboard,
                        BottomNavItem.Achievements,
                        BottomNavItem.Ranking,
                        BottomNavItem.Duels,
                        BottomNavItem.Profile
                    )
                    items.forEach { item ->
                        val labelText = LocalizedStrings.get(item.translationKey, lang)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentDestination?.hierarchy?.any { it.route == item.route } == true) item.selectedIcon else item.icon,
                                    contentDescription = labelText
                                )
                            },
                            label = { Text(labelText) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onStartFocus = { goal ->
                        navController.navigate(Routes.createFocusRoute(goal.id))
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Routes.ANALYTICS)
                    }
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(viewModel = viewModel)
            }
            composable(Routes.RANKING) {
                RankingScreen(viewModel = viewModel)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.FOCUS_MODE,
                arguments = listOf(navArgument("goalId") { type = NavType.IntType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getInt("goalId") ?: return@composable
                FocusModeScreen(
                    goalId = goalId,
                    viewModel = viewModel,
                    onComplete = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.DUELS) {
                DuelsScreen(viewModel = viewModel)
            }
        }
    }
}
}
