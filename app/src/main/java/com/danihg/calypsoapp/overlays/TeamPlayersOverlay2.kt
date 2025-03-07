package com.danihg.calypsoapp.overlays

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.danihg.calypsoapp.R
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import java.util.Locale


data class PlayerEntry(
    val number: String,
    val name: String
)

/**
 * Draws a bitmap onto the canvas with a vertical gradient fade.
 * @param canvas The canvas to draw on.
 * @param bitmap The bitmap to draw.
 * @param x The left coordinate where the bitmap will be drawn.
 * @param y The top coordinate where the bitmap will be drawn.
 * @param width The target width.
 * @param height The target height.
 * @param progress A float value (0f to 1f) controlling the reveal: 0f = fully faded; 1f = fully visible.
 * @param fallbackColor A fallback color if bitmap is null.
 */
fun drawFadedBitmap(
    canvas: Canvas,
    bitmap: Bitmap?,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    progress: Float,
    fallbackColor: Int = Color.Black.toArgb()
) {
    // Define the rectangle where the bitmap will be drawn.
    val rect = Rect(x, y, x + width, y + height)
    if (bitmap != null) {
        // Scale the bitmap to match the target dimensions.
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)

        // Calculate the vertical gradient fade.
        val gradientHeight = height * 0.4f // Extend gradient for a smoother transition
        val transitionPoint = y + (height * progress)
        val startY = transitionPoint - gradientHeight
        val endY = transitionPoint

        // Create a vertical gradient from WHITE (fade) to TRANSPARENT.
        val gradient = LinearGradient(
            x.toFloat(), startY,
            x.toFloat(), endY,
            intArrayOf(Color.White.toArgb(), Color.Transparent.toArgb()),
            null,
            Shader.TileMode.CLAMP
        )

        // Use DST_IN so that only the parts under the opaque area are kept.
        val fadePaint = Paint().apply {
            shader = gradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // Only draw the fade if the bitmap is not fully revealed.
        if (progress < 1.0f) {
            canvas.drawRect(rect, fadePaint)
        }
    } else {
        // Fallback if the bitmap is null.
        val fallbackPaint = Paint().apply { color = fallbackColor }
        canvas.drawRect(rect, fallbackPaint)
    }
}

/**
 * Draws text onto the canvas with a vertical gradient fade.
 *
 * @param canvas The canvas to draw on.
 * @param text The string to draw.
 * @param x The x-coordinate for the text. Its interpretation depends on paint.textAlign.
 * @param y The y-coordinate for the text’s baseline.
 * @param paint The Paint used for drawing the text (its textAlign property controls alignment).
 * @param progress A float value (0f to 1f) controlling the reveal:
 *                 0f = fully faded; 1f = fully visible.
 */
