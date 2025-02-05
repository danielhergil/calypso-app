// CameraScreen.kt
package com.danihg.calypsoapp.presentation.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.overlays.drawOverlay
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.utils.AuxButton
import com.danihg.calypsoapp.utils.ModernDropdown
import com.danihg.calypsoapp.utils.PreventScreenLock
import com.danihg.calypsoapp.utils.ScoreboardActionButtons
import com.danihg.calypsoapp.utils.SectionSubtitle
import com.danihg.calypsoapp.utils.getAvailableAudioCodecs
import com.danihg.calypsoapp.utils.getAvailableVideoCodecs
import com.danihg.calypsoapp.utils.getSupportedAudioCodecs
import com.danihg.calypsoapp.utils.getSupportedVideoCodecs
import com.danihg.calypsoapp.utils.rememberToast
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.extrasources.CameraUvcSource
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.delay

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    var isOrientationSet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        delay(300)
        isOrientationSet = true
        delay(100)
        showContent = true
    }

    DisposableEffect(Unit) {
        onDispose {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (isOrientationSet) {
                CameraScreenContent()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CameraScreenContent() {
    val context = LocalContext.current
    PreventScreenLock()
    val showToast = rememberToast()

    // Retrieve video/audio settings from SharedPreferences
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
    var videoWidth by remember { mutableIntStateOf(sharedPreferences.getInt("videoWidth", 1920)) }
    var videoHeight by remember { mutableIntStateOf(sharedPreferences.getInt("videoHeight", 1080)) }
    val videoBitrate = sharedPreferences.getInt("videoBitrate", 5000 * 1000)
    val videoFPS = sharedPreferences.getInt("videoFPS", 30)
    val audioSampleRate = sharedPreferences.getInt("audioSampleRate", 32000)
    val audioIsStereo = sharedPreferences.getBoolean("audioIsStereo", true)
    val audioBitrate = sharedPreferences.getInt("audioBitrate", 128 * 1000)

    var selectedCameraSource by remember { mutableStateOf("Device Camera") }
    var selectedAudioSource by remember { mutableStateOf("Device Microphone") }
    var selectedVideoEncoder by remember { mutableStateOf("H.264") }
    var selectedAudioEncoder by remember { mutableStateOf("AAC") }
    var selectedFPS by remember { mutableStateOf("30") }
    var selectedResolution by remember { mutableStateOf("1080p") }

    val enumAudioMapping = mapOf(
        "AAC" to AudioCodec.AAC,
        "OPUS" to AudioCodec.OPUS,
        "G711" to AudioCodec.G711
    )

    val enumVideoMapping = mapOf(
        "H264" to VideoCodec.H264,
        "H265" to VideoCodec.H265,
        "AV1" to VideoCodec.AV1
    )

    var isStreaming by rememberSaveable { mutableStateOf(false) }
    var isSettingsMenuVisible by rememberSaveable { mutableStateOf(false) }

    // Setup notification channel for foreground service
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "CameraStreamChannel",
            "Camera Stream",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    // Scoreboard variables
    val options = BitmapFactory.Options()
    options.inScaled = false

    val leftLogoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.rivas_50, options)
    val rightLogoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.alcorcon_50, options)
    var selectedTeam1 by remember { mutableStateOf("Rivas") }
    var selectedTeam2 by remember { mutableStateOf("Rivas") }
    val selectedBackgroundColor: Int = Color.Transparent.toArgb()
    var leftTeamGoals = 0
    var rightTeamGoals = 0
    val imageObjectFilterRender = ImageObjectFilterRender()

    var overlayDrawn by remember { mutableStateOf(false) }
    var showApplyButton by rememberSaveable { mutableStateOf(false) }
    var showScoreboardOverlay by rememberSaveable { mutableStateOf(false) }
    var selectedCamera by rememberSaveable { mutableStateOf("Camera2") }

    val availableVideoCodecs = remember { getAvailableVideoCodecs() }
    val availableAudioCodecs = remember { getAvailableAudioCodecs() }

    val surfaceViewRef = remember { mutableStateOf<SurfaceView?>(null) }
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
        }
    }

    fun startForegroundService() {
        val notification = NotificationCompat.Builder(context, "CameraStreamChannel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Streaming Active")
            .setContentText("Streaming in progress")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1001, notification)

        if (!isStreaming) {
            genericStream.startStream("rtmp://192.168.1.109:1935/live/streamname")
            isStreaming = true
        }
    }

    fun stopForegroundService() {
        if (isStreaming) {
            genericStream.stopStream()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1001)
            isStreaming = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            surfaceViewRef.value = this
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
                    modifier = Modifier.fillMaxSize()
                )

                if (showScoreboardOverlay && genericStream.isOnPreview && !showApplyButton) {
                    genericStream.getGlInterface().addFilter(imageObjectFilterRender)
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
                    ScoreboardActionButtons(
                        onLeftButtonClick = {
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
                        },
                        onRightButtonClick = {
                            rightTeamGoals++
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
                } else {
                    if (!showScoreboardOverlay && !showApplyButton) {
                        genericStream.getGlInterface().removeFilter(imageObjectFilterRender)
                        leftTeamGoals = 0
                        rightTeamGoals = 0
                    }
                }

                if (selectedCamera == "Camera2" && !showApplyButton) {
                    genericStream.changeVideoSource(Camera2Source(context))
                } else if (selectedCamera == "CameraX" && !showApplyButton) {
                    genericStream.changeVideoSource(Camera1Source(context))
                } else if (selectedCamera == "USBCamera" && !showApplyButton) {
                    genericStream.changeVideoSource(CameraUvcSource())
                }

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
                        AuxButton(
                            modifier = Modifier
                                .size(50.dp)
                                .zIndex(2f),
                            painter = painterResource(id = R.drawable.ic_rocket),
                            onClick = { showApplyButton = !showApplyButton }
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Button(
                            onClick = {
                                if (isStreaming) {
                                    stopForegroundService()
                                } else {
                                    startForegroundService()
                                }
                            },
                            modifier = Modifier
                                .size(70.dp)
                                .border(3.dp, Color.White, CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = CircleShape
                        ) {}

                        Spacer(modifier = Modifier.height(5.dp))

                        AuxButton(
                            modifier = Modifier
                                .size(50.dp)
                                .zIndex(2f),
                            painter = painterResource(id = R.drawable.ic_settings),
                            onClick = { isSettingsMenuVisible = !isSettingsMenuVisible }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isSettingsMenuVisible,
                    enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels.dp
                    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels.dp

                    // Calculate the width of the camera preview (assuming 16:9 aspect ratio)
                    val cameraPreviewHeight = screenHeight // Full height in landscape
                    val cameraPreviewWidth = cameraPreviewHeight * (16f / 9f) // 16:9 aspect ratio

                    // Calculate the horizontal padding to center the settings menu
                    val horizontalPadding = (screenWidth - cameraPreviewWidth) / 2

                    Box(
                        modifier = Modifier
                            .fillMaxSize() // Fill the entire screen
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cameraPreviewWidth) // Match the width of the camera preview
                                .fillMaxHeight() // Take up full height
                                .padding(horizontal = horizontalPadding) // Add padding to match the camera preview
                                .background(Color.Black.copy(alpha = 0.95f))
                                .align(Alignment.BottomCenter) // Align at the bottom
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()) // Make the content scrollable
                            ) {
                                // Close Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { isSettingsMenuVisible = false } // Close the settings menu
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close), // Replace with your close icon
                                            contentDescription = "Close",
                                            tint = Color.White
                                        )
                                    }
                                }

                                // Camera Settings Section
                                Text(
                                    text = "Camera Settings",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)

                                // Camera Source Dropdown
                                SectionSubtitle("Camera Source")
                                ModernDropdown(
                                    items = listOf("Device Camera", "USB Camera"),
                                    selectedValue = selectedCameraSource,
                                    displayMapper = { it },
                                    onValueChange = { selectedCameraSource = it }
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Streaming Settings Section
                                Text(
                                    text = "Streaming Settings",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)

                                // Video Encoder Dropdown
                                SectionSubtitle("Video Encoder")
                                ModernDropdown(
                                    items = availableVideoCodecs,
                                    selectedValue = selectedVideoEncoder,
                                    displayMapper = { it },
                                    onValueChange = { selectedVideoEncoder = it }
                                )

                                // Audio Encoder Dropdown
                                SectionSubtitle("Audio Encoder")
                                ModernDropdown(
                                    items = availableAudioCodecs,
                                    selectedValue = selectedAudioEncoder,
                                    displayMapper = { it },
                                    onValueChange = { selectedAudioEncoder = it }
                                )

                                // Stream FPS Dropdown
                                SectionSubtitle("Stream FPS")
                                ModernDropdown(
                                    items = if (selectedCameraSource == "USB Camera") listOf("30", "60") else listOf("30"),
                                    selectedValue = selectedFPS,
                                    displayMapper = { it },
                                    onValueChange = { selectedFPS = it }
                                )

                                // Resolution Dropdown
                                SectionSubtitle("Resolution")
                                ModernDropdown(
                                    items = listOf("1080p", "720p"),
                                    selectedValue = selectedResolution,
                                    displayMapper = { it },
                                    onValueChange = { selectedResolution = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Apply Button
                                Button(
                                    onClick = {
                                        // Save settings and apply changes
                                        genericStream.stopPreview()
                                        when (selectedResolution) {
                                            "1080p" -> {
                                                videoWidth = 1920
                                                videoHeight = 1080
                                            }
                                            "720p" -> {
                                                videoWidth = 1280
                                                videoHeight = 720
                                            }
                                        }
                                        sharedPreferences.edit().apply {
                                            putInt("videoWidth", videoWidth)
                                            putInt("videoHeight", videoHeight)
                                            apply()
                                        }
                                        genericStream.prepareVideo(videoWidth, videoHeight, videoBitrate, videoFPS)
                                        genericStream.prepareAudio(audioSampleRate, audioIsStereo, audioBitrate)
                                        enumAudioMapping[selectedAudioEncoder]?.let { audioCodec ->
                                            genericStream.setAudioCodec(audioCodec)
                                            Log.d("CodecCheck", "Set audio codec to: $audioCodec")
                                        }
                                        enumVideoMapping[selectedVideoEncoder]?.let { videoCodec ->
                                            genericStream.setVideoCodec(videoCodec)
                                            Log.d("CodecCheck", "Set audio codec to: $videoCodec")
                                        }
                                        // Restart the preview using the stored SurfaceView reference
                                        surfaceViewRef.value?.let { surfaceView ->
                                            genericStream.startPreview(surfaceView)
                                        } ?: run {
                                            // Optionally handle the error if the SurfaceView is not available
                                            showToast("Error: SurfaceView not available")
                                        }
                                        if(selectedCameraSource == "USB Camera") {
                                            genericStream.changeVideoSource(CameraUvcSource())
                                        } else {
                                            genericStream.changeVideoSource(Camera2Source(context))
                                        }
                                        isSettingsMenuVisible = false // Close the settings menu after applying
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CalypsoRed,
                                        contentColor = Color.White
                                    ),
                                    shape = CircleShape
                                ) {
                                    Text("Apply", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showApplyButton,
                    enter = fadeIn(tween(500)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut(tween(500)) + slideOutVertically(targetOffsetY = { it })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.Gray.copy(alpha = 0.2f))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.Top,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier.padding(start = 5.dp),
                                                text = "Scoreboard",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Switch(
                                                checked = showScoreboardOverlay,
                                                onCheckedChange = { showScoreboardOverlay = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = CalypsoRed,
                                                    uncheckedThumbColor = Color.Gray
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(15.dp))

                                        SectionSubtitle("Select Team 1")
                                        ModernDropdown(
                                            items = listOf("Rivas", "Alcorcón"),
                                            selectedValue = selectedTeam1,
                                            displayMapper = { it },
                                            onValueChange = { selectedTeam1 = it }
                                        )
                                        Spacer(modifier = Modifier.height(25.dp))
                                        SectionSubtitle("Select Team 2")
                                        ModernDropdown(
                                            items = listOf("Rivas", "Alcorcón"),
                                            selectedValue = selectedTeam2,
                                            displayMapper = { it },
                                            onValueChange = { selectedTeam2 = it }
                                        )
                                        Spacer(modifier = Modifier.height(25.dp))
                                        Button(
                                            onClick = {},
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(4.dp)
                                                .height(40.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CalypsoRed,
                                                contentColor = Color.White
                                            ),
                                            shape = CircleShape
                                        ) {
                                            Text("Apply", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SectionSubtitle("Camera Selection")
                                    ModernDropdown(
                                        items = listOf("Camera2", "Camera1", "USBCamera"),
                                        selectedValue = selectedCamera,
                                        displayMapper = { it },
                                        onValueChange = { selectedCamera = it }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.Gray.copy(alpha = 0.4f))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Section 3",
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
