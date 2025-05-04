package com.danihg.calypsoapp.overlays

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

fun normalizeColorName(colorName: String): String {
    return colorName.lowercase().replace(" ", "_") // Convert to lowercase and replace spaces
}

/**
 * Creates a scoreboard bitmap using your attached scoreboard PNG (with transparency)
 * with a fixed layout:
 *
 * - Overall canvas size is fixed at 770×200.
 * - The left logo is flush at the very left edge.
 * - The scoreboard background is drawn immediately to the right of the left logo with a minimal gap.
 * - The right logo is flush at the right edge.
 * - The scoreboard background displays numeric score (centered) plus hardcoded team names ("RIV" and "ALC").
 *
 * Adjust the constants below to fine‑tune spacing and positions.
 */
@SuppressLint("DiscouragedApi")
fun createScoreboardBitmap(
    context: Context,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    leftTeamAlias: String,
    rightTeamAlias: String,
    leftTeamColor: String,
    rightTeamColor: String,
    showLogos: Boolean,
    showAlias: Boolean,
    backgroundColor: Int  // Not used visually; kept for parameter compatibility.
): Bitmap {
    // Fixed sizes for this layout:
    val leftLogoSize = 60
    val rightLogoSize = 60
    // Make the logos narrower in X – for instance, 70% of the original width.
    val leftLogoWidth = (leftLogoSize * 0.8).toInt()  // e.g., 56
    val rightLogoWidth = (rightLogoSize * 0.8).toInt()  // e.g., 56

    val scoreboardWidth = 350   // Width of the scoreboard background area.
    val scoreboardHeight = 110  // Height of the scoreboard background area.
    val gapBetween = 1          // Minimal gap between logos and the scoreboard background.

    // Overall canvas dimensions:
    // Overall width = left logo (original full size) + gap + scoreboard + gap + right logo.
    val overallWidth = 550  // For example, 550.
    val overallHeight = 250  // For example, 250.

    // Logo positions:
    val leftLogoX = 140  // Flush to the left.
    // Adjust the vertical position if needed. Here we add 55 to center relative to the scoreboard.
    val leftLogoY = (overallHeight - leftLogoSize - 40) / 2

    // The right logo is positioned flush at the right edge.
    val rightLogoX = overallWidth - rightLogoSize - 130
    val rightLogoY = (overallHeight - rightLogoSize - 40) / 2

    // Scoreboard background position:
    val scoreboardX = 100  // Immediately to the right of left logo.
    val scoreboardY = 50
    val scoreboardRect = Rect(scoreboardX, scoreboardY, scoreboardX + scoreboardWidth, scoreboardY + scoreboardHeight)

    // Scoreboard background left
    val leftResourceName = "scoreboard_left_${normalizeColorName(leftTeamColor)}"
    val leftResourceId = context.resources.getIdentifier(leftResourceName, "drawable", context.packageName)
    Log.d("Scoreboard", "Left resource: $leftResourceName -> ID: $leftResourceId")
    val scoreboardBgLeft = getBitmapFromResource(context, leftResourceId)
    val scoreboardBgLeftX = scoreboardX - 85
    val scoreboardBgLeftY = scoreboardY + 20
    val scoreboardBgLeftWidth = 110
    val scoreboardBgLeftHeight = 70
    val scoreboardBgLeftRect = Rect(scoreboardBgLeftX, scoreboardBgLeftY, scoreboardBgLeftX + scoreboardBgLeftWidth, scoreboardBgLeftY + scoreboardBgLeftHeight)

    // Scoreboard background right
    val rightResourceName = "scoreboard_right_${normalizeColorName(rightTeamColor)}"
    val rightResourceId = context.resources.getIdentifier(rightResourceName, "drawable", context.packageName)
    Log.d("Scoreboard", "Right resource: $rightResourceName -> ID: $rightResourceId")
    val scoreboardBgRight = getBitmapFromResource(context, rightResourceId)
    val scoreboardBgRightX = scoreboardX + scoreboardWidth - 25
    val scoreboardBgRightY = scoreboardY + 20
    val scoreboardBgRightWidth = 110
    val scoreboardBgRightHeight = 70
    val scoreboardBgRightRect = Rect(scoreboardBgRightX, scoreboardBgRightY, scoreboardBgRightX + scoreboardBgRightWidth, scoreboardBgRightY + scoreboardBgRightHeight)

    // Create the overall bitmap and canvas.
    val bitmap = Bitmap.createBitmap(overallWidth, overallHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Load and scale the scoreboard background PNG to exactly scoreboardWidth×scoreboardHeight.
    val scoreboardBg = getBitmapFromResource(context, R.drawable.fondo_tablero)
    if (scoreboardBg != null) {
        val scaledBg = Bitmap.createScaledBitmap(scoreboardBg, scoreboardWidth, scoreboardHeight, true)
        canvas.drawBitmap(scaledBg, null, scoreboardRect, null)
    } else {
        // Fallback: fill the scoreboard area with black.
        val fallbackPaint = Paint().apply { color = Color.Black.toArgb() }
        canvas.drawRect(scoreboardRect, fallbackPaint)
    }

    if (scoreboardBgLeft != null) {
        val scaledBgLeft = Bitmap.createScaledBitmap(scoreboardBgLeft, scoreboardBgLeftWidth, scoreboardBgLeftHeight, true)
        canvas.drawBitmap(scaledBgLeft, null, scoreboardBgLeftRect, null)
    } else {
        // Fallback: fill the scoreboard area with black.
        val fallbackPaint = Paint().apply { color = Color.Black.toArgb() }
        canvas.drawRect(scoreboardBgLeftRect, fallbackPaint)
    }

    if (scoreboardBgRight != null) {
        val scaledBgRight = Bitmap.createScaledBitmap(
            scoreboardBgRight,
            scoreboardBgRightWidth,
            scoreboardBgRightHeight,
            true
        )
        canvas.drawBitmap(scaledBgRight, null, scoreboardBgRightRect, null)
    } else {
        // Fallback: fill the scoreboard area with black.
        val fallbackPaint = Paint().apply { color = Color.Black.toArgb() }
        canvas.drawRect(scoreboardBgRightRect, fallbackPaint)
    }

    var leftTextColor = Color.White.toArgb()
    if (normalizeColorName(leftTeamColor) == "white" || normalizeColorName(leftTeamColor) == "yellow" || normalizeColorName(leftTeamColor) == "cyan") {
        leftTextColor = Color.Black.toArgb()
    }

    var rightTextColor = Color.White.toArgb()
    if (normalizeColorName(rightTeamColor) == "white" || normalizeColorName(rightTeamColor) == "yellow" || normalizeColorName(rightTeamColor) == "cyan") {
        rightTextColor = Color.Black.toArgb()
    }
    // Prepare text paints (fixed text sizes regardless of scoreboard background size).
    val bigTextPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = leftTextColor
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
        textSize = 50f
        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
    }
    val bigTextPaintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = rightTextColor
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
        textSize = 50f
        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
    }
    val bigTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
        textSize = 50f
        setShadowLayer(4f, 2f, 2f, Color(0x80000000).toArgb())
    }
