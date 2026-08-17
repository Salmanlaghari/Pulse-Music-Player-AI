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

/**
 * Audio output formats.
 *
 * [encodable] marks the formats the Android platform can genuinely produce:
 *  - WAV  : written directly from decoded PCM (uncompressed RIFF/WAVE).
 *  - M4A  : AAC-LC encoded by MediaCodec inside an MP4 container.
 *  - AAC  : same encoder; delivered in the .m4a container because MediaMuxer
 *           cannot write raw ADTS streams.
 *
 * MP3, FLAC and OGG/Vorbis have NO encoder in the Android framework
 * (MediaCodec only *decodes* them), so they are not offered as conversion
 * targets. Producing a file with one of those extensions from an AAC/PCM
 * stream would be mislabelling the container, so the Audio Studio falls back to
 * a real .m4a with the correct MIME type instead.
 */
enum class AudioFormat(val extension: String, val encodable: Boolean = false) {
    MP3("mp3"),
    WAV("wav", true),
    AAC("aac", true),
    FLAC("flac"),
    OGG("ogg"),
    M4A("m4a", true)
}

enum class CompressionPreset {
    HIGH,   // High Compression -> Low Quality/Size
    MEDIUM, // Medium Compression -> Medium Quality/Size
    LOW     // Low Compression -> High Quality/Size
}
