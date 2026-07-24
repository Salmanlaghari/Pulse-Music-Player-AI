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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.salmanlaghari.pulsemusicplayerai.presentation.MainViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.VideoPreviewEditScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.AudioStudioViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.AudioStudioViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salmanlaghari.pulsemusicplayerai.domain.model.ExportedFile
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.aihub.AIHubScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.AudioToolsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.home.HomeScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.home.MiniPlayer
import com.salmanlaghari.pulsemusicplayerai.presentation.library.LibraryScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsAboutScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsFeedbackScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsPrivacyScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsTermsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.splash.SplashScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.EqualizerScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.FullPlayerScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.QueueScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.SearchScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Library : BottomNavItem(Screen.Library.route, "Library", Icons.Default.LibraryMusic)
    object AudioTools : BottomNavItem(Screen.AudioTools.route, "Audio Tools", Icons.Default.Tune)
    object AIHub : BottomNavItem(Screen.AIHub.route, "AI Hub", Icons.Default.AutoAwesome)
    object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    musicViewModel: MusicViewModel,
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
                    MiniPlayer(
                        viewModel = musicViewModel,
                        onExpand = { navController.navigate(Screen.FullPlayer.route) }
                    )

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
                HomeScreen(
                    viewModel = musicViewModel,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToPlayer = { navController.navigate(Screen.FullPlayer.route) },
                    onNavigateToAIHub = {
                        navController.navigate(Screen.AIHub.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToFavorites = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToEqualizer = {
                        navController.navigate(Screen.Equalizer.route)
                    }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(viewModel = musicViewModel)
            }
            composable(Screen.AudioTools.route) {
                AudioToolsScreen(onNavigateToPreview = { file ->
                    navController.navigate("video_preview_edit?videoUri=${file.uriString}&outputName=${file.name}&duration=${file.duration}&size=${file.size}")
                })
            }
            composable(Screen.AIHub.route) {
                AIHubScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChanged = { enabled ->
                        mainViewModel.setDarkTheme(enabled)
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

            // Playback routes
            composable(Screen.FullPlayer.route) {
                FullPlayerScreen(
                    viewModel = musicViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onShowQueue = { navController.navigate(Screen.Queue.route) },
                    onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) }
                )
            }
            composable(Screen.Equalizer.route) {
                EqualizerScreen(
                    viewModel = musicViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = musicViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Queue.route) {
                QueueScreen(
                    viewModel = musicViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "video_preview_edit?videoUri={videoUri}&outputName={outputName}&duration={duration}&size={size}",
                arguments = listOf(
                    navArgument("videoUri") { type = NavType.StringType; defaultValue = "" },
                    navArgument("outputName") { type = NavType.StringType; defaultValue = "" },
                    navArgument("duration") { type = NavType.LongType; defaultValue = 30000L },
                    navArgument("size") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                val videoUri = backStackEntry.arguments?.getString("videoUri") ?: ""
                val outputName = backStackEntry.arguments?.getString("outputName") ?: ""
                val duration = backStackEntry.arguments?.getLong("duration") ?: 30000L
                val size = backStackEntry.arguments?.getLong("size") ?: 0L

                val context = androidx.compose.ui.platform.LocalContext.current
                val studioViewModel: AudioStudioViewModel = viewModel(
                    factory = AudioStudioViewModelFactory(context.applicationContext)
                )

                val file = ExportedFile(
                    id = 0L,
                    name = outputName,
                    path = "",
                    uriString = videoUri,
                    size = size,
                    duration = duration,
                    format = "MP4",
                    dateAdded = System.currentTimeMillis()
                )

                VideoPreviewEditScreen(
                    file = file,
                    viewModel = studioViewModel,
                    onNavigateBackToStudio = { navController.popBackStack() },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
