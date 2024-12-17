package com.danihg.calypsoapp.presentation.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.PowerManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.GreyTransparent
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.library.generic.GenericStream
import com.danihg.calypsoapp.overlays.drawOverlay


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
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    PreventScreenLock()

    val showToast = rememberToast()
    val context = LocalContext.current

    // Retrieve video/audio settings from SharedPreferences with default values
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
    val videoWidth = sharedPreferences.getInt("videoWidth", 1920)
    val videoHeight = sharedPreferences.getInt("videoHeight", 1080)
    val videoBitrate = sharedPreferences.getInt("videoBitrate", 5000 * 1000)
    val videoFPS = sharedPreferences.getInt("videoFPS", 30)
    val audioSampleRate = sharedPreferences.getInt("audioSampleRate", 32000)
    val audioIsStereo = sharedPreferences.getBoolean("audioIsStereo", true)
    val audioBitrate = sharedPreferences.getInt("audioBitrate", 128 * 1000)


    // Scoreboard variables
    val options = BitmapFactory.Options()
    options.inScaled = false

    val leftLogoBitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.rivas_50,
        options
    )
    val rightLogoBitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.alcorcon_50,
        options
    )
    val selectedBackgroundColor: Int = Color.Transparent.toArgb()
    var leftTeamGoals = 0
    var rightTeamGoals = 0
    val imageObjectFilterRender = ImageObjectFilterRender()

    var overlayDrawn by remember { mutableStateOf(false) }

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
            prepareVideo(videoWidth, videoHeight, videoBitrate, videoFPS)
            prepareAudio(audioSampleRate, audioIsStereo, audioBitrate)
            getGlInterface().autoHandleOrientation = true
//            setOrientation(180)
        }
    }

    var isStreaming by remember { mutableStateOf(false) }
    var isSettingsMenuVisible by remember { mutableStateOf(false) }

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

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(160.dp)
                        .padding(end = 80.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top button (Left Button in previous version)
                        AuxButton(
                            modifier = Modifier
                                .size(50.dp)
                                .zIndex(2f),
                            painter = painterResource(id = R.drawable.ic_rocket),
                            onClick = {
                                if (!overlayDrawn) {
                                    genericStream.getGlInterface()
                                        .addFilter(imageObjectFilterRender)
                                    drawOverlay(
                                        context = context,
                                        leftLogoBitmap = leftLogoBitmap,
                                        rightLogoBitmap = rightLogoBitmap,
                                        leftTeamGoals = leftTeamGoals,
                                        rightTeamGoals = rightTeamGoals,
                                        backgroundColor = selectedBackgroundColor,
                                        imageObjectFilterRender = imageObjectFilterRender,
                                        isOnPreview = genericStream.isOnPreview
                                    )
                                    overlayDrawn = true
                                }
                                else {
                                    genericStream.getGlInterface().removeFilter(imageObjectFilterRender)
                                    leftTeamGoals = 0
                                    rightTeamGoals = 0
                                    overlayDrawn = false
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // Red circular button (Center Button)
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

                        Spacer(modifier = Modifier.height(5.dp))

                        // Bottom button (Right Button in previous version)
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
                    enter = fadeIn(tween(500)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut(tween(500)) + slideOutVertically(targetOffsetY = { it })
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
                        ){
                            AuxButton(
                                modifier = Modifier
                                    .size(50.dp)
                                    .zIndex(2f),
                                painter = painterResource(id = R.drawable.ic_settings),
                                onClick = {
                                    leftTeamGoals++
                                    drawOverlay(
                                        context = context,
                                        leftLogoBitmap = leftLogoBitmap,
                                        rightLogoBitmap = rightLogoBitmap,
                                        leftTeamGoals = leftTeamGoals,
                                        rightTeamGoals = rightTeamGoals,
                                        backgroundColor = selectedBackgroundColor,
                                        imageObjectFilterRender = imageObjectFilterRender,
                                        isOnPreview = genericStream.isOnPreview
                                    )
                                }
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

