package com.danihg.calypsoapp.overlays

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.danihg.calypsoapp.R
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender

fun createScoreboardBitmap(
    context: Context,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    backgroundColor: Int
): Bitmap {
    val logoSize = 80
    val logoPadding = 20 // Padding between logos and scoreboard

    // Make the scoreboard box narrower by reducing its width.
    val scoreboardWidth = 300 // Was 500
    val scoreboardHeight = 100

    // Overall bitmap width accounts for logos and padding.
    val width = scoreboardWidth + 2 * logoSize + 4 * logoPadding
    val height = scoreboardHeight

    // The scoreboard box starts after the left logo plus some extra padding.
    val scoreboardLeft = logoSize + 2 * logoPadding.toFloat()
    val scoreboardRight = scoreboardLeft + scoreboardWidth

    val scoreboardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(scoreboardBitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        typeface = ResourcesCompat.getFont(context, R.font.montserrat_medium)
    }

    // Draw scoreboard box background.
    paint.color = Color(0xFF222222).toArgb()
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(scoreboardLeft, 0f, scoreboardRight, height.toFloat(), 20f, 20f, paint)

    // Draw rounded border for the scoreboard.
    paint.color = Color.White.toArgb()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 8f
    canvas.drawRoundRect(scoreboardLeft + 4f, 4f, scoreboardRight - 4f, height - 4f, 20f, 20f, paint)
    paint.style = Paint.Style.FILL

    // Set up text for scores with a reduced size.
    paint.color = Color.White.toArgb()
    paint.textSize = 50f  // Reduced from 80f
    paint.textAlign = Paint.Align.CENTER  // Center the text horizontally.
    paint.typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)

    // Calculate the vertical center so that text is vertically centered.
    val textY = height / 2f - (paint.descent() + paint.ascent()) / 2f

    // Compute the x positions for the left score, dash, and right score.
    val leftScoreX = scoreboardLeft + scoreboardWidth / 4f
    val dashX = scoreboardLeft + scoreboardWidth / 2f
    val rightScoreX = scoreboardLeft + 3 * scoreboardWidth / 4f

    canvas.drawText(leftTeamGoals.toString(), leftScoreX, textY, paint)
    canvas.drawText("-", dashX, textY, paint)
    canvas.drawText(rightTeamGoals.toString(), rightScoreX, textY, paint)

    // Draw team logos on the sides (logo sizes remain unchanged).
    leftLogoBitmap?.let {
        val destRect1 = Rect(
            logoPadding,
            (height - logoSize) / 2,
            logoPadding + logoSize,
            (height + logoSize) / 2
        )
        canvas.drawBitmap(it, null, destRect1, null)
    }

    rightLogoBitmap?.let {
        val destRect2 = Rect(
            width - logoPadding - logoSize,
            (height - logoSize) / 2,
            width - logoPadding,
            (height + logoSize) / 2
        )
        canvas.drawBitmap(it, null, destRect2, null)
    }

    return scoreboardBitmap
}

fun updateOverlay(
    context: Context,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    backgroundColor: Int,
    imageObjectFilterRender: ImageObjectFilterRender
) {
    Handler(Looper.getMainLooper()).post {
        val scoreboardBitmap: Bitmap = createScoreboardBitmap(
            context,
            leftLogoBitmap,
            rightLogoBitmap,
            leftTeamGoals,
            rightTeamGoals,
            backgroundColor
        )
        imageObjectFilterRender.setImage(scoreboardBitmap)

        val bitmapWidth = scoreboardBitmap.width
        val bitmapHeight = scoreboardBitmap.height
        val scaleX = 33.3f
        val scaleY = bitmapHeight.toFloat() / bitmapWidth.toFloat() * scaleX

        imageObjectFilterRender.setScale(scaleX, scaleY)

        // Calculate new position for left-right alignment with padding.
        val paddingLeft = 3f
        val paddingTop = 3f
        imageObjectFilterRender.setPosition(paddingLeft, paddingTop)
    }
}

fun drawOverlay(
    context: Context,
    leftLogoBitmap: Bitmap?,
    rightLogoBitmap: Bitmap?,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    backgroundColor: Int,
    imageObjectFilterRender: ImageObjectFilterRender,
    isOnPreview: Boolean
) {
    if (isOnPreview) {
        updateOverlay(context, leftLogoBitmap, rightLogoBitmap, leftTeamGoals, rightTeamGoals, backgroundColor, imageObjectFilterRender)
    }
}
