package com.salmanlaghari.pulsemusicplayerai.core.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
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
 * Every one of the 50+ [VideoVisualizerPreset] values is driven by the real
 * audio [magnitudes] (normalised 0..1 frequency bins) and [waveform] (normalised
 * -1..1 time-domain samples) plus a cyclic [beat] phase, so nothing here is a
 * decorative animation detached from the sound. The same renderer + same data
 * feed both the preview and the exporter, keeping them in sync.
 */
class VisualizerFrameRenderer(
    private val config: VisualizerVideoConfig,
    /** Optional high-resolution logo bitmap burned into the frame as a watermark. */
    private val watermark: Bitmap? = null
) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val dimPaint = Paint()
    private val watermarkPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // Cached background shader (rebuilt only when the style or frame size changes)
    // so we don't allocate a new Gradient object on every rendered frame.
    private var cachedBgKey = ""
    private var cachedBgShader: Shader? = null

    // Fixed phase per particle index so motion is deterministic and stable.
    private val phaseA = FloatArray(64) { it * 0.197f }
    private val phaseB = FloatArray(64) { it * 0.311f }

    // Signs used by the diagonal (cross) bars preset — precomputed once so the
    // render loop does not allocate a list every frame.
    private val diagonalSigns = floatArrayOf(1f, -1f)

    /** Light Gaussian-style smoothing of the spectrum to kill frame-to-frame
     *  jitter without flattening real peaks. Applied to every preset so the
     *  whole library reacts smoothly and naturally to the music. */
    private fun smoothSpectrum(src: FloatArray): FloatArray {
        if (src.size <= 2) return src
        val out = FloatArray(src.size)
        for (i in src.indices) {
            val a = src[(i - 1).coerceIn(0, src.size - 1)]
            val b = src[i]
            val c = src[(i + 1).coerceIn(0, src.size - 1)]
            // 0.25/0.5/0.25 low-pass, then blend slightly toward the original so
            // transient hits still read as punchy (not over-smoothed / sluggish).
            val smoothed = a * 0.25f + b * 0.5f + c * 0.25f
            out[i] = (b * 0.7f + smoothed * 0.3f).coerceIn(0f, 1f)
        }
        return out
    }

    // ---- internal style enums (kept private; selection is by preset) ----
    private enum class BarsAlign { BOTTOM, MIRROR, TOPBOTTOM, CENTEROUT, DIAGONAL }
    private enum class BarsPalette { NORMAL, NEON, FLAME, RAINBOW, GLOW, PEAK, LINEAR, THICK }
    private enum class CircularMode { RING, DOTS, RAINBOW, GALAXY, SPIRAL, ORBIT, PULSE, CONCENTRIC, WHEEL, TELEMETRY }
    private enum class WaveMode { SINGLE, DUAL, MULTI, MIRROR, FILLED, RIBBON, STEPS, CROSS, ECHO, SMOOTH }
    private enum class ParticleMode { BURST, ORB, STAR, CLOUD, FIREWORKS, METEOR, SNOW, RADIAL, ORBIT, GALAXY }
    private enum class GeoMode { HEX, CRYSTAL, ISO, HELIX, KALEIDO, PRISM, DIAMOND, LASER, INFINITY, FREQ, TUNNEL }
    private enum class MinimalMode { LINE, DOT, PULSE, BARS, EQUALIZER, TICK }

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

        // Smooth the spectrum once so every preset reacts with natural,
        // jitter-free motion (flagship polish applied library-wide).
        val magnitudes = smoothSpectrum(magnitudes)

        when (config.preset) {
            // ---------------- BARS ----------------
            VideoVisualizerPreset.SPECTRUM_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.BOTTOM, BarsPalette.NORMAL)
            VideoVisualizerPreset.MIRROR_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.MIRROR, BarsPalette.NORMAL)
            VideoVisualizerPreset.NEON_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.TOPBOTTOM, BarsPalette.NEON)
            VideoVisualizerPreset.FLAME_SPECTRUM -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.BOTTOM, BarsPalette.FLAME)
            VideoVisualizerPreset.INFINITY_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.CENTEROUT, BarsPalette.NORMAL)
            VideoVisualizerPreset.EXTREME_SPECTRUM_X -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.DIAGONAL, BarsPalette.NORMAL)
            VideoVisualizerPreset.LINEAR_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.BOTTOM, BarsPalette.LINEAR)
            VideoVisualizerPreset.DUAL_ENDED_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.CENTEROUT, BarsPalette.PEAK)
            VideoVisualizerPreset.RAINBOW_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.BOTTOM, BarsPalette.RAINBOW)
            VideoVisualizerPreset.GLOW_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.BOTTOM, BarsPalette.GLOW)
            VideoVisualizerPreset.PEAK_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.MIRROR, BarsPalette.PEAK)
            VideoVisualizerPreset.THICK_BARS -> drawBars(canvas, width, height, magnitudes, scale, BarsAlign.BOTTOM, BarsPalette.THICK)

            // ---------------- CIRCULAR ----------------
            VideoVisualizerPreset.CIRCULAR_SPECTRUM -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.RING)
            VideoVisualizerPreset.RAINBOW_RING -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.RAINBOW)
            VideoVisualizerPreset.GALAXY_RING -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.GALAXY)
            VideoVisualizerPreset.SPIRAL_GALAXY -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.SPIRAL)
            VideoVisualizerPreset.RADIAL_DOTS -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.DOTS)
            VideoVisualizerPreset.FUTURE_PULSE -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.TELEMETRY)
            VideoVisualizerPreset.ORBITAL_SR -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.ORBIT)
            VideoVisualizerPreset.PULSE_RING -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.PULSE)
            VideoVisualizerPreset.CONCENTRIC_DOTS -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.CONCENTRIC)
            VideoVisualizerPreset.WHEEL_SPECTRUM -> drawCircular(canvas, cx, cy, width, magnitudes, scale, CircularMode.WHEEL)

            // ---------------- WAVE ----------------
            VideoVisualizerPreset.WAVEFORM -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.SINGLE)
            VideoVisualizerPreset.DUAL_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.DUAL)
            VideoVisualizerPreset.MULTI_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.MULTI)
            VideoVisualizerPreset.MIRRORED_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.MIRROR)
            VideoVisualizerPreset.FILLED_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.FILLED)
            VideoVisualizerPreset.RIBBON_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.RIBBON)
            VideoVisualizerPreset.STEP_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.STEPS)
            VideoVisualizerPreset.SMOOTH_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.SMOOTH)
            VideoVisualizerPreset.CROSS_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.CROSS)
            VideoVisualizerPreset.ECHO_WAVE -> drawWave(canvas, width, height, waveform, magnitudes, scale, WaveMode.ECHO)

            // ---------------- PARTICLE ----------------
            VideoVisualizerPreset.PARTICLE_BEAT -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.BURST)
            VideoVisualizerPreset.PARTICLE_ORB -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.ORB)
            VideoVisualizerPreset.STARFIELD -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.STAR)
            VideoVisualizerPreset.QUANTUM_CLOUD -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.CLOUD)
            VideoVisualizerPreset.FIREWORKS -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.FIREWORKS)
            VideoVisualizerPreset.METEOR_SHOWER -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.METEOR)
            VideoVisualizerPreset.SNOWFALL -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.SNOW)
            VideoVisualizerPreset.COLOR_BURST -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.RADIAL)
            VideoVisualizerPreset.ORBIT_PARTICLES -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.ORBIT)
            VideoVisualizerPreset.GALAXY_CLOUD -> drawParticles(canvas, cx, cy, width, height, beat, magnitudes, scale, ParticleMode.GALAXY)

            // ---------------- GEOMETRIC ----------------
            VideoVisualizerPreset.HEXAGON_MESH -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.HEX)
            VideoVisualizerPreset.CRYSTAL_MESH -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.CRYSTAL)
            VideoVisualizerPreset.ISOMETRIC_GRID -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.ISO)
            VideoVisualizerPreset.DOUBLE_HELIX -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.HELIX)
            VideoVisualizerPreset.KALEIDOSCOPE -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.KALEIDO)
            VideoVisualizerPreset.PRISM -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.PRISM)
            VideoVisualizerPreset.DIAMOND_GLOW -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.DIAMOND)
            VideoVisualizerPreset.LASER_BEAMS -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.LASER)
            VideoVisualizerPreset.INFINITY_LOOP -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.INFINITY)
            VideoVisualizerPreset.FREQUENCY_LINES -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.FREQ)
            VideoVisualizerPreset.TUNNEL_WARP -> drawGeometric(canvas, cx, cy, width, height, beat, magnitudes, scale, GeoMode.TUNNEL)

            // ---------------- MINIMAL ----------------
            VideoVisualizerPreset.LINE_SPECTRUM -> drawMinimal(canvas, width, height, magnitudes, scale, MinimalMode.LINE)
            VideoVisualizerPreset.DOT_SPECTRUM -> drawMinimal(canvas, width, height, magnitudes, scale, MinimalMode.DOT)
            VideoVisualizerPreset.PULSE_LINE -> drawMinimal(canvas, width, height, magnitudes, scale, MinimalMode.PULSE)
            VideoVisualizerPreset.MINIMAL_BARS -> drawMinimal(canvas, width, height, magnitudes, scale, MinimalMode.BARS)
            VideoVisualizerPreset.EQUALIZER_DOTS -> drawMinimal(canvas, width, height, magnitudes, scale, MinimalMode.EQUALIZER)
            VideoVisualizerPreset.TICK_SPECTRUM -> drawMinimal(canvas, width, height, magnitudes, scale, MinimalMode.TICK)
        }

        if (config.showText) drawText(canvas, width, height)

        if (watermark != null && config.watermarkEnabled) drawWatermark(canvas, width, height)
    }

    /** Burns the Pulse logo into the bottom-right corner at a fixed opacity. */
    private fun drawWatermark(canvas: Canvas, width: Int, height: Int) {
        val wm = watermark ?: return
        val r = WatermarkLayout.computeRect(width, height)
        watermarkPaint.alpha = (WatermarkLayout.OPACITY * 255f).toInt().coerceIn(0, 255)
        canvas.drawBitmap(
            wm,
            null,
            RectF(r.left, r.top, r.right, r.bottom),
            watermarkPaint
        )
    }

    // ===================================================================
    // Shared helpers
    // ===================================================================
    private fun withAlpha(color: Int, a: Float): Int {
        val alpha = (a.coerceIn(0f, 1f) * 255f).toInt()
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }

    private fun hsv(h: Float, s: Float, v: Float): Int =
        Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))

    private fun avg(arr: FloatArray, from: Int, to: Int): Float {
        if (arr.isEmpty()) return 0f
        val a = from.coerceIn(0, arr.size)
        val b = to.coerceIn(0, arr.size)
        if (b <= a) return 0f
        var s = 0f
        for (i in a until b) s += arr[i]
        return s / (b - a)
    }

    private fun lerp(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * tt).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * tt).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * tt).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * tt).toInt()
        )
    }

    private fun setFill(color: Int, alpha: Float = 1f, glow: Boolean, glowColor: Int = config.accentColor, glowRadius: Float = 16f) {
        fill.style = Paint.Style.FILL
        fill.color = withAlpha(color, alpha)
        if (glow && config.glow) fill.setShadowLayer(glowRadius, 0f, 0f, glowColor) else fill.clearShadowLayer()
    }

    private fun setStroke(color: Int, alpha: Float = 1f, width: Float, glow: Boolean, glowColor: Int = config.accentColor, glowRadius: Float = 14f) {
        stroke.style = Paint.Style.STROKE
        stroke.color = withAlpha(color, alpha)
        stroke.strokeWidth = width
        if (glow && config.glow) stroke.setShadowLayer(glowRadius, 0f, 0f, glowColor) else stroke.clearShadowLayer()
    }

    private fun barColor(palette: BarsPalette, m: Float, idx: Int, count: Int): Int = when (palette) {
        BarsPalette.NORMAL -> lerp(config.secondaryColor, config.accentColor, m)
        BarsPalette.NEON -> lerp(0xFF00E5FF.toInt(), 0xFFFF00E5.toInt(), m)
        BarsPalette.FLAME -> lerp(0xFFFF2200.toInt(), 0xFFFFE000.toInt(), m)
        BarsPalette.RAINBOW -> hsv(idx.toFloat() / count * 320f, 0.85f, 1f)
        BarsPalette.GLOW -> withAlpha(config.accentColor, 0.5f + m * 0.5f)
        BarsPalette.PEAK -> lerp(config.secondaryColor, 0xFFFFFFFF.toInt(), m * 0.6f)
        BarsPalette.LINEAR -> lerp(config.secondaryColor, config.accentColor, m)
        BarsPalette.THICK -> withAlpha(config.accentColor, 0.6f + m * 0.4f)
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        when (config.backgroundStyle) {
            VideoBackgroundStyle.SOLID_BLACK -> canvas.drawColor(Color.BLACK)
            VideoBackgroundStyle.DARK_GRADIENT -> {
                val key = "dark:$width:$height"
                if (key != cachedBgKey || cachedBgShader == null) {
                    cachedBgShader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        Color.parseColor("#0a1128"), Color.parseColor("#050510"),
                        Shader.TileMode.CLAMP
                    )
                    cachedBgKey = key
                }
                fill.shader = cachedBgShader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
                fill.shader = null
            }
            VideoBackgroundStyle.ACCENT_GLOW -> {
                val key = "accent:$width:$height"
                if (key != cachedBgKey || cachedBgShader == null) {
                    cachedBgShader = RadialGradient(
                        width / 2f, height / 2f, width.coerceAtLeast(height).toFloat() / 1.4f,
                        intArrayOf((config.accentColor and 0x00FFFFFF) or 0x55000000, Color.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    cachedBgKey = key
                }
                canvas.drawColor(Color.parseColor("#05060f"))
                fill.shader = cachedBgShader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
                fill.shader = null
            }
        }
    }

    // ===================================================================
    // BARS
    // ===================================================================
    private fun drawBars(
        canvas: Canvas, width: Int, height: Int, mags: FloatArray,
        scale: Float, align: BarsAlign, palette: BarsPalette
    ) {
        val bars = min(mags.size, 64)
        if (bars == 0) return
        val gap = width * 0.004f
        val barWidth = (width - gap * (bars + 1)) / bars
        val posY = (config.visualizerPositionY * height).coerceIn(height * 0.08f, height * 0.95f)

        when (align) {
            BarsAlign.BOTTOM -> {
                val baseY = posY.coerceIn(height * 0.12f, height * 0.95f)
                val maxH = (height - baseY).coerceAtLeast(height * 0.1f) * scale
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val h = m * maxH
                    val left = gap + i * (barWidth + gap)
                    setFill(barColor(palette, m, i, bars), glow = true, glowRadius = if (palette == BarsPalette.GLOW) 26f else 16f)
                    if (palette == BarsPalette.THICK) {
                        canvas.drawRoundRect(left, baseY - h, left + barWidth, baseY, barWidth / 3f, barWidth / 3f, fill)
                    } else {
                        canvas.drawRoundRect(left, baseY - h, left + barWidth, baseY, barWidth / 2f, barWidth / 2f, fill)
                    }
                    if (palette == BarsPalette.PEAK && m > 0.05f) {
                        setFill(0xFFFFFFFF.toInt(), glow = false)
                        canvas.drawCircle(left + barWidth / 2f, baseY - h, barWidth * 0.5f, fill)
                    }
                }
            }
            BarsAlign.MIRROR -> {
                val midY = posY
                val maxH = min(midY, height - midY).coerceAtLeast(height * 0.1f) * scale
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val h = m * maxH
                    val left = gap + i * (barWidth + gap)
                    setFill(barColor(palette, m, i, bars), glow = true)
                    canvas.drawRoundRect(left, midY - h, left + barWidth, midY + h, barWidth / 2f, barWidth / 2f, fill)
                }
            }
            BarsAlign.TOPBOTTOM -> {
                val maxH = height * 0.4f * scale
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val h = m * maxH
                    val left = gap + i * (barWidth + gap)
                    setFill(barColor(palette, m, i, bars), glow = true, glowRadius = 22f)
                    canvas.drawRoundRect(left, 0f, left + barWidth, h, barWidth / 2f, barWidth / 2f, fill)
                    canvas.drawRoundRect(left, height - h, left + barWidth, height.toFloat(), barWidth / 2f, barWidth / 2f, fill)
                }
            }
            BarsAlign.CENTEROUT -> {
                val midY = posY
                val maxLen = width * 0.45f * scale
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val len = m * maxLen
                    val y = midY + (i - bars / 2f) * (height / bars.toFloat())
                    setFill(barColor(palette, m, i, bars), glow = true)
                    canvas.drawRoundRect(width / 2f - len, y - barWidth / 2f, width / 2f + len, y + barWidth / 2f, barWidth / 2f, barWidth / 2f, fill)
                }
            }
            BarsAlign.DIAGONAL -> {
                val m = avg(mags, 0, mags.size)
                val maxH = height * 0.42f * scale
                for (sign in diagonalSigns) {
                    canvas.save()
                    canvas.rotate(sign * 45f, width / 2f, posY)
                    for (i in 0 until bars) {
                        val mv = mags[i * mags.size / bars].coerceIn(0f, 1f)
                        val h = mv * maxH
                        val left = gap + i * (barWidth + gap)
                        setFill(barColor(palette, mv, i, bars), glow = true)
                        canvas.drawRoundRect(left, posY - h, left + barWidth, posY + h, barWidth / 2f, barWidth / 2f, fill)
                    }
                    canvas.restore()
                }
                setFill(config.accentColor, m, glow = true)
                canvas.drawCircle(width / 2f, posY, barWidth * 1.5f * (0.5f + m), fill)
            }
        }
    }

    // ===================================================================
    // CIRCULAR
    // ===================================================================
    private fun drawCircular(
        canvas: Canvas, cx: Float, cy: Float, width: Int,
        mags: FloatArray, scale: Float, mode: CircularMode
    ) {
        val bars = min(mags.size, 90)
        val baseR = width * 0.12f * scale
        when (mode) {
            CircularMode.RING -> {
                setStroke(config.accentColor, 1f, width * 0.006f, true)
                for (i in 0 until bars) {
                    val angle = (i.toFloat() / bars) * 2f * Math.PI.toFloat()
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val len = baseR + m * width * 0.16f * scale
                    stroke.color = withAlpha(lerp(config.secondaryColor, config.accentColor, m), 1f)
                    canvas.drawLine(
                        cx + cos(angle) * baseR, cy + sin(angle) * baseR,
                        cx + cos(angle) * len, cy + sin(angle) * len, stroke
                    )
                }
            }
            CircularMode.DOTS -> {
                setFill(config.accentColor, glow = true)
                for (i in 0 until bars) {
                    val angle = (i.toFloat() / bars) * 2f * Math.PI.toFloat()
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val len = baseR + m * width * 0.18f * scale
                    fill.color = withAlpha(lerp(config.secondaryColor, config.accentColor, m), 1f)
                    canvas.drawCircle(cx + cos(angle) * len, cy + sin(angle) * len, width * 0.006f * (1f + m * 2f), fill)
                }
            }
            CircularMode.RAINBOW -> {
                val sweep = SweepGradient(cx, cy, intArrayOf(
                    hsv(0f, 0.9f, 1f), hsv(120f, 0.9f, 1f), hsv(240f, 0.9f, 1f), hsv(360f, 0.9f, 1f)
                ), null)
                fill.shader = sweep
                for (i in 0 until bars) {
                    val angle = (i.toFloat() / bars) * 2f * Math.PI.toFloat()
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val len = baseR + m * width * 0.2f * scale
                    if (config.glow) fill.setShadowLayer(12f, 0f, 0f, hsv(i.toFloat() / bars * 320f, 0.9f, 1f))
                    canvas.drawCircle(cx + cos(angle) * len, cy + sin(angle) * len, width * 0.007f * (1f + m * 2f), fill)
                }
                fill.shader = null
                fill.clearShadowLayer()
            }
            CircularMode.GALAXY -> {
                val t = (avg(mags, 0, mags.size / 3)) * 6f
                for (i in 0 until bars) {
                    val angle = (i.toFloat() / bars) * 2f * Math.PI.toFloat() + t
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val len = baseR * 1.4f + m * width * 0.22f * scale
                    setFill(lerp(config.secondaryColor, config.accentColor, m), glow = true)
                    canvas.drawCircle(cx + cos(angle) * len, cy + sin(angle) * len, width * 0.006f * (1f + m), fill)
                }
            }
            CircularMode.SPIRAL -> {
                val arms = 3
                val dots = 36
                val t = avg(mags, 0, mags.size) * 8f
                for (a in 0 until arms) {
                    for (d in 0 until dots) {
                        val f = d.toFloat() / dots
                        val angle = (a * 2f * Math.PI.toFloat() / arms) + f * 2f * Math.PI.toFloat() * 2f + t
                        val len = baseR * 0.6f + f * width * 0.32f * scale
                        val m = mags[(d * mags.size / dots).coerceIn(0, mags.size - 1)].coerceIn(0f, 1f)
                        setFill(lerp(config.secondaryColor, config.accentColor, f), m, glow = true)
                        canvas.drawCircle(cx + cos(angle) * len, cy + sin(angle) * len, width * 0.005f * (1f + m), fill)
                    }
                }
            }
            CircularMode.ORBIT -> {
                val tilt = 0.45f
                val rings = 3
                for (r in 0 until rings) {
                    val radiusX = baseR * (r + 1) * 1.1f
                    val radiusY = radiusX * tilt * (0.8f + avg(mags, 0, mags.size) * scale)
                    canvas.save()
                    canvas.rotate(r * 30f, cx, cy)
                    setStroke(if (r % 2 == 0) config.accentColor else config.secondaryColor, 0.8f, width * 0.004f, true)
                    canvas.drawOval(cx - radiusX, cy - radiusY, cx + radiusX, cy + radiusY, stroke)
                    canvas.restore()
                }
            }
            CircularMode.PULSE -> {
                val bass = avg(mags, 0, mags.size / 6).coerceIn(0f, 1f)
                for (r in 0 until 4) {
                    val radius = baseR * (r + 1) * (1f + bass * 0.4f * scale)
                    setStroke(config.accentColor, (1f - r * 0.2f), width * 0.006f * (0.6f + bass), true)
                    canvas.drawCircle(cx, cy, radius, stroke)
                }
                setFill(config.secondaryColor, 1f, glow = true)
                canvas.drawCircle(cx, cy, baseR * 0.6f * (1f + bass), fill)
            }
            CircularMode.CONCENTRIC -> {
                val rings = 4
                for (r in 0 until rings) {
                    val radius = baseR + r * width * 0.05f * scale
                    val count = bars / (r + 1).coerceAtLeast(1)
                    for (i in 0 until count) {
                        val angle = (i.toFloat() / count) * 2f * Math.PI.toFloat()
                        val m = mags[(i * mags.size / count).coerceIn(0, mags.size - 1)].coerceIn(0f, 1f)
                        setFill(lerp(config.secondaryColor, config.accentColor, m), glow = true)
                        canvas.drawCircle(cx + cos(angle) * radius, cy + sin(angle) * radius, width * 0.005f * (1f + m), fill)
                    }
                }
            }
            CircularMode.WHEEL -> {
                val spokes = 24
                setStroke(config.accentColor, 1f, width * 0.004f, true)
                for (i in 0 until spokes) {
                    val angle = (i.toFloat() / spokes) * 2f * Math.PI.toFloat()
                    val m = mags[i * mags.size / spokes].coerceIn(0f, 1f)
                    val inner = baseR * 0.5f
                    val outer = baseR + m * width * 0.18f * scale
                    stroke.color = withAlpha(lerp(config.secondaryColor, config.accentColor, m), 1f)
                    canvas.drawLine(
                        cx + cos(angle) * inner, cy + sin(angle) * inner,
                        cx + cos(angle) * outer, cy + sin(angle) * outer, stroke
                    )
                }
                setStroke(config.accentColor, 0.6f, width * 0.004f, glow = false)
                canvas.drawCircle(cx, cy, baseR * 0.5f, stroke)
            }
            CircularMode.TELEMETRY -> {
                val t = avg(mags, 0, mags.size) * 10f
                setStroke(config.accentColor, 1f, width * 0.006f, true)
                canvas.drawCircle(cx, cy, baseR * (1f + 0.1f * sin(t)), stroke)
                for (i in 0 until min(mags.size, 48)) {
                    val angle = (i.toFloat() / min(mags.size, 48)) * 2f * Math.PI.toFloat()
                    val m = mags[i].coerceIn(0f, 1f)
                    val len = baseR * 1.1f + m * width * 0.16f * scale
                    setFill(lerp(config.secondaryColor, config.accentColor, m), glow = true)
                    canvas.drawCircle(cx + cos(angle) * len, cy + sin(angle) * len, width * 0.006f, fill)
                }
            }
        }
    }

    // ===================================================================
    // WAVE
    // ===================================================================
    private fun drawWave(
        canvas: Canvas, width: Int, height: Int, wave: FloatArray,
        mags: FloatArray, scale: Float, mode: WaveMode
    ) {
        if (wave.isEmpty()) return
        val midY = (config.visualizerPositionY * height).coerceIn(height * 0.15f, height * 0.85f)
        val amp = height * 0.3f * scale
        val step = width.toFloat() / (wave.size - 1).coerceAtLeast(1)

        fun pathOffset(offset: Float, phase: Float, gain: Float): Path {
            val p = Path()
            var prevX = 0f
            var prevY = 0f
            for (i in wave.indices) {
                val x = i * step
                val y = midY + offset + wave[i].coerceIn(-1f, 1f) * amp * gain
                    + sin((i.toFloat() / wave.size) * 6.28f + phase) * amp * 0.1f
                if (i == 0) {
                    p.moveTo(x, y)
                } else {
                    // Quadratic smoothing through midpoints -> fluid, anti-aliased
                    // curves instead of jagged line segments.
                    val midX = (prevX + x) / 2f
                    val midY2 = (prevY + y) / 2f
                    p.quadTo(prevX, prevY, midX, midY2)
                }
                prevX = x
                prevY = y
            }
            p.lineTo(prevX, prevY)
            return p
        }

        when (mode) {
            WaveMode.SINGLE -> {
                setStroke(config.accentColor, 1f, width * 0.006f, true, glowRadius = 14f)
                canvas.drawPath(pathOffset(0f, 0f, 1f), stroke)
            }
            WaveMode.DUAL -> {
                setStroke(config.accentColor, 1f, width * 0.006f, true)
                canvas.drawPath(pathOffset(0f, 0f, 1f), stroke)
                setStroke(config.secondaryColor, 0.9f, width * 0.006f, true)
                canvas.drawPath(pathOffset(0f, Math.PI.toFloat(), 1f), stroke)
            }
            WaveMode.MULTI -> {
                val cols = listOf(config.accentColor, config.secondaryColor, 0xFF00E5FF.toInt(), 0xFFFF00E5.toInt())
                for (k in cols.indices) {
                    setStroke(cols[k], 0.8f - k * 0.12f, width * 0.004f, true, glowRadius = 10f)
                    canvas.drawPath(pathOffset(0f, k * 1.3f, 0.8f + k * 0.08f), stroke)
                }
            }
            WaveMode.MIRROR -> {
                setStroke(config.accentColor, 1f, width * 0.006f, true)
                canvas.drawPath(pathOffset(0f, 0f, 1f), stroke)
                setStroke(config.secondaryColor, 1f, width * 0.006f, true)
                val p = Path()
                for (i in wave.indices) {
                    val x = width - i * step
                    val y = midY + wave[i].coerceIn(-1f, 1f) * amp
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                canvas.drawPath(p, stroke)
            }
            WaveMode.FILLED -> {
                val p = pathOffset(0f, 0f, 1f)
                p.lineTo(width.toFloat(), midY)
                p.lineTo(0f, midY)
                p.close()
                fill.shader = LinearGradient(0f, midY - amp, 0f, midY + amp,
                    intArrayOf(
                        withAlpha(config.accentColor, 0.85f),
                        withAlpha(lerp(config.accentColor, config.secondaryColor, 0.5f), 0.4f),
                        withAlpha(config.secondaryColor, 0.08f)
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawPath(p, fill)
                fill.shader = null
                setStroke(config.accentColor, 1f, width * 0.005f, true)
                canvas.drawPath(pathOffset(0f, 0f, 1f), stroke)
            }
            WaveMode.RIBBON -> {
                val p = Path()
                for (i in wave.indices) {
                    val x = i * step
                    val y = midY + sin(i.toFloat() / wave.size * 12.56f + wave[i] * 2f) * amp * 0.8f
                    if (i == 0) p.moveTo(x, y) else p.quadTo(x - step / 2f, y, x, y)
                }
                fill.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, config.accentColor, config.secondaryColor, Shader.TileMode.CLAMP)
                fill.alpha = 200
                canvas.drawPath(p, fill)
                fill.alpha = 255
                fill.shader = null
            }
            WaveMode.STEPS -> {
                setStroke(config.accentColor, 1f, width * 0.008f, true)
                for (i in 1 until wave.size) {
                    val x0 = (i - 1) * step
                    val x1 = i * step
                    val y = midY + wave[i].coerceIn(-1f, 1f) * amp
                    canvas.drawLine(x0, midY + wave[i - 1].coerceIn(-1f, 1f) * amp, x1, y, stroke)
                    canvas.drawLine(x1, midY + wave[i - 1].coerceIn(-1f, 1f) * amp, x1, y, stroke)
                }
            }
            WaveMode.SMOOTH -> {
                val p = pathOffset(0f, 0f, 1f)
                p.lineTo(width.toFloat(), height.toFloat())
                p.lineTo(0f, height.toFloat())
                p.close()
                fill.shader = LinearGradient(0f, midY - amp, 0f, height.toFloat(),
                    withAlpha(config.accentColor, 0.6f), withAlpha(config.secondaryColor, 0.05f), Shader.TileMode.CLAMP)
                canvas.drawPath(p, fill)
                fill.shader = null
                setStroke(config.accentColor, 1f, width * 0.005f, true)
                canvas.drawPath(pathOffset(0f, 0f, 1f), stroke)
            }
            WaveMode.CROSS -> {
                setStroke(config.accentColor, 1f, width * 0.005f, true)
                canvas.drawPath(pathOffset(0f, 0f, 1f), stroke)
                setStroke(config.secondaryColor, 1f, width * 0.005f, true)
                canvas.drawPath(pathOffset(0f, Math.PI.toFloat(), -1f), stroke)
            }
            WaveMode.ECHO -> {
                for (k in 3 downTo 0) {
                    setStroke(config.accentColor, (0.25f + k * 0.2f), width * 0.005f, k == 0, glowRadius = 10f)
                    canvas.drawPath(pathOffset(0f, k * 0.6f, 1f - k * 0.12f), stroke)
                }
            }
        }
    }

    // ===================================================================
    // PARTICLES
    // ===================================================================
    private fun drawParticles(
        canvas: Canvas, cx: Float, cy: Float, width: Int, height: Int,
        beat: Float, mags: FloatArray, scale: Float, mode: ParticleMode
    ) {
        val energy = avg(mags, 0, mags.size).coerceIn(0f, 1f)
        val bass = avg(mags, 0, mags.size / 6).coerceIn(0f, 1f)
        when (mode) {
            ParticleMode.BURST -> {
                setFill(config.accentColor, glow = true)
                val count = 48
                for (i in 0 until count) {
                    val angle = (i.toFloat() / count) * 2f * Math.PI.toFloat() + beat * 2f
                    val dist = width * 0.08f + (0.4f + energy) * width * 0.34f * scale * ((phaseA[i % 64] + beat) % 1f)
                    val x = cx + cos(angle) * dist
                    val y = cy + sin(angle) * dist
                    fill.color = withAlpha(lerp(config.secondaryColor, config.accentColor, (i.toFloat() / count)), 1f)
                    canvas.drawCircle(x, y, width * 0.006f * (1f + energy * 2f), fill)
                }
            }
            ParticleMode.ORB -> {
                val orbR = width * 0.12f * (1f + bass * scale)
                fill.shader = RadialGradient(cx, cy, orbR * 1.6f, withAlpha(config.accentColor, 0.9f), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(cx, cy, orbR * 1.6f, fill)
                fill.shader = null
                setFill(config.secondaryColor, 1f, glow = true)
                canvas.drawCircle(cx, cy, orbR * (0.6f + bass), fill)
                val count = 40
                for (i in 0 until count) {
                    val angle = (i.toFloat() / count) * 2f * Math.PI.toFloat() + beat * 3f
                    val r = orbR * 1.4f + sin(beat * 6f + i) * width * 0.08f * (1f + energy)
                    setFill(if (i % 3 == 0) config.accentColor else config.secondaryColor, glow = true)
                    canvas.drawCircle(cx + cos(angle) * r, cy + sin(angle) * r, width * 0.005f * (1f + bass), fill)
                }
            }
            ParticleMode.STAR -> {
                val count = 90
                for (i in 0 until count) {
                    val ang = phaseA[i % 64] * 6.28f
                    val z = ((phaseB[i % 64] + beat * (0.5f + energy)) % 1f)
                    val radius = width * 0.05f + z * width * 0.45f * scale
                    val x = cx + cos(ang) * radius
                    val y = cy + sin(ang) * radius
                    setFill(0xFFFFFFFF.toInt(), (1f - z).coerceIn(0.1f, 1f), glow = true)
                    canvas.drawCircle(x, y, (2f + (1f - z) * 6f) * scale, fill)
                }
            }
            ParticleMode.CLOUD -> {
                val count = 70
                for (i in 0 until count) {
                    val ang = phaseA[i % 64] * 6.28f + beat
                    val radius = width * (0.1f + 0.35f * ((phaseB[i % 64] + beat) % 1f)) * scale
                    val x = cx + cos(ang) * radius
                    val y = cy + sin(ang * 1.3f) * radius * 0.7f
                    setFill(lerp(config.secondaryColor, config.accentColor, (i.toFloat() / count)), 0.8f, glow = true)
                    canvas.drawCircle(x, y, width * 0.004f * (1f + energy * 2f), fill)
                }
            }
            ParticleMode.FIREWORKS -> {
                val shells = 5
                for (s in 0 until shells) {
                    val sAng = (s.toFloat() / shells) * 6.28f
                    val sx = cx + cos(sAng) * width * 0.25f * scale
                    val sy = cy + sin(sAng) * width * 0.25f * scale
                    val radius = width * 0.04f + ((phaseB[s] + beat) % 1f) * width * 0.18f * scale
                    val count = 24
                    for (i in 0 until count) {
                        val ang = (i.toFloat() / count) * 6.28f
                        setFill(hsv(s * 70f + i * 4f, 0.9f, 1f), (1f - ((phaseB[s] + beat) % 1f)).coerceIn(0.1f, 1f), glow = true)
                        canvas.drawCircle(sx + cos(ang) * radius, sy + sin(ang) * radius, width * 0.004f, fill)
                    }
                }
            }
            ParticleMode.METEOR -> {
                val count = 26
                for (i in 0 until count) {
                    val prog = ((phaseA[i % 64] + beat * (0.6f + energy)) % 1f)
                    val x = width * (1.2f - prog * 1.4f)
                    val y = height * (0.2f + i.toFloat() / count * 0.6f)
                    val len = width * 0.06f * (1f + energy)
                    setStroke(0xFFFFFFFF.toInt(), 1f, width * 0.004f, true, glowRadius = 12f)
                    canvas.drawLine(x, y, x - len, y - len, stroke)
                }
            }
            ParticleMode.SNOW -> {
                val count = 70
                for (i in 0 until count) {
                    val x = (i.toFloat() / count) * width + sin(beat * 2f + i) * width * 0.04f
                    val y = (height * ((phaseA[i % 64] + beat) % 1f))
                    setFill(0xFFFFFFFF.toInt(), 0.8f, glow = true)
                    canvas.drawCircle(x, y, (3f + ((phaseB[i % 64] + beat) % 1f) * 4f) * scale, fill)
                }
            }
            ParticleMode.RADIAL -> {
                val count = 60
                for (i in 0 until count) {
                    val ang = (i.toFloat() / count) * 6.28f
                    val radius = width * 0.06f + ((phaseA[i % 64] + beat) % 1f) * width * 0.4f * scale
                    setFill(hsv(i.toFloat() / count * 360f, 0.9f, 1f), (1f - ((phaseA[i % 64] + beat) % 1f)).coerceIn(0.1f, 1f), glow = true)
                    canvas.drawCircle(cx + cos(ang) * radius, cy + sin(ang) * radius, width * 0.006f, fill)
                }
            }
            ParticleMode.ORBIT -> {
                val rings = 3
                for (r in 0 until rings) {
                    val count = 18 + r * 6
                    val radius = width * (0.12f + r * 0.1f) * (1f + bass * scale)
                    for (i in 0 until count) {
                        val ang = (i.toFloat() / count) * 6.28f + beat * (2f + r)
                        setFill(if (i % 2 == 0) config.accentColor else config.secondaryColor,
                            (0.5f + 0.5f * sin(ang)), glow = true)
                        canvas.drawCircle(cx + cos(ang) * radius, cy + sin(ang) * radius * 0.6f, width * 0.005f, fill)
                    }
                }
            }
            ParticleMode.GALAXY -> {
                val arms = 4
                val dots = 40
                for (a in 0 until arms) {
                    for (d in 0 until dots) {
                        val f = d.toFloat() / dots
                        val ang = (a * 6.28f / arms) + f * 6f + beat * 4f
                        val radius = width * (0.05f + f * 0.4f) * scale
                        setFill(lerp(config.secondaryColor, config.accentColor, f), 0.9f, glow = true)
                        canvas.drawCircle(cx + cos(ang) * radius, cy + sin(ang) * radius, width * 0.004f * (1f + f), fill)
                    }
                }
            }
        }
    }

    // ===================================================================
    // GEOMETRIC
    // ===================================================================
    private fun drawGeometric(
        canvas: Canvas, cx: Float, cy: Float, width: Int, height: Int,
        beat: Float, mags: FloatArray, scale: Float, mode: GeoMode
    ) {
        val energy = avg(mags, 0, mags.size).coerceIn(0f, 1f)
        val bass = avg(mags, 0, mags.size / 6).coerceIn(0f, 1f)
        when (mode) {
            GeoMode.HEX -> {
                val size = width * 0.07f * scale
                for (r in -2..2) for (c in -2..2) {
                    val offX = if (r % 2 == 0) 0f else size * 1.5f
                    val hx = cx + c * size * 3f + offX
                    val hy = cy + r * size * 0.86f * 2f
                    val pulse = sin(beat * 4f + r + c).coerceIn(0f, 1f)
                    val cur = size * (0.6f + pulse * 0.4f * (0.5f + energy))
                    val p = Path()
                    for (i in 0..5) {
                        val a = i * 60f * Math.PI.toFloat() / 180f
                        val x = hx + cur * cos(a)
                        val y = hy + cur * sin(a)
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    p.close()
                    setStroke(if ((r + c) % 2 == 0) config.accentColor else config.secondaryColor, 0.7f, width * 0.004f, true)
                    canvas.drawPath(p, stroke)
                }
            }
            GeoMode.CRYSTAL -> {
                val nodes = 8
                val radius = width * 0.22f * (1f + bass * 0.2f * scale)
                val pts = List(nodes) { idx ->
                    val a = (idx.toFloat() / nodes) * 6.28f + beat * 1.5f
                    val rr = radius * (0.8f + 0.2f * sin(beat * 3f + idx))
                    kotlin.Pair(cx + cos(a) * rr, cy + sin(a) * rr)
                }
                for (i in pts.indices) for (j in i + 1 until pts.size) {
                    setStroke(config.accentColor, 0.25f, width * 0.003f, glow = false)
                    canvas.drawLine(pts[i].first, pts[i].second, pts[j].first, pts[j].second, stroke)
                }
                for (p in pts) {
                    setFill(config.secondaryColor, 1f, glow = true)
                    canvas.drawCircle(p.first, p.second, width * 0.01f, fill)
                }
            }
            GeoMode.ISO -> {
                val n = 6
                val s = width * 0.045f * scale
                for (r in 0 until n) for (c in 0 until n) {
                    val ix = cx + (c - r) * s * 1.5f
                    val iy = cy * 0.8f + (c + r) * s * 0.75f
                    val depth = sin(beat * 3f + (r + c) * 0.4f).coerceIn(0f, 1f) * (0.5f + energy)
                    val h = s * 3f * depth
                    val top = Path().apply {
                        moveTo(ix, iy - h); lineTo(ix + s * 1.5f, iy - s * 0.75f - h)
                        lineTo(ix, iy - s * 1.5f - h); lineTo(ix - s * 1.5f, iy - s * 0.75f - h); close()
                    }
                    setFill(config.accentColor, 0.8f, glow = false)
                    canvas.drawPath(top, fill)
                    val left = Path().apply {
                        moveTo(ix - s * 1.5f, iy - s * 0.75f - h); lineTo(ix, iy - h)
                        lineTo(ix, iy); lineTo(ix - s * 1.5f, iy - s * 0.75f); close()
                    }
                    setFill(config.secondaryColor, 0.6f, glow = false)
                    canvas.drawPath(left, fill)
                    val right = Path().apply {
                        moveTo(ix, iy - h); lineTo(ix + s * 1.5f, iy - s * 0.75f - h)
                        lineTo(ix + s * 1.5f, iy - s * 0.75f); lineTo(ix, iy); close()
                    }
                    setFill(config.accentColor, 0.45f, glow = false)
                    canvas.drawPath(right, fill)
                }
            }
            GeoMode.HELIX -> {
                val nodes = 22
                val span = width * 0.8f
                val startX = (width - span) / 2f
                val spacing = span / (nodes - 1)
                for (i in 0 until nodes) {
                    val nx = startX + i * spacing
                    val off = i * 0.4f + beat * 2f
                    val yA = cy + sin(off) * height * 0.22f * scale
                    val yB = cy - sin(off) * height * 0.22f * scale
                    setStroke(0xFFFFFFFF.toInt(), 0.25f, width * 0.003f, glow = false)
                    canvas.drawLine(nx, yA, nx, yB, stroke)
                    setFill(config.accentColor, 0.85f, glow = true)
                    canvas.drawCircle(nx, yA, width * 0.008f + cos(off) * width * 0.003f, fill)
                    setFill(config.secondaryColor, 0.85f, glow = true)
                    canvas.drawCircle(nx, yB, width * 0.008f - cos(off) * width * 0.003f, fill)
                }
            }
            GeoMode.KALEIDO -> {
                val seg = 8
                val maxLen = width * 0.4f * (0.6f + energy)
                for (s in 0 until seg) {
                    canvas.save()
                    canvas.rotate(s * 45f + beat * 20f, cx, cy)
                    val p = Path()
                    p.moveTo(cx, cy)
                    val w = sin(beat * 2f).coerceIn(0f, 1f) * maxLen
                    p.lineTo(cx + w, cy + sin(beat * 1.5f) * height * 0.1f)
                    setStroke(config.secondaryColor, 0.7f, width * 0.005f, true)
                    canvas.drawPath(p, stroke)
                    setFill(config.accentColor, 0.9f, glow = true)
                    canvas.drawCircle(cx + w, cy + sin(beat * 1.5f) * height * 0.1f, width * 0.012f, fill)
                    canvas.restore()
                }
            }
            GeoMode.PRISM -> {
                val size = width * 0.18f * (1f + bass * 0.1f)
                val p = Path().apply {
                    moveTo(cx, cy - size); lineTo(cx + size * 0.86f, cy + size * 0.5f)
                    lineTo(cx - size * 0.86f, cy + size * 0.5f); close()
                }
                val sweep = SweepGradient(cx, cy, intArrayOf(
                    hsv(0f, 0.9f, 1f), hsv(120f, 0.9f, 1f), hsv(240f, 0.9f, 1f), hsv(360f, 0.9f, 1f)), null)
                fill.shader = sweep
                fill.alpha = 70
                canvas.drawPath(p, fill)
                fill.alpha = 255
                fill.shader = null
                setStroke(config.accentColor, 1f, width * 0.006f, true)
                canvas.drawPath(p, stroke)
                for (i in 0..6) {
                    val a = -120f + i * 40f
                    setStroke(hsv(i * 60f, 0.9f, 1f), 0.8f, width * 0.004f, true)
                    canvas.drawLine(cx, cy + size * 0.5f, cx + cos(a * Math.PI.toFloat() / 180f) * width * 0.4f,
                        cy + size * 0.5f + sin(a * Math.PI.toFloat() / 180f) * width * 0.4f, stroke)
                }
            }
            GeoMode.DIAMOND -> {
                val d = 4
                val base = width * 0.08f * scale
                for (k in 0 until d) {
                    val sz = base * (k + 1) * (1f + sin(beat * 3f - k * 0.5f).coerceIn(0f, 1f) * 0.3f * (0.5f + energy))
                    val p = Path().apply {
                        moveTo(cx, cy - sz); lineTo(cx + sz, cy); lineTo(cx, cy + sz); lineTo(cx - sz, cy); close()
                    }
                    setStroke(if (k % 2 == 0) config.accentColor else config.secondaryColor, 0.8f, width * 0.005f, true)
                    canvas.drawPath(p, stroke)
                }
            }
            GeoMode.LASER -> {
                val beams = 16
                for (i in 0 until beams) {
                    val ang = (i.toFloat() / beams) * 6.28f + beat * 8f
                    val len = width * (0.6f + energy) * scale
                    setStroke(if (i % 2 == 0) config.accentColor else config.secondaryColor,
                        (0.4f + 0.6f * sin(beat * 5f + i).coerceIn(0f, 1f)), width * 0.004f, true, glowRadius = 18f)
                    canvas.drawLine(cx, cy, cx + cos(ang) * len, cy + sin(ang) * len, stroke)
                }
            }
            GeoMode.INFINITY -> {
                val steps = 80
                val radius = width * 0.3f * scale
                val p = Path()
                for (i in 0..steps) {
                    val t = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
                    val denom = 1f + sin(t) * sin(t)
                    val x = cx + (radius * cos(t)) / denom
                    val y = cy + (radius * sin(t) * cos(t)) / denom
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                canvas.save()
                canvas.rotate(beat * 30f, cx, cy)
                setStroke(config.accentColor, 1f, width * 0.007f, true, glowRadius = 16f)
                canvas.drawPath(p, stroke)
                canvas.restore()
            }
            GeoMode.FREQ -> {
                val lines = 2
                for (l in 0 until lines) {
                    val p = Path()
                    val amp = height * 0.25f * (1f + energy) * scale
                    for (i in 0..48) {
                        val x = i * width / 48f
                        val y = (cy + (l * 2 - 1) * height * 0.12f) + sin(i * 0.4f + beat * 4f + l * 2f) * amp
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    setStroke(if (l == 0) config.accentColor else config.secondaryColor, 0.9f, width * 0.005f, true)
                    canvas.drawPath(p, stroke)
                }
            }
            GeoMode.TUNNEL -> {
                val squares = 8
                for (i in 0 until squares) {
                    val f = (i.toFloat() / squares + beat * 0.5f) % 1f
                    val sz = width * (0.04f + f * 0.45f) * scale
                    canvas.save()
                    canvas.rotate(beat * 30f + i * 12f, cx, cy)
                    setStroke(lerp(config.secondaryColor, config.accentColor, f), (1f - f) * 0.9f + 0.1f,
                        width * 0.005f * (0.5f + bass), true)
                    canvas.drawRect(cx - sz, cy - sz, cx + sz, cy + sz, stroke)
                    canvas.restore()
                }
            }
        }
    }

    // ===================================================================
    // MINIMAL
    // ===================================================================
    private fun drawMinimal(
        canvas: Canvas, width: Int, height: Int, mags: FloatArray,
        scale: Float, mode: MinimalMode
    ) {
        val bars = min(mags.size, 64)
        if (bars == 0) return
        val gap = width * 0.006f
        val barWidth = (width - gap * (bars + 1)) / bars
        val posY = (config.visualizerPositionY * height).coerceIn(height * 0.1f, height * 0.9f)
        when (mode) {
            MinimalMode.LINE -> {
                setStroke(config.accentColor, 1f, width * 0.0025f, glow = false)
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val h = m * height * 0.4f * scale
                    val x = gap + i * (barWidth + gap) + barWidth / 2f
                    canvas.drawLine(x, posY - h, x, posY + h, stroke)
                }
            }
            MinimalMode.DOT -> {
                setFill(config.accentColor, glow = false)
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val h = m * height * 0.4f * scale
                    val x = gap + i * (barWidth + gap) + barWidth / 2f
                    canvas.drawCircle(x, posY - h, barWidth * 0.5f, fill)
                }
            }
            MinimalMode.PULSE -> {
                val energy = avg(mags, 0, mags.size).coerceIn(0f, 1f)
                setStroke(config.accentColor, 1f, width * 0.004f, glow = false)
                val p = Path()
                for (x in 0..width step 4) {
                    val m = mags[(x * mags.size / width).coerceIn(0, mags.size - 1)].coerceIn(0f, 1f)
                    val y = posY + (m - energy) * height * 0.25f * scale
                    if (x == 0) p.moveTo(x.toFloat(), y) else p.lineTo(x.toFloat(), y)
                }
                canvas.drawPath(p, stroke)
            }
            MinimalMode.BARS -> {
                setFill(config.accentColor, glow = false)
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val h = m * height * 0.4f * scale
                    val left = gap + i * (barWidth + gap)
                    canvas.drawRect(left, posY - h, left + barWidth, posY, fill)
                }
            }
            MinimalMode.EQUALIZER -> {
                setFill(config.accentColor, glow = false)
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val colH = height * 0.45f * scale
                    val dotCount = (m * 10f).toInt().coerceIn(0, 10)
                    val left = gap + i * (barWidth + gap) + barWidth / 2f
                    for (d in 0 until dotCount) {
                        canvas.drawCircle(left, posY - d * (colH / 10f), barWidth * 0.4f, fill)
                    }
                }
            }
            MinimalMode.TICK -> {
                setStroke(config.accentColor, 1f, width * 0.002f, glow = false)
                for (i in 0 until bars) {
                    val m = mags[i * mags.size / bars].coerceIn(0f, 1f)
                    val h = m * height * 0.4f * scale
                    val x = gap + i * (barWidth + gap) + barWidth / 2f
                    canvas.drawLine(x, posY - h, x, posY - h * 0.4f, stroke)
                    canvas.drawLine(x, posY + h * 0.4f, x, posY + h, stroke)
                }
            }
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
    ): RectF {
        val bw = bitmapWidth.toFloat()
        val bh = bitmapHeight.toFloat()
        if (bw <= 0f || bh <= 0f) return RectF(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat())
        val scale = if (fit == BackgroundFit.CROP) {
            maxOf(frameWidth / bw, frameHeight / bh)
        } else {
            minOf(frameWidth / bw, frameHeight / bh)
        }
        val dw = (bw * scale)
        val dh = (bh * scale)
        val left = (frameWidth - dw) / 2f
        val top = (frameHeight - dh) / 2f
        return RectF(left, top, left + dw, top + dh)
    }
}

/**
 * Small, dependency-free radix-2 FFT + helpers used to turn a window of PCM
 * samples into normalised frequency magnitudes for the export path.
 */
object AudioSpectrum {

    fun magnitudes(samples: FloatArray, outBins: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(outBins)
        var n = 1
        while (n < samples.size) n = n shl 1
        if (n > 2048) n = 2048
        val re = FloatArray(n)
        val im = FloatArray(n)
        val copy = min(n, samples.size)
        for (i in 0 until copy) {
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

    fun beat(mags: FloatArray): Float {
        if (mags.isEmpty()) return 0f
        val lowEnd = (mags.size / 6).coerceAtLeast(1)
        var sum = 0f
        for (i in 0 until lowEnd) sum += mags[i]
        return (sum / lowEnd).coerceIn(0f, 1f)
    }

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
