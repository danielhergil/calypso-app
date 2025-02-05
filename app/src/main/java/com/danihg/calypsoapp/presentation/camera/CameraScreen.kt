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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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
import com.danihg.calypsoapp.utils.PathUtils
import com.danihg.calypsoapp.utils.PreventScreenLock
import com.danihg.calypsoapp.utils.ScoreboardActionButtons
import com.danihg.calypsoapp.utils.SectionSubtitle
import com.danihg.calypsoapp.utils.getAvailableAudioCodecs
import com.danihg.calypsoapp.utils.getAvailableVideoCodecs
import com.danihg.calypsoapp.utils.rememberToast
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import java.util.Date
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.extrasources.CameraUvcSource
import com.pedro.library.base.recording.RecordController
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    var isOrientationSet by remember { mutableStateOf(false) }

    // Set the device orientation to landscape on launch.
    LaunchedEffect(Unit) {
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        delay(300)
        isOrientationSet = true
        delay(100)
        showContent = true
    }

    // Reset orientation when leaving the screen.
    DisposableEffect(Unit) {
        onDispose {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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

    // Retrieve streaming settings from SharedPreferences.
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
    var videoWidth by remember { mutableIntStateOf(sharedPreferences.getInt("videoWidth", 1920)) }
    var videoHeight by remember { mutableIntStateOf(sharedPreferences.getInt("videoHeight", 1080)) }
    val videoBitrate = sharedPreferences.getInt("videoBitrate", 5000 * 1000)
    val videoFPS = sharedPreferences.getInt("videoFPS", 30)
    val audioSampleRate = sharedPreferences.getInt("audioSampleRate", 32000)
    val audioIsStereo = sharedPreferences.getBoolean("audioIsStereo", true)
    val audioBitrate = sharedPreferences.getInt("audioBitrate", 128 * 1000)

    // Streaming and camera settings.
    var selectedCameraSource by remember { mutableStateOf("Device Camera") }
    var selectedVideoEncoder by remember { mutableStateOf("H264") }
    var selectedAudioEncoder by remember { mutableStateOf("AAC") }
    var selectedFPS by remember { mutableStateOf("30") }
    var selectedResolution by remember { mutableStateOf("1080p") }

    // Map string values to codec objects.
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

    // UI state variables.
    var isStreaming by rememberSaveable { mutableStateOf(false) }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var isSettingsMenuVisible by rememberSaveable { mutableStateOf(false) }
    var showApplyButton by rememberSaveable { mutableStateOf(false) }
    var showScoreboardOverlay by rememberSaveable { mutableStateOf(false) }
    var selectedCamera by rememberSaveable { mutableStateOf("Camera2") }

    // State for recording timer (in seconds).
    var streamingTimerSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(isStreaming) {
        if (isStreaming) {
            while (true) {
                delay(1000)
                streamingTimerSeconds++
            }
        } else {
            streamingTimerSeconds = 0L
        }
    }

    // Timer for recording (when record button is active)
    var recordingTimerSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(1000)
                recordingTimerSeconds++
            }
        } else {
            recordingTimerSeconds = 0L
        }
    }

    // Create notification channel for streaming.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "CameraStreamChannel",
            "Camera Stream",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    // Scoreboard settings.
    val options = BitmapFactory.Options().apply { inScaled = false }
    val leftLogoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.rivas_50, options)
    val rightLogoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.alcorcon_50, options)
    var selectedTeam1 by remember { mutableStateOf("Rivas") }
    var selectedTeam2 by remember { mutableStateOf("Rivas") }
    val selectedBackgroundColor = Color.Transparent.toArgb()
    var leftTeamGoals by remember { mutableStateOf(0) }
    var rightTeamGoals by remember { mutableStateOf(0) }
    val imageObjectFilterRender = remember { ImageObjectFilterRender() }

    // Reference for the SurfaceView.
    val surfaceViewRef = remember { mutableStateOf<SurfaceView?>(null) }

    // Initialize the streaming library.
    val genericStream = remember {
        GenericStream(context, object : ConnectChecker {
            override fun onConnectionStarted(url: String) {}
            override fun onConnectionSuccess() { showToast("Connected") }
            override fun onConnectionFailed(reason: String) { showToast("Connection failed: $reason") }
            override fun onNewBitrate(bitrate: Long) {}
            override fun onDisconnect() { showToast("Disconnected") }
            override fun onAuthError() { showToast("Authentication error") }
            override fun onAuthSuccess() { showToast("Authentication success") }
        }).apply {
            prepareVideo(videoWidth, videoHeight, videoBitrate, videoFPS)
            prepareAudio(audioSampleRate, audioIsStereo, audioBitrate)
            getGlInterface().autoHandleOrientation = true
        }
    }

    // Functions to start and stop streaming.
    fun startForegroundService() {
        val notification = NotificationCompat.Builder(context, "CameraStreamChannel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Streaming Active")
            .setContentText("Streaming in progress")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1001, notification)
        if (!isStreaming) {
            genericStream.startStream("rtmp://a.rtmp.youtube.com/live2/j2sh-690b-fg9y-2fah-7444")
            isStreaming = true
        }
    }

    fun stopForegroundService() {
        if (isStreaming) {
            genericStream.stopStream()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(1001)
            isStreaming = false
        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = {
            // Root container.
            Box(modifier = Modifier.fillMaxSize()) {

                // Camera preview.
                CameraPreview(
                    genericStream = genericStream,
                    surfaceViewRef = surfaceViewRef,
                    context = context
                )

                // Scoreboard overlay (if enabled).
                ScoreboardOverlay(
                    visible = showScoreboardOverlay && genericStream.isOnPreview && !showApplyButton,
                    genericStream = genericStream,
                    leftLogo = leftLogoBitmap,
                    rightLogo = rightLogoBitmap,
                    leftTeamGoals = leftTeamGoals,
                    rightTeamGoals = rightTeamGoals,
                    backgroundColor = selectedBackgroundColor,
                    imageObjectFilterRender = imageObjectFilterRender,
                    onLeftIncrement = {
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
                    onRightIncrement = {
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
                // Reset scoreboard if overlay is hidden.
                if (!showScoreboardOverlay && !showApplyButton) {
                    genericStream.getGlInterface().removeFilter(imageObjectFilterRender)
                    leftTeamGoals = 0
                    rightTeamGoals = 0
                }

                // Right-side action buttons.
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
                                if (isStreaming) stopForegroundService() else startForegroundService()
                            },
                            modifier = Modifier
                                .size(70.dp)
                                .border(3.dp, Color.White, CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = CircleShape
                        ) { /* Streaming button (big red circle) */ }
                        Spacer(modifier = Modifier.height(5.dp))
                        // Conditionally show the record video button when streaming,
                        // or show the settings button when not streaming.
                        if (isStreaming) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.5f))
                                    .clickable {
                                        if (!isRecording) {
                                            // Start recording: call recordVideoStreaming and update state.
                                            recordVideoStreaming(context, genericStream) { }
                                            isRecording = true
                                        } else {
                                            // Stop recording.
                                            recordVideoStreaming(context, genericStream) { }
                                            isRecording = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(id = if (isRecording) R.drawable.ic_stop else R.drawable.ic_record),
                                    contentDescription = "Button Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        } else {
                            AuxButton(
                                modifier = Modifier
                                    .size(50.dp)
                                    .zIndex(2f),
                                painter = painterResource(id = R.drawable.ic_settings),
                                onClick = { isSettingsMenuVisible = !isSettingsMenuVisible }
                            )
                        }
                    }
                }

                // Calculate dimensions for the settings menu.
                val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels.dp
                val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels.dp

                // Settings menu overlay.
                SettingsMenu(
                    visible = isSettingsMenuVisible,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    selectedCameraSource = selectedCameraSource,
                    onCameraSourceChange = { selectedCameraSource = it },
                    selectedVideoEncoder = selectedVideoEncoder,
                    onVideoEncoderChange = { selectedVideoEncoder = it },
                    selectedAudioEncoder = selectedAudioEncoder,
                    onAudioEncoderChange = { selectedAudioEncoder = it },
                    selectedFPS = selectedFPS,
                    onFPSChange = { selectedFPS = it },
                    selectedResolution = selectedResolution,
                    onResolutionChange = { selectedResolution = it },
                    availableVideoCodecs = getAvailableVideoCodecs(),
                    availableAudioCodecs = getAvailableAudioCodecs(),
                    onApply = {
                        // Apply settings: update resolution, prepare codecs, and restart preview.
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
                            Log.d("CodecCheck", "Set video codec to: $videoCodec")
                        }
                        // Restart preview using the stored SurfaceView.
                        surfaceViewRef.value?.let { surfaceView ->
                            genericStream.startPreview(surfaceView)
                        } ?: run {
                            showToast("Error: SurfaceView not available")
                        }
                        // Update video source based on camera source setting.
                        if (selectedCameraSource == "USB Camera") {
                            genericStream.changeVideoSource(CameraUvcSource())
                        } else {
                            genericStream.changeVideoSource(Camera2Source(context))
                        }
                        isSettingsMenuVisible = false
                    },
                    onClose = { isSettingsMenuVisible = false }
                )

                // Auxiliary overlay menu.
                OverlayMenu(
                    visible = showApplyButton,
                    selectedTeam1 = selectedTeam1,
                    onTeam1Change = { selectedTeam1 = it },
                    selectedTeam2 = selectedTeam2,
                    onTeam2Change = { selectedTeam2 = it },
                    showScoreboardOverlay = showScoreboardOverlay,
                    onToggleScoreboard = { showScoreboardOverlay = it },
                    selectedCamera = selectedCamera,
                    onCameraChange = { selectedCamera = it },
                    onApply = { showApplyButton = false }
                )

                // Place the recording timer at the very top with a high z-index.
                if (isStreaming) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(10f), // Ensures the timer is drawn above all other UI elements.
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Streaming timer
                            RecordingTimer(recordingSeconds = streamingTimerSeconds)
                            // If recording, show recording timer just below
                            if (isRecording) {
                                Spacer(modifier = Modifier.height(4.dp))
                                RecordingTimer(recordingSeconds = recordingTimerSeconds)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CameraPreview(
    genericStream: GenericStream,
    surfaceViewRef: MutableState<SurfaceView?>,
    context: Context
) {
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
}

@Composable
fun SettingsMenu(
    visible: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    selectedCameraSource: String,
    onCameraSourceChange: (String) -> Unit,
    selectedVideoEncoder: String,
    onVideoEncoderChange: (String) -> Unit,
    selectedAudioEncoder: String,
    onAudioEncoderChange: (String) -> Unit,
    selectedFPS: String,
    onFPSChange: (String) -> Unit,
    selectedResolution: String,
    onResolutionChange: (String) -> Unit,
    availableVideoCodecs: List<String>,
    availableAudioCodecs: List<String>,
    onApply: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        // Calculate dimensions for a 16:9 preview.
        val cameraPreviewHeight = screenHeight
        val cameraPreviewWidth = cameraPreviewHeight * (16f / 9f)
        val horizontalPadding = (screenWidth - cameraPreviewWidth) / 2

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(cameraPreviewWidth)
                    .fillMaxHeight()
                    .padding(horizontal = horizontalPadding)
                    .background(Color.Black.copy(alpha = 0.95f))
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Close button.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                    Text(
                        text = "Camera Settings",
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.material3.Divider(
                        color = Color.White.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    SectionSubtitle("Camera Source")
                    ModernDropdown(
                        items = listOf("Device Camera", "USB Camera"),
                        selectedValue = selectedCameraSource,
                        displayMapper = { it },
                        onValueChange = onCameraSourceChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Streaming Settings",
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.material3.Divider(
                        color = Color.White.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    SectionSubtitle("Video Encoder")
                    ModernDropdown(
                        items = availableVideoCodecs,
                        selectedValue = selectedVideoEncoder,
                        displayMapper = { it },
                        onValueChange = onVideoEncoderChange
                    )
                    SectionSubtitle("Audio Encoder")
                    ModernDropdown(
                        items = availableAudioCodecs,
                        selectedValue = selectedAudioEncoder,
                        displayMapper = { it },
                        onValueChange = onAudioEncoderChange
                    )
                    SectionSubtitle("Stream FPS")
                    ModernDropdown(
                        items = if (selectedCameraSource == "USB Camera") listOf("30", "60") else listOf("30"),
                        selectedValue = selectedFPS,
                        displayMapper = { it },
                        onValueChange = onFPSChange
                    )
                    SectionSubtitle("Resolution")
                    ModernDropdown(
                        items = listOf("1080p", "720p"),
                        selectedValue = selectedResolution,
                        displayMapper = { it },
                        onValueChange = onResolutionChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalypsoRed,
                            contentColor = Color.White
                        ),
                        shape = CircleShape
                    ) {
                        Text("Apply", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayMenu(
    visible: Boolean,
    selectedTeam1: String,
    onTeam1Change: (String) -> Unit,
    selectedTeam2: String,
    onTeam2Change: (String) -> Unit,
    showScoreboardOverlay: Boolean,
    onToggleScoreboard: (Boolean) -> Unit,
    selectedCamera: String,
    onCameraChange: (String) -> Unit,
    onApply: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
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
                    // Left Section: Scoreboard.
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
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = Color.White
                                )
                                Switch(
                                    checked = showScoreboardOverlay,
                                    onCheckedChange = onToggleScoreboard,
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
                                onValueChange = onTeam1Change
                            )
                            Spacer(modifier = Modifier.height(25.dp))
                            SectionSubtitle("Select Team 2")
                            ModernDropdown(
                                items = listOf("Rivas", "Alcorcón"),
                                selectedValue = selectedTeam2,
                                displayMapper = { it },
                                onValueChange = onTeam2Change
                            )
                            Spacer(modifier = Modifier.height(25.dp))
                            Button(
                                onClick = onApply,
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
                                Text("Apply", color = Color.White, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                    // Center Section: Camera Selection.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {}
                    // Right Section: Placeholder.
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

@Composable
fun ScoreboardOverlay(
    visible: Boolean,
    genericStream: GenericStream,
    leftLogo: android.graphics.Bitmap,
    rightLogo: android.graphics.Bitmap,
    leftTeamGoals: Int,
    rightTeamGoals: Int,
    backgroundColor: Int,
    imageObjectFilterRender: ImageObjectFilterRender,
    onLeftIncrement: () -> Unit,
    onRightIncrement: () -> Unit
) {
    // Add or remove the overlay filter based on visibility.
    LaunchedEffect(visible) {
        if (visible && genericStream.isOnPreview) {
            genericStream.getGlInterface().addFilter(imageObjectFilterRender)
        } else {
            genericStream.getGlInterface().removeFilter(imageObjectFilterRender)
        }
    }
    if (visible && genericStream.isOnPreview) {
        drawOverlay(
            context = LocalContext.current,
            leftLogoBitmap = leftLogo,
            rightLogoBitmap = rightLogo,
            leftTeamGoals = leftTeamGoals,
            rightTeamGoals = rightTeamGoals,
            backgroundColor = backgroundColor,
            imageObjectFilterRender = imageObjectFilterRender,
            isOnPreview = genericStream.isOnPreview
        )
        ScoreboardActionButtons(
            onLeftButtonClick = onLeftIncrement,
            onRightButtonClick = onRightIncrement
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun RecordingTimer(recordingSeconds: Long) {
    val hours = recordingSeconds / 3600
    val minutes = (recordingSeconds % 3600) / 60
    val seconds = recordingSeconds % 60
    val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Red.copy(alpha = 0.5f))
            .padding(top = 2.dp, bottom = 2.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = formattedTime, color = Color.White, fontSize = 20.sp)
    }
}

fun recordVideoStreaming(context: Context, genericStream: GenericStream, state: (RecordController.Status) -> Unit) {
    var recordPath = ""
    if (!genericStream.isRecording) {
        val folder = PathUtils.getRecordPath()
        if (!folder.exists()) folder.mkdir()
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        recordPath = "${folder.absolutePath}/${sdf.format(Date())}.mp4"
        genericStream.startRecord(recordPath) { status ->
            if (status == RecordController.Status.RECORDING) {
                state(RecordController.Status.RECORDING)
            }
        }
        state(RecordController.Status.STARTED)
    } else {
        genericStream.stopRecord()
        state(RecordController.Status.STOPPED)
        PathUtils.updateGallery(context, recordPath)
    }
}