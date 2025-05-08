// CameraScreen.kt
package com.danihg.calypsoapp.presentation.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.request.ImageRequest
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.data.FirestoreManager
import com.danihg.calypsoapp.data.Team
import com.danihg.calypsoapp.overlays.PlayerEntry
import com.danihg.calypsoapp.overlays.drawOverlay
import com.danihg.calypsoapp.presentation.camera.menus.OverlayMenu2
import com.danihg.calypsoapp.services.StreamingService
import com.danihg.calypsoapp.sources.CameraCalypsoSource
import com.danihg.calypsoapp.utils.AuxButton
import com.danihg.calypsoapp.utils.PathUtils
import com.danihg.calypsoapp.utils.PreventScreenLock
import com.danihg.calypsoapp.utils.getAvailableAudioCodecs
import com.danihg.calypsoapp.utils.getAvailableVideoCodecs
import com.danihg.calypsoapp.utils.rememberToast
import com.danihg.calypsoapp.presentation.camera.menus.SettingsMenu
import com.danihg.calypsoapp.presentation.camera.overlays.ScoreboardOverlay
import com.danihg.calypsoapp.presentation.camera.overlays.TeamPlayersOverlay
import com.danihg.calypsoapp.presentation.camera.ui.CameraUI
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import java.util.Date
import com.pedro.extrasources.CameraUvcSource
import com.pedro.library.base.recording.RecordController
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(navHostController: NavHostController) {
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
                CameraScreenContent(navHostController)
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CameraScreenContent(navHostController: NavHostController) {
    val context = LocalContext.current
    val isServiceRunning by remember { mutableStateOf(false) }
    PreventScreenLock()
    val showToast = rememberToast()

    // Retrieve streaming settings from SharedPreferences.
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
    var videoWidth by remember { mutableIntStateOf(sharedPreferences.getInt("videoWidth", 1920)) }
    var videoHeight by remember { mutableIntStateOf(sharedPreferences.getInt("videoHeight", 1080)) }
//    var streamUrl by remember { mutableStateOf(sharedPreferences.getString("streamUrl", "") ?: "") }
//    var rtmpUrl by remember { mutableStateOf(sharedPreferences.getString("rtmpUrl", "") ?: "") }
//    var streamKey by remember { mutableStateOf(sharedPreferences.getString("streamKey", "") ?: "") }
    var streamUrl by remember { mutableStateOf("") }
    var rtmpUrl by remember { mutableStateOf("") }
    var streamKey by remember { mutableStateOf("") }

    val firestoreManager = remember { FirestoreManager() }

    // This will update the state variables, which in turn update your streaming instance.
    LaunchedEffect(Unit) {
        val configs = firestoreManager.getRTMPConfigs()  // returns List<RTMPConfig>
        if (configs.isNotEmpty()) {
            // Choose a configuration—here we select the first one.
            val config = configs.first()
            streamUrl = config.constructedUrl  // The full stream URL (rtmpUrl/streamKey)
            rtmpUrl = config.rtmpUrl
            streamKey = config.streamKey
            // Optionally log it or show a toast
            Log.d("CameraScreen", "RTMP config loaded. Stream URL: $streamUrl")
        } else {
            Log.d("CameraScreen", "No RTMP configuration found in Firestore.")
        }
    }

    var videoBitrate = sharedPreferences.getInt("videoBitrate", 5000 * 1000)
    val videoFPS = sharedPreferences.getInt("videoFPS", 30)
    val audioSampleRate = sharedPreferences.getInt("audioSampleRate", 48000)
    val audioIsStereo = sharedPreferences.getBoolean("audioIsStereo", true)
    val audioBitrate = sharedPreferences.getInt("audioBitrate", 128 * 1000)

    // Streaming and camera settings.
    var selectedCameraSource by remember { mutableStateOf("Device Camera") }
    var selectedAudioSource by remember { mutableStateOf("Device Audio") }
    var selectedVideoEncoder by remember { mutableStateOf("H264") }
    var selectedAudioEncoder by remember { mutableStateOf("AAC") }
    var selectedFPS by remember { mutableStateOf("30") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedBitrate by remember { mutableIntStateOf(5000 * 1000) }
    var selectedTeamsOverlayDuration by remember { mutableStateOf("10s") }

    var snapshot by remember { mutableStateOf<Bitmap?>(null) }

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
    var showOverlaySubMenu by rememberSaveable { mutableStateOf(false) }
    var showScoreboardOverlay by rememberSaveable { mutableStateOf(false) }
    var showScoreboardLogos by rememberSaveable { mutableStateOf(true) }
    var showScoreboardAlias by rememberSaveable { mutableStateOf(true) }
    var showSettingsSubMenu  by rememberSaveable { mutableStateOf(false) }
    var showTeamPlayersOverlayMenu by rememberSaveable { mutableStateOf(false) }
    var showLineUpOverlay by rememberSaveable { mutableStateOf(false) }
    var showTeamPlayersOverlay by rememberSaveable { mutableStateOf(false) }
    var showReplays by rememberSaveable { mutableStateOf(false) }
    var wasScoreboardActive by remember { mutableStateOf(true) }

    var selectedReplayImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedReplayDuration by remember { mutableStateOf(20) }
    var showReplayMenu by rememberSaveable { mutableStateOf(false) }

    var backgroundRecordPath by remember { mutableStateOf<String?>(null) }
    var currentRecordPath: String? = null


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
    val selectedBackgroundColor = Color.Transparent.toArgb()
    var leftTeamGoals by remember { mutableStateOf(0) }
    var rightTeamGoals by remember { mutableStateOf(0) }
    val imageObjectFilterRender = remember { ImageObjectFilterRender() }
    val lineUpFilter = remember { ImageObjectFilterRender() }
    // Reference for the SurfaceView.
    val surfaceViewRef = remember { mutableStateOf<SurfaceView?>(null) }

    // Which scoreboard model is selected in the menu
    var selectedScoreboardModel by rememberSaveable { mutableStateOf("") }

    // Which lineup model is selected in the menu
    var selectedLineupModel by rememberSaveable { mutableStateOf("") }

    // Interval (in seconds) for the lineup tab
    var selectedLineupInterval by rememberSaveable { mutableStateOf(15) }

    // Retrieve teams from Firestore.
    // (Make sure FirestoreManager is properly initialized in your app.)
    var teams by remember { mutableStateOf<List<Team>>(emptyList()) }
    LaunchedEffect(Unit) {
        teams = FirestoreManager().getTeams()
    }

    // Store team logo URLs persistently
    var leftLogoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var rightLogoUrl by rememberSaveable { mutableStateOf<String?>(null) }

    // For default logos in case no team is selected or loading fails.
    val defaultOptions = BitmapFactory.Options()
    val defaultLeftLogo = BitmapFactory.decodeResource(context.resources, R.drawable.rivas_50, defaultOptions)
    defaultLeftLogo.setDensity(context.resources.displayMetrics.densityDpi)
    val defaultRightLogo = BitmapFactory.decodeResource(context.resources, R.drawable.alcorcon_50, defaultOptions)
    defaultRightLogo.setDensity(context.resources.displayMetrics.densityDpi)

    // The selected team names for scoreboard overlay.
    var selectedTeam1 by rememberSaveable { mutableStateOf(if (teams.isNotEmpty()) teams.first().name else "") }
    var selectedTeam2 by rememberSaveable { mutableStateOf(if (teams.size > 1) teams[1].name else "") }

    // Look up the teams based on the selected names.
    val team1 = teams.find { it.name == selectedTeam1 }
    val team2 = teams.find { it.name == selectedTeam2 }
    // Look up the teams alias based on the selected names.
    val team1Alias = team1?.alias ?: "RIV"
    val team2Alias = team2?.alias ?: "ALC"
    // Look up the teams colors based on the selected names.
    var leftTeamColor by remember { mutableStateOf("Blue") }
    var rightTeamColor by remember { mutableStateOf("Red") }
    // Load team logos from URL (if available) using a helper composable.
    val leftLogoBitmap: Bitmap? = team1?.logo?.takeIf { it.isNotEmpty() }?.let { rememberBitmapFromUrl(it) }
    val rightLogoBitmap: Bitmap? = team2?.logo?.takeIf { it.isNotEmpty() }?.let { rememberBitmapFromUrl(it) }
    // Fallback to default logos.
    val finalLeftLogo = leftLogoBitmap ?: defaultLeftLogo
    val finalRightLogo = rightLogoBitmap ?: defaultRightLogo

    val team1Players: List<PlayerEntry> = team1?.players?.map { playerStr ->
        val parts = playerStr.split(",")
        if (parts.size == 2) {
            // parts[0] is player name and parts[1] is the number.
            PlayerEntry(parts[1].trim(), parts[0].trim())
        } else {
            // Fallback if the format is not as expected.
            PlayerEntry("", playerStr)
        }
    } ?: emptyList()

    val team2Players: List<PlayerEntry> = team2?.players?.map { playerStr ->
        val parts = playerStr.split(",")
        if (parts.size == 2) {
            PlayerEntry(parts[1].trim(), parts[0].trim())
        } else {
            PlayerEntry("", playerStr)
        }
    } ?: emptyList()

    // Use a custom factory to pass context to your CameraViewModel.
    val cameraViewModel: CameraViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CameraViewModel(context) as T
            }
        }
    )
    // Retrieve the same camera instance
    var activeCameraSource by remember { mutableStateOf(cameraViewModel.activeCameraSource) }

    val micSource: MicrophoneSource = remember { MicrophoneSource(MediaRecorder.AudioSource.DEFAULT) }
    val audio: AudioSource = remember { micSource }
    val externalAudioSource: MicrophoneSource = remember { MicrophoneSource(MediaRecorder.AudioSource.MIC) }
    val externalAudio: AudioSource = remember { externalAudioSource }
    var isMicMuted by remember { mutableStateOf(false) }

    // For showing/hiding the zoom slider overlay
    var showZoomSlider by rememberSaveable { mutableStateOf(false) }
    // For holding the current zoom level (e.g., 1x to 5x)
    var zoomLevel by rememberSaveable { mutableFloatStateOf(1f) }

    // For the exposure slider (horizontal, bottom center)
    var showExposureSlider by rememberSaveable { mutableStateOf(false) }
    var exposureLevel by rememberSaveable { mutableStateOf(0f) }
    var exposureMode by rememberSaveable { mutableStateOf("AUTO") } // "AUTO" or "MANUAL"
    val defaultExposure by remember { mutableStateOf(activeCameraSource.getExposure()) }
    var baseExposureLevel by remember { mutableStateOf(exposureLevel) }

    var isoSliderValue by rememberSaveable { mutableStateOf(0f) }

    // State variables for white balance
    var showWhiteBalanceSlider by rememberSaveable { mutableStateOf(false) }
    var whiteBalanceMode by rememberSaveable { mutableStateOf("AUTO") } // "AUTO" or "MANUAL"
    var manualWhiteBalanceTemperature by rememberSaveable { mutableStateOf(5000f) } // in Kelvin

    // State for Optical Video Stabilization
    var showOpticalVideoStabilization by rememberSaveable { mutableStateOf(false) }
    var opticalVideoStabilizationMode by rememberSaveable { mutableStateOf("DISABLE") }

    // States for exposure compensation and sensor exposure time
    var showExposureCompensationSlider by rememberSaveable { mutableStateOf(false) }
    var exposureCompensation by rememberSaveable { mutableStateOf(0f) }

    var showCameraModeSelection by rememberSaveable { mutableStateOf(false) }
    var showManualSubMenu by rememberSaveable { mutableStateOf(false) }
    var showAutoSubMenu by rememberSaveable { mutableStateOf(false) }
    var showIsoSlider by rememberSaveable { mutableStateOf(false) }
    var cameraMode by rememberSaveable { mutableStateOf("AUTO") }
    var isVolumeMenuVisible by rememberSaveable { mutableStateOf(false) }

    var showSensorExposureTimeSlider by rememberSaveable { mutableStateOf(false) }
    var sensorExposureTimeIndex by rememberSaveable { mutableStateOf<Float?>(null) }
    val defaultSensorExposureIndex = 3f
    var sensorExposureTimeMode by rememberSaveable { mutableStateOf("AUTO") }
    var currentSensorExposureTime by rememberSaveable { mutableStateOf(20000000L) }
    val minSensorExposure = 2000000L
    val maxSensorExposure = 33333333L

    // Define your common sensor exposure options.
    val sensorExposureTimeOptions = listOf(
        "1/30" to 33333333L,
        "1/40" to 25000000L,
        "1/50" to 20000000L,
        "1/60" to 16666667L, // We'll use this as the default.
        "1/100" to 10000000L,
        "1/120" to 8333333L,
        "1/250" to 4000000L,
        "1/500" to 2000000L
    )

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
        }, activeCameraSource, audio).apply {
            prepareVideo(videoWidth, videoHeight, videoBitrate, videoFPS)
            prepareAudio(audioSampleRate, audioIsStereo, audioBitrate)
            getGlInterface().autoHandleOrientation = true
        }
    }

    // Track lifecycle changes
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope() // Create a coroutine scope

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    Log.d("CameraScreen", "App minimized")
                }
                Lifecycle.Event.ON_START -> {
                    Log.d("CameraScreen", "App restored")

                    Log.d("CameraScreen", "Exposure Level: $exposureLevel")
                    Log.d("CameraScreen", "Exposure Mode: $exposureMode")

                    coroutineScope.launch {
                        while (!genericStream.isOnPreview) {
                            delay(100) // Wait for surface to be recreated
                        }
                        // Reapply the saved settings:

                        // Zoom
                        activeCameraSource.setZoom(zoomLevel)
                        activeCameraSource.reapplySettings()

                        // Reapply White Balance
                        if (whiteBalanceMode == "MANUAL") {
                            activeCameraSource.setManualWhiteBalance(manualWhiteBalanceTemperature)
                        } else {
                            activeCameraSource.setWhiteBalance(CaptureRequest.CONTROL_AWB_MODE_AUTO)
                        }

                        // Reapply Optical Stabilization
                        when (opticalVideoStabilizationMode) {
                            "ENABLE" -> activeCameraSource.enableOpticalVideoStabilization()
                            "DISABLE" -> activeCameraSource.disableOpticalVideoStabilization()
                        }

                        activeCameraSource.setExposure(exposureCompensation.toInt())
                    }
                    coroutineScope.launch {
                        // Wait until the preview is ready.
                        while (!genericStream.isOnPreview) {
                            delay(100)
                        }
                        // If the scoreboard overlay should be visible, reapply it.
                        if (showScoreboardOverlay && !isStreaming) {
//                            genericStream.getGlInterface().clearFilters()
                            genericStream.getGlInterface().addFilter(imageObjectFilterRender)
                            drawOverlay(
                                context = context,
                                leftLogoBitmap = finalLeftLogo,
                                rightLogoBitmap = finalRightLogo,
                                leftTeamGoals = leftTeamGoals,
                                rightTeamGoals = rightTeamGoals,
                                leftTeamAlias = team1Alias,
                                rightTeamAlias = team2Alias,
                                leftTeamColor = leftTeamColor,
                                rightTeamColor = rightTeamColor,
                                backgroundColor = selectedBackgroundColor,
                                showLogos = showScoreboardLogos,
                                showAlias = showScoreboardAlias,
                                imageObjectFilterRender = imageObjectFilterRender,
                                isOnPreview = genericStream.isOnPreview
                            )
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }



    fun startForegroundService(mode: String) {

        // Check if the FOREGROUND_SERVICE_CAMERA permission is granted
        if (Build.VERSION.SDK_INT >= 34 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showToast("Camera permission for foreground service not granted!")
            return
        }

        val serviceIntent = Intent(context, StreamingService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Add slight delay before starting stream
            CoroutineScope(Dispatchers.Main).launch {
                delay(500)
                if (mode == "Stream") {
                    if (!genericStream.isStreaming) {
                        genericStream.startStream(streamUrl)
                        isStreaming = true
                    }
                }
                if (mode == "Record") {
                    if (!isRecording) {
                        recordVideoStreaming(context, genericStream) { }
                        isRecording = true
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("StreamService", "Failed to start service: ${e.message}")
            // Handle permission error
        }
    }

    // Replace stopForegroundService function
    fun stopForegroundService(mode: String) {
        val serviceIntent = Intent(context, StreamingService::class.java)
        context.stopService(serviceIntent)

        if (genericStream.isStreaming && mode == "Stream") {
            genericStream.stopStream()
            isStreaming = false
        }
        if (genericStream.isRecording && mode == "Record") {
            recordVideoStreaming(context, genericStream) { }
            isRecording = false
        }
    }

    // Add service cleanup when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            if (isServiceRunning) {
                context.stopService(Intent(context, StreamingService::class.java))
            }
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
                    surfaceViewRef = surfaceViewRef
                )

                // Team Players Overlay.
                Log.d("PixelsWidth", LocalContext.current.resources.displayMetrics.widthPixels.toString())
                Log.d("PixelsHeight", LocalContext.current.resources.displayMetrics.heightPixels.toString())

                TeamPlayersOverlay(
                    visible = showLineUpOverlay,
                    genericStream = genericStream,
                    screenWidth = LocalContext.current.resources.displayMetrics.widthPixels,
                    screenHeight = LocalContext.current.resources.displayMetrics.heightPixels,
                    team1Name = selectedTeam1,
                    team2Name = selectedTeam2,
                    team1Players = team1Players,
                    team2Players = team2Players,
                    leftLogo = finalLeftLogo,
                    rightLogo = finalRightLogo,
                    selectedTeamsOverlayDuration = selectedTeamsOverlayDuration,
                    lineUpFilter = lineUpFilter,
                    context = context,
                    onLineUpFinished = {}
//                    onLineUpFinished = {
//                        showLineUpOverlay = false
////                        if (wasScoreboardActive) {
////                            CoroutineScope(Dispatchers.Main).launch {
////                                genericStream.getGlInterface().clearFilters()
////                                showScoreboardOverlay = false
////                                delay(50)
////                                showScoreboardOverlay = true
////                            }
////                        }
//                    }
                )

                // Scoreboard overlay.
                ScoreboardOverlay(
                    visible = showScoreboardOverlay,
                    genericStream = genericStream,
                    leftLogo = finalLeftLogo,
                    rightLogo = finalRightLogo,
                    leftTeamGoals = leftTeamGoals,
                    rightTeamGoals = rightTeamGoals,
                    leftTeamAlias = team1Alias,
                    rightTeamAlias = team2Alias,
                    leftTeamColor = leftTeamColor,
                    rightTeamColor = rightTeamColor,
                    backgroundColor = selectedBackgroundColor,
                    showLogos = showScoreboardLogos,
                    showAlias = showScoreboardAlias,
                    imageObjectFilterRender = imageObjectFilterRender,
                    onLeftIncrement = {
                        leftTeamGoals++
                        drawOverlay(
                            context = context,
                            leftLogoBitmap = finalLeftLogo,
                            rightLogoBitmap = finalRightLogo,
                            leftTeamGoals = leftTeamGoals,
                            rightTeamGoals = rightTeamGoals,
                            leftTeamAlias = team1Alias,
                            rightTeamAlias = team2Alias,
                            leftTeamColor = leftTeamColor,
                            rightTeamColor = rightTeamColor,
                            backgroundColor = selectedBackgroundColor,
                            showLogos = showScoreboardLogos,
                            showAlias = showScoreboardAlias,
                            imageObjectFilterRender = imageObjectFilterRender,
                            isOnPreview = genericStream.isOnPreview
                        )
                    },
                    onRightIncrement = {
                        rightTeamGoals++
                        drawOverlay(
                            context = context,
                            leftLogoBitmap = finalLeftLogo,
                            rightLogoBitmap = finalRightLogo,
                            leftTeamGoals = leftTeamGoals,
                            rightTeamGoals = rightTeamGoals,
                            leftTeamAlias = team1Alias,
                            rightTeamAlias = team2Alias,
                            leftTeamColor = leftTeamColor,
                            rightTeamColor = rightTeamColor,
                            backgroundColor = selectedBackgroundColor,
                            showLogos = showScoreboardLogos,
                            showAlias = showScoreboardAlias,
                            imageObjectFilterRender = imageObjectFilterRender,
                            isOnPreview = genericStream.isOnPreview
                        )
                    },
                    onLeftDecrement = {
                        leftTeamGoals--
                        drawOverlay(
                            context = context,
                            leftLogoBitmap = finalLeftLogo,
                            rightLogoBitmap = finalRightLogo,
                            leftTeamGoals = leftTeamGoals,
                            rightTeamGoals = rightTeamGoals,
                            leftTeamAlias = team1Alias,
                            rightTeamAlias = team2Alias,
                            leftTeamColor = leftTeamColor,
                            rightTeamColor = rightTeamColor,
                            backgroundColor = selectedBackgroundColor,
                            showLogos = showScoreboardLogos,
                            showAlias = showScoreboardAlias,
                            imageObjectFilterRender = imageObjectFilterRender,
                            isOnPreview = genericStream.isOnPreview
                        )
                    },
                    onRightDecrement = {
                        rightTeamGoals--
                        drawOverlay(
                            context = context,
                            leftLogoBitmap = finalLeftLogo,
                            rightLogoBitmap = finalRightLogo,
                            leftTeamGoals = leftTeamGoals,
                            rightTeamGoals = rightTeamGoals,
                            leftTeamAlias = team1Alias,
                            rightTeamAlias = team2Alias,
                            leftTeamColor = leftTeamColor,
                            rightTeamColor = rightTeamColor,
                            backgroundColor = selectedBackgroundColor,
                            showLogos = showScoreboardLogos,
                            showAlias = showScoreboardAlias,
                            imageObjectFilterRender = imageObjectFilterRender,
                            isOnPreview = genericStream.isOnPreview
                        )
                    }
                )
                // Reset scoreboard if overlay is hidden.
                if (!showScoreboardOverlay) {
                    genericStream.getGlInterface().removeFilter(imageObjectFilterRender)
                    leftTeamGoals = 0
                    rightTeamGoals = 0
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(top = 30.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showTeamPlayersOverlay && !showLineUpOverlay) {
                            AuxButton(
                                modifier = Modifier
                                    .size(40.dp)
                                    .zIndex(2f),
                                painter = painterResource(id = R.drawable.ic_line_up),
                                onClick = {
                                    wasScoreboardActive = showScoreboardOverlay
//                                    showScoreboardOverlay = false
                                    showLineUpOverlay = true
//                                    val gifFilter = GifObjectFilterRender()
//                                    gifFilter.setGif(context.resources.openRawResource(R.raw.gif_banana))
//                                    genericStream.getGlInterface().addFilter(gifFilter)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
//                        if (showReplays) {
//                            AuxButton(
//                                modifier = Modifier
//                                    .size(40.dp)
//                                    .zIndex(2f),
//                                painter = painterResource(id = R.drawable.ic_replay),
//                                onClick = {
////                                    coroutineScope.launch {
////                                        backgroundRecordPath?.let { path ->
////                                            try {
////                                                // Wait for FFmpegKit to finish processing.
////                                                val savedReplay = handleReplaySuspended(context, genericStream, path, selectedReplayDuration)
////                                                val replayUri = Uri.fromFile(File(savedReplay))
////                                                genericStream.changeVideoSource(VideoFileSource(context, replayUri, false) {})
////                                                // Keep the replay on screen for the selected duration.
////                                                val time = selectedReplayDuration.split("s")[0].toLong() * 1000
////                                                delay(time)
////                                                genericStream.changeVideoSource(activeCameraSource)
////                                            } catch (e: Exception) {
////                                                Log.e("Replay", "Error processing replay: ${e.message}")
////                                            }
////                                        } ?: run {
////                                            Log.e("Replay", "No background recording found!")
////                                        }
////                                    }
//
//                                        //ESTO ERA OTRO CODIGO COMENTADO
////                                    coroutineScope.launch {
////                                        backgroundRecordPath?.let { path ->
////                                            val savedReplay = handleReplay(context, genericStream, path, selectedReplayDuration)
////                                            val replayUri = Uri.fromFile(File(savedReplay))
////                                            genericStream.changeVideoSource(VideoFileSource(context, replayUri, false) {})
////                                            val time = selectedReplayDuration.split("s")[0].toLong() * 1000
////                                            delay(time)
////                                            genericStream.changeVideoSource(activeCameraSource)
////                                        } ?: run {
////                                            Log.e("Replay", "No background recording found!")
////                                        }
////                                    }
//                                }
//                            )
//                        }
                    }
                }

                snapshot?.let { capturedBitmap ->
                    // Calculate the aspect ratio of the bitmap
                    val aspectRatio = capturedBitmap.width.toFloat() / capturedBitmap.height.toFloat()

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .zIndex(4f)
                            .align(Alignment.BottomStart)
                            .padding(start = 80.dp, bottom = 40.dp)
                    ) {
                        // Wrap in a Box that adapts to the bitmap's aspect ratio
                        Box(
                            modifier = Modifier
                                // Set a fixed width (or height) as desired:
                                .width(120.dp)
                                // Adjust the height based on the calculated aspect ratio
                                .aspectRatio(aspectRatio)
                                .border(
                                    width = 2.dp,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Image(
                                bitmap = capturedBitmap.asImageBitmap(),
                                contentDescription = "Snapshot of the taken picture",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                var teamsSelected = selectedTeam1.isNotEmpty() && selectedTeam2.isNotEmpty()
                // Right-side action buttons.
                CameraUI(
                    onShowApplyButton = { showApplyButton = !showApplyButton },
                    streamUrl = streamUrl,
                    onStartStreaming = {
                        if (isStreaming) stopForegroundService("Stream") else startForegroundService("Stream")
                    },
                    isStreaming = isStreaming,
                    onRecordWhileStreaming = {
                        if (!isRecording) {
                            recordVideoStreaming(context, genericStream) { }
                            isRecording = true
                        } else {
                            recordVideoStreaming(context, genericStream) { }
                            isRecording = false
                        }
                    },
                    isRecording = isRecording,
                    showZoomSlider = showZoomSlider,
                    onToggleShowZoomSlider = { showZoomSlider = !showZoomSlider },
                    isMicMuted = isMicMuted,
                    onToggleVolumeMenuVisible = { isVolumeMenuVisible = !isVolumeMenuVisible },
                    isVolumeMenuVisible = isVolumeMenuVisible,
                    onToggleMuteUnmute = {
                        isMicMuted = !isMicMuted
                        if (isMicMuted) {
                            micSource.mute()
                            externalAudioSource.mute()
                        } else {
                            micSource.unMute()
                            externalAudioSource.unMute()
                        }
                    },
                    onToggleSettingsMenu = {
                        showSettingsSubMenu = !showSettingsSubMenu
                        // Reset all nested menus when toggling settings
                        showCameraModeSelection = false
                        showManualSubMenu = false
                        showAutoSubMenu = false
                        showIsoSlider = false
                        showExposureCompensationSlider = false
                        showExposureSlider = false
                        showSensorExposureTimeSlider = false
                    },
                    showSettingsSubMenu = showSettingsSubMenu,
                    showCameraModeSelection = showCameraModeSelection,
                    showManualSubMenu = showManualSubMenu,
                    showAutoSubMenu = showAutoSubMenu,
                    onShowSettingsCameraMenu = {
                        showCameraModeSelection = !showCameraModeSelection
                        // Reset any lower-level menus
                        showManualSubMenu = false
                        showAutoSubMenu = false
                        showIsoSlider = false
                        showExposureCompensationSlider = false
                        showExposureSlider = false
                        showSensorExposureTimeSlider = false
                    },
                    onShowSettingsStreamMenu = { isSettingsMenuVisible = !isSettingsMenuVisible },
                    onShowSettingsCameraManualMenu = {
                        cameraMode = "MANUAL"
                        showCameraModeSelection = false
                        showManualSubMenu = true
                        showAutoSubMenu = false
                    },
                    onShowSettingsCameraAutoMenu = {
                        cameraMode = "AUTO"
                        showCameraModeSelection = false
                        showAutoSubMenu = true
                        showManualSubMenu = false

                        activeCameraSource.setIsoAuto()
                        activeCameraSource.enableAutoExposure()
                        val isAutoExposure = activeCameraSource.isAutoExposureEnabled()
                        activeCameraSource.setExposure(defaultExposure)
                        exposureLevel = defaultExposure.toFloat()
                    },
                    onCameraSettingsManualAutoBack = { showCameraModeSelection = false },
                    onShowIsoSlider = {
                        showIsoSlider = true
                        showManualSubMenu = false
                    },
                    onShowSensorExposureTimeSlider = {
                        sensorExposureTimeMode = "MANUAL"
                        showSensorExposureTimeSlider = true
                        showManualSubMenu = false
                    },
                    onCameraManualModeBack = {
                        showManualSubMenu = false
                        showCameraModeSelection = true
                    },
                    onShowExposureCompensationSlider = {
                        showExposureCompensationSlider = true
                        showAutoSubMenu = false
                    },
                    onCameraAutoModeBack = {
                        showAutoSubMenu = false
                        showCameraModeSelection = true
                    },
                    zoomLevel = zoomLevel,
                    onZoomLevelFunction = { delta ->
                        zoomLevel = (zoomLevel + delta).coerceIn(1f, 5f)
                        activeCameraSource.setZoom(zoomLevel)
                    },
                    micSource = micSource,
                    externalAudioSource = externalAudioSource,
                    showIsoSlider = showIsoSlider,
                    isoSliderValue = isoSliderValue,
                    onIsoSliderValue = { newValue ->
                        isoSliderValue = newValue
                    },
                    onUpdateSensorSensitivity = { newIso ->
                        activeCameraSource.setSensorSensitivity(newIso)
                    },
                    onIsoSliderBack = {
                        showIsoSlider = false
                        showManualSubMenu = true
                    },
                    showSensorExposureTimeSlider = showSensorExposureTimeSlider,
                    sensorExposureTimeMode = sensorExposureTimeMode,
                    sensorExposureTimeIndex = sensorExposureTimeIndex,
                    defaultSensorExposureIndex = defaultSensorExposureIndex,
                    sensorExposureTimeOptions = sensorExposureTimeOptions,
                    onSensorExposureTimeSliderChange = { newIndex ->
                        sensorExposureTimeIndex = newIndex
                        val idx = newIndex.toInt().coerceIn(0, sensorExposureTimeOptions.size - 1)
                        val newSensorTime = sensorExposureTimeOptions[idx].second
                        activeCameraSource.setSensorExposureTime(newSensorTime)
                        // Store the exact value for later reapplication.
                        currentSensorExposureTime = newSensorTime
                        // Update the normalized exposure slider value.
                        exposureLevel =
                            ((newSensorTime - minSensorExposure).toFloat() / (maxSensorExposure - minSensorExposure))
                        baseExposureLevel = exposureLevel
                    },
                    onSensorExposureTimeSliderBack = {
                        showSensorExposureTimeSlider = false
                        showManualSubMenu = true
                    },
                    showExposureCompensationSlider = showExposureCompensationSlider,
                    activeCameraSource = activeCameraSource,
                    onExposureCompensationBack = {
                        showExposureCompensationSlider = false
                        showAutoSubMenu = true
                    },
                    onStartRecord = {
                        if (isRecording) stopForegroundService("Record") else startForegroundService("Record")
                    },
                    onTakePicture = {
                        genericStream.getGlInterface().takePhoto { bitmap ->
                            val success = saveBitmapToDevice(context, bitmap)
                            if (success) {
                                Log.d("Picture", "Picture saved successfully!")
                            } else {
                                Log.e("Picture", "Error saving picture!")
                            }
                            // Set snapshot state to display the overlay:
                            snapshot = bitmap
                            coroutineScope.launch {
                                delay(5000)
                                snapshot = null
                            }
                        }
                    },
                    teamsSelected = teamsSelected,
                    showLineUpOverlay = showLineUpOverlay,
                    showScoreboardOverlay = showScoreboardOverlay,
                    onToggleLineUpOverlay = { showLineUpOverlay = !showLineUpOverlay },
                    onToggleScoreboardOverlay = { showScoreboardOverlay = !showScoreboardOverlay },
                    showReplayMenuBtn = showReplayMenu,
                    onTogglePlayReplay = {  },
                    onToggleSaveReplay = {  }
                )


                // Calculate dimensions for the settings menu.
                val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels.dp
                val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels.dp

                // Settings menu overlay.
                SettingsMenu(
                    visible = isSettingsMenuVisible,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    isStreaming = isStreaming,
                    selectedCameraSource = selectedCameraSource,
                    onCameraSourceChange = { selectedCameraSource = it },
                    selectedAudioSource = selectedAudioSource,
                    onAudioSourceChange = { selectedAudioSource = it },
                    selectedVideoEncoder = selectedVideoEncoder,
                    onVideoEncoderChange = { selectedVideoEncoder = it },
                    selectedAudioEncoder = selectedAudioEncoder,
                    onAudioEncoderChange = { selectedAudioEncoder = it },
                    selectedBitrate = selectedBitrate,
                    onBitrateChange = { selectedBitrate = it },
                    selectedFPS = selectedFPS,
                    onFPSChange = { selectedFPS = it },
                    selectedResolution = selectedResolution,
                    onResolutionChange = { selectedResolution = it },
                    rtmpUrl = rtmpUrl,
                    onRtmpUrlChange = { rtmpUrl = it },
                    streamKey = streamKey,
                    onStreamKeyChange = { streamKey = it },
                    availableVideoCodecs = getAvailableVideoCodecs(),
                    availableAudioCodecs = getAvailableAudioCodecs(),
                    onApply = { finalStreamUrl, newRtmpUrl, newStreamKey ->

                        streamUrl = finalStreamUrl
                        rtmpUrl = newRtmpUrl
                        streamKey = newStreamKey
                        // Launch a coroutine to introduce a delay after stopping the preview.
                        coroutineScope.launch {
                            if (!isStreaming) {

                                // 1. Clear any active GL filters to prevent lingering state.
                                genericStream.getGlInterface().clearFilters()

                                // 2. Stop the current preview so that the GL context can properly shut down.
                                genericStream.stopPreview()

                                // Introduce a delay (e.g., 500ms) to allow the GL context to settle.
                                delay(1000)

                                // Apply new resolution settings.
                                when (selectedResolution) {
                                    "1080p" -> {
                                        videoWidth = 1920
                                        videoHeight = 1080
                                    }
                                    "720p" -> {
                                        videoWidth = 1280
                                        videoHeight = 720
                                    }
                                    "1440p" -> {
                                        videoWidth = 2560
                                        videoHeight = 1440
                                    }
                                }
                                videoBitrate = selectedBitrate

                                // Save settings.
                                sharedPreferences.edit().apply {
                                    putInt("videoWidth", videoWidth)
                                    putInt("videoHeight", videoHeight)
                                    putInt("videoBitrate", videoBitrate)
                                    putString("streamUrl", finalStreamUrl)
                                    putString("rtmpUrl", rtmpUrl)
                                    putString("streamKey", streamKey)
                                    apply()
                                }

                                // Update codecs.
                                enumAudioMapping[selectedAudioEncoder]?.let { audioCodec ->
                                    genericStream.setAudioCodec(audioCodec)
                                    Log.d("CodecCheck", "Set audio codec to: $audioCodec")
                                }
                                enumVideoMapping[selectedVideoEncoder]?.let { videoCodec ->
                                    genericStream.setVideoCodec(videoCodec)
                                    Log.d("CodecCheck", "Set video codec to: $videoCodec")
                                }

                                // Prepare video and audio with new settings.
                                genericStream.prepareVideo(videoWidth, videoHeight, videoBitrate, selectedFPS.toInt())
                                genericStream.prepareAudio(audioSampleRate, audioIsStereo, audioBitrate)

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
                                    val newCameraSource = CameraCalypsoSource(context)
                                    genericStream.changeVideoSource(newCameraSource)
                                    activeCameraSource = newCameraSource
                                }
                                // Update audio source.
                                if (selectedAudioSource == "USB Mic") {
                                    genericStream.changeAudioSource(externalAudio)
                                } else {
                                    genericStream.changeAudioSource(audio)
                                }
                                showScoreboardOverlay = false
                            } else {
                                videoBitrate = selectedBitrate
                                genericStream.setVideoBitrateOnFly(videoBitrate)
                            }
                            isSettingsMenuVisible = false
                        }
                    },
                    onClose = { isSettingsMenuVisible = false }
                )



                // Auxiliary overlay menu.
                OverlayMenu2(
                    visible = showApplyButton,
                    teams = teams,
                    selectedTeam1 = selectedTeam1,
                    onTeam1Change = { selectedTeam1 = it },
                    selectedTeam2 = selectedTeam2,
                    onTeam2Change = { selectedTeam2 = it },
                    onLeftLogoUrlChange = { leftLogoUrl = it },
                    onRightLogoUrlChange = { rightLogoUrl = it },
                    onAddTeam = { navHostController.navigate("addTeam") },
                    selectedLeftColor = leftTeamColor,
                    onLeftColorChange = { leftTeamColor = it },
                    selectedRightColor = rightTeamColor,
                    onRightColorChange = { rightTeamColor = it },
                    showLogos = showScoreboardLogos,
                    onToggleShowLogos = { showScoreboardLogos = it },
                    showAlias = showScoreboardAlias,
                    onToggleShowAlias = { showScoreboardAlias = it },
                    showScoreboard = showScoreboardOverlay,
                    onToggleScoreboard = { showScoreboardOverlay = it },
                    selectedScoreboard = selectedScoreboardModel,
                    onScoreboardSelected = { selectedScoreboardModel = it },
                    selectedLineup = selectedLineupModel,
                    onLineupSelected = { selectedLineupModel = it },
                    selectedIntervalSeconds = selectedLineupInterval,
                    onIntervalChange = { selectedLineupInterval = it },
                    showLineup = showTeamPlayersOverlay,
                    onToggleLineup = { showTeamPlayersOverlay = it },
                    selectedReplayImageUri = selectedReplayImageUri,
                    onReplayImageUriChange = { selectedReplayImageUri = it },
                    selectedReplayDuration = selectedReplayDuration,
                    onReplayDurationChange = { selectedReplayDuration = it },
                    showReplays = showReplayMenu,
                    onToggleShowReplays = { showReplayMenu = it },
                    onClose = { showApplyButton = false}
                )

                Log.d("ShowReplays", "ShowReplays: $showReplayMenu")
//                OverlayMenu(
//                    visible = showApplyButton,
//                    screenWidth = screenWidth,
//                    screenHeight = screenHeight,
//                    teams = teams,  // Pass the list of teams from Firestore.
//                    selectedTeam1 = selectedTeam1,
//                    onTeam1Change = { selectedTeam1 = it },
//                    selectedTeam2 = selectedTeam2,
//                    onTeam2Change = { selectedTeam2 = it },
//                    onLeftLogoUrlChange = { leftLogoUrl = it }, // Save the new left logo URL
//                    onRightLogoUrlChange = { rightLogoUrl = it }, // Save the new right logo URL
//                    showScoreboardOverlay = showScoreboardOverlay,
//                    onToggleScoreboard = { showScoreboardOverlay = it },
//                    selectedLeftColor = leftTeamColor,
//                    onLeftColorChange = { leftTeamColor = it },
//                    selectedRightColor = rightTeamColor,
//                    onRightColorChange = { rightTeamColor = it },
//                    selectedTeamsOverlayDuration = selectedTeamsOverlayDuration,
//                    onTeamsOverlayDurationChange = { selectedTeamsOverlayDuration = it },
//                    showLineUpOverlay = showTeamPlayersOverlay,
//                    onToggleLineUp = { showTeamPlayersOverlay = it },
//                    showReplays = showReplays,
//                    onToggleReplays = { showReplays = it },
//                    selectedReplaysDuration = selectedReplayDuration,
//                    onReplaysDurationChange = { selectedReplayDuration = it },
//                    onClose = {
//                        showApplyButton = false
//                        showOverlaySubMenu = false
//                    }
//                )

                // Place the recording timer at the very top with a high z-index.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f), // Ensures the timer is drawn above all other UI elements.
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Streaming timer
                        if (isStreaming) {
                            RecordingTimer(recordingSeconds = streamingTimerSeconds)
                        }
                        // If recording, show recording timer just below
                        if (isRecording) {
                            Spacer(modifier = Modifier.height(4.dp))
                            RecordingTimer(recordingSeconds = recordingTimerSeconds)
                        }
                    }
                }
            }
        }
    )
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

suspend fun safeLoadBitmap(context: Context, url: String?): Bitmap? {
    val originalBitmap = url?.let { loadBitmapFromUrl(context, it) }
    return originalBitmap
}

// Helper composable to load a Bitmap from a URL using Coil.
@Composable
fun rememberBitmapFromUrl(url: String): Bitmap? {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(url) {
        bitmap = loadBitmapFromUrl(context, url)
    }
    return bitmap
}

suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap? {
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false)
//        .transformations(RemoveBorderWhiteTransformation(tolerance = 30))
        .build()

    val result = loader.execute(request)
    return (result.drawable as? BitmapDrawable)?.bitmap
}

// Declare a property at the class level so that the file path persists between start and stop.
private var currentRecordPath: String? = null

fun recordVideoStreaming(
    context: Context,
    genericStream: GenericStream,
    state: (RecordController.Status) -> Unit
) {
    if (!genericStream.isRecording) {
        val folder = PathUtils.getRecordPath()
        if (!folder.exists()) folder.mkdir()
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        currentRecordPath = "${folder.absolutePath}/${sdf.format(Date())}.mp4"

        genericStream.startRecord(currentRecordPath!!) { status ->
            when (status) {
                RecordController.Status.RECORDING -> {
                    state(RecordController.Status.RECORDING)
                }
                RecordController.Status.STOPPED -> {
                    // When the STOPPED status is delivered, trigger the gallery update with polling.
                    updateGalleryWithPolling(context, initialDelay = 2000, timeoutMillis = 5000, pollInterval = 500)
                }
                else -> { /* handle other states if needed */ }
            }
        }
        state(RecordController.Status.STARTED)
    } else {
        // Stop recording
        genericStream.stopRecord()
        state(RecordController.Status.STOPPED)
        // As a fallback, if the callback isn't reliable, schedule an update.
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            updateGalleryWithPolling(context, initialDelay = 0, timeoutMillis = 5000, pollInterval = 500)
        }
    }
}

// This helper suspend function polls the file until its size is greater than zero or a timeout is reached.
private suspend fun waitForFileToBeFinalized(
    filePath: String,
    timeoutMillis: Long,
    pollInterval: Long
): Boolean {
    val startTime = System.currentTimeMillis()
    val file = File(filePath)

    while (System.currentTimeMillis() - startTime < timeoutMillis) {
        if (file.exists() && file.length() > 0L) {
            // File is no longer empty.
            return true
        }
        delay(pollInterval)
    }
    return false
}

// This function delays the gallery update until the file is confirmed to be finalized.
private fun updateGalleryWithPolling(
    context: Context,
    initialDelay: Long,
    timeoutMillis: Long,
    pollInterval: Long
) {
    CoroutineScope(Dispatchers.Main).launch {
        // Wait an initial delay if necessary.
        if (initialDelay > 0) delay(initialDelay)

        currentRecordPath?.let { path ->
            val isFinalized = waitForFileToBeFinalized(path, timeoutMillis, pollInterval)
            if (isFinalized) {
                // Update the gallery with the finalized file.
                PathUtils.updateGallery(context, path)
            } else {
                // Optionally, log an error indicating that the file didn't finalize in time.
                Log.e("RecordVideo", "File finalization timeout: $path still appears empty.")
            }
        }
        // Clear the stored path after updating.
        currentRecordPath = null
    }
}

fun saveBitmapToDevice(context: Context, bitmap: Bitmap): Boolean {
    // Generate a unique file name using a timestamp
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "IMG_$timeStamp.jpg"

    // Choose a directory to save the image; here we use the public pictures directory.
    // Note: Starting with API 29 (Android 10), you might want to use MediaStore for better integration with scoped storage.
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (!picturesDir.exists()) {
        picturesDir.mkdirs() // Create folder if it does not exist
    }

    val imageFile = File(picturesDir, fileName)
    var fos: FileOutputStream? = null

    return try {
        fos = FileOutputStream(imageFile)
        // Compress the bitmap into JPEG format and write the image data to the output stream.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.flush()

        // Optionally, update the gallery so that the new image appears in gallery apps.
        MediaScannerConnection.scanFile(context,
            arrayOf(imageFile.absolutePath),
            arrayOf("image/jpeg"),
            null)

        true // Successfully saved
    } catch (e: Exception) {
        e.printStackTrace()
        false // Error occurred
    } finally {
        fos?.close()
    }
}
//fun startBackgroundRecording(context: Context, genericStream: GenericStream): String {
//    val folder = PathUtils.getRecordPath()
//    if (!folder.exists()) folder.mkdir()
//    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
//    val path = "${folder.absolutePath}/background_${sdf.format(Date())}.mp4"
//    genericStream.startRecord(path) { status ->
//        // You might want to handle status updates if needed.
//    }
//    return path
//}
//
//suspend fun handleReplaySuspended(
//    context: Context,
//    genericStream: GenericStream,
//    backgroundRecordPath: String,
//    secondsClip: String,
//): String = suspendCancellableCoroutine { continuation ->
//    // Stop background recording if it is still running.
//    if (genericStream.isRecording) {
//        genericStream.stopRecord()
//    }
//
//    // Define the output path for the replay clip.
//    val folder = PathUtils.getRecordPath()
//    if (!folder.exists()) folder.mkdir()
//    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
//    val replayPath = "${folder.absolutePath}/replay_${sdf.format(Date())}.mp4"
//
//    // Build the FFmpeg command to clip the last `secondsClip` seconds.
////    val command = """-sseof -"$secondsClip" -i "$backgroundRecordPath" -filter_complex '[0:v]setpts=1.25*PTS[v];[0:a]atempo=0.8[a]' -map "[v]" -map "[a]" -c:v h264 -preset ultrafast -tune zerolatency "$replayPath""""
//    val command = "-sseof -\"$secondsClip\" -i \"$backgroundRecordPath\" -c copy \"$replayPath\""
//
//    // Execute the FFmpeg command asynchronously.
//    FFmpegKit.executeAsync(command) { session ->
//        val returnCode = session.returnCode
//        if (ReturnCode.isSuccess(returnCode)) {
//            Log.d("Replay", "Replay saved at $replayPath")
//            // Optionally, delete the original background recording file.
//            File(backgroundRecordPath).delete()
//            // Resume with the replay path once processing is complete.
//            continuation.resume(replayPath)
//        } else {
//            Log.e("Replay", "FFmpegKit failed to clip video, return code: $returnCode")
//            continuation.resumeWithException(Exception("FFmpegKit failed"))
//        }
//    }
//}

//fun handleReplay(context: Context, genericStream: GenericStream, backgroundRecordPath: String, secondsClip: String): String {
//    // Stop background recording if it is still running.
//    if (genericStream.isRecording) {
//        genericStream.stopRecord()
//    }
//
//    // Define the output path for the replay clip.
//    val folder = PathUtils.getRecordPath()
//    if (!folder.exists()) folder.mkdir()
//    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
//    val replayPath = "${folder.absolutePath}/replay_${sdf.format(Date())}.mp4"
//
//    // Build the FFmpeg command to clip the last 10 seconds.
//    // The -sseof -10 flag seeks 10 seconds from the end.
//    val command = "-sseof -\"$secondsClip\" -i \"$backgroundRecordPath\" -c copy \"$replayPath\""
//
//    // Execute the FFmpeg command asynchronously.
//    FFmpegKit.executeAsync(command) { session ->
//        val returnCode = session.returnCode
//        if (ReturnCode.isSuccess(returnCode)) {
//            Log.d("Replay", "Replay saved at $replayPath")
//            // Optionally, delete the original background recording file.
//            File(backgroundRecordPath).delete()
//        } else {
//            Log.e("Replay", "FFmpegKit failed to clip video, return code: $returnCode")
//        }
//    }
//
//    return replayPath
//}
