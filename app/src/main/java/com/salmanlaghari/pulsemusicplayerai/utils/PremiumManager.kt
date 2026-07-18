package com.salmanlaghari.pulsemusicplayerai.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.premiumDataStore by preferencesDataStore(name = "premium_preferences")

enum class UserTier {
    GUEST,
    BASIC,
    PREMIUM
}

class PremiumManager(private val context: Context) {

    companion object {
        private val TIER_KEY = stringPreferencesKey("user_tier")
    }

    private val _sessionTier = MutableStateFlow<UserTier?>(null)
    val sessionTier: StateFlow<UserTier?> = _sessionTier.asStateFlow()

    val persistedTierFlow: Flow<UserTier> = context.premiumDataStore.data.map { preferences ->
        val tierName = preferences[TIER_KEY] ?: UserTier.GUEST.name
        try {
            UserTier.valueOf(tierName)
        } catch (e: Exception) {
            UserTier.GUEST
        }
    }

    suspend fun getActiveTier(): UserTier {
        return _sessionTier.value ?: persistedTierFlow.first()
    }

    suspend fun setPersistedTier(tier: UserTier) {
        context.premiumDataStore.edit { preferences ->
            preferences[TIER_KEY] = tier.name
        }
    }

    fun setTemporarySessionTier(tier: UserTier?) {
        _sessionTier.value = tier
    }

    suspend fun resetToDefault() {
        setPersistedTier(UserTier.GUEST)
        _sessionTier.value = null
    }
}
