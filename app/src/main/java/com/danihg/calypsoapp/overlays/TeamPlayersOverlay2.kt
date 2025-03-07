package com.danihg.calypsoapp.overlays

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.animation.AccelerateDecelerateInterpolator
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

// Global object to hold row animation state
object TeamPlayersAnimationState {
    // currentRow: -1 means no row is being animated yet.
    var currentRow: Int = -1
    // currentRowProgress: progress (0f to 1f) for the currently animating row.
    var currentRowProgress: Float = 0f
}

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
    val rect = Rect(x, y, x + width, y + height)
    if (bitmap != null) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)

        // Use a smaller fade region (30% instead of 40%)
        val gradientHeight = height * 0.3f
        val transitionPoint = y + (height * progress)
        val startY = transitionPoint - gradientHeight
        val endY = transitionPoint

        val gradient = LinearGradient(
            x.toFloat(), startY,
            x.toFloat(), endY,
            intArrayOf(Color.White.toArgb(), Color.Transparent.toArgb()),
            null,
            Shader.TileMode.CLAMP
        )

        val fadePaint = Paint().apply {
            shader = gradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        if (progress < 1.0f) {
            canvas.drawRect(rect, fadePaint)
        }
    } else {
        val fallbackPaint = Paint().apply { color = fallbackColor }
        canvas.drawRect(rect, fallbackPaint)
    }
}

/**
 * Draws text onto the canvas with a vertical gradient fade.
 *
 * @param canvas The canvas to draw on.
 * @param text The string to draw.
 * @param x The x-coordinate for the text.
 * @param y The y-coordinate for the text’s baseline.
 * @param paint The Paint used for drawing the text.
 * @param progress A float value (0f to 1f) controlling the reveal.
 */
