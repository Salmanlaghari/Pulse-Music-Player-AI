package com.salmanlaghari.pulsemusicplayerai.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.salmanlaghari.pulsemusicplayerai.presentation.MainViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.aihub.AIHubScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.AudioToolsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.home.HomeScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.home.MiniPlayerPlaceholder
import com.salmanlaghari.pulsemusicplayerai.presentation.library.LibraryScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsAboutScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsFeedbackScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsPrivacyScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsTermsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.splash.SplashScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Library : BottomNavItem(Screen.Library.route, "Library", Icons.Default.LibraryMusic)
    object AudioTools : BottomNavItem(Screen.AudioTools.route, "Audio Tools", Icons.Default.Tune)
    object AIHub : BottomNavItem(Screen.AIHub.route, "AI Hub", Icons.Default.AutoAwesome)
    object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define bottom nav items
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Library,
        BottomNavItem.AudioTools,
        BottomNavItem.AIHub,
        BottomNavItem.Settings
    )

    // Only show bottom navigation and mini player on main screens (not splash/sub-screens)
    val showNavigationAndPlayer = currentRoute in listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.AudioTools.route,
        Screen.AIHub.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showNavigationAndPlayer) {
                Column {
                    // Modern Mini Player Floating over Bottom Navigation
                    MiniPlayerPlaceholder()

                    NavigationBar(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
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
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Library.route) {
                LibraryScreen()
            }
            composable(Screen.AudioTools.route) {
                AudioToolsScreen()
            }
            composable(Screen.AIHub.route) {
                AIHubScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChanged = { enabled ->
                        viewModel.setDarkTheme(enabled)
                    },
                    onNavigateToAbout = { navController.navigate(Screen.SettingsAbout.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.SettingsPrivacy.route) },
                    onNavigateToTerms = { navController.navigate(Screen.SettingsTerms.route) },
                    onNavigateToFeedback = { navController.navigate(Screen.SettingsFeedback.route) }
                )
            }
            composable(Screen.SettingsAbout.route) {
                SettingsAboutScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsPrivacy.route) {
                SettingsPrivacyScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsTerms.route) {
                SettingsTermsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsFeedback.route) {
                SettingsFeedbackScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
