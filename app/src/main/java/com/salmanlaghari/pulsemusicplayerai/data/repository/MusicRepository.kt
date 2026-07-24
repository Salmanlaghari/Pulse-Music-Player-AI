package com.salmanlaghari.pulsemusicplayerai.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.salmanlaghari.pulsemusicplayerai.data.local.AudioScanner
import com.salmanlaghari.pulsemusicplayerai.domain.model.Album
import com.salmanlaghari.pulsemusicplayerai.domain.model.Artist
import com.salmanlaghari.pulsemusicplayerai.domain.model.Folder
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import com.salmanlaghari.pulsemusicplayerai.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class MusicRepository(
    private val context: Context,
    private val audioScanner: AudioScanner
) {
    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorite_song_ids")
    }

    // Cached current song list
    private var cachedSongs: List<Song> = emptyList()

    // 1. Get/Set Favorites Flow
    val favoriteIdsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITES_KEY] ?: emptySet()
    }

    suspend fun toggleFavorite(songId: Long) {
        context.dataStore.edit { preferences ->
            val currentFavs = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            val idStr = songId.toString()
            if (currentFavs.contains(idStr)) {
                currentFavs.remove(idStr)
            } else {
                currentFavs.add(idStr)
            }
            preferences[FAVORITES_KEY] = currentFavs
        }
    }

    // 2. Scan and fetch raw Songs
    suspend fun getAllSongs(forceRefresh: Boolean = false): List<Song> {
        if (cachedSongs.isEmpty() || forceRefresh) {
            cachedSongs = audioScanner.scanLocalAudio()
        }
        val favIds = favoriteIdsFlow.first()
        return cachedSongs.map { song ->
            song.copy(isFavorite = favIds.contains(song.id.toString()))
        }
    }

    // 3. Get Albums
    suspend fun getAlbums(forceRefresh: Boolean = false): List<Album> {
        val songs = getAllSongs(forceRefresh)
        return songs.groupBy { it.album }.map { (albumName, albumSongs) ->
            val sampleSong = albumSongs.first()
            Album(
                id = sampleSong.id, // reference ID for grouping / art loading
                name = albumName,
                artist = sampleSong.artist,
                songsCount = albumSongs.size,
                artUri = sampleSong.artUri
            )
        }.sortedBy { it.name }
    }

    // 4. Get Artists
    suspend fun getArtists(forceRefresh: Boolean = false): List<Artist> {
        val songs = getAllSongs(forceRefresh)
        return songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
            val albumsCount = artistSongs.map { it.album }.distinct().size
            Artist(
                name = artistName,
                songsCount = artistSongs.size,
                albumsCount = albumsCount
            )
        }.sortedBy { it.name }
    }

    // 5. Get Folders
    suspend fun getFolders(forceRefresh: Boolean = false): List<Folder> {
        val songs = getAllSongs(forceRefresh)
        return songs.groupBy {
            val file = File(it.path)
            file.parentFile?.absolutePath ?: "Root"
        }.map { (folderPath, folderSongs) ->
            val folderName = File(folderPath).name
            Folder(
                name = folderName,
                path = folderPath,
                songsCount = folderSongs.size
            )
        }.sortedBy { it.name }
    }

    // 6. Get Recently Added (last added songs first)
    suspend fun getRecentlyAdded(forceRefresh: Boolean = false): List<Song> {
        return getAllSongs(forceRefresh).sortedByDescending { it.dateAdded }
    }

    // 7. Get Favorite Songs List
    suspend fun getFavoriteSongs(forceRefresh: Boolean = false): List<Song> {
        val songs = getAllSongs(forceRefresh)
        val favIds = favoriteIdsFlow.first()
        return songs.filter { favIds.contains(it.id.toString()) }
    }
}
