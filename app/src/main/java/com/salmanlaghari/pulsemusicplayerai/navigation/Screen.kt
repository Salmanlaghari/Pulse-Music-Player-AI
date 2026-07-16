package com.salmanlaghari.pulsemusicplayerai.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Library : Screen("library")
    object AudioTools : Screen("audio_tools")
    object AIHub : Screen("ai_hub")
    object Settings : Screen("settings")

    // Sub-screens under Settings
    object SettingsAbout : Screen("settings_about")
    object SettingsPrivacy : Screen("settings_privacy")
    object SettingsTerms : Screen("settings_terms")
    object SettingsFeedback : Screen("settings_feedback")

    // Playback navigation entries
    object FullPlayer : Screen("full_player")
    object Search : Screen("search")
    object Queue : Screen("queue")
}