fun drawFadedText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    progress: Float
) {
    // Save a canvas layer so that the fade is only applied to our text.
    val saveCount = canvas.saveLayer(null, null)

    // Draw the text normally.
    canvas.drawText(text, x, y, paint)

    if (progress < 1f) {
        // Measure the text width and compute text height.
        val textWidth = paint.measureText(text)
        val textHeight = paint.descent() - paint.ascent()

        // Determine the left edge of the text based on alignment.
        val textLeft = when (paint.textAlign) {
            Paint.Align.LEFT -> x
            Paint.Align.CENTER -> x - textWidth / 2f
            Paint.Align.RIGHT -> x - textWidth
        }

        // Compute top and bottom relative to the baseline.
        val textTop = y + paint.ascent()
        val textBottom = y + paint.descent()
        val textRect = RectF(textLeft, textTop, textLeft + textWidth, textBottom)

        // Define the gradient fade zone.
        val gradientHeight = textHeight * 0.4f  // 40% of the text height.
        val revealLine = textTop + textHeight * progress
        val gradientStart = revealLine - gradientHeight
        val gradientEnd = revealLine

        // Create a vertical gradient from opaque white to transparent.
        val gradient = LinearGradient(
            0f, gradientStart,
            0f, gradientEnd,
            intArrayOf(Color.White.toArgb(), Color.Transparent.toArgb()),
            null,
            Shader.TileMode.CLAMP
        )

        // Create a paint that applies the gradient fade using DST_IN.
        val fadePaint = Paint().apply {
            shader = gradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // Apply the gradient fade over the text's bounds.
        canvas.drawRect(textRect, fadePaint)
    }

    // Restore the canvas.
    canvas.restoreToCount(saveCount)
}

/**
 * Creates a team players bitmap with two images (left and right) that are drawn with a fade effect.
 * @param progress A float value from 0f (fully faded) to 1f (fully visible).
 */
fun createTeamPlayersBitmap(context: Context, screenWidth: Int, screenHeight: Int, leftLogoBitmap: Bitmap?, rightLogoBitmap: Bitmap?, leftTeamName: String, rightTeamName: String, leftTeamPlayers: List<PlayerEntry>, rightTeamPlayers: List<PlayerEntry>, progress: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Load both image resources.
    val leftBackground = getBitmapFromResource(context, R.drawable.team_players_bg_left)
    val rightBackground = getBitmapFromResource(context, R.drawable.team_players_bg_right)

    val leftBackgroundWidth = leftBackground?.width

    Log.d("TeamPlayersBitmap", "Left background width: ${leftBackground?.width}")
    Log.d("TeamPlayersBitmap", "Left background height: ${leftBackground?.height}")

    // Use a scaling factor (adjust as needed).
    val scaleFactor = 0.3f
    val scaleFactorLogo = 0.4f

    // Draw left image.
    leftBackground?.let {
        val scaledWidth = (it.width * scaleFactor).toInt()
        val scaledHeight = (it.height * scaleFactor).toInt()
        val x = screenWidth / 5
        val y = screenHeight / 6
        drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)

        leftLogoBitmap?.let { logo ->
            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            val logoX = screenWidth / 5 + 50
            val logoY = screenHeight / 6 + 15
            drawFadedBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight, progress)
        }

        val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
            textSize = 50f
            setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
        }

        val textX = x + (scaledWidth + (leftLogoBitmap?.width?.times(scaleFactorLogo)?.toInt()!!)) / 2f
        val textY = y + 60

        drawFadedText(canvas, leftTeamName.uppercase(Locale.ROOT), textX.toFloat(), textY.toFloat(), bigTextPaintLeft, progress)
    }

    // Draw right image.
    rightBackground?.let {
        val scaledWidth = (it.width * scaleFactor).toInt()
        val scaledHeight = (it.height * scaleFactor).toInt()
        val x = screenWidth / 2
        val y = screenHeight / 6
        drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)

        // Draw right logo at the end of rightBackground.
        rightLogoBitmap?.let { logo ->
            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            // Calculate x to start at the end of rightBackground.
            val logoX = x + scaledWidth - logo.width - 50
            val logoY = y + 15
            drawFadedBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight, progress)

            val bigTextPaintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
                typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                textSize = 50f
                setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
            }

            val textX = x + (scaledWidth - (rightLogoBitmap.width.times(scaleFactorLogo).toInt())) / 2f
            val textY = y + 60

            drawFadedText(canvas, rightTeamName.uppercase(Locale.ROOT), textX.toFloat(), textY.toFloat(), bigTextPaintRight, progress)
        }
    }

    val leftPlayerRow_1 = getBitmapFromResource(context, R.drawable.team_players_row_1_left)
    val rightPlayerRow_1 = getBitmapFromResource(context, R.drawable.team_players_row_1_right)
    val leftPlayerRow_2 = getBitmapFromResource(context, R.drawable.team_players_row_2_left)
    val rightPlayerRow_2 = getBitmapFromResource(context, R.drawable.team_players_row_2_right)


    val leftTeamCount = leftTeamPlayers.size
    val rightTeamCount = rightTeamPlayers.size

    val maxCount = maxOf(leftTeamCount, rightTeamCount)

    val initialGap = 30

    for (i in 0 until maxCount) {
        if (i % 2 == 0) {
            if (i == 0) {
                leftPlayerRow_1?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt()
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth?.minus(scaledWidth)!!)
                    val y = screenHeight / 6 + (leftBackground.height * scaleFactor).toInt() + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)
                }
                rightPlayerRow_1?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt()!!) + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)
                }
            } else {
                leftPlayerRow_1?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt()
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth?.minus(scaledWidth)!!)
                    val y = screenHeight / 6 + ((leftBackground.height.times(scaleFactor)).toInt()) + scaledHeight * i + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)
                }
                rightPlayerRow_1?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt()!!) + scaledHeight * i + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)
                }
            }
        } else {
            leftPlayerRow_2?.let {
                val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt()
                val scaledWidth = (it.width * scaleFactor).toInt()
                val scaledHeight = (it.height * scaleFactor).toInt()
                val x = screenWidth / 5 + (bgScaledWidth?.minus(scaledWidth)!!)
                val y = screenHeight / 6 + ((leftBackground.height.times(scaleFactor)).toInt()) + scaledHeight * i + initialGap
                drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)
            }
            rightPlayerRow_2?.let {
                val scaledWidth = (it.width * scaleFactor).toInt()
                val scaledHeight = (it.height * scaleFactor).toInt()
                val x = screenWidth / 2
                val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt()!!) + scaledHeight * i + initialGap
                drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, progress)
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
 * Updates the team players overlay using a ValueAnimator to animate the gradient fade.
 */
@SuppressLint("DiscouragedApi")
fun updateTeamPlayersOverlay(
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
        // Animate the progress value from 0 (fully faded) to 1 (fully revealed).
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500L // Duration in milliseconds (adjust as needed)
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val playersBitmap = createTeamPlayersBitmap(context, screenWidth, screenHeight, leftLogoBitmap, rightLogoBitmap, leftTeamName, rightTeamName, leftTeamPlayers, rightTeamPlayers, progress)
                imageObjectFilterRender.setImage(playersBitmap)
                // Optionally, adjust scale and position of your overlay:
                // imageObjectFilterRender.setScale(...)
                // imageObjectFilterRender.setPosition(...)
            }
        }
        animator.start()
    }
}

/**
 * Call this function to draw the overlay.
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
        updateTeamPlayersOverlay(context, screenWidth, screenHeight, leftLogoBitmap, rightLogoBitmap, leftTeamName, rightTeamName, leftTeamPlayers, rightTeamPlayers, imageObjectFilterRender)
    }
}