//    val mediumTextPaint = Paint(bigTextPaint).apply {
//        textSize = 30f
//        typeface = ResourcesCompat.getFont(context, R.font.montserrat_medium)
//    }

    // Draw the numeric score centered within the scoreboard background.
    val scoreboardCenterX = scoreboardX + scoreboardWidth / 2f
    val scoreY = scoreboardY + scoreboardHeight * 0.65f
    val scoreText = if (leftTeamGoals >= 10 && rightTeamGoals >= 10) {
        "$leftTeamGoals - $rightTeamGoals"
    } else {
        "$leftTeamGoals  -  $rightTeamGoals"
    }
    canvas.drawText(scoreText, scoreboardCenterX, scoreY, bigTextPaint)

    // Draw the team names ("RIV" on left portion and "ALC" on right portion).
    bigTextPaintLeft.textSize = 35f
    bigTextPaintRight.textSize = 35f
    val leftTextX = scoreboardX - 95 + scoreboardWidth * 0.15f
    val rightTextX = scoreboardX + 85 + scoreboardWidth * 0.85f
    val teamTextY = scoreboardY + 15 + scoreboardHeight * 0.5f
    if (showAlias) {
        canvas.drawText(leftTeamAlias, leftTextX, teamTextY, bigTextPaintLeft)
        canvas.drawText(rightTeamAlias, rightTextX, teamTextY, bigTextPaintRight)
    }

    // Optionally, if you want to add "FULL TIME" at the top, you can do so here:
    // mediumTextPaint.textSize = 30f
    // val fullTimeY = scoreboardY + scoreboardHeight * 0.25f
    // canvas.drawText("FULL TIME", scoreboardCenterX, fullTimeY, mediumTextPaint)

    // Draw the logos at their fixed positions.
    // For the left logo, use the narrower width (leftLogoWidth) but the original height (leftLogoSize).
    leftLogoBitmap?.let {
        val destRect1 = Rect(
            leftLogoX,
            leftLogoY,
            leftLogoX + leftLogoWidth,
            leftLogoY + leftLogoSize
        )
        if(showLogos) {
            canvas.drawBitmap(it, null, destRect1, null)
        }
    }
    // For the right logo, use the narrower width (rightLogoWidth).
    rightLogoBitmap?.let {
        val destRect2 = Rect(
            rightLogoX,
            rightLogoY,
            rightLogoX + rightLogoWidth,
            rightLogoY + rightLogoSize
        )
        if (showLogos) {
            canvas.drawBitmap(it, null, destRect2, null)
        }
    }

    return bitmap
}

