package com.salmanlaghari.pulsemusicplayerai.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.salmanlaghari.pulsemusicplayerai.presentation.MainViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.AudioToolsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.home.HomeScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.home.MiniPlayer
import com.salmanlaghari.pulsemusicplayerai.presentation.library.LibraryScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.youtube.YouTubeScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.youtube.YouTubeViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsAboutScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsFeedbackScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsPrivacyScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.settings.SettingsTermsScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.splash.LoadingOverlay
import com.salmanlaghari.pulsemusicplayerai.presentation.splash.SplashScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.EqualizerScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.FullPlayerScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.QueueScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.SearchScreen
import com.salmanlaghari.pulsemusicplayerai.presentation.youtube.ChannelPlayerScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Library : BottomNavItem(Screen.Library.route, "Library", Icons.Default.LibraryMusic)
    object YouTube : BottomNavItem(Screen.YouTube.route, "YouTube", Icons.Default.OndemandVideo)
    object AudioTools : BottomNavItem(Screen.AudioTools.route, "Audio Tools", Icons.Default.Tune)
    object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    youTubeViewModel: YouTubeViewModel,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Loading state from MusicViewModel — shown as overlay during initial data load
    val isLoading by musicViewModel.isLoading.collectAsState()

    // Define bottom nav items
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Library,
        BottomNavItem.YouTube,
        BottomNavItem.AudioTools,
        BottomNavItem.Settings
    )

    // Only show bottom navigation and mini player on main screens (not splash/sub-screens)
    val showNavigationAndPlayer = currentRoute in listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.YouTube.route,
        Screen.AudioTools.route,
        Screen.Settings.route
    )

    // Show loading overlay when:
    //  - We're on the Home screen (or transitioning to it from splash)
    //  - MusicViewModel is still loading initial data
    //  - We're NOT still on the splash screen (splash has its own animation)
    val showLoadingOverlay = isLoading && currentRoute != Screen.Splash.route

    Box(modifier = Modifier.fillMaxSize()) {
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
                                        fontSize = 10.5.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
            modifier = Modifier.padding(innerPadding),
            // Premium page transition: smooth slide + fade
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(350)
                ) + fadeOut(animationSpec = tween(350))
            }
        ) {
            composable(
                route = Screen.Splash.route,
                enterTransition = { fadeIn(animationSpec = tween(600)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
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
                    onNavigateToYouTube = {
                        navController.navigate(Screen.YouTube.route) {
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
            composable(Screen.YouTube.route) {
                YouTubeScreen(
                    viewModel = youTubeViewModel,
                    onNavigateToPlayer = { navController.navigate(Screen.FullPlayer.route) },
                    onNavigateToChannelPlayer = { videoId, title ->
                        navController.navigate(
                            "channel_player/$videoId/${Uri.encode(title)}"
                        )
                    }
                )
            }
            composable(Screen.AudioTools.route) {
                AudioToolsScreen()
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
            composable(
                route = Screen.ChannelPlayer.route,
                arguments = listOf(
                    navArgument("videoId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                val title = backStackEntry.arguments?.getString("title")?.let { Uri.decode(it) } ?: ""
                ChannelPlayerScreen(
                    videoId = videoId,
                    title = title,
                    channelName = "A D&E Song Music",
                    onNavigateBack = { navController.popBackStack() }
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
        }
    }

        // Loading overlay — shown on top of everything during initial data load.
        // This prevents the user from seeing a blank/blue screen while the app
        // scans MediaStore and loads albums/artists (can take 5-30 seconds).
        if (showLoadingOverlay) {
            LoadingOverlay()
        }
    }
}
