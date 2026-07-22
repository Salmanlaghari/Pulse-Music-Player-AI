package com.salmanlaghari.pulsemusicplayerai.core.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

class AudioEffectManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var isEqSupported = false
        private set
    var isBassSupported = false
        private set
    var isVirtualizerSupported = false
        private set
    var isLoudnessSupported = false
        private set

    // Cached state to return safe fallbacks if hardware effects fail
    var bassStrength: Short = 0
        private set
    var virtualizerStrength: Short = 0
        private set
    var loudnessGainDb: Float = 0f
        private set

    private val bandLevels = mutableMapOf<Short, Short>()

    fun initEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return

        // 1. Equalizer Init
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            isEqSupported = true
            // Cache starting levels
            val bandsCount = equalizer?.numberOfBands ?: 0
            for (i in 0 until bandsCount) {
                val band = i.toShort()
                bandLevels[band] = equalizer?.getBandLevel(band) ?: 0
            }
        } catch (e: Exception) {
            isEqSupported = false
            Log.e("AudioEffectManager", "Equalizer hardware not supported or initialized: ${e.message}")
        }

        // 2. Bass Boost Init
        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            isBassSupported = true
            bassStrength = bassBoost?.roundedStrength ?: 0
        } catch (e: Exception) {
            isBassSupported = false
            Log.e("AudioEffectManager", "BassBoost hardware not supported: ${e.message}")
        }

        // 3. Virtualizer Init
        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
            }
            isVirtualizerSupported = true
            virtualizerStrength = virtualizer?.roundedStrength ?: 0
        } catch (e: Exception) {
            isVirtualizerSupported = false
            Log.e("AudioEffectManager", "Virtualizer hardware not supported: ${e.message}")
        }

        // 4. Loudness Enhancer Init
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
            }
            isLoudnessSupported = true
        } catch (e: Exception) {
            isLoudnessSupported = false
            Log.e("AudioEffectManager", "LoudnessEnhancer hardware not supported: ${e.message}")
        }
    }

    // --- Equalizer Controls ---
    fun getBandsCount(): Int = equalizer?.numberOfBands?.toInt() ?: 5

    fun getBandFrequency(band: Short): Int {
        return equalizer?.getCenterFreq(band) ?: (band.toInt() * 1000 + 60)
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val range = equalizer?.bandLevelRange
        return if (range != null && range.size >= 2) {
            Pair(range[0], range[1])
        } else {
            Pair(-1500, 1500) // Default standard milliBels (-15dB to +15dB)
        }
    }

    fun getBandLevel(band: Short): Short {
        return if (isEqSupported) {
            equalizer?.getBandLevel(band) ?: 0
        } else {
            bandLevels[band] ?: 0
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        bandLevels[band] = level
        if (isEqSupported) {
            try {
                equalizer?.setBandLevel(band, level)
            } catch (e: Exception) {
                Log.e("AudioEffectManager", "Error setting EQ band level: ${e.message}")
            }
        }
    }

    // --- Bass Boost Controls ---
    fun setBassStrength(strength: Short) {
        bassStrength = strength
        if (isBassSupported) {
            try {
                bassBoost?.setStrength(strength)
            } catch (e: Exception) {
                Log.e("AudioEffectManager", "Error setting bass strength: ${e.message}")
            }
        }
    }

    // --- Virtualizer Controls ---
    fun setVirtualizerStrength(strength: Short) {
        virtualizerStrength = strength
        if (isVirtualizerSupported) {
            try {
                virtualizer?.setStrength(strength)
            } catch (e: Exception) {
                Log.e("AudioEffectManager", "Error setting virtualizer strength: ${e.message}")
            }
        }
    }

    // --- Loudness Enhancer Controls ---
    fun setLoudnessGain(gainDb: Float) {
        loudnessGainDb = gainDb
        if (isLoudnessSupported) {
            try {
                // gainDb is specified in millibels, e.g. 1dB = 100mB
                val gainMb = (gainDb * 100).toInt()
                loudnessEnhancer?.setTargetGain(gainMb)
            } catch (e: Exception) {
                Log.e("AudioEffectManager", "Error setting loudness gain: ${e.message}")
            }
        }
    }

    // --- Preset Apply ---
    fun applyPreset(presetName: String) {
        // Simple mapping representing standard preset band curves (dB delta levels)
        val presetCurves = mapOf(
            "Normal" to listOf(0, 0, 0, 0, 0),
            "Classical" to listOf(5, 3, -1, 4, 4),
            "Dance" to listOf(6, 0, 2, 4, 1),
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Folk" to listOf(3, 0, 0, 2, -1),
            "Heavy Metal" to listOf(4, 1, 9, 3, 0),
            "Hip Hop" to listOf(5, 3, 0, 1, 3),
            "Jazz" to listOf(4, 2, -2, 2, 5),
            "Pop" to listOf(-1, 2, 5, 1, -2),
            "Rock" to listOf(5, 3, -1, 3, 5)
        )

        val curve = presetCurves[presetName] ?: return
        val bandsCount = getBandsCount()
        for (i in 0 until bandsCount) {
            val band = i.toShort()
            val gainFactor = curve.getOrNull(i) ?: 0
            // Convert dB to milliBels (1dB = 100 mB)
            val milliBels = (gainFactor * 100).toShort()
            setBandLevel(band, milliBels)
        }
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        loudnessEnhancer?.release()

        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
