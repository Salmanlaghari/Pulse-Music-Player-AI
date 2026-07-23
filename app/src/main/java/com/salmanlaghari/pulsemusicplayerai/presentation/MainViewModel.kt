package com.salmanlaghari.pulsemusicplayerai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.salmanlaghari.pulsemusicplayerai.utils.ThemePreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class MainViewModel(private val themePreferenceManager: ThemePreferenceManager) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferenceManager.isDarkModeEnabled.collect { isEnabled ->
                _isDarkTheme.value = isEnabled
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            themePreferenceManager.setDarkMode(enabled)
        }
    }
}

class MainViewModelFactory(private val themePreferenceManager: ThemePreferenceManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(themePreferenceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
