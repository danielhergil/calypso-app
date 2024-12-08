package com.danihg.calypsoapp.presentation.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.PowerManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.GreyTransparent
import com.pedro.common.ConnectChecker
import com.pedro.library.generic.GenericStream


@Composable
fun rememberToast(): (String) -> Unit {

    val context = LocalContext.current
    return remember { { message: String ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        // Lock orientation when the composable is active
        activity?.requestedOrientation = orientation

        onDispose {
            // Restore default orientation when leaving the composable
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

@SuppressLint("Wakelock", "WakelockTimeout")
@Composable
fun PreventScreenLock() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CameraScreen::WakeLock"
        )
        wakeLock.acquire()

        onDispose {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CameraScreen () {

    // Lock the screen to portrait mode
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    PreventScreenLock()

    val showToast = rememberToast()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current // To detect orientation changes
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val genericStream = remember {
        GenericStream(context, object : ConnectChecker {
            override fun onConnectionStarted(url: String) {}
            override fun onConnectionSuccess() {
                showToast("Connected")
            }

            override fun onConnectionFailed(reason: String) {
                showToast("Connection failed: $reason")
            }

            override fun onNewBitrate(bitrate: Long) {}
            override fun onDisconnect() {
                showToast("Disconnected")
            }

            override fun onAuthError() {
                showToast("Authentication error")
            }

            override fun onAuthSuccess() {
                showToast("Authentication success")
            }
        }).apply {
            prepareVideo(1920, 1080, 3000 * 1000, 30)
            prepareAudio(32000, true, 128 * 1000)
            getGlInterface().autoHandleOrientation = true
        }
    }

    var isStreaming by remember { mutableStateOf(false) }
    var isSettingsMenuVisible by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFPS by remember { mutableIntStateOf(30) }
    var selectedBitrate by remember { mutableIntStateOf(3000 * 1000) }
    var rtmpEndpoint by remember { mutableStateOf(TextFieldValue("rtmp://a.rtmp.youtube.com/live2/")) }

    Scaffold(
        modifier = Modifier.fillMaxSize(), // Fills the entire screen
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Removes insets added by Scaffold
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                // SurfaceView for camera preview

                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    if (!genericStream.isOnPreview) {
                                        genericStream.startPreview(this@apply)
                                    }
                                }
                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                    genericStream.getGlInterface().setPreviewResolution(width, height)
                                }
                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    if (genericStream.isOnPreview) {
                                        genericStream.stopPreview()
                                    }
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize() // Fills the screen
                )

                // Black box at the bottom with a red circular button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // Fixed height for the black box
                        .align(Alignment.BottomCenter)
                        .background(Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.Center, // Centers everything horizontally
                        verticalAlignment = Alignment.CenterVertically // Centers everything vertically
                    ) {
                        // Left button (optional)
                        AuxButton(
                            modifier = Modifier
                                .size(50.dp)
                                .zIndex(2f),
                            painter = painterResource(id = R.drawable.ic_rocket),
                            onClick = {

                            }
                        )

                        Spacer(modifier = Modifier.width(60.dp))

                        // Red button (Center Button)
                        Button(
                            onClick = {
                                if (isStreaming) {
                                    genericStream.stopStream()
                                    isStreaming = false
                                } else {
                                    genericStream.startStream("rtmp://a.rtmp.youtube.com/live2/j2sh-690b-fg9y-2fah-7444")
                                    isStreaming = true
                                }
                            },
                            modifier = Modifier
                                .size(70.dp)
                                .border(3.dp, Color.White, CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            shape = CircleShape
                        ) {}

                        Spacer(modifier = Modifier.width(60.dp))

                        // Right button (AuxButton with Icon)
                        AuxButton(
                            modifier = Modifier
                                .size(50.dp)
                                .zIndex(2f),
                            painter = painterResource(id = R.drawable.ic_settings),
                            onClick = {
                                isSettingsMenuVisible = !isSettingsMenuVisible
                            }
                        )

                    }
                }

                // Fullscreen Settings Menu
                AnimatedVisibility(
                    visible = isSettingsMenuVisible,
                    enter = androidx.compose.animation.fadeIn(tween(500)) + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                    exit = androidx.compose.animation.fadeOut(tween(500)) + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Resolution Section
                            Text("Resolution")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedResolution == "1080p",
                                    onClick = { selectedResolution = "1080p" }
                                )
                                Text("1080p")
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(
                                    selected = selectedResolution == "720p",
                                    onClick = { selectedResolution = "720p" }
                                )
                                Text("720p")
                            }

                            // FPS Section
                            Text("FPS")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedFPS == 30,
                                    onClick = { selectedFPS = 30 }
                                )
                                Text("30 FPS")
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(
                                    selected = selectedFPS == 60,
                                    onClick = { selectedFPS = 60 }
                                )
                                Text("60 FPS")
                            }

                            // Bitrate Section
                            Text("Bitrate")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedBitrate == 3000 * 1000,
                                    onClick = { selectedBitrate = 3000 * 1000 }
                                )
                                Text("High (1080p)")
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(
                                    selected = selectedBitrate == 1500 * 1000,
                                    onClick = { selectedBitrate = 1500 * 1000 }
                                )
                                Text("High (720p)")
                            }

                            // RTMP Endpoint Section
                            Text("RTMP Endpoint")
                            BasicTextField(
                                value = rtmpEndpoint,
                                onValueChange = { rtmpEndpoint = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .background(Color.White)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}


@Composable
fun AuxButton(modifier: Modifier = Modifier, painter: Painter, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(GreyTransparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter,
            contentDescription = "Right Button Icon",
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}