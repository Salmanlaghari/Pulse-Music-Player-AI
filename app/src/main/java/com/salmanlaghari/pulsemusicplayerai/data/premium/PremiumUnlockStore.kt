package com.salmanlaghari.pulsemusicplayerai.data.premium

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Session-scoped store for "Watch & Unlock" Audio Tools features.
 *
 * IMPORTANT: unlock state is intentionally NOT persisted (no DataStore /
 * SharedPreferences / file). It lives only for the lifetime of the current app
 * process, so a feature unlocked by watching a rewarded ad stays available for
 * THIS session but is reset on app restart. This enforces "watch an ad each
 * session" instead of a permanent, once-and-done unlock.
 *
 * State is held in a companion object so every PremiumUnlockStore instance
 * (AudioToolsScreen, StudioUtilityScreens, etc.) shares the same in-memory
 * flags — unlocking in one place reflects everywhere for the session.
 */
class PremiumUnlockStore(context: android.content.Context) {

    fun isUnlocked(feature: String): Flow<Boolean> = SessionState.flowFor(feature)

    suspend fun unlock(feature: String) {
        SessionState.unlock(feature)
    }

    /** Reset all session unlocks (e.g. for a manual "lock again" action). */
    fun clearSession() = SessionState.clear()

    private companion object SessionState {
        private val unlocked = mutableSetOf<String>()
        private val flows = mutableMapOf<String, MutableStateFlow<Boolean>>()

        @Synchronized
        fun flowFor(feature: String): Flow<Boolean> {
            return flows.getOrPut(feature) { MutableStateFlow(feature in unlocked) }
        }

        @Synchronized
        fun unlock(feature: String) {
            unlocked.add(feature)
            flows[feature]?.value = true
        }

        @Synchronized
        fun clear() {
            unlocked.clear()
            flows.values.forEach { it.value = false }
        }
    }
}
