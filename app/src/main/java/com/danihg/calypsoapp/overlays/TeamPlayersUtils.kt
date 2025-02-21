package com.danihg.calypsoapp.overlays

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.danihg.calypsoapp.R
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import kotlin.math.min

/**
 * Data class representing a player entry (name, optional number, etc.).
 */
data class PlayerEntry(
    val number: String,
    val name: String
)

/**
 * Creates a bitmap with two columns of players, a header for the teams, and a row-based layout
 * similar to the reference image. One side is tinted blue, the other red, and each side has
 * a list of players.
 *
 * @param context The Android context.
 * @param teamAName The left team name.
 * @param teamBName The right team name.
 * @param teamAPlayers A list of PlayerEntry for the left column.
 * @param teamBPlayers A list of PlayerEntry for the right column.
 * @param rowsVisible How many rows are currently visible in each column (for the "reveal" animation).
 * @return A bitmap with the team composition layout drawn.
 */
fun createTeamPlayersBitmap(
    context: Context,
    teamAName: String,
    teamBName: String,
    teamAPlayers: List<PlayerEntry>,
    teamBPlayers: List<PlayerEntry>,
    rowsVisible: Int
): Bitmap {
    // Basic layout sizes
    val width = 900
    val height = 600

    // Create the output bitmap/canvas
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color(0xFF1A1A1A).toArgb())  // Dark gray background (or your preference)

    // Title bar at the top: "TEAM A vs TEAM B"
    val titleBarHeight = 100
    val titleBarRect = Rect(0, 0, width, titleBarHeight)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), titleBarHeight.toFloat(),
            intArrayOf(Color(0xFF444444).toArgb(), Color(0xFF222222).toArgb()),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(titleBarRect, titlePaint)

    // Paint for the text in the title bar
    val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
    }

    // Draw the "TEAM A vs TEAM B" text in the center of the title bar
    val title = "$teamAName   vs   $teamBName"
    val titleX = width / 2f
    val titleY = titleBarHeight / 2f - (titleTextPaint.descent() + titleTextPaint.ascent()) / 2f
    canvas.drawText(title, titleX, titleY, titleTextPaint)

    // Each column is half the width, minus some spacing for the middle "vs" region if you want.
    // For simplicity, let's just do half & half.
    val columnWidth = width / 2
    val contentTop = titleBarHeight + 10
    val contentBottom = height - 10

    // We'll define 11 rows (or as many as your list has). If rowsVisible < 11, only some rows are drawn.
    // The row height depends on how many total rows you expect (here we assume 11).
    val maxRows = 11
    val rowCount = min(maxRows, rowsVisible)
    val rowHeight = (contentBottom - contentTop) / maxRows

    // Paint for the row backgrounds
    val leftBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, contentTop.toFloat(),
            0f, contentBottom.toFloat(),
            intArrayOf(Color(0xFF0033CC).toArgb(), Color(0xFF001F80).toArgb()),
            null,
            Shader.TileMode.CLAMP
        )
    }
    val rightBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            columnWidth.toFloat(), contentTop.toFloat(),
            columnWidth.toFloat(), contentBottom.toFloat(),
            intArrayOf(Color(0xFFCC0000).toArgb(), Color(0xFF800000).toArgb()),
            null,
            Shader.TileMode.CLAMP
        )
    }

    // Text paint for player entries
    val playerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textSize = 28f
        textAlign = Paint.Align.LEFT
        typeface = ResourcesCompat.getFont(context, R.font.montserrat_medium)
    }

    // Draw rows for Team A (left column)
    val leftColumnRect = Rect(0, contentTop, columnWidth, contentBottom)
    canvas.drawRect(leftColumnRect, leftBackgroundPaint)

    // Draw rows for Team B (right column)
    val rightColumnRect = Rect(columnWidth, contentTop, width, contentBottom)
    canvas.drawRect(rightColumnRect, rightBackgroundPaint)

    // For each visible row, draw the player's info
    for (i in 0 until rowCount) {
        // Defensive check if your team lists have fewer than i elements
        val leftPlayer = teamAPlayers.getOrNull(i)
        val rightPlayer = teamBPlayers.getOrNull(i)

        // Compute the y for this row
        val rowTop = contentTop + i * rowHeight
        val rowCenterY = rowTop + rowHeight / 2f

        // Left side text
        leftPlayer?.let {
            val textX = 20f
            // We'll combine the number and name: e.g. "1. Player name"
            val textStr = "${it.number}.  ${it.name}"
            // Center the text vertically
            val textY = rowCenterY - (playerTextPaint.descent() + playerTextPaint.ascent()) / 2f
            canvas.drawText(textStr, textX, textY, playerTextPaint)
        }

        // Right side text
        rightPlayer?.let {
            // We'll draw the text near the middle of the right column
            val textX = columnWidth + 20f
            val textStr = "${it.number}.  ${it.name}"
            val textY = rowCenterY - (playerTextPaint.descent() + playerTextPaint.ascent()) / 2f
            canvas.drawText(textStr, textX, textY, playerTextPaint)
        }
    }

    return bitmap
}

