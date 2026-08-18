package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.salmanlaghari.pulsemusicplayerai.R

/**
 * Builds the high-resolution Pulse Music Player watermark bitmap used when
 * burning the logo into exported MP4s.
 *
 * The glyph is rasterised from the vector drawable [R.drawable.ic_pulse_watermark]
 * (resolution independent -> crisp at 1080p) and a bold "PULSE" wordmark is
 * rendered alongside it. The result is a single transparent ARGB_8888 bitmap
 * that [VisualizerFrameRenderer] composites in the bottom-right corner. Because
 * it is generated at a large size and then scaled DOWN into the frame, it never
 * looks pixelated — satisfying the "HD / crisp" watermark requirement without
 * shipping a heavy bitmap asset.
 */
object WatermarkAssets {
    private const val WIDTH = 768
    private const val HEIGHT = 240

    fun loadPulseWatermark(context: Context): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)

            // --- glyph (left) ---
            val glyphSize = (HEIGHT * 0.82f).toInt()
            val glyphTop = ((HEIGHT - glyphSize) / 2f).toInt()
            val drawable = context.getDrawable(R.drawable.ic_pulse_watermark)
            if (drawable != null) {
                drawable.setBounds(0, glyphTop, glyphSize, glyphTop + glyphSize)
                drawable.draw(canvas)
            }

            // --- wordmark (right of glyph) ---
            val textX = glyphSize + 24f
            val wordmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.LEFT
                typeface = Typeface.DEFAULT_BOLD
                textSize = HEIGHT * 0.46f
            }
            val wordmarkY = HEIGHT * 0.62f
            canvas.drawText("PULSE", textX, wordmarkY, wordmarkPaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.LEFT
                typeface = Typeface.DEFAULT
                textSize = HEIGHT * 0.14f
                alpha = 200
            }
            canvas.drawText("MUSIC PLAYER", textX + 4f, wordmarkY + HEIGHT * 0.18f, subPaint)

            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
