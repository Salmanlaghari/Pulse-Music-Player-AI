package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.salmanlaghari.pulsemusicplayerai.core.service.AudioSpectrum
import com.salmanlaghari.pulsemusicplayerai.core.service.AudioStudioProcessor
import com.salmanlaghari.pulsemusicplayerai.core.service.VisualizerBackground
import com.salmanlaghari.pulsemusicplayerai.core.service.VisualizerFrameRenderer
import com.salmanlaghari.pulsemusicplayerai.core.service.WatermarkAssets
import com.salmanlaghari.pulsemusicplayerai.domain.model.VisualizerVideoConfig

/**
 * LIVE video preview for the MP3 -> MP4 studio.
 *
 * Key design point: this composable renders through the very same
 * [VisualizerFrameRenderer] that the MediaCodec exporter uses, and it is fed
 * from the very same decoded-PCM analysis table
 * ([AudioStudioProcessor.SpectrumTrack]) that the exporter analyses. So the
 * preview is not a decorative animation — it is the export renderer running on
 * the real audio data at the current playback position.
 *
 * Because [positionMs] comes from the actual player, play/pause and seeking
 * update the preview immediately, and changing [config] (preset, background,
 * aspect ratio, text, scale, colours) re-renders live.
 */
@Composable
fun LiveVisualizerPreview(
    config: VisualizerVideoConfig,
    spectrum: AudioStudioProcessor.SpectrumTrack?,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Renderer instance is recreated whenever the config changes so preview
    // output always reflects the current selections.
    val watermark: Bitmap? = remember(config.watermarkEnabled) {
        if (config.watermarkEnabled) WatermarkAssets.loadPulseWatermark(context) else null
    }
    val renderer = remember(config, watermark != null) { VisualizerFrameRenderer(config, watermark) }

    val background: Bitmap? = remember(config.backgroundImageUri) {
        val uriStr = config.backgroundImageUri
        if (uriStr.isNullOrBlank()) null else try {
            context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    val bgPaint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }

    val aspect = config.aspectRatio.widthRatio.toFloat() / config.aspectRatio.heightRatio.toFloat()

    // Empty analysis frames keep the preview stable before analysis completes.
    val emptyMags = remember { FloatArray(AudioStudioProcessor.ANALYSIS_BINS) }
    val emptyWave = remember { FloatArray(AudioStudioProcessor.WAVEFORM_POINTS) }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(aspect)) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(aspect)) {
            val w = size.width.toInt()
            val h = size.height.toInt()
            if (w <= 0 || h <= 0) return@Canvas

            val frameIndex = spectrum?.frameAt(positionMs) ?: 0
            val mags = spectrum?.magnitudes?.getOrNull(frameIndex) ?: emptyMags
            val wave = spectrum?.waveforms?.getOrNull(frameIndex) ?: emptyWave
            val beatEnergy = AudioSpectrum.beat(mags)
            // Identical phase formula to the exporter.
            val beatPhase = ((positionMs / 1000f) * (0.5f + beatEnergy)) % 1f

            drawIntoCanvas { canvasWrapper ->
                val native = canvasWrapper.nativeCanvas
                var drewBackground = false
                if (background != null) {
                    native.drawColor(android.graphics.Color.BLACK)
                    native.drawBitmap(
                        background,
                        null,
                        VisualizerBackground.destRect(
                            background.width, background.height, w, h, config.backgroundFit
                        ),
                        bgPaint
                    )
                    drewBackground = true
                }
                renderer.draw(native, w, h, mags, wave, beatPhase, drewBackground)
            }
        }
    }
}
