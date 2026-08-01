package com.salmanlaghari.pulsemusicplayerai

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.salmanlaghari.pulsemusicplayerai.data.local.AudioScanner
import com.salmanlaghari.pulsemusicplayerai.data.repository.MusicRepository
import com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackConnectionManager
import com.salmanlaghari.pulsemusicplayerai.navigation.AppNavigation
import com.salmanlaghari.pulsemusicplayerai.presentation.MainViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.MainViewModelFactory
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModelFactory
import com.salmanlaghari.pulsemusicplayerai.presentation.youtube.YouTubeViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.youtube.YouTubeViewModelFactory
import com.salmanlaghari.pulsemusicplayerai.data.repository.YouTubeRepository
import coil.Coil
import coil.ImageLoader
import com.salmanlaghari.pulsemusicplayerai.theme.PulseMusicPlayerAITheme
import com.salmanlaghari.pulsemusicplayerai.utils.ThemePreferenceManager
import com.salmanlaghari.pulsemusicplayerai.utils.SongArtworkFetcher
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager

class MainActivity : ComponentActivity() {

    private val themePreferenceManager by lazy { ThemePreferenceManager(applicationContext) }
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(themePreferenceManager)
    }

    private val audioScanner by lazy { AudioScanner(applicationContext) }
    private val musicRepository by lazy { MusicRepository(applicationContext, audioScanner) }
    private val youTubeRepository by lazy { YouTubeRepository() }
    private val playbackConnectionManager by lazy { PlaybackConnectionManager(applicationContext) }
    private val musicViewModel: MusicViewModel by viewModels {
        MusicViewModelFactory(musicRepository, playbackConnectionManager)
    }
    private val youTubeViewModel: YouTubeViewModel by viewModels {
        YouTubeViewModelFactory(application, youTubeRepository, playbackConnectionManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 120fps Ultra Smooth Rendering — request highest refresh rate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display ?: return
            val modes = display.supportedModes
            val highestMode = modes.maxByOrNull { it.refreshRate }
            if (highestMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = highestMode.modeId
                window.attributes = params
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }

        // Enable hardware accelerated rendering for silky smooth animations
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        // Setup Coil image loader for premium custom artwork fetching and caching
        val imageLoader = ImageLoader.Builder(applicationContext)
            .components {
                add(SongArtworkFetcher.Factory(applicationContext))
            }
            .build()
        Coil.setImageLoader(imageLoader)

        // Show App Open Ad on launch
        AdManager.showAppOpen(this)

        setContent {
            val userDarkModePreference by mainViewModel.isDarkTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()
            val isDarkTheme = userDarkModePreference ?: systemInDarkTheme

            // Check if permission is already granted on launch
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            val hasPermission = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

            LaunchedEffect(key1 = hasPermission) {
                musicViewModel.setPermissionGranted(hasPermission)
            }

            PulseMusicPlayerAITheme(darkTheme = isDarkTheme) {
                Surface {
                    AppNavigation(
                        mainViewModel = mainViewModel,
                        musicViewModel = musicViewModel,
                        youTubeViewModel = youTubeViewModel,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Show interstitial ad when returning from background
        AdManager.showInterstitialResume(this)
    }
}
