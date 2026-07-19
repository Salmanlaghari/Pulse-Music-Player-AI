package com.salmanlaghari.pulsemusicplayerai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import coil.Coil
import coil.ImageLoader
import com.salmanlaghari.pulsemusicplayerai.theme.PulseMusicPlayerAITheme
import com.salmanlaghari.pulsemusicplayerai.utils.ThemePreferenceManager
import com.salmanlaghari.pulsemusicplayerai.utils.SongArtworkFetcher

class MainActivity : ComponentActivity() {

    private val themePreferenceManager by lazy { ThemePreferenceManager(applicationContext) }
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(themePreferenceManager)
    }

    private val audioScanner by lazy { AudioScanner(applicationContext) }
    private val musicRepository by lazy { MusicRepository(applicationContext, audioScanner) }
    private val playbackConnectionManager by lazy { PlaybackConnectionManager(applicationContext) }
    private val musicViewModel: MusicViewModel by viewModels {
        MusicViewModelFactory(musicRepository, playbackConnectionManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup Coil image loader for premium custom artwork fetching and caching
        val imageLoader = ImageLoader.Builder(applicationContext)
            .components {
                add(SongArtworkFetcher.Factory(applicationContext))
            }
            .build()
        Coil.setImageLoader(imageLoader)

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
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}