/**
 * Sets the newly created TeamPlayers bitmap to the filter.
 * @param rowsVisible How many rows are visible (for partial reveal).
 */
fun updateTeamPlayersOverlay(
    context: Context,
    teamAName: String,
    teamBName: String,
    teamAPlayers: List<PlayerEntry>,
    teamBPlayers: List<PlayerEntry>,
    rowsVisible: Int,
    imageObjectFilterRender: ImageObjectFilterRender
) {
    val bitmap = createTeamPlayersBitmap(context, teamAName, teamBName, teamAPlayers, teamBPlayers, rowsVisible)
    imageObjectFilterRender.setImage(bitmap)

    // Adjust scale/position if needed:
    val scaleX = 35f
    val scaleY = bitmap.height.toFloat() / bitmap.width.toFloat() * scaleX
    imageObjectFilterRender.setScale(scaleX, scaleY)
    // Position the overlay (tweak as needed).
    imageObjectFilterRender.setPosition(10f, 10f)
}

/**
 * A simple "animation" that reveals rows from top to bottom.
 * We'll show 1 row, then 2 rows, then 3 rows... up to the total (11).
 * You can adjust the speed/delay as you like.
 */
fun animateTeamPlayersOverlay(
    context: Context,
    teamAName: String,
    teamBName: String,
    teamAPlayers: List<PlayerEntry>,
    teamBPlayers: List<PlayerEntry>,
    imageObjectFilterRender: ImageObjectFilterRender
) {
    val handler = Handler(Looper.getMainLooper())
    val maxRows = 11  // or however many you want to reveal
    var currentRows = 0

    fun revealNext() {
        currentRows++
        updateTeamPlayersOverlay(
            context,
            teamAName,
            teamBName,
            teamAPlayers,
            teamBPlayers,
            currentRows,
            imageObjectFilterRender
        )
        if (currentRows < maxRows) {
            handler.postDelayed({ revealNext() }, 500) // 0.5 second delay between rows
        }
    }

    // Start from 0 rows visible:
    updateTeamPlayersOverlay(context, teamAName, teamBName, teamAPlayers, teamBPlayers, 0, imageObjectFilterRender)
    // Then reveal row by row
    handler.postDelayed({ revealNext() }, 500)
}

/**
 * Called to start the players composition overlay. If you want to animate the rows from top
 * to bottom, call animateTeamPlayersOverlay. If you prefer to show all at once, call
 * updateTeamPlayersOverlay with rowsVisible = 11 (or the total number).
 */
fun drawTeamPlayersOverlay(
    context: Context,
    teamAName: String,
    teamBName: String,
    teamAPlayers: List<PlayerEntry>,
    teamBPlayers: List<PlayerEntry>,
    imageObjectFilterRender: ImageObjectFilterRender,
    isOnPreview: Boolean
) {
    if (isOnPreview) {
        // Option 1: Animate row by row
        animateTeamPlayersOverlay(context, teamAName, teamBName, teamAPlayers, teamBPlayers, imageObjectFilterRender)

        // Option 2 (no animation, show everything):
        // updateTeamPlayersOverlay(context, teamAName, teamBName, teamAPlayers, teamBPlayers, 11, imageObjectFilterRender)
    }
}
