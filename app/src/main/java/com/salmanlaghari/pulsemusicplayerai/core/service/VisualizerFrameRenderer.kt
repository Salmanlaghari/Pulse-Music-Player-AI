package com.salmanlaghari.pulsemusicplayerai.core.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.salmanlaghari.pulsemusicplayerai.domain.model.BackgroundFit
import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoBackgroundStyle
import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoVisualizerPreset
import com.salmanlaghari.pulsemusicplayerai.domain.model.VisualizerVideoConfig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws a single visualizer frame onto a plain [android.graphics.Canvas].
 *
 * This is deliberately Canvas-based (not Compose) so the *exact same code* can
 * render the MediaCodec input surface during export and the on-screen live
 * preview (via `Canvas.drawIntoCanvas { it.nativeCanvas }`). That is what makes
 * the preview genuinely match the exported MP4.
 *
 * All presets are driven by [magnitudes] (normalised 0..1 frequency bins) and
 * [waveform] (normalised -1..1 time-domain samples), both computed from the real
 * audio, so nothing here is a decorative animation detached from the sound.
 */
class VisualizerFrameRenderer(private val config: VisualizerVideoConfig) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val dimPaint = Paint()

    /**
     * @param backgroundBitmapDrawn true when the caller already painted a
     *        background image; in that case we only apply the dim overlay.
     */
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        magnitudes: FloatArray,
        waveform: FloatArray,
        beat: Float,
        backgroundBitmapDrawn: Boolean
    ) {
        if (!backgroundBitmapDrawn) {
            drawBackground(canvas, width, height)
        } else if (config.backgroundDim > 0f) {
            dimPaint.color = Color.argb((config.backgroundDim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        }

        val cx = width / 2f
        val cy = height * config.visualizerPositionY
        val scale = config.visualizerScale.coerceIn(0.4f, 1.6f)

        when (config.preset) {
            VideoVisualizerPreset.SPECTRUM_BARS -> drawSpectrumBars(canvas, width, height, magnitudes, scale)
            VideoVisualizerPreset.MIRROR_BARS -> drawMirrorBars(canvas, width, height, magnitudes, scale)
            VideoVisualizerPreset.CIRCULAR_SPECTRUM -> drawCircular(canvas, cx, cy, width, magnitudes, scale)
            VideoVisualizerPreset.WAVEFORM -> drawWaveform(canvas, width, height, waveform, scale)
            VideoVisualizerPreset.RADIAL_PULSE -> drawRadialPulse(canvas, cx, cy, width, beat, magnitudes, scale)
            VideoVisualizerPreset.PARTICLE_BEAT -> drawParticles(canvas, cx, cy, width, beat, magnitudes, scale)
        }

        if (config.showText) drawText(canvas, width, height)
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        when (config.backgroundStyle) {
            VideoBackgroundStyle.SOLID_BLACK -> canvas.drawColor(Color.BLACK)
            VideoBackgroundStyle.DARK_GRADIENT -> {
                fill.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#0a1128"), Color.parseColor("#050510"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
                fill.shader = null
            }
            VideoBackgroundStyle.ACCENT_GLOW -> {
                canvas.drawColor(Color.parseColor("#05060f"))
                fill.shader = RadialGradient(
                    width / 2f, height / 2f, width.coerceAtLeast(height).toFloat() / 1.4f,
                    intArrayOf(
                        (config.accentColor and 0x00FFFFFF) or 0x55000000,
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
                fill.shader = null
            }
        }
    }

    private fun configureBarPaint() {
        fill.style = Paint.Style.FILL
        fill.color = config.accentColor
        if (config.glow) fill.setShadowLayer(18f, 0f, 0f, config.accentColor) else fill.clearShadowLayer()
    }

    private fun drawSpectrumBars(canvas: Canvas, width: Int, height: Int, mags: FloatArray, scale: Float) {
        configureBarPaint()
        val bars = min(mags.size, 64)
        if (bars == 0) return
        val gap = width * 0.004f
        val barWidth = (width - gap * (bars + 1)) / bars
        val maxH = height * 0.4f * scale
        val baseY = (config.visualizerPositionY * height).coerceIn(maxH, height.toFloat())
        for (i in 0 until bars) {
            val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
            val h = m * maxH
            val left = gap + i * (barWidth + gap)
            fill.color = lerpColor(config.secondaryColor, config.accentColor, m)
            canvas.drawRoundRect(left, baseY - h, left + barWidth, baseY, barWidth / 2f, barWidth / 2f, fill)
        }
    }

    private fun drawMirrorBars(canvas: Canvas, width: Int, height: Int, mags: FloatArray, scale: Float) {
        configureBarPaint()
        val bars = min(mags.size, 64)
        if (bars == 0) return
        val gap = width * 0.004f
        val barWidth = (width - gap * (bars + 1)) / bars
        val maxH = height * 0.28f * scale
        val midY = config.visualizerPositionY * height
        for (i in 0 until bars) {
            val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
            val h = m * maxH
            val left = gap + i * (barWidth + gap)
            fill.color = lerpColor(config.secondaryColor, config.accentColor, m)
            canvas.drawRoundRect(left, midY - h, left + barWidth, midY + h, barWidth / 2f, barWidth / 2f, fill)
        }
    }

    private fun drawCircular(canvas: Canvas, cx: Float, cy: Float, width: Int, mags: FloatArray, scale: Float) {
        stroke.color = config.accentColor
        stroke.strokeWidth = width * 0.006f
        stroke.strokeCap = Paint.Cap.ROUND
        if (config.glow) stroke.setShadowLayer(16f, 0f, 0f, config.accentColor) else stroke.clearShadowLayer()
        val radius = width * 0.13f * scale
        val bars = min(mags.size, 90)
        val maxLen = width * 0.16f * scale
        for (i in 0 until bars) {
            val angle = (i.toFloat() / bars) * 2f * Math.PI.toFloat()
            val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
            val len = radius + m * maxLen
            stroke.color = lerpColor(config.secondaryColor, config.accentColor, m)
            canvas.drawLine(
                cx + cos(angle) * radius, cy + sin(angle) * radius,
                cx + cos(angle) * len, cy + sin(angle) * len, stroke
            )
        }
    }

    private fun drawWaveform(canvas: Canvas, width: Int, height: Int, wave: FloatArray, scale: Float) {
        if (wave.isEmpty()) return
        stroke.color = config.accentColor
        stroke.strokeWidth = width * 0.005f
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeJoin = Paint.Join.ROUND
        if (config.glow) stroke.setShadowLayer(14f, 0f, 0f, config.accentColor) else stroke.clearShadowLayer()
        val path = Path()
        val midY = config.visualizerPositionY * height
        val amp = height * 0.3f * scale
        val step = width.toFloat() / (wave.size - 1).coerceAtLeast(1)
        path.moveTo(0f, midY + wave[0] * amp)
        for (i in 1 until wave.size) {
            path.lineTo(i * step, midY + wave[i].coerceIn(-1f, 1f) * amp)
        }
        canvas.drawPath(path, stroke)
    }

    private fun drawRadialPulse(canvas: Canvas, cx: Float, cy: Float, width: Int, beat: Float, mags: FloatArray, scale: Float) {
        stroke.style = Paint.Style.STROKE
        val bass = if (mags.isNotEmpty()) mags.copyOfRange(0, (mags.size / 8).coerceAtLeast(1)).average().toFloat() else beat
        val rings = 5
        for (r in 0 until rings) {
            val phase = ((beat + r.toFloat() / rings) % 1f)
            val radius = width * 0.06f + phase * width * 0.4f * scale
            val alpha = ((1f - phase) * 220).toInt().coerceIn(0, 255)
            stroke.color = Color.argb(alpha, Color.red(config.accentColor), Color.green(config.accentColor), Color.blue(config.accentColor))
            stroke.strokeWidth = width * 0.006f * (0.5f + bass)
            canvas.drawCircle(cx, cy, radius, stroke)
        }
        fill.style = Paint.Style.FILL
        fill.color = config.secondaryColor
        if (config.glow) fill.setShadowLayer(30f, 0f, 0f, config.accentColor) else fill.clearShadowLayer()
        canvas.drawCircle(cx, cy, width * 0.05f * (1f + bass), fill)
    }

    private val particlePhase = FloatArray(64) { it * 0.19f }

    private fun drawParticles(canvas: Canvas, cx: Float, cy: Float, width: Int, beat: Float, mags: FloatArray, scale: Float) {
        fill.style = Paint.Style.FILL
        val count = 48
        val energy = if (mags.isNotEmpty()) mags.average().toFloat() else beat
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2f * Math.PI.toFloat() + beat * 2f
            val dist = width * 0.08f + (0.4f + energy) * width * 0.34f * scale * ((particlePhase[i % 64] + beat) % 1f)
            val x = cx + cos(angle) * dist
            val y = cy + sin(angle) * dist
            val radius = width * 0.006f * (1f + energy * 2f)
            fill.color = lerpColor(config.secondaryColor, config.accentColor, (i.toFloat() / count))
            if (config.glow) fill.setShadowLayer(12f, 0f, 0f, config.accentColor) else fill.clearShadowLayer()
            canvas.drawCircle(x, y, radius, fill)
        }
    }

    private fun drawText(canvas: Canvas, width: Int, height: Int) {
        val title = config.title.ifBlank { config.outputName }
        if (title.isBlank() && config.artist.isBlank()) return
        textPaint.clearShadowLayer()
        textPaint.setShadowLayer(8f, 0f, 2f, Color.BLACK)
        val base = min(width, height)
        textPaint.color = Color.WHITE
        textPaint.textSize = base * 0.06f
        if (title.isNotBlank()) canvas.drawText(title, width / 2f, height * 0.14f, textPaint)
        if (config.artist.isNotBlank()) {
            textPaint.color = Color.argb(220, 220, 220, 230)
            textPaint.textSize = base * 0.04f
            canvas.drawText(config.artist, width / 2f, height * 0.14f + base * 0.07f, textPaint)
        }
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * tt).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * tt).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * tt).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * tt).toInt()
        )
    }
}

