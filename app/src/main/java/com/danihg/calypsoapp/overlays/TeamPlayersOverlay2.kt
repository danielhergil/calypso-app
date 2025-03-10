package com.danihg.calypsoapp.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.danihg.calypsoapp.R
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import java.util.Locale

// Global object to hold row animation state (only row index is needed)
object TeamPlayersAnimationState {
    var currentRow: Int = -1
}

data class PlayerEntry(
    val number: String,
    val name: String
)

/**
 * Draws a bitmap onto the canvas without any fade.
 */
fun drawStaticBitmap(
    canvas: Canvas,
    bitmap: Bitmap?,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) {
    val rect = Rect(x, y, x + width, y + height)
    if (bitmap != null) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
    } else {
        val fallbackPaint = Paint().apply { color = Color.Black.toArgb() }
        canvas.drawRect(rect, fallbackPaint)
    }
}

/**
 * Draws text onto the canvas without any fade.
 */
fun drawStaticText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint
) {
    canvas.drawText(text, x, y, paint)
}

/**
 * Creates a team players overlay bitmap that includes the main elements (backgrounds, logos, team names)
 * and player rows. Only rows with index <= TeamPlayersAnimationState.currentRow are drawn.
 */
fun createTeamPlayersBitmapSequential(
    context: Context,
    screenWidth: Int,
    screenHeight: Int,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamName: String,
    rightTeamName: String,
    leftTeamPlayers: List<PlayerEntry>,
    rightTeamPlayers: List<PlayerEntry>
): Bitmap {
    val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Load background images.
    val leftBackground = getBitmapFromResource(context, R.drawable.team_players_bg_left)
    val rightBackground = getBitmapFromResource(context, R.drawable.team_players_bg_right)

    // Define scaling factors.
    val scaleFactor = 0.3f
    val scaleFactorLogo = 0.4f

    // --- Left Side Main Section ---
    leftBackground?.let { lb ->
        val scaledWidth = (lb.width * scaleFactor).toInt()
        val scaledHeight = (lb.height * scaleFactor).toInt()
        val x = screenWidth / 5
        val y = screenHeight / 6
        drawStaticBitmap(canvas, lb, x, y, scaledWidth, scaledHeight)
        leftLogoBitmap?.let { logo ->
            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            val logoX = x + 50
            val logoY = y + 15
            drawStaticBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight)
        }
        val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
            textSize = 50f
            setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
        }
        val textX = x + scaledWidth / 2f
        val textY = y + scaledHeight / 2f - ((bigTextPaintLeft.descent() + bigTextPaintLeft.ascent()) / 2f)
        drawStaticText(canvas, leftTeamName.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft)
    }

    // --- Right Side Main Section ---
    rightBackground?.let { rb ->
        val scaledWidth = (rb.width * scaleFactor).toInt()
        val scaledHeight = (rb.height * scaleFactor).toInt()
        val x = screenWidth / 2
        val y = screenHeight / 6
        drawStaticBitmap(canvas, rb, x, y, scaledWidth, scaledHeight)
        rightLogoBitmap?.let { logo ->
            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            val logoX = x + scaledWidth - logoScaledWidth - 70
            val logoY = y + 15
            drawStaticBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight)
            val bigTextPaintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
                typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                textSize = 50f
                setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
            }
            val textX = x + scaledWidth / 2f
            val textY = y + scaledHeight / 2f - ((bigTextPaintRight.descent() + bigTextPaintRight.ascent()) / 2f)
            drawStaticText(canvas, rightTeamName.uppercase(Locale.ROOT), textX, textY, bigTextPaintRight)
        }
    }

    // --- Draw Player Rows Sequentially ---
    // Load row resources.
    val leftPlayerRow_1 = getBitmapFromResource(context, R.drawable.team_players_row_1_left)
    val rightPlayerRow_1 = getBitmapFromResource(context, R.drawable.team_players_row_1_right)
    val leftPlayerRow_2 = getBitmapFromResource(context, R.drawable.team_players_row_2_left)
    val rightPlayerRow_2 = getBitmapFromResource(context, R.drawable.team_players_row_2_right)

    val leftTeamCount = leftTeamPlayers.size
    val rightTeamCount = rightTeamPlayers.size
    val maxCount = maxOf(leftTeamCount, rightTeamCount)
    val initialGap = 30
    val numberGap = 30

    // For each row index, draw the row only if it is less than or equal to currentRow.
    for (i in 0 until maxCount) {
        if (i <= TeamPlayersAnimationState.currentRow) {
            if (i % 2 == 0) {
                // Row type 1.
                leftPlayerRow_1?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt() ?: 0
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth - scaledWidth)
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth + numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawStaticText(canvas, leftTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft)
                }
                rightPlayerRow_1?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth - numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawStaticText(canvas, rightTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft)
                }
            } else {
                // Row type 2.
                leftPlayerRow_2?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt() ?: 0
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth - scaledWidth)
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth + numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawStaticText(canvas, leftTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft)
                }
                rightPlayerRow_2?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth - numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawStaticText(canvas, rightTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft)
                }
            }
        }
    }

    return bitmap
}

private fun getBitmapFromResource(context: Context, resId: Int): Bitmap? {
    return try {
        BitmapFactory.decodeResource(context.resources, resId)
    } catch (e: Exception) {
        null
    }
}

/**
 * Updates the team players overlay sequentially: first the main overlay is shown,
 * then the player rows are revealed one by one (without fade).
 */
@SuppressLint("DiscouragedApi")
fun updateTeamPlayersOverlaySequential(
    context: Context,
    screenWidth: Int,
    screenHeight: Int,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamName: String,
    rightTeamName: String,
    leftTeamPlayers: List<PlayerEntry>,
    rightTeamPlayers: List<PlayerEntry>,
    imageObjectFilterRender: ImageObjectFilterRender
) {
    Handler(Looper.getMainLooper()).post {
        // Immediately show the main overlay (without rows).
        TeamPlayersAnimationState.currentRow = -1
        val mainOverlay = createTeamPlayersBitmapSequential(
            context, screenWidth, screenHeight,
            leftLogoBitmap, rightLogoBitmap,
            leftTeamName, rightTeamName,
            leftTeamPlayers, rightTeamPlayers
        )
        imageObjectFilterRender.setImage(mainOverlay)

        // Determine the maximum number of rows.
        val maxCount = maxOf(leftTeamPlayers.size, rightTeamPlayers.size)

        // Sequentially reveal rows using a Handler with delay.
        fun revealNextRow(current: Int) {
            if (current < maxCount) {
                TeamPlayersAnimationState.currentRow = current
                val updatedOverlay = createTeamPlayersBitmapSequential(
                    context, screenWidth, screenHeight,
                    leftLogoBitmap, rightLogoBitmap,
                    leftTeamName, rightTeamName,
                    leftTeamPlayers, rightTeamPlayers
                )
                imageObjectFilterRender.setImage(updatedOverlay)
                Handler(Looper.getMainLooper()).postDelayed({
                    revealNextRow(current + 1)
                }, 50L) // Delay (in milliseconds) between rows.
            }
        }
        // Start revealing rows after a short delay.
        Handler(Looper.getMainLooper()).postDelayed({
            revealNextRow(0)
        }, 1000L)
    }
}

/**
 * Call this function to draw the overlay sequentially (first main overlay then rows).
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
    if (isOnPreview) {
        updateTeamPlayersOverlaySequential(
            context, screenWidth, screenHeight,
            leftLogoBitmap, rightLogoBitmap,
            leftTeamName, rightTeamName,
            leftTeamPlayers, rightTeamPlayers,
            imageObjectFilterRender
        )
    }
}
