package com.danihg.calypsoapp.presentation.camera

import android.content.res.Configuration
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pedro.common.ConnectChecker
import com.pedro.library.generic.GenericStream


@Composable
fun rememberToast(): (String) -> Unit {

    val context = LocalContext.current
    return remember { { message: String ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } }
}

@Preview
@Composable
fun CameraScreen () {

    val showToast = rememberToast()
    val context = LocalContext.current
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

                // Button to start/stop streaming
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
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(if (isStreaming) "Stop Streaming" else "Start Streaming")
                }
            }
        }
    )
}