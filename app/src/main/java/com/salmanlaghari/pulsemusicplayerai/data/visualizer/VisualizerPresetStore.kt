package com.salmanlaghari.pulsemusicplayerai.data.visualizer

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.VisualizerPreset
import com.salmanlaghari.pulsemusicplayerai.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

/**
 * Persistent store for the Visualizer Studio Pro preset picker.
 *
 * Two pieces of state, both backed by DataStore so they survive process
 * restarts (the "persists between sessions" requirement):
 *
 *  - [favorites]  — the set of [VisualizerPreset] names the user hearted.
 *  - [recentlyUsed] — the last N preset names the user selected, newest first.
 *
 * Both are exposed as [Flow]s so the picker Composable recomposes instantly
 * when the user hearted / picked a preset.
 */
class VisualizerPresetStore(private val context: android.content.Context) {

    private companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("viz_favorites")
        private val RECENT_KEY = stringSetPreferencesKey("viz_recently_used")
        private const val MAX_RECENT = 12
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    /** All favourited preset display names, as a live set. */
    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[FAVORITES_KEY] ?: emptySet() }

    /** Recently-used preset display names, newest first (capped at [MAX_RECENT]). */
    val recentlyUsed: Flow<Set<String>> = context.dataStore.data.map { it[RECENT_KEY] ?: emptySet() }

    /** Ordered, newest-first list of recently used presets (for the Recent row). */
    val recentlyUsedOrdered: Flow<List<String>> = recentlyUsed.map { it.toList() }

    private val _favoritesState = MutableStateFlow<Set<String>>(emptySet())
    val favoritesState: Flow<Set<String>> = _favoritesState.asStateFlow()

    init {
        scope.launch {
            favorites.collect { _favoritesState.value = it }
        }
    }

    /** Toggle the heart/favourite state for a preset. */
    suspend fun toggleFavorite(preset: VisualizerPreset) {
        context.dataStore.edit { prefs ->
            val current = (prefs[FAVORITES_KEY] ?: emptySet()).toMutableSet()
            if (!current.add(preset.displayName)) current.remove(preset.displayName)
            prefs[FAVORITES_KEY] = current
        }
    }

    /** Whether a preset is currently favourited. */
    suspend fun isFavorite(preset: VisualizerPreset): Boolean {
        return preset.displayName in (context.dataStore.data.first()[FAVORITES_KEY] ?: emptySet())
    }

    /** Record that the user just selected [preset] (newest first, capped). */
    suspend fun recordRecent(preset: VisualizerPreset) {
        context.dataStore.edit { prefs ->
            val current = (prefs[RECENT_KEY] ?: emptySet()).toMutableList()
            current.removeAll { it == preset.displayName }
            current.add(0, preset.displayName)
            if (current.size > MAX_RECENT) current.subList(MAX_RECENT, current.size).clear()
            prefs[RECENT_KEY] = current.toSet()
        }
    }
}