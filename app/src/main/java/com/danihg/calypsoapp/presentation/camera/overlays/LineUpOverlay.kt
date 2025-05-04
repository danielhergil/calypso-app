package com.danihg.calypsoapp.presentation.camera.overlays

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.danihg.calypsoapp.overlays.PlayerEntry
import com.danihg.calypsoapp.overlays.drawTeamPlayersOverlay
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.delay

@Composable
fun TeamPlayersOverlay(
    visible: Boolean,
    genericStream: GenericStream,
    screenWidth: Int,
    screenHeight: Int,
    team1Name: String,
    team2Name: String,
    team1Players: List<PlayerEntry>,
    team2Players: List<PlayerEntry>,
    leftLogo: Bitmap,
    rightLogo: Bitmap,
    selectedTeamsOverlayDuration: String,
    lineUpFilter: ImageObjectFilterRender,
    context: Context,
    onLineUpFinished: () -> Unit
){
    // Add or remove the overlay filter based on visibility.
    Log.d("TeamPlayersOverlay", "Visibility: $visible")
    Log.d("TeamPlayersOverlay", "isOnPreview: ${genericStream.isOnPreview}")
    LaunchedEffect(visible) {
        if (visible && genericStream.isOnPreview) {
//            genericStream.getGlInterface().clearFilters()
            delay(500)
            genericStream.getGlInterface().addFilter(lineUpFilter)
        } else {
            genericStream.getGlInterface().removeFilter(lineUpFilter)
        }

        if (visible && genericStream.isOnPreview) {
            drawTeamPlayersOverlay(
                context = context,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                leftLogoBitmap = leftLogo,
                rightLogoBitmap = rightLogo,
                leftTeamName = team1Name,
                rightTeamName = team2Name,
                leftTeamPlayers = team1Players,
                rightTeamPlayers = team2Players,
                imageObjectFilterRender = lineUpFilter,
                isOnPreview = genericStream.isOnPreview
            )

            val teamPlayersOverlayDelay = selectedTeamsOverlayDuration.split("s").first()
            delay(teamPlayersOverlayDelay.toLong() * 1000)
            genericStream.getGlInterface().removeFilter(lineUpFilter)
            onLineUpFinished()
        }
    }
}