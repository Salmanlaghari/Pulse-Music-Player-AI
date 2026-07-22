package com.salmanlaghari.pulsemusicplayerai.domain.model

import android.net.Uri

data class ExportedFile(
    val id: Long,
    val name: String,
    val path: String,
    val uriString: String,
    val size: Long,
    val duration: Long, // in milliseconds
    val format: String,
    val dateAdded: Long
) {
    val uri: Uri get() = Uri.parse(uriString)
}

enum class AudioFormat(val extension: String) {
    MP3("mp3"),
    WAV("wav"),
    AAC("aac"),
    FLAC("flac"),
    OGG("ogg"),
    M4A("m4a")
}

enum class CompressionPreset {
    HIGH,   // High Compression -> Low Quality/Size
    MEDIUM, // Medium Compression -> Medium Quality/Size
    LOW     // Low Compression -> High Quality/Size
}
