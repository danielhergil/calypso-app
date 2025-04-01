package com.danihg.calypsoapp.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.danihg.calypsoapp.R
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import java.util.Locale

// Global object to hold row animation state (only row index is needed)
object TeamPlayersAnimationState {
    var currentRow: Int = -1
    var animationJob: Job? = null
}

data class PlayerEntry(
    val number: String,
    val name: String
)

/**
 * Draws a bitmap onto the canvas with a given alpha.
 */
fun drawStaticBitmap(
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
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val paint = Paint().apply { this.alpha = alpha }
        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), paint)
    } else {
        val fallbackPaint = Paint().apply {
            color = Color.Black.toArgb()
            this.alpha = alpha
        }
        canvas.drawRect(rect, fallbackPaint)
    }
}

/**
 * Draws text onto the canvas with a given alpha.
 */
fun drawStaticText(
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
 * Creates a team players overlay bitmap that includes the main elements (backgrounds, logos, team names)
 * and player rows. Only rows with index <= TeamPlayersAnimationState.currentRow are drawn.
 *
 * The new parameter fadeAlpha controls the opacity for the currently animating row.
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
    rightTeamPlayers: List<PlayerEntry>,
    fadeAlpha: Float = 1f
): Bitmap {
    val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Load background images.
    val leftBackground = getBitmapFromResource(context, R.drawable.team_players_bg_left)
    val rightBackground = getBitmapFromResource(context, R.drawable.team_players_bg_right)

    // Define scaling factors.
    val scaleFactor = 0.3f
    val scaleFactorLogo = 0.4f

    var teamNameSize = 50f

    if (leftTeamName.length > 10 || rightTeamName.length > 10) {
        teamNameSize = 40f
    }


    // --- Left Side Main Section ---
    leftBackground?.let { lb ->
        val scaledWidth = (lb.width * scaleFactor).toInt()
        val scaledHeight = (lb.height * scaleFactor).toInt()
        val x = screenWidth / 5
        val y = screenHeight / 6
        drawStaticBitmap(canvas, lb, x, y, scaledWidth, scaledHeight)
        leftLogoBitmap?.let { logo ->
//            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
//            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            val logoScaledWidth = 50
            val logoScaledHeight = 50
            val logoX = x + 50
            val logoY = y + 15
            drawStaticBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight)
        }
        val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
            textSize = teamNameSize
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
//            val logoScaledWidth = (logo.width * scaleFactorLogo).toInt()
//            val logoScaledHeight = (logo.height * scaleFactorLogo).toInt()
            val logoScaledWidth = 50
            val logoScaledHeight = 50
            val logoX = x + scaledWidth - logoScaledWidth - 50
            val logoY = y + 15
            drawStaticBitmap(canvas, logo, logoX, logoY, logoScaledWidth, logoScaledHeight)
            val bigTextPaintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
                typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                textSize = teamNameSize
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
            // Determine the alpha value for the row.
            val currentAlpha = if (i == TeamPlayersAnimationState.currentRow)
                (fadeAlpha * 255).toInt().coerceIn(0, 255)
            else 255

            if (i % 2 == 0) {
                // Row type 1.
                leftPlayerRow_1?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt() ?: 0
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth - scaledWidth)
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight, currentAlpha)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth + numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    val leftPlayerName = if (i < leftTeamPlayers.size) {
                        leftTeamPlayers[i].name.uppercase(Locale.ROOT)
                    } else ""
                    drawStaticText(canvas, leftPlayerName, textX, textY, bigTextPaintLeft, currentAlpha)

                    val leftPlayerNumber = if (i < leftTeamPlayers.size) {
                        leftTeamPlayers[i].number
                    } else ""

                    val numberXPos = 50

                    val numberX = x + (numberGap + numberXPos) / 2f
                    val numberY = y + (scaledHeight + initialGap - 10) / 2f

                    drawStaticText(canvas, leftPlayerNumber, numberX, numberY, bigTextPaintLeft, currentAlpha)
                }
                rightPlayerRow_1?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight, currentAlpha)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth - numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    val rightPlayerName = if (i < rightTeamPlayers.size) {
                        rightTeamPlayers[i].name.uppercase(Locale.ROOT)
                    } else ""
                    drawStaticText(canvas, rightPlayerName, textX, textY, bigTextPaintLeft, currentAlpha)

                    val rightPlayerNumber = if (i < rightTeamPlayers.size) {
                        rightTeamPlayers[i].number
                    } else ""

                    val numberXPos = 45

                    val numberX = x + (scaledWidth - numberXPos) / 1f
                    val numberY = y + (scaledHeight + initialGap - 10) / 2f

                    drawStaticText(canvas, rightPlayerNumber, numberX, numberY, bigTextPaintLeft, currentAlpha)
                }
            } else {
                // Row type 2.
                leftPlayerRow_2?.let {
                    val bgScaledWidth = (leftBackground?.width?.times(scaleFactor))?.toInt() ?: 0
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 5 + (bgScaledWidth - scaledWidth)
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight, currentAlpha)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth + numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    val leftPlayerName = if (i < leftTeamPlayers.size) {
                        leftTeamPlayers[i].name.uppercase(Locale.ROOT)
                    } else ""
                    drawStaticText(canvas, leftPlayerName, textX, textY, bigTextPaintLeft, currentAlpha)

                    val leftPlayerNumber = if (i < leftTeamPlayers.size) {
                        leftTeamPlayers[i].number
                    } else ""

                    val numberXPos = 50

                    val numberX = x + (numberGap + numberXPos) / 2f
                    val numberY = y + (scaledHeight + initialGap - 10) / 2f

                    drawStaticText(canvas, leftPlayerNumber, numberX, numberY, bigTextPaintLeft, currentAlpha)
                }
                rightPlayerRow_2?.let {
                    val scaledWidth = (it.width * scaleFactor).toInt()
                    val scaledHeight = (it.height * scaleFactor).toInt()
                    val x = screenWidth / 2
                    val y = screenHeight / 6 + ((leftBackground?.height?.times(scaleFactor))?.toInt() ?: 0) + initialGap + scaledHeight * i
                    drawStaticBitmap(canvas, it, x, y, scaledWidth, scaledHeight, currentAlpha)
                    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                        textSize = 30f
                        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
                    }
                    val textX = x + (scaledWidth - numberGap) / 2f
                    val textY = y + (scaledHeight + initialGap - 10) / 2f
                    val rightPlayerName = if (i < rightTeamPlayers.size) {
                        rightTeamPlayers[i].name.uppercase(Locale.ROOT)
                    } else ""
                    drawStaticText(canvas, rightPlayerName, textX, textY, bigTextPaintLeft, currentAlpha)

                    val rightPlayerNumber = if (i < rightTeamPlayers.size) {
                        rightTeamPlayers[i].number
                    } else ""

                    val numberXPos = 45

                    val numberX = x + (scaledWidth - numberXPos) / 1f
                    val numberY = y + (scaledHeight + initialGap - 10) / 2f

                    drawStaticText(canvas, rightPlayerNumber, numberX, numberY, bigTextPaintLeft, currentAlpha)
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
 * then each row fades in fast.
 *
 * Before starting, any previous animation is canceled, and the filter image is cleared.
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
    // Cancel any ongoing animation.
    TeamPlayersAnimationState.animationJob?.cancel()
    // Reset state and clear any previous image.
    TeamPlayersAnimationState.currentRow = -1
    imageObjectFilterRender.setImage(null)

    TeamPlayersAnimationState.animationJob = CoroutineScope(Dispatchers.Main).launch {
        // Immediately show the main overlay (without rows).
        val mainOverlay = withContext(Dispatchers.Default) {
            createTeamPlayersBitmapSequential(
                context, screenWidth, screenHeight,
                leftLogoBitmap, rightLogoBitmap,
                leftTeamName, rightTeamName,
                leftTeamPlayers, rightTeamPlayers
            )
        }
        imageObjectFilterRender.setImage(mainOverlay)

        // Determine the maximum number of rows.
        val maxCount = maxOf(leftTeamPlayers.size, rightTeamPlayers.size)
        delay(1000L)

        // For each row index, animate a fade-in.
        suspend fun revealNextRow(current: Int) {
            if (current < maxCount) {
                TeamPlayersAnimationState.currentRow = current
                val fadeSteps = 15  // Increased fade steps for smoother transition.
                for (step in 0 until fadeSteps) {
                    val fadeAlpha = (step + 1) / fadeSteps.toFloat()  // Gradually increase from 0 to 1.
                    val updatedOverlay = withContext(Dispatchers.Default) {
                        createTeamPlayersBitmapSequential(
                            context, screenWidth, screenHeight,
                            leftLogoBitmap, rightLogoBitmap,
                            leftTeamName, rightTeamName,
                            leftTeamPlayers, rightTeamPlayers,
                            fadeAlpha = fadeAlpha
                        )
                    }
                    imageObjectFilterRender.setImage(updatedOverlay)
                    delay(15L)  // Shorter delay for smoother fade (total fade duration ~225ms).
                }
                delay(50L)
                revealNextRow(current + 1)
            }
        }
        revealNextRow(0)
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