fun drawFadedText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    progress: Float
) {
    val saveCount = canvas.saveLayer(null, null)
    canvas.drawText(text, x, y, paint)

    if (progress < 1f) {
        val textWidth = paint.measureText(text)
        val textHeight = paint.descent() - paint.ascent()
        val textLeft = when (paint.textAlign) {
            Paint.Align.LEFT -> x
            Paint.Align.CENTER -> x - textWidth / 2f
            Paint.Align.RIGHT -> x - textWidth
        }
        val textTop = y + paint.ascent()
        val textBottom = y + paint.descent()
        val textRect = RectF(textLeft, textTop, textLeft + textWidth, textBottom)

        // Use a smaller fade zone (30% of text height).
        val gradientHeight = textHeight * 0.3f
        val revealLine = textTop + textHeight * progress
        val gradientStart = revealLine - gradientHeight
        val gradientEnd = revealLine

        val gradient = LinearGradient(
            0f, gradientStart,
            0f, gradientEnd,
            intArrayOf(Color.White.toArgb(), Color.Transparent.toArgb()),
            null,
            Shader.TileMode.CLAMP
        )

        val fadePaint = Paint().apply {
            shader = gradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawRect(textRect, fadePaint)
    }
    canvas.restoreToCount(saveCount)
}

/**
 * Creates a team players bitmap with two images (left and right) and rows of player entries.
 *
 * The main part is drawn using the "progress" parameter.
 * The rows are drawn using the global TeamPlayersAnimationState:
 * - Rows with index less than currentRow are fully drawn (progress = 1f)
 * - The row with index equal to currentRow is drawn partially based on currentRowProgress.
 * - Later rows are not drawn (progress = 0f).
 */
fun createTeamPlayersBitmap(
    context: Context,
    screenWidth: Int,
    screenHeight: Int,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamName: String,
    rightTeamName: String,
    leftTeamPlayers: List<PlayerEntry>,
    rightTeamPlayers: List<PlayerEntry>,
    progress: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val leftBackground = getBitmapFromResource(context, R.drawable.team_players_bg_left)
    val rightBackground = getBitmapFromResource(context, R.drawable.team_players_bg_right)

    Log.d("TeamPlayersBitmap", "Left background width: ${leftBackground?.width}")
    Log.d("TeamPlayersBitmap", "Left background height: ${leftBackground?.height}")

    val scaleFactor = 0.3f
    val scaleFactorLogo = 0.4f

    // Left Side Main Section
    leftBackground?.let { lb ->
        val scaledWidth = (lb.width * scaleFactor).toInt()
        val scaledHeight = (lb.height * scaleFactor).toInt()
        val x = screenWidth / 5
        val y = screenHeight / 6
        drawFadedBitmap(canvas, lb, x, y, scaledWidth, scaledHeight, progress)
        leftLogoBitmap?.let { logo ->
            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            val logoX = x + 50
            val logoY = y + 15
            drawFadedBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight, progress)
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
        drawFadedText(canvas, leftTeamName.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft, progress)
    }

    // Right Side Main Section
    rightBackground?.let { rb ->
        val scaledWidth = (rb.width * scaleFactor).toInt()
        val scaledHeight = (rb.height * scaleFactor).toInt()
        val x = screenWidth / 2
        val y = screenHeight / 6
        drawFadedBitmap(canvas, rb, x, y, scaledWidth, scaledHeight, progress)
        rightLogoBitmap?.let { logo ->
            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            val logoX = x + scaledWidth - logoScaledWidth - 70
            val logoY = y + 15
            drawFadedBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight, progress)
            val bigTextPaintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
                typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                textSize = 50f
                setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
            }
            val textX = x + scaledWidth / 2f
            val textY = y + scaledHeight / 2f - ((bigTextPaintRight.descent() + bigTextPaintRight.ascent()) / 2f)
            drawFadedText(canvas, rightTeamName.uppercase(Locale.ROOT), textX, textY, bigTextPaintRight, progress)
        }
    }

    // Draw Player Rows (animated sequentially)
    val leftPlayerRow_1 = getBitmapFromResource(context, R.drawable.team_players_row_1_left)
    val rightPlayerRow_1 = getBitmapFromResource(context, R.drawable.team_players_row_1_right)
    val leftPlayerRow_2 = getBitmapFromResource(context, R.drawable.team_players_row_2_left)
    val rightPlayerRow_2 = getBitmapFromResource(context, R.drawable.team_players_row_2_right)

    val leftTeamCount = leftTeamPlayers.size
    val rightTeamCount = rightTeamPlayers.size
    val maxCount = maxOf(leftTeamCount, rightTeamCount)
    val initialGap = 30
    val numberGap = 30

    for (i in 0 until maxCount) {
        val rowProgress = when {
            TeamPlayersAnimationState.currentRow == -1 -> 0f
            i < TeamPlayersAnimationState.currentRow -> 1f
            i == TeamPlayersAnimationState.currentRow -> TeamPlayersAnimationState.currentRowProgress
            else -> 0f
        }
        if (i % 2 == 0) {
            if (i == 0) {
                leftPlayerRow_1?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt() ?: 0
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth - scaledWidth)
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, rowProgress)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth + numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawFadedText(canvas, leftTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft, rowProgress)
                }
                rightPlayerRow_1?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, rowProgress)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth - numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawFadedText(canvas, rightTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft, rowProgress)
                }
            } else {
                leftPlayerRow_1?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt() ?: 0
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth - scaledWidth)
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + scaledHeight * i + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, rowProgress)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth + numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawFadedText(canvas, leftTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft, rowProgress)
                }
                rightPlayerRow_1?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + scaledHeight * i + initialGap
                    drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, rowProgress)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth - numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    drawFadedText(canvas, rightTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft, rowProgress)
                }
            }
        } else {
            leftPlayerRow_2?.let {
                val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt() ?: 0
                val scaledWidth = (it.width * scaleFactor).toInt()
                val scaledHeight = (it.height * scaleFactor).toInt()
                val x = screenWidth / 5 + (bgScaledWidth - scaledWidth)
                val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + scaledHeight * i + initialGap
                drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, rowProgress)
                val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.White.toArgb()
                    textAlign = Paint.Align.CENTER
                    typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                    textSize = 30f
                    setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                }
                val textX = x + (scaledWidth + numberGap) / 2f
                val textY = y + (scaledHeight + initialGap - 10) / 2f
                drawFadedText(canvas, leftTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft, rowProgress)
            }
            rightPlayerRow_2?.let {
                val scaledWidth = (it.width * scaleFactor).toInt()
                val scaledHeight = (it.height * scaleFactor).toInt()
                val x = screenWidth / 2
                val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + scaledHeight * i + initialGap
                drawFadedBitmap(canvas, it, x, y, scaledWidth, scaledHeight, rowProgress)
                val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.White.toArgb()
                    textAlign = Paint.Align.CENTER
                    typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                    textSize = 30f
                    setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                }
                val textX = x + (scaledWidth - numberGap) / 2f
                val textY = y + (scaledHeight + initialGap - 10) / 2f
                drawFadedText(canvas, rightTeamPlayers[i].name.uppercase(Locale.ROOT), textX, textY, bigTextPaintLeft, rowProgress)
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
 * Updates the team players overlay by first animating the main background (with logos and team names),
 * then sequentially animating each row.
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
        // Main animator: 400ms with smooth interpolator.
        val mainAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val bitmap = createTeamPlayersBitmap(
                    context, screenWidth, screenHeight,
                    leftLogoBitmap, rightLogoBitmap,
                    leftTeamName, rightTeamName,
                    leftTeamPlayers, rightTeamPlayers,
                    progress
                )
                imageObjectFilterRender.setImage(bitmap)
            }
        }
        mainAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animateRows(
                    context, screenWidth, screenHeight,
                    leftLogoBitmap, rightLogoBitmap,
                    leftTeamName, rightTeamName,
                    leftTeamPlayers, rightTeamPlayers,
                    imageObjectFilterRender
                )
            }
        })
        mainAnimator.start()
    }
}

/**
 * Animates the player rows sequentially.
 */
private fun animateRows(
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
    val leftTeamCount = leftTeamPlayers.size
    val rightTeamCount = rightTeamPlayers.size
    val maxCount = maxOf(leftTeamCount, rightTeamCount)
    var currentRow = 0

    fun animateNextRow() {
        if (currentRow < maxCount) {
            TeamPlayersAnimationState.currentRow = currentRow
            val rowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 250L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    TeamPlayersAnimationState.currentRowProgress = anim.animatedValue as Float
                    val bitmap = createTeamPlayersBitmap(
                        context, screenWidth, screenHeight,
                        leftLogoBitmap, rightLogoBitmap,
                        leftTeamName, rightTeamName,
                        leftTeamPlayers, rightTeamPlayers,
                        1f
                    )
                    imageObjectFilterRender.setImage(bitmap)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        currentRow++
                        animateNextRow()
                    }
                })
            }
            rowAnimator.start()
        } else {
            TeamPlayersAnimationState.currentRow = -1
            TeamPlayersAnimationState.currentRowProgress = 0f
        }
    }
    animateNextRow()
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
        updateTeamPlayersOverlay(
            context, screenWidth, screenHeight,
            leftLogoBitmap, rightLogoBitmap,
            leftTeamName, rightTeamName,
            leftTeamPlayers, rightTeamPlayers,
            imageObjectFilterRender
        )
    }
}
