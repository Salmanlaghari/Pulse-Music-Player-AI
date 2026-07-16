package com.salmanlaghari.pulsemusicplayerai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.salmanlaghari.pulsemusicplayerai.navigation.AppNavigation
import com.salmanlaghari.pulsemusicplayerai.presentation.MainViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.MainViewModelFactory
import com.salmanlaghari.pulsemusicplayerai.theme.PulseMusicPlayerAITheme
import com.salmanlaghari.pulsemusicplayerai.utils.ThemePreferenceManager

class MainActivity : ComponentActivity() {

    private val themePreferenceManager by lazy { ThemePreferenceManager(applicationContext) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(themePreferenceManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val userDarkModePreference by viewModel.isDarkTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()

            // Resolve actual theme selection: fallback to system dark theme if preference not set yet
            val isDarkTheme = userDarkModePreference ?: systemInDarkTheme

            PulseMusicPlayerAITheme(darkTheme = isDarkTheme) {
                Surface {
                    AppNavigation(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}
