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
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Range
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.request.ImageRequest
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.data.FirestoreManager
import com.danihg.calypsoapp.data.Team
import com.danihg.calypsoapp.overlays.PlayerEntry
import com.danihg.calypsoapp.overlays.drawOverlay
import com.danihg.calypsoapp.overlays.drawTeamPlayersOverlay
import com.danihg.calypsoapp.services.StreamingService
import com.danihg.calypsoapp.sources.CameraCalypsoSource
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import com.danihg.calypsoapp.utils.AuxButton
import com.danihg.calypsoapp.utils.ColorDropdown
import com.danihg.calypsoapp.utils.ExposureCompensationSlider
import com.danihg.calypsoapp.utils.ExposureModeSelector
import com.danihg.calypsoapp.utils.ExposureSlider
import com.danihg.calypsoapp.utils.IsoSlider
import com.danihg.calypsoapp.utils.ManualWhiteBalanceSlider
import com.danihg.calypsoapp.utils.ModernDropdown
import com.danihg.calypsoapp.utils.OpticalStabilizationModeSelector
import com.danihg.calypsoapp.utils.PathUtils
import com.danihg.calypsoapp.utils.PreventScreenLock
import com.danihg.calypsoapp.utils.ScoreboardActionButtons
import com.danihg.calypsoapp.utils.SectionSubtitle
import com.danihg.calypsoapp.utils.SensorExposureTimeModeSelector
import com.danihg.calypsoapp.utils.SensorExposureTimeSlider
import com.danihg.calypsoapp.utils.ToggleAuxButton
import com.danihg.calypsoapp.utils.WhiteBalanceModeSelector
import com.danihg.calypsoapp.utils.ZoomControls
import com.danihg.calypsoapp.utils.getAvailableAudioCodecs
import com.danihg.calypsoapp.utils.getAvailableVideoCodecs
import com.danihg.calypsoapp.utils.rememberToast
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.encoder.TimestampMode
import com.pedro.encoder.input.gl.render.filters.`object`.GifObjectFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.VideoFileSource
import com.pedro.encoder.utils.gl.TranslateTo
import java.util.Date
import com.pedro.extrasources.CameraUvcSource
import com.pedro.library.base.recording.RecordController
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.pow
import kotlin.math.roundToInt

