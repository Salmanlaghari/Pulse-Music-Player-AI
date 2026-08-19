package com.salmanlaghari.pulsemusicplayerai.data.premium

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists which premium ("Watch & Unlock") Audio Tools features a user has
 * unlocked by watching a rewarded ad.
 *
 * Unlock SCOPE: permanent-until-app-data-cleared.
 *   Once a feature is unlocked via a rewarded ad it stays unlocked for the
 *   lifetime of the app install (survives process death / app restart). It is
 *   cleared only when the user clears the app's data. This avoids re-prompting
 *   the user to watch an ad every session while still keeping the feature
 *   genuinely gated behind a completed reward.
 */
val Context.premiumUnlockDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "premium_unlocks"
)

/** Stable keys identifying each gated premium feature in Audio Tools. */
object PremiumFeature {
    const val VIDEO_STUDIO = "audio_tools_video_studio"
    const val COMPRESSOR = "audio_tools_compressor"
    const val SPEED_PITCH = "audio_tools_speed_pitch"
    const val EXPORT_1080P = "audio_tools_export_1080p"
}

class PremiumUnlockStore(private val context: Context) {

    /** Emits whether [feature] has been unlocked. Defaults to false. */
    fun isUnlocked(feature: String): Flow<Boolean> =
        context.premiumUnlockDataStore.data.map { prefs ->
            prefs[booleanPreferencesKey(feature)] == true
        }

    /** Persist that [feature] is now unlocked. */
    suspend fun unlock(feature: String) {
        context.premiumUnlockDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(feature)] = true
        }
    }
}