/**
 * Loads a bitmap from the given resource ID.
 */
private fun getBitmapFromResource(context: Context, resId: Int): Bitmap? {
    return try {
        BitmapFactory.decodeResource(context.resources, resId)
    } catch (e: Exception) {
        null
    }
}

/**
 * Updates the overlay by creating a new scoreboard bitmap and applying it.
 */
fun updateOverlay(
    context: Context,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    leftTeamAlias: String,
    rightTeamAlias: String,
    leftTeamColor: String,
    rightTeamColor: String,
    backgroundColor: Int,
    showLogos: Boolean,
    showAlias: Boolean,
    imageObjectFilterRender: ImageObjectFilterRender
) {
    Handler(Looper.getMainLooper()).post {
        val scoreboardBitmap = createScoreboardBitmap(
            context,
            leftLogoBitmap,
            rightLogoBitmap,
            leftTeamGoals,
            rightTeamGoals,
            leftTeamAlias,
            rightTeamAlias,
            leftTeamColor,
            rightTeamColor,
            showLogos,
            showAlias,
            backgroundColor
        )
        imageObjectFilterRender.setImage(scoreboardBitmap)

        // Set a scale factor to adjust the on-screen size.
        val scaleX = 30f
        val scaleY = scoreboardBitmap.height.toFloat() / scoreboardBitmap.width.toFloat() * scaleX
        imageObjectFilterRender.setScale(scaleX, scaleY)

        // Set the overlay position on screen.
        imageObjectFilterRender.setPosition(0f, 0f)
    }
}

/**
 * Draws the scoreboard overlay if the camera preview is active.
 */
fun drawOverlay(
    context: Context,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    leftTeamAlias: String,
    rightTeamAlias: String,
    leftTeamColor: String,
    rightTeamColor: String,
    backgroundColor: Int,
    showLogos: Boolean,
    showAlias: Boolean,
    imageObjectFilterRender: ImageObjectFilterRender,
    isOnPreview: Boolean
) {
    if (isOnPreview) {
        updateOverlay(
            context,
            leftLogoBitmap,
            rightLogoBitmap,
            leftTeamGoals,
            rightTeamGoals,
            leftTeamAlias,
            rightTeamAlias,
            leftTeamColor,
            rightTeamColor,
            backgroundColor,
            showLogos,
            showAlias,
            imageObjectFilterRender
        )
    }
}
