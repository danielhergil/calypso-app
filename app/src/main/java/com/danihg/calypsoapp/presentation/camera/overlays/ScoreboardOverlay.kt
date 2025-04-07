package com.danihg.calypsoapp.presentation.camera.overlays

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.danihg.calypsoapp.overlays.drawOverlay
import com.danihg.calypsoapp.utils.ScoreboardActionButtons
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.library.generic.GenericStream

@Composable
fun ScoreboardOverlay(
    visible: Boolean,
    genericStream: GenericStream,
    leftLogo: Bitmap,
    rightLogo: Bitmap,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    leftTeamAlias: String,
    rightTeamAlias: String,
    leftTeamColor: String,
    rightTeamColor: String,
    backgroundColor: Int,
    imageObjectFilterRender: ImageObjectFilterRender,
    onLeftIncrement: () -> Unit,
    onRightIncrement: () -> Unit,
    onLeftDecrement: () -> Unit,
    onRightDecrement: () -> Unit
) {

    Log.d("ScoreboardOverlay", "Visible: $visible")
    Log.d("ScoreboardOverlay", "isOnPreview: ${genericStream.isOnPreview}")

    // Add or remove the overlay filter based on visibility.
    LaunchedEffect(visible) {
        if (visible) {
            genericStream.getGlInterface().clearFilters()
            genericStream.getGlInterface().addFilter(imageObjectFilterRender)
        } else {
            genericStream.getGlInterface().removeFilter(imageObjectFilterRender)
        }
    }
    if (visible) {
        drawOverlay(
            context = LocalContext.current,
            leftLogoBitmap = leftLogo,
            rightLogoBitmap = rightLogo,
            leftTeamGoals = leftTeamGoals,
            rightTeamGoals = rightTeamGoals,
            leftTeamAlias = leftTeamAlias,
            rightTeamAlias = rightTeamAlias,
            leftTeamColor = leftTeamColor,
            rightTeamColor = rightTeamColor,
            backgroundColor = backgroundColor,
            imageObjectFilterRender = imageObjectFilterRender,
            isOnPreview = genericStream.isOnPreview
        )
        ScoreboardActionButtons(
            onLeftButtonClick = onLeftIncrement,
            onRightButtonClick = onRightIncrement,
            onLeftDecrement = onLeftDecrement,
            onRightDecrement = onRightDecrement
        )
    }
}
