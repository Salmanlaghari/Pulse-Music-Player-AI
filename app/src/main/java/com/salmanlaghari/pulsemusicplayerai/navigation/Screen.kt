package com.salmanlaghari.pulsemusicplayerai.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Library : Screen("library")
    object YouTube : Screen("youtube")
    object AudioTools : Screen("audio_tools")
    object AIHub : Screen("ai_hub")
    object Settings : Screen("settings")

    // Sub-screens under Settings
    object SettingsAbout : Screen("settings_about")
    object SettingsPrivacy : Screen("settings_privacy")
    object SettingsTerms : Screen("settings_terms")
    object SettingsFeedback : Screen("settings_feedback")
    object SettingsCrashLog : Screen("settings_crash_log")

    // Playback navigation entries
    object FullPlayer : Screen("full_player")
    object Search : Screen("search")
    object Queue : Screen("queue")
    object Equalizer : Screen("equalizer")

    // Embedded official YouTube player for the owner's channel videos
    object ChannelPlayer : Screen("channel_player/{videoId}/{title}")
}
