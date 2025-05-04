package com.danihg.calypsoapp.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.danihg.calypsoapp.R
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import java.util.Locale

/**
 * Draws a bitmap onto the canvas with a given alpha.
 */
private fun drawStaticBitmap(
    canvas: Canvas,
    bitmap: Bitmap?,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    alpha: Int = 255
) {
    val rect = Rect(x, y, x + width, y + height)
    if (bitmap != null) {
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.alpha = alpha }
        canvas.drawBitmap(scaled, x.toFloat(), y.toFloat(), Paint().apply { this.alpha = alpha })
    } else {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.Black.toArgb()
            this.alpha = alpha
        }.also { canvas.drawRect(rect, it) }
    }
}

/**
 * Draws text onto the canvas with a given alpha.
 */
private fun drawStaticText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    alpha: Int = 255
) {
    paint.alpha = alpha
    canvas.drawText(text, x, y, paint)
}

/**
 * Creates a static team players overlay bitmap.
 */
fun createTeamPlayersBitmap(
    context: Context,
    screenWidth: Int,
    screenHeight: Int,
    leftLogo: Bitmap?,
    rightLogo: Bitmap?,
    leftName: String,
    rightName: String,
    leftPlayers: List<PlayerEntry>,
    rightPlayers: List<PlayerEntry>
): Bitmap {
    val bmp = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Load backgrounds
    val leftBg = getBitmap(context, R.drawable.team_players_bg_left)
    val rightBg = getBitmap(context, R.drawable.team_players_bg_right)

    val scale = 0.3f
    val scaleLogo = 0.4f
    var nameSize = 50f
    if (leftName.length > 10 || rightName.length > 10) nameSize = 40f

    // Draw left main
    leftBg?.let { lb ->
        val w = (lb.width * scale).toInt()
        val h = (lb.height * scale).toInt()
        val x = screenWidth / 5
        val y = screenHeight / 6
        drawStaticBitmap(canvas, lb, x, y, w, h)
        leftLogo?.let {
            drawStaticBitmap(canvas, it, x + 50, y + 15, 50, 50)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
            textSize = nameSize
            setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
        }
        val tx = x + w / 2f
        val ty = y + h / 2f - ((paint.descent() + paint.ascent()) / 2f)
        drawStaticText(canvas, leftName.uppercase(Locale.ROOT), tx, ty, paint)
    }

    // Draw right main
    rightBg?.let { rb ->
        val w = (rb.width * scale).toInt()
        val h = (rb.height * scale).toInt()
        val x = screenWidth / 2
        val y = screenHeight / 6
        drawStaticBitmap(canvas, rb, x, y, w, h)
        rightLogo?.let {
            drawStaticBitmap(canvas, it, x + w - 50 - 50, y + 15, 50, 50)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
            textSize = nameSize
            setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
        }
        val tx = x + w / 2f
        val ty = y + h / 2f - ((paint.descent() + paint.ascent()) / 2f)
        drawStaticText(canvas, rightName.uppercase(Locale.ROOT), tx, ty, paint)
    }

    // Draw player rows
    val row1L = getBitmap(context, R.drawable.team_players_row_1_left)
    val row1R = getBitmap(context, R.drawable.team_players_row_1_right)
    val row2L = getBitmap(context, R.drawable.team_players_row_2_left)
    val row2R = getBitmap(context, R.drawable.team_players_row_2_right)

    val max = maxOf(leftPlayers.size, rightPlayers.size)
    val gap = 30
    val numGap = 30
    val baseY = screenHeight / 6 + ((leftBg?.height?.times(scale))?.toInt() ?: 0) + gap
    for (i in 0 until max) {
        val alpha = 255
        val isEven = i % 2 == 0
        val scaledH = ((if (isEven) row1L else row2L)?.height ?: row1L!!.height) * scale
        // Left row
        (if (isEven) row1L else row2L)?.let {
            val w = (it.width * scale).toInt()
            val h = (it.height * scale).toInt()
            val x = screenWidth / 5 + (((leftBg?.width?.times(scale))?.toInt() ?: 0) - w)
            val y = baseY + (h * i).toInt()
            drawStaticBitmap(canvas, it, x, y, w, h, alpha)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
                typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                textSize = 30f
                setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
            }
            val name = leftPlayers.getOrNull(i)?.name?.uppercase(Locale.ROOT) ?: ""
            val num = leftPlayers.getOrNull(i)?.number ?: ""
            drawStaticText(canvas, name, x + (w + numGap) / 2f, y + (h + gap - 10) / 2f, paint, alpha)
            drawStaticText(canvas, num, x + ((numGap + 50) / 2f), y + (h + gap - 10) / 2f, paint, alpha)
        }
        // Right row
        (if (isEven) row1R else row2R)?.let {
            val w = (it.width * scale).toInt()
            val h = (it.height * scale).toInt()
            val x = screenWidth / 2
            val y = baseY + (h * i).toInt()
            drawStaticBitmap(canvas, it, x, y, w, h, alpha)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
                typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                textSize = 30f
                setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
            }
            val name = rightPlayers.getOrNull(i)?.name?.uppercase(Locale.ROOT) ?: ""
            val num = rightPlayers.getOrNull(i)?.number ?: ""
            drawStaticText(canvas, name, x + (w - numGap) / 2f, y + (h + gap - 10) / 2f, paint, alpha)
            drawStaticText(canvas, num, x + (w - 45).toFloat(), y + (h + gap - 10) / 2f, paint, alpha)
        }
    }

    return bmp
}

private fun getBitmap(context: Context, resId: Int): Bitmap? {
    return try {
        BitmapFactory.decodeResource(context.resources, resId)
    } catch (_: Exception) {
        null
    }
}

/**
 * Draws the overlay:
 */
fun drawTeamPlayersOverlay(
    context: Context,
    screenWidth: Int,
    screenHeight: Int,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamName: String,
    rightTeamName: String,
    leftTeamPlayers: List<PlayerEntry>,
    rightTeamPlayers: List<PlayerEntry>,
    imageObjectFilterRender: ImageObjectFilterRender,
    isOnPreview: Boolean
) {

    val bmp = createTeamPlayersBitmap(
        context, screenWidth, screenHeight,
        leftLogoBitmap, rightLogoBitmap,
        leftTeamName, rightTeamName,
        leftTeamPlayers, rightTeamPlayers
    )
    imageObjectFilterRender.setImage(bmp)
}

data class PlayerEntry(
    val number: String,
    val name: String
)
