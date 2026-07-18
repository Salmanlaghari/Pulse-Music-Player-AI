package com.salmanlaghari.pulsemusicplayerai.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lyricsDataStore by preferencesDataStore(name = "lyrics_cache")

data class LyricLine(val timestampMs: Long, val text: String)

class LyricsRepository(private val context: Context) {

    companion object {
        private fun getCacheKey(title: String, artist: String): String {
            return "lyrics_${title.lowercase().trim().replace(Regex("[^a-z0-9]"), "_")}_${artist.lowercase().trim().replace(Regex("[^a-z0-9]"), "_")}"
        }
    }

    suspend fun getLyrics(title: String, artist: String, durationMs: Long): List<LyricLine> {
        val cacheKey = getCacheKey(title, artist)
        val prefKey = stringPreferencesKey(cacheKey)

        val cachedLrc = context.lyricsDataStore.data.map { it[prefKey] }.first()
        val lrcString = if (cachedLrc != null) {
            cachedLrc
        } else {
            val generated = generateLrcForSong(title, artist, durationMs)
            context.lyricsDataStore.edit { it[prefKey] = generated }
            generated
        }

        return parseLrc(lrcString)
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = lrcText.split("\n")
        val parsedLines = mutableListOf<LyricLine>()
        val timeRegex = Regex("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]")
        for (line in lines) {
            val matchResult = timeRegex.find(line)
            if (matchResult != null) {
                val min = matchResult.groupValues[1].toLongOrNull() ?: 0L
                val sec = matchResult.groupValues[2].toLongOrNull() ?: 0L
                val msVal = matchResult.groupValues[3]?.toLongOrNull() ?: 0L
                val msFactor = if (matchResult.groupValues[3]?.length == 2) 10L else 1L
                val timestampMs = (min * 60 + sec) * 1000 + msVal * msFactor
                val lyricText = line.replace(timeRegex, "").trim()
                if (lyricText.isNotEmpty()) {
                    parsedLines.add(LyricLine(timestampMs, lyricText))
                }
            }
        }
        return parsedLines.sortedBy { it.timestampMs }
    }

    private fun generateLrcForSong(title: String, artist: String, durationMs: Long): String {
        val sb = StringBuilder()
        sb.append("[00:02.00] 🎵 (Intro Beats) 🎵\n")
        sb.append("[00:08.00] Welcome to Pulse Music Player AI\n")
        sb.append("[00:13.00] Listening to: $title\n")
        sb.append("[00:18.00] Beautifully crafted by $artist\n")

        val linesCount = (durationMs / 10000L).coerceIn(5, 30).toInt()
        val themes = listOf(
            "Feel the dynamic sound waves and pulse...",
            "Neon lights dancing on the active visualizer...",
            "Every beat resonates through the clean architecture...",
            "Lost in the premium stereo acoustic details...",
            "This is the premium music experience by Prince Laghari...",
            "We are sailing through the synchronized waves of sound...",
            "Let the high-fidelity bass wash over your soul...",
            "The perfect harmony of artificial intelligence and music...",
            "Unlocking the ultimate flagship performance...",
            "Your music, your pulse, your unique rhythm..."
        )

        var currentMs = 24000L
        val intervalMs = (durationMs - 30000L) / (linesCount - 4).coerceAtLeast(1)
        for (i in 0 until linesCount - 4) {
            if (currentMs >= durationMs - 10000L) break
            val min = currentMs / 60000L
            val sec = (currentMs % 60000L) / 1000L
            val ms = (currentMs % 1000L) / 10L
            val timeStr = String.format("[%02d:%02d.%02d]", min, sec, ms)
            val themeText = themes[i % themes.size]
            sb.append("$timeStr $themeText\n")
            currentMs += intervalMs
        }

        val endMin = (durationMs - 5000L).coerceAtLeast(0L) / 60000L
        val endSec = ((durationMs - 5000L).coerceAtLeast(0L) % 60000L) / 1000L
        val endTimeStr = String.format("[%02d:%02d.00]", endMin, endSec)
        sb.append("$endTimeStr Thank you for playing $title on Pulse AI! 🎉\n")
        return sb.toString()
    }
}
