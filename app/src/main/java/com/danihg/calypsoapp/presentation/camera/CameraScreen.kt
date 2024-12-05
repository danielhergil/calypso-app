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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.Black
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.ShapeButton
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

@Preview
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

    Scaffold(
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        .height(120.dp) // Fixed height for the black box
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .background(androidx.compose.ui.graphics.Color.Black)
                ) {
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
                            .size(60.dp)
                            .align(androidx.compose.ui.Alignment.Center)
                            .border(3.dp, Color.White, CircleShape),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        shape = CircleShape
                    ) {}
                    // Left button
                    Button(
                        onClick = { /* Handle left button click */ },
                        modifier = Modifier
                            .size(40.dp)
                            .align(androidx.compose.ui.Alignment.Center)
                            .offset(x = (-120).dp), // Position to the left of the red button
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0x66FFFFFF) // White-grey with transparency
                        ),
                        shape = CircleShape
                    ) {}

                    // Right button
                    Button(
                        onClick = { /* Handle right button click */ },
                        modifier = Modifier
                            .size(40.dp)
                            .align(androidx.compose.ui.Alignment.Center)
                            .offset(x = 120.dp), // Position to the right of the red button
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0x66FFFFFF) // White-grey with transparency
                        ),
                        shape = CircleShape
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Right Button Icon",
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(30.dp) // Adjust size as needed
                        )
                    }
                }
            }
        }
    )
}