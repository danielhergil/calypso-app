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

// Global object to hold row animation state.
object TeamPlayersAnimationState {
    // currentRow: -1 means no row is being animated yet.
    var currentRow: Int = -1
    // currentRowProgress: progress (0f to 1f) for the currently animating row.
    var currentRowProgress: Float = 0f
}

// Global cache for the fully drawn main overlay.
var cachedMainBitmap: Bitmap? = null

data class PlayerEntry(
    val number: String,
    val name: String
)

/**
 * Draws a bitmap onto the canvas with a vertical gradient fade.
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

        // Use a smaller fade region (30% of height)
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

        // Use a smaller fade zone (30% of text height)
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
 * The main part (backgrounds, logos, team names) is drawn using the "progress" parameter.
 * The rows are drawn using the global TeamPlayersAnimationState.
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
    // (During the main animation, row progress is 0 so rows are not drawn.)
    drawPlayerRows(context, canvas, screenWidth, screenHeight, leftBackground, scaleFactor, leftTeamPlayers, rightTeamPlayers)

    return bitmap
}

/**
 * Draws the player rows on the provided canvas.
 */
private fun drawPlayerRows(
    context: Context,
    canvas: Canvas,
    screenWidth: Int,
    screenHeight: Int,
    leftBackground: Bitmap?,
    scaleFactor: Float,
    leftTeamPlayers: List<PlayerEntry>,
    rightTeamPlayers: List<PlayerEntry>
) {
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
            // Using row_1 resources.
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
                    // Draw the row text with the row's progress
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
}

/**
 * Helper: Creates a team players bitmap using the cached main overlay and draws only the player rows.
 */
fun createTeamPlayersBitmapWithCachedMain(
    context: Context,
    screenWidth: Int,
    screenHeight: Int,
    cachedMain: Bitmap,
    leftTeamPlayers: List<PlayerEntry>,
    rightTeamPlayers: List<PlayerEntry>
): Bitmap? {
    // Make a mutable copy of the cached main overlay.
    val bitmap = cachedMain.config?.let { cachedMain.copy(it, true) }
    val canvas = bitmap?.let { Canvas(it) }
    // Redraw only the rows.
    // We need the left background for positioning; reuse the cached version.
    val leftBackground = getBitmapFromResource(context, R.drawable.team_players_bg_left)
    val scaleFactor = 0.3f
    if (canvas != null) {
        drawPlayerRows(context, canvas, screenWidth, screenHeight, leftBackground, scaleFactor, leftTeamPlayers, rightTeamPlayers)
    }
    return bitmap
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
                // Cache the fully drawn main overlay.
                cachedMainBitmap = createTeamPlayersBitmap(
                    context, screenWidth, screenHeight,
                    leftLogoBitmap, rightLogoBitmap,
                    leftTeamName, rightTeamName,
                    leftTeamPlayers, rightTeamPlayers,
                    1f
                )
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
                    // Use cached main overlay and update rows only.
                    val bitmap = cachedMainBitmap?.let { cached ->
                        createTeamPlayersBitmapWithCachedMain(context, screenWidth, screenHeight, cached, leftTeamPlayers, rightTeamPlayers)
                    }
                    bitmap?.let { imageObjectFilterRender.setImage(it) }
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

private fun getBitmapFromResource(context: Context, resId: Int): Bitmap? {
    return try {
        BitmapFactory.decodeResource(context.resources, resId)
    } catch (e: Exception) {
        null
    }
}
