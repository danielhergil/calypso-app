package com.danihg.calypsoapp.presentation.camera.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// File: UiComponents.kt
@Composable
fun BottomSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .wrapContentWidth()
                .padding(bottom = bottomPadding),
            snackbar = { data ->
                Snackbar(
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .wrapContentWidth(),
                    containerColor = Color.DarkGray,
                    contentColor = Color.White,
                    shape = SnackbarDefaults.shape
                ) {
                    Text(
                        text = data.visuals.message,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }
}
