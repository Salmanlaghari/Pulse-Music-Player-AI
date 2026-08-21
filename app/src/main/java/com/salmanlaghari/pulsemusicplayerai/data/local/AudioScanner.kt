package com.salmanlaghari.pulsemusicplayerai.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import com.salmanlaghari.pulsemusicplayerai.utils.CrashLogger
import java.io.File

class AudioScanner(private val context: Context) {

    fun scanLocalAudio(): List<Song> {
        val songList = mutableListOf<Song>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            CrashLogger.logMessage("Starting MediaStore audio scan", "AudioScanner")
            contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                CrashLogger.logMessage("Cursor columns: id=$idColumn title=$titleColumn artist=$artistColumn album=$albumColumn duration=$durationColumn data=$dataColumn dateAdded=$dateAddedColumn albumId=$albumIdColumn", "AudioScanner")

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown Song"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val album = cursor.getString(albumColumn) ?: "Unknown Album"
                        val duration = cursor.getLong(durationColumn)
                        val path = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val albumId = if (albumIdColumn >= 0) cursor.getLong(albumIdColumn) else 0L

                        val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                        val albumArtUri = if (albumId > 0L) ContentUris.withAppendedId(sArtworkUri, albumId) else null

                        songList.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                path = path,
                                uri = songUri,
                                dateAdded = dateAdded,
                                artUri = albumArtUri
                            )
                        )
                    } catch (rowEx: Exception) {
                        CrashLogger.logException(rowEx, "AudioScanner.row")
                        Log.e("AudioScanner", "Row error: ${rowEx.message}", rowEx)
                    }
                }
                CrashLogger.logMessage("Scan complete. Songs found: ${songList.size}", "AudioScanner")
            } ?: run {
                CrashLogger.logMessage("MediaStore query returned null cursor", "AudioScanner")
            }
        } catch (e: Exception) {
            CrashLogger.logException(e, "AudioScanner.scanLocalAudio")
            Log.e("AudioScanner", "Scan failed: ${e.message}", e)
        }
        return songList
    }
}