class CameraViewModel(context: Context) : ViewModel() {
    // Create the camera instance once and keep it here.
    val activeCameraSource: CameraCalypsoSource = CameraCalypsoSource(context)
}

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
    val isServiceRunning by remember { mutableStateOf(false) }
    PreventScreenLock()
    val showToast = rememberToast()

    // Retrieve streaming settings from SharedPreferences.
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
    var videoWidth by remember { mutableIntStateOf(sharedPreferences.getInt("videoWidth", 1920)) }
    var videoHeight by remember { mutableIntStateOf(sharedPreferences.getInt("videoHeight", 1080)) }
    var streamUrl by remember {
        mutableStateOf(
            sharedPreferences.getString("streamUrl", "") ?: ""
        )
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
    var selectedReplayDuration by remember { mutableStateOf("10s") }


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
    var showSettingsSubMenu  by rememberSaveable { mutableStateOf(false) }
    var showCameraSubSettings  by rememberSaveable { mutableStateOf(false) }
    var showTeamPlayersOverlayMenu by rememberSaveable { mutableStateOf(false) }
    var showLineUpOverlay by rememberSaveable { mutableStateOf(false) }
    var showTeamPlayersOverlay by rememberSaveable { mutableStateOf(false) }
    var showReplays by rememberSaveable { mutableStateOf(false) }
    var wasScoreboardActive by remember { mutableStateOf(true) }

    var backgroundRecordPath by remember { mutableStateOf<String?>(null) }


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
    var selectedTeam1 by rememberSaveable { mutableStateOf(if (teams.isNotEmpty()) teams.first().name else "Rivas") }
    var selectedTeam2 by rememberSaveable { mutableStateOf(if (teams.size > 1) teams[1].name else "Alcorcón") }

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


    // Example player data (in a real app, you might load from Firestore, etc.)
    val teamAPlayers = listOf(
        PlayerEntry("1", "John Keeper"),
        PlayerEntry("2", "Chris Defender"),
        PlayerEntry("3", "Alex Midfielder"),
        PlayerEntry("4", "James Forward"),
        // etc., up to 11
    )
    val teamBPlayers = listOf(
        PlayerEntry("1", "Mike Keeper"),
        PlayerEntry("2", "Luke Defender"),
        PlayerEntry("3", "Ryan Midfielder"),
        PlayerEntry("4", "David Forward"),
        // etc.
    )

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

//    var activeCameraSource by remember { mutableStateOf(CameraCalypsoSource(context)) }
    val micSource: MicrophoneSource = remember { MicrophoneSource(MediaRecorder.AudioSource.DEFAULT) }
    val audio: AudioSource = remember { micSource }
    val externalAudio: AudioSource = remember { MicrophoneSource(MediaRecorder.AudioSource.MIC) }
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

    var isoIndex by rememberSaveable { mutableStateOf(0f) }
    val isoOptions = listOf(100, 200, 400, 800, 1600, 3200)
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
    // Define the discrete exposure compensation options.
    val exposureCompOptions = listOf(-2, -1, 0, 1, 2)
    // Start at index 2 (which corresponds to 0 compensation)
    var exposureCompIndex by rememberSaveable { mutableStateOf(2f) }

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
            setTimestampMode(TimestampMode.CLOCK, TimestampMode.BUFFER)
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
//                        if (sensorExposureTimeMode == "MANUAL") {
//                            // Reapply the exact sensor exposure time that was stored.
//                            activeCameraSource.setSensorExposureTime(currentSensorExposureTime)
//                            // Update the slider state accordingly.
//                            exposureLevel = ((currentSensorExposureTime - minSensorExposure).toFloat() / (maxSensorExposure - minSensorExposure))
//                            baseExposureLevel = exposureLevel
//                        } else if (exposureMode == "MANUAL") {
//                            activeCameraSource.setExposure(exposureLevel.toInt())
//                            baseExposureLevel = exposureLevel
//                        } else {
//                            activeCameraSource.enableAutoExposure()
//                        }
//                        if (sensorExposureTimeMode == "MANUAL" && sensorExposureTimeIndex != null) {
//                            val idx = sensorExposureTimeIndex!!.toInt().coerceIn(0, sensorExposureTimeOptions.size - 1)
//                            val sensorTime = sensorExposureTimeOptions[idx].second
//                            activeCameraSource.setSensorExposureTime(sensorTime)
//                            currentSensorExposureTime = sensorTime
//                            // Update the exposureLevel slider value so it reflects the current sensor time:
//                            exposureLevel = ((sensorTime - minSensorExposure).toFloat() / (maxSensorExposure - minSensorExposure))
//                            baseExposureLevel = exposureLevel
//                        } else if (exposureMode == "MANUAL") {
//                            activeCameraSource.setExposure(exposureLevel.toInt())
//                            baseExposureLevel = exposureLevel
//                        } else {
//                            activeCameraSource.enableAutoExposure()
//                        }

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

                        // Reapply sensor exposure time if separately controlled (only if needed)
//                        if (sensorExposureTimeMode == "MANUAL" && sensorExposureTimeIndex != null) {
//                            val idx = sensorExposureTimeIndex!!.toInt().coerceIn(0, sensorExposureTimeOptions.size - 1)
//                            activeCameraSource.setSensorExposureTime(sensorExposureTimeOptions[idx].second)
//                        }

                        // Optionally, reapply exposure compensation if needed:
//                        activeCameraSource.setExposure(exposureCompensation.toInt())
                    }
                    coroutineScope.launch {
                        // Wait until the preview is ready.
                        while (!genericStream.isOnPreview) {
                            delay(100)
                        }
                        // If the scoreboard overlay should be visible, reapply it.
                        if (showScoreboardOverlay && !isStreaming) {
                            genericStream.getGlInterface().clearFilters()
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



    fun startForegroundService() {

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
                if (!genericStream.isStreaming) {
                    genericStream.startStream(streamUrl)
                    isStreaming = true
                }
            }
        } catch (e: SecurityException) {
            Log.e("StreamService", "Failed to start service: ${e.message}")
            // Handle permission error
        }
    }

    // Replace stopForegroundService function
    fun stopForegroundService() {
        val serviceIntent = Intent(context, StreamingService::class.java)
        context.stopService(serviceIntent)

        if (genericStream.isStreaming) {
            genericStream.stopStream()
            isStreaming = false
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
                    surfaceViewRef = surfaceViewRef,
                    context = context,
                    videoSource = activeCameraSource
                )

                // Team Players Overlay.
                Log.d("PixelsWidth", LocalContext.current.resources.displayMetrics.widthPixels.toString())
                Log.d("PixelsHeight", LocalContext.current.resources.displayMetrics.heightPixels.toString())

                TeamPlayersOverlay(
                    visible = showLineUpOverlay && !showTeamPlayersOverlayMenu,
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
                    onLineUpFinished = {
                        showLineUpOverlay = false
                        if (wasScoreboardActive) {
                            CoroutineScope(Dispatchers.Main).launch {
                                genericStream.getGlInterface().clearFilters()
                                showScoreboardOverlay = false
                                delay(50)
                                showScoreboardOverlay = true
                            }
                        }
                    }
                )

                // Scoreboard overlay.
                ScoreboardOverlay(
                    visible = showScoreboardOverlay && !showApplyButton,
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
                                    showScoreboardOverlay = false
                                    showLineUpOverlay = true
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (showReplays) {
                            AuxButton(
                                modifier = Modifier
                                    .size(40.dp)
                                    .zIndex(2f),
                                painter = painterResource(id = R.drawable.ic_replay),
                                onClick = {
//                                    coroutineScope.launch {
//                                        backgroundRecordPath?.let { path ->
//                                            try {
//                                                // Wait for FFmpegKit to finish processing.
//                                                val savedReplay = handleReplaySuspended(context, genericStream, path, selectedReplayDuration)
//                                                val replayUri = Uri.fromFile(File(savedReplay))
//                                                genericStream.changeVideoSource(VideoFileSource(context, replayUri, false) {})
//                                                // Keep the replay on screen for the selected duration.
//                                                val time = selectedReplayDuration.split("s")[0].toLong() * 1000
//                                                delay(time)
//                                                genericStream.changeVideoSource(activeCameraSource)
//                                            } catch (e: Exception) {
//                                                Log.e("Replay", "Error processing replay: ${e.message}")
//                                            }
//                                        } ?: run {
//                                            Log.e("Replay", "No background recording found!")
//                                        }
//                                    }

                                        //ESTO ERA OTRO CODIGO COMENTADO
//                                    coroutineScope.launch {
//                                        backgroundRecordPath?.let { path ->
//                                            val savedReplay = handleReplay(context, genericStream, path, selectedReplayDuration)
//                                            val replayUri = Uri.fromFile(File(savedReplay))
//                                            genericStream.changeVideoSource(VideoFileSource(context, replayUri, false) {})
//                                            val time = selectedReplayDuration.split("s")[0].toLong() * 1000
//                                            delay(time)
//                                            genericStream.changeVideoSource(activeCameraSource)
//                                        } ?: run {
//                                            Log.e("Replay", "No background recording found!")
//                                        }
//                                    }
                                }
                            )
                        }
                    }
                }

                // Right-side action buttons.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(), // Outer container now spans the full width
                    contentAlignment = Alignment.CenterEnd // All children will be placed at the right edge
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 80.dp),  // Keeps the same offset as before
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(50.dp)
                                .zIndex(3f) // Ensure submenu appears above
                        ){
                            // Rocket button (this is the reference position)
                            AuxButton(
                                modifier = Modifier
                                    .size(50.dp)
                                    .align(Alignment.CenterEnd)
                                    .zIndex(2f),
                                painter = painterResource(id = R.drawable.ic_rocket),
                                onClick = {
                                    showApplyButton = !showApplyButton
                                }
                            )
                            // Comment the overlays sumbenu
//                            androidx.compose.animation.AnimatedVisibility(
//                                visible = showOverlaySubMenu,
//                                enter = fadeIn(tween(200)) + slideInHorizontally (initialOffsetX = { it / 2 }),
//                                exit = fadeOut(tween(200)) + slideOutHorizontally (targetOffsetX = { it / 2 }),
//                                modifier = Modifier
//                                    .align(Alignment.CenterEnd)
//                                    .offset(x = (-100).dp) // Adjust offset to align with button
//                            ) {
//                                Row(
//                                    modifier = Modifier
//                                        .wrapContentWidth()
//                                        .padding(6.dp),
//                                    horizontalArrangement = Arrangement.End
//                                ) {
//                                    AuxButton(
//                                        modifier = Modifier.size(40.dp),
//                                        painter = painterResource(id = R.drawable.ic_del_1),
//                                        onClick = {
//                                            showApplyButton = !showApplyButton
//                                        }
//                                    )
//                                }
//                            }
                        }


                        Spacer(modifier = Modifier.height(5.dp))

                        // Streaming button
                        Button(
                            onClick = {
                                if (isStreaming) stopForegroundService() else startForegroundService()
                            },
                            modifier = Modifier
                                .size(70.dp)
                                .border(3.dp, Color.White, CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = CircleShape
                        ) {
                            // Optional button content
                        }

                        Spacer(modifier = Modifier.height(5.dp))

//                        if (isStreaming && backgroundRecordPath == null) {
//                            backgroundRecordPath = startBackgroundRecording(context, genericStream)
//                        }

                        if (isStreaming) {
                            // Recording button
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.5f))
                                    .clickable {
                                        if (!isRecording) {
                                            recordVideoStreaming(context, genericStream) { }
                                            isRecording = true
                                        } else {
                                            recordVideoStreaming(context, genericStream) { }
                                            isRecording = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = if (isRecording) R.drawable.ic_stop else R.drawable.ic_record),
                                    contentDescription = "Button Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(50.dp)
                                .zIndex(3f) // Ensure submenu appears above
                        ) {
                            AuxButton(
                                modifier = Modifier
                                    .size(50.dp)
                                    .align(Alignment.CenterEnd),
                                painter = painterResource(id = R.drawable.ic_settings),
                                onClick = {
                                    showSettingsSubMenu = !showSettingsSubMenu
                                    showCameraSubSettings = false
                                    isSettingsMenuVisible = false
                                    showWhiteBalanceSlider = false
                                    showOpticalVideoStabilization = false
                                    showExposureSlider = false
                                    showExposureCompensationSlider = false
                                    showSensorExposureTimeSlider = false
//                                    showZoomSlider = false
                                }
                            )
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showSettingsSubMenu,
                                enter = fadeIn(tween(200)) + slideInHorizontally (initialOffsetX = { it / 2 }),
                                exit = fadeOut(tween(200)) + slideOutHorizontally (targetOffsetX = { it / 2 }),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = (-100).dp) // Adjust offset to align with button
                            ) {
                                Row(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (!showCameraSubSettings) {
                                        AuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_camera),
                                            onClick = {
                                                showCameraSubSettings = !showCameraSubSettings
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(22.dp))
                                        AuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_record),
                                            onClick = {
                                                isSettingsMenuVisible = !isSettingsMenuVisible
                                            }
                                        )
                                    }
                                    else {
                                        ToggleAuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_mute),
                                            toggled = isMicMuted,
                                            onToggle = {
                                                // Toggle the zoom slider overlay
                                                isMicMuted = !isMicMuted
                                                if(isMicMuted) {
                                                    micSource.mute()
                                                } else {
                                                    micSource.unMute()
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(22.dp))
                                        ToggleAuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_zoom),
                                            toggled = showZoomSlider,
                                            onToggle = {
                                                // Toggle the zoom slider overlay
                                                showZoomSlider = !showZoomSlider
                                                showExposureSlider = false
                                                showWhiteBalanceSlider = false
                                                showOpticalVideoStabilization = false
                                                showExposureCompensationSlider = false
                                                showSensorExposureTimeSlider = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(22.dp))
                                        AuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_iso),
                                            onClick = {
//                                                showZoomSlider = false
                                                showExposureSlider = !showExposureSlider
                                                showWhiteBalanceSlider = false
                                                showOpticalVideoStabilization = false
                                                showExposureCompensationSlider = false
                                                showSensorExposureTimeSlider = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(22.dp))
                                        AuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_wb),
                                            onClick = {
                                                showWhiteBalanceSlider = !showWhiteBalanceSlider
//                                                showZoomSlider = false
                                                showExposureSlider = false
                                                showOpticalVideoStabilization = false
                                                showExposureCompensationSlider = false
                                                showSensorExposureTimeSlider = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(22.dp))
                                        AuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_optical_stabilization),
                                            onClick = {
                                                showOpticalVideoStabilization = !showOpticalVideoStabilization
//                                                showZoomSlider = false
                                                showExposureSlider = false
                                                showWhiteBalanceSlider = false
                                                showExposureCompensationSlider = false
                                                showSensorExposureTimeSlider = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(22.dp))
                                        AuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_exposure_compensation),
                                            onClick = {
                                                showExposureCompensationSlider = !showExposureCompensationSlider
//                                                showZoomSlider = false
                                                showExposureSlider = false
                                                showWhiteBalanceSlider = false
                                                showOpticalVideoStabilization = false
                                                showSensorExposureTimeSlider = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(22.dp))
                                        AuxButton(
                                            modifier = Modifier.size(40.dp),
                                            painter = painterResource(id = R.drawable.ic_exposure_time),
                                            onClick = {
                                                showSensorExposureTimeSlider = !showSensorExposureTimeSlider
//                                                showZoomSlider = false
                                                showExposureSlider = false
                                                showWhiteBalanceSlider = false
                                                showOpticalVideoStabilization = false
                                                showExposureCompensationSlider = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showZoomSlider) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 50.dp)) {
                            // Assume zoomLevel is a mutable state.
                            ZoomControls(
                                zoomLevel = zoomLevel,
                                onZoomDelta = { delta ->
                                    // Update the zoom level smoothly and ensure it stays within your bounds (e.g. 1f to 5f)
                                    zoomLevel = (zoomLevel + delta).coerceIn(1f, 5f)
                                    activeCameraSource.setZoom(zoomLevel)
                                },
                                modifier = Modifier
                                    .padding(start = 40.dp, top = 50.dp) // adjust padding as needed
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }

                    // Inside your Scaffold's root Box (or similar), add this block:
                    if (showExposureSlider) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 100.dp) // Adjust so it sits above your ic_add buttons.
                                    .width(screenWidth * 0.7f) // 70% of the screen width.
                            ) {
                                ExposureModeSelector(
                                    selectedMode = exposureMode,
                                    onModeChange = { newMode ->
                                        exposureMode = newMode
                                        if (newMode == "AUTO") {
                                            activeCameraSource.setIsoAuto()
                                            activeCameraSource.enableAutoExposure()
                                            val isAutoExposure = activeCameraSource.isAutoExposureEnabled()
                                            activeCameraSource.setExposure(defaultExposure)
                                            exposureLevel = defaultExposure.toFloat()
                                            Log.d("ExposureCheck", "Auto exposure enabled? $isAutoExposure")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (exposureMode == "MANUAL") {
                                    LaunchedEffect(isoIndex) {
                                        activeCameraSource.disableAutoExposure()
                                        val selectedISO = isoOptions[isoIndex.toInt()]
                                        activeCameraSource.setSensorSensitivity(selectedISO)
                                    }
                                    IsoSlider(
                                        isoValue = isoSliderValue,
                                        onValueChange = { newValue -> isoSliderValue = newValue },
                                        updateSensorSensitivity = { newIso ->
                                            activeCameraSource.setSensorSensitivity(newIso)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    // White balance controls UI:
                    if (showWhiteBalanceSlider) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 100.dp)
                                    .width(screenWidth * 0.7f)
                            ) {
                                // White Balance Mode Selector
                                WhiteBalanceModeSelector(
                                    selectedMode = whiteBalanceMode,
                                    onModeChange = { newMode ->
                                        whiteBalanceMode = newMode
                                        if (newMode == "AUTO") {
                                            // Update the camera immediately with auto mode.
                                            activeCameraSource.setWhiteBalance(CaptureRequest.CONTROL_AWB_MODE_AUTO)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Show the manual slider only in MANUAL mode.
                                if (whiteBalanceMode == "MANUAL") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    ManualWhiteBalanceSlider(
                                        temperature = manualWhiteBalanceTemperature,
                                        onValueChange = { newTemperature ->
                                            manualWhiteBalanceTemperature = newTemperature
                                            // Update the camera with manual white balance settings.
                                            activeCameraSource.setManualWhiteBalance(newTemperature)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    if (showOpticalVideoStabilization) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 100.dp)
                                    .width(screenWidth * 0.7f)
                            ) {
                                OpticalStabilizationModeSelector(
                                    selectedMode = opticalVideoStabilizationMode,
                                    onModeChange = { newMode ->
                                        opticalVideoStabilizationMode = newMode
                                        when (newMode) {
                                            "ENABLE" -> {
                                                activeCameraSource.enableOpticalVideoStabilization()
                                            }
                                            "DISABLE" -> {
                                                activeCameraSource.disableOpticalVideoStabilization()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    // New overlay: Exposure Compensation Slider.
                    if (showExposureCompensationSlider) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp
                        // Use the helper to determine if compensation should be enabled.
                        val isExposureCompAvailable = activeCameraSource.isExposureCompensationAvailable()

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 100.dp)
                                    .width(screenWidth * 0.7f)
                            ) {
                                if (isExposureCompAvailable) {
                                    Text(
                                        text = "Exposure Compensation",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    ExposureCompensationSlider(
                                        // The slider shows discrete values -2, -1, 0, 1, 2.
                                        // It will update the compensation value on drag (or when released, depending on your slider implementation)
                                        compensation = activeCameraSource.getExposureCompensationManual(),
                                        onValueChange = { newComp ->
                                            activeCameraSource.setExposureCompensationManual(newComp)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    // Optionally, you can show a label displaying the current compensation value:
                                    Text(
                                        text = "EV: ${activeCameraSource.getExposureCompensationManual()}",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Text(
                                        text = "Exposure compensation is disabled\nbecause both ISO and sensor exposure time are set to manual.",
                                        color = Color.Gray,
                                        fontSize = 16.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // New overlay: Sensor Exposure Time Slider.
                    if (showSensorExposureTimeSlider) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 100.dp)
                                    .width(screenWidth * 0.7f)
                            ) {
                                // Sensor Exposure Time mode selector (Auto/Manual)
                                SensorExposureTimeModeSelector(
                                    selectedMode = sensorExposureTimeMode,
                                    onModeChange = { newMode ->
                                        sensorExposureTimeMode = newMode
                                        if (newMode == "AUTO") {
                                            activeCameraSource.setSensorExposureAuto()
                                            sensorExposureTimeIndex = null
                                            // Re-enable auto exposure (letting the camera decide the sensor exposure time)
                                            activeCameraSource.enableAutoExposure()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (sensorExposureTimeMode == "MANUAL") {
                                    // Determine the slider value (if no manual change yet, use default)
                                    val sliderValue = sensorExposureTimeIndex ?: defaultSensorExposureIndex
                                    Text(
                                        text = "Sensor Exposure Time: ${sensorExposureTimeOptions[sliderValue.toInt()].first}",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    SensorExposureTimeSlider(
                                        index = sliderValue,
                                        onValueChange = { newIndex ->
                                            sensorExposureTimeIndex = newIndex
                                            val idx = newIndex.toInt().coerceIn(0, sensorExposureTimeOptions.size - 1)
                                            val newSensorTime = sensorExposureTimeOptions[idx].second
                                            activeCameraSource.setSensorExposureTime(newSensorTime)
                                            // Store the exact value for later reapplication.
                                            currentSensorExposureTime = newSensorTime
                                            // Update the normalized exposure slider value.
                                            exposureLevel = ((newSensorTime - minSensorExposure).toFloat() / (maxSensorExposure - minSensorExposure))
                                            baseExposureLevel = exposureLevel
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        exposureOptions = sensorExposureTimeOptions
                                    )
                                } else {
                                    // In auto mode, display the auto label.
                                    Text(
                                        text = "Sensor Exposure Time: AUTO",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
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
                    streamUrl = streamUrl,
                    onStreamUrlChange = { streamUrl = it },
                    availableVideoCodecs = getAvailableVideoCodecs(),
                    availableAudioCodecs = getAvailableAudioCodecs(),
                    onApply = {
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
                                    "2k" -> {
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
                                    putString("streamUrl", streamUrl)
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
                OverlayMenu(
                    visible = showApplyButton,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    teams = teams,  // Pass the list of teams from Firestore.
                    selectedTeam1 = selectedTeam1,
                    onTeam1Change = { selectedTeam1 = it },
                    selectedTeam2 = selectedTeam2,
                    onTeam2Change = { selectedTeam2 = it },
                    onLeftLogoUrlChange = { leftLogoUrl = it }, // Save the new left logo URL
                    onRightLogoUrlChange = { rightLogoUrl = it }, // Save the new right logo URL
                    showScoreboardOverlay = showScoreboardOverlay,
                    onToggleScoreboard = { showScoreboardOverlay = it },
                    selectedLeftColor = leftTeamColor,
                    onLeftColorChange = { leftTeamColor = it },
                    selectedRightColor = rightTeamColor,
                    onRightColorChange = { rightTeamColor = it },
                    selectedTeamsOverlayDuration = selectedTeamsOverlayDuration,
                    onTeamsOverlayDurationChange = { selectedTeamsOverlayDuration = it },
                    showLineUpOverlay = showTeamPlayersOverlay,
                    onToggleLineUp = { showTeamPlayersOverlay = it },
                    showReplays = showReplays,
                    onToggleReplays = { showReplays = it },
                    selectedReplaysDuration = selectedReplayDuration,
                    onReplaysDurationChange = { selectedReplayDuration = it },
                    onClose = {
                        showApplyButton = false
                        showOverlaySubMenu = false
                    }
                )

                // Auxiliary Teams Overlay Menu
                OverlayTeamsMenu(
                    visible = showTeamPlayersOverlayMenu,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    showTeamsOverlay = showTeamPlayersOverlay,
                    selectedTeamsOverlayDuration = selectedTeamsOverlayDuration,
                    onTeamsOverlayDurationChange = { selectedTeamsOverlayDuration = it },
                    onToggleTeamsOverlay = {
                        showTeamPlayersOverlay = it
                    },
                    onClose = {
                        showTeamPlayersOverlayMenu = false
                        if (showTeamPlayersOverlay) {
                            showScoreboardOverlay = false
                        }
                    }
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

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreview(
    genericStream: GenericStream,
    surfaceViewRef: MutableState<SurfaceView?>,
    context: Context,
    videoSource: CameraCalypsoSource
) {

    // Cache the "fingerSpacing" field reference to avoid reflection overhead per event.
    val fingerSpacingField = remember {
        try {
            videoSource.javaClass.getDeclaredField("fingerSpacing").apply { isAccessible = true }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {


                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                surfaceViewRef.value = this

                holder.setFormat(PixelFormat.TRANSLUCENT)

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
    isStreaming: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    selectedCameraSource: String,
    onCameraSourceChange: (String) -> Unit,
    selectedAudioSource: String,
    onAudioSourceChange: (String) -> Unit,
    selectedVideoEncoder: String,
    onVideoEncoderChange: (String) -> Unit,
    selectedAudioEncoder: String,
    onAudioEncoderChange: (String) -> Unit,
    selectedBitrate: Int,
    onBitrateChange: (Int) -> Unit,
    selectedFPS: String,
    onFPSChange: (String) -> Unit,
    selectedResolution: String,
    onResolutionChange: (String) -> Unit,
    streamUrl: String,
    onStreamUrlChange: (String) -> Unit,
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
                    .height(cameraPreviewHeight)
                    .padding(horizontal = horizontalPadding)
                    .background(Gray.copy(alpha = 0.92f), shape = RoundedCornerShape(16.dp))
                    .border(2.dp, Color.White, shape = RoundedCornerShape(16.dp))
                    .align(Alignment.Center)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp))
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(
                        color = Color.White.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    SectionSubtitle("Camera Source")
                    ModernDropdown(
                        items = listOf("Device Camera", "USB Camera"),
                        selectedValue = selectedCameraSource,
                        displayMapper = { it },
                        onValueChange = onCameraSourceChange,
                        enabled = !isStreaming
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionSubtitle("Audio Source")
                    ModernDropdown(
                        items = listOf("Device Audio", "USB Mic"),
                        selectedValue = selectedAudioSource,
                        displayMapper = { it },
                        onValueChange = onAudioSourceChange,
                        enabled = !isStreaming
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Streaming Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(
                        color = Color.White.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    SectionSubtitle("Video Encoder")
                    ModernDropdown(
                        items = availableVideoCodecs,
                        selectedValue = selectedVideoEncoder,
                        displayMapper = { it },
                        onValueChange = onVideoEncoderChange,
                        enabled = !isStreaming
                    )
                    SectionSubtitle("Audio Encoder")
                    ModernDropdown(
                        items = availableAudioCodecs,
                        selectedValue = selectedAudioEncoder,
                        displayMapper = { it },
                        onValueChange = onAudioEncoderChange,
                        enabled = !isStreaming
                    )
                    SectionSubtitle("Bitrate")
                    ModernDropdown(
                        items = listOf(3000 * 1000, 5000 * 1000, 7000 * 1000, 10000 * 1000, 12000 * 1000, 15000 * 1000, 20000 * 1000, 25000 * 1000, 30000 * 1000, 35000 * 1000, 40000 * 1000, 45000 * 1000),
                        selectedValue = selectedBitrate,
                        displayMapper = { "${it / 1000 / 1000} Mbps" },
                        onValueChange = onBitrateChange
                    )
                    SectionSubtitle("Stream FPS")
                    ModernDropdown(
                        items = if (selectedCameraSource == "USB Camera") listOf("30", "60") else listOf("30", "60"),
                        selectedValue = selectedFPS,
                        displayMapper = { it },
                        onValueChange = onFPSChange,
                        enabled = !isStreaming
                    )
                    SectionSubtitle("Resolution")
                    ModernDropdown(
                        items = listOf("1080p", "720p", "2k"),
                        selectedValue = selectedResolution,
                        displayMapper = { it },
                        onValueChange = onResolutionChange,
                        enabled = !isStreaming
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // New field for the Stream URL.
                    SectionSubtitle("Stream URL")
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = onStreamUrlChange,
                        label = { Text("Stream URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isStreaming,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CalypsoRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = CalypsoRed,
                            unfocusedLabelColor = Color.Gray
                        )
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
                        Text("Apply", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayTeamsMenu(
    visible: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    selectedTeamsOverlayDuration: String,
    onTeamsOverlayDurationChange: (String) -> Unit,
    showTeamsOverlay: Boolean,
    onToggleTeamsOverlay: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        val cameraPreviewHeight = screenHeight
        val cameraPreviewWidth = cameraPreviewHeight * (16f / 9f)
        val horizontalPadding = (screenWidth - cameraPreviewWidth) / 2

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(cameraPreviewWidth)
                    .height(cameraPreviewHeight)
                    .padding(horizontal = horizontalPadding)
                    .background(Gray.copy(alpha = 0.92f), shape = RoundedCornerShape(16.dp))
                    .border(2.dp, Color.White, shape = RoundedCornerShape(16.dp))
                    .align(Alignment.Center)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close Button (X)
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

                    // Scoreboard Toggle (Header)
                    Text(
                        text = "Teams Overlay Configuration",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Teams",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = showTeamsOverlay,
                            onCheckedChange = onToggleTeamsOverlay,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CalypsoRed,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Team Selection Layout (Side by Side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Duration")
                            ModernDropdown(
                                items = listOf("5s", "10s", "15s", "20s", "25s", "30s"),
                                selectedValue = selectedTeamsOverlayDuration,
                                displayMapper = { it },
                                onValueChange = {
                                    onTeamsOverlayDurationChange(it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

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
            genericStream.getGlInterface().clearFilters()
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

@Composable
fun OverlayMenu(
    visible: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    teams: List<Team>,
    selectedTeam1: String,
    onTeam1Change: (String) -> Unit,
    selectedTeam2: String,
    onTeam2Change: (String) -> Unit,
    onLeftLogoUrlChange: (String?) -> Unit, // New parameter to update logo URL
    onRightLogoUrlChange: (String?) -> Unit, // New parameter to update logo URL
    showScoreboardOverlay: Boolean,
    onToggleScoreboard: (Boolean) -> Unit,
    selectedTeamsOverlayDuration: String,
    onTeamsOverlayDurationChange: (String) -> Unit,
    showLineUpOverlay: Boolean,
    onToggleLineUp: (Boolean) -> Unit,
    showReplays: Boolean,
    onToggleReplays: (Boolean) -> Unit,
    selectedReplaysDuration: String,
    onReplaysDurationChange: (String) -> Unit,
    selectedLeftColor: String,
    onLeftColorChange: (String) -> Unit,
    selectedRightColor: String,
    onRightColorChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val colorOptions = listOf(
        "Black" to Color.Black,
        "Blue" to Color.Blue,
        "Cyan" to Color.Cyan,
        "Green" to Color.Green,
        "Orange" to Color(0xFFFFA500),
        "Pink" to Color(0xFFFF69B4),
        "Purple" to Color(0xFF800080),
        "Red" to Color.Red,
        "Soft Blue" to Color(0xFF87CEFA),
        "White" to Color.White,
        "Yellow" to Color.Yellow
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        val cameraPreviewHeight = screenHeight
        val cameraPreviewWidth = cameraPreviewHeight * (16f / 9f)

        // Center horizontally.
        val horizontalPadding = (screenWidth - cameraPreviewWidth) / 2

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(cameraPreviewWidth)
                    .height(cameraPreviewHeight)
                    .padding(horizontal = horizontalPadding)
                    .background(Gray.copy(alpha = 0.92f), shape = RoundedCornerShape(16.dp))
                    .border(2.dp, Color.White, shape = RoundedCornerShape(16.dp))
                    .align(Alignment.Center)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close Button (X)
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

                    // Scoreboard Toggle (Header)
                    Text(
                        text = "Scoreboard Configuration",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Scoreboard",
                            fontSize = 18.sp,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Team Selection Layout (Side by Side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Team 1")
                            ModernDropdown(
                                items = teams.map { it.name },
                                selectedValue = selectedTeam1,
                                displayMapper = { it },
                                onValueChange = {
                                    onTeam1Change(it)
                                    val team = teams.find { team -> team.name == it }
                                    onLeftLogoUrlChange(team?.logo) // Save the new logo URL
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Team 2")
                            ModernDropdown(
                                items = teams.map { it.name },
                                selectedValue = selectedTeam2,
                                displayMapper = { it },
                                onValueChange = {
                                    onTeam2Change(it)
                                    val team = teams.find { team -> team.name == it }
                                    onRightLogoUrlChange(team?.logo) // Save the new logo URL
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Selection Layout (Side by Side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Left Color")
                            ColorDropdown(
                                colorOptions = colorOptions,
                                selectedColorName = selectedLeftColor,
                                onColorChange = onLeftColorChange
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Right Color")
                            ColorDropdown(
                                colorOptions = colorOptions,
                                selectedColorName = selectedRightColor,
                                onColorChange = onRightColorChange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Line Up Config
                    Text(
                        text = "Line Up Configuration",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Line Up",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = showLineUpOverlay,
                            onCheckedChange = onToggleLineUp,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CalypsoRed,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Line Up Duration",
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.align(
                                Alignment.CenterVertically
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ModernDropdown(
                            items = listOf("5s", "10s", "15s", "20s", "25s", "30s"),
                            selectedValue = selectedTeamsOverlayDuration,
                            displayMapper = { it },
                            onValueChange = {
                                onTeamsOverlayDurationChange(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Replays Config
                    Text(
                        text = "Replays Configuration",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Replays",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = showReplays,
                            onCheckedChange = onToggleReplays,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CalypsoRed,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Replays Duration",
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.align(
                                Alignment.CenterVertically
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ModernDropdown(
                            items = listOf("5s", "10s", "15s", "20s"),
                            selectedValue = selectedReplaysDuration,
                            displayMapper = { it },
                            onValueChange = {
                                onReplaysDurationChange(it)
                            }
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