/**
 * Background fitting maths shared by the live preview and the exporter so a
 * chosen background image is framed identically in both.
 */
object VisualizerBackground {
    fun destRect(
        bitmapWidth: Int,
        bitmapHeight: Int,
        frameWidth: Int,
        frameHeight: Int,
        fit: BackgroundFit
    ): android.graphics.Rect {
        val bw = bitmapWidth.toFloat()
        val bh = bitmapHeight.toFloat()
        if (bw <= 0f || bh <= 0f) return android.graphics.Rect(0, 0, frameWidth, frameHeight)
        val scale = if (fit == BackgroundFit.CROP) {
            maxOf(frameWidth / bw, frameHeight / bh)
        } else {
            minOf(frameWidth / bw, frameHeight / bh)
        }
        val dw = (bw * scale).toInt()
        val dh = (bh * scale).toInt()
        val left = (frameWidth - dw) / 2
        val top = (frameHeight - dh) / 2
        return android.graphics.Rect(left, top, left + dw, top + dh)
    }
}

/**
 * Small, dependency-free radix-2 FFT + helpers used to turn a window of PCM
 * samples into normalised frequency magnitudes for the export path.
 */
object AudioSpectrum {

    /** Computes [outBins] normalised (0..1) magnitudes from mono float samples. */
    fun magnitudes(samples: FloatArray, outBins: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(outBins)
        var n = 1
        while (n < samples.size) n = n shl 1
        if (n > 2048) n = 2048
        val re = FloatArray(n)
        val im = FloatArray(n)
        val copy = min(n, samples.size)
        for (i in 0 until copy) {
            // Hann window to reduce spectral leakage.
            val w = 0.5f - 0.5f * cos(2f * Math.PI.toFloat() * i / (n - 1))
            re[i] = samples[i] * w
        }
        fft(re, im)
        val half = n / 2
        val raw = FloatArray(outBins)
        for (b in 0 until outBins) {
            val start = (b * half / outBins).coerceIn(0, half - 1)
            val end = ((b + 1) * half / outBins).coerceIn(start + 1, half)
            var sum = 0f
            for (k in start until end) {
                sum += kotlin.math.sqrt(re[k] * re[k] + im[k] * im[k])
            }
            val avg = sum / (end - start)
            // Log scale for a more musical response.
            raw[b] = (kotlin.math.ln(1f + avg * 8f) / kotlin.math.ln(9f)).coerceIn(0f, 1f)
        }
        return raw
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        if (n <= 1) return
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j or bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2f * Math.PI.toFloat() / len
            val wr = cos(ang); val wi = sin(ang)
            var i = 0
            while (i < n) {
                var curR = 1f; var curI = 0f
                for (k in 0 until len / 2) {
                    val aR = re[i + k]; val aI = im[i + k]
                    val bR = re[i + k + len / 2]; val bI = im[i + k + len / 2]
                    val tR = bR * curR - bI * curI
                    val tI = bR * curI + bI * curR
                    re[i + k] = aR + tR; im[i + k] = aI + tI
                    re[i + k + len / 2] = aR - tR; im[i + k + len / 2] = aI - tI
                    val ncurR = curR * wr - curI * wi
                    curI = curR * wi + curI * wr
                    curR = ncurR
                }
                i += len
            }
            len = len shl 1
        }
    }

    /** Simple energy-based beat value (0..1) from a magnitude array's low band. */
    fun beat(mags: FloatArray): Float {
        if (mags.isEmpty()) return 0f
        val lowEnd = (mags.size / 6).coerceAtLeast(1)
        var sum = 0f
        for (i in 0 until lowEnd) sum += mags[i]
        return (sum / lowEnd).coerceIn(0f, 1f)
    }

    /** Down-mixes interleaved 16-bit PCM bytes to a mono float window. */
    fun pcmWindowToMono(pcm: ByteArray, byteOffset: Int, sampleCount: Int, channels: Int): FloatArray {
        val out = FloatArray(sampleCount)
        val frameBytes = 2 * channels
        var p = byteOffset
        for (i in 0 until sampleCount) {
            if (p + frameBytes > pcm.size) break
            var acc = 0
            for (c in 0 until channels) {
                val lo = pcm[p + c * 2].toInt() and 0xFF
                val hi = pcm[p + c * 2 + 1].toInt()
                acc += (hi shl 8) or lo
            }
            out[i] = (acc.toFloat() / channels) / 32768f
            p += frameBytes
        }
        return out
    }
}
