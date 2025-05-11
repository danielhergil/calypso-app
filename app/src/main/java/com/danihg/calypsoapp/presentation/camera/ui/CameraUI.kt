package com.danihg.calypsoapp.presentation.camera.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.presentation.camera.ui.components.BatteryIndicator
import com.danihg.calypsoapp.sources.CameraCalypsoSource
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.utils.AuxButton
import com.danihg.calypsoapp.utils.AuxButtonSquare
import com.danihg.calypsoapp.utils.ExposureCompensationSlider
import com.danihg.calypsoapp.utils.IsoSlider
import com.danihg.calypsoapp.utils.SensorExposureTimeSlider
import com.danihg.calypsoapp.utils.ToggleAuxButtonSquare
import com.danihg.calypsoapp.utils.ZoomControls
import com.danihg.calypsoapp.utils.rememberToast
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraUI(
    onShowApplyButton: () -> Unit,
    streamUrl: String,
    onStartStreaming: () -> Unit,
    isStreaming: Boolean,
    onRecordWhileStreaming: () -> Unit,
    isRecording: Boolean,
    showZoomSlider: Boolean,
    onToggleShowZoomSlider: () -> Unit,
    isMicMuted: Boolean,
    onToggleVolumeMenuVisible: () -> Unit,
    isVolumeMenuVisible: Boolean,
    onToggleMuteUnmute: () -> Unit,
    onToggleSettingsMenu: () -> Unit,
    showSettingsSubMenu: Boolean,
    showCameraModeSelection: Boolean,
    showManualSubMenu: Boolean,
    showAutoSubMenu: Boolean,
    onShowSettingsCameraMenu: () -> Unit,
    onShowSettingsStreamMenu: () -> Unit,
    onShowSettingsCameraManualMenu: () -> Unit,
    onShowSettingsCameraAutoMenu: () -> Unit,
    onCameraSettingsManualAutoBack: () -> Unit,
    onShowIsoSlider: () -> Unit,
    onShowSensorExposureTimeSlider: () -> Unit,
    onCameraManualModeBack: () -> Unit,
    onShowExposureCompensationSlider: () -> Unit,
    onCameraAutoModeBack: () -> Unit,
    zoomLevel: Float,
    onZoomLevelFunction: (Float) -> Unit,
    micSource: MicrophoneSource,
    externalAudioSource: MicrophoneSource,
    showIsoSlider: Boolean,
    isoSliderValue: Float,
    onIsoSliderValue: (Float) -> Unit,
    onUpdateSensorSensitivity: (Int) -> Unit,
    onIsoSliderBack: () -> Unit,
    showSensorExposureTimeSlider: Boolean,
    sensorExposureTimeMode: String,
    sensorExposureTimeIndex: Float?,
    defaultSensorExposureIndex: Float,
    sensorExposureTimeOptions: List<Pair<String, Long>>,
    onSensorExposureTimeSliderChange: (Float) -> Unit,
    onSensorExposureTimeSliderBack: () -> Unit,
    showExposureCompensationSlider: Boolean,
    activeCameraSource: CameraCalypsoSource,
    onExposureCompensationBack: () -> Unit,
    onStartRecord: () -> Unit,
    onTakePicture: () -> Unit,
    teamsSelected: Boolean,
    showLineUpOverlay: Boolean,
    showScoreboardOverlay: Boolean,
    onToggleLineUpOverlay: () -> Unit,
    onToggleScoreboardOverlay: () -> Unit,
    showReplayMenuBtn: Boolean,
    onTogglePlayReplay: () -> Unit,
    onToggleSaveReplay: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val screenWidthDpReference = 788.dp
    var paddingFix = 0.dp
    if (screenWidthDpReference < screenWidthDp) {
        paddingFix = (screenWidthDp - screenWidthDpReference) / 2
    }

    val coroutineScope = rememberCoroutineScope()
    // State to show the waiting (progress) indicator after stop
    val isWaiting = remember { mutableStateOf(false) }

    val showToast = rememberToast()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOverlayMenu by remember { mutableStateOf(false) }
    var showReplayMenu  by remember { mutableStateOf(false) }

    Log.d("CameraUI", "screenWidthDp: $screenWidthDp, screenHeightDp: $screenHeightDp")
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(start = paddingFix, end = paddingFix),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .background(Color.Black)
                .align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier
                    .size(55.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = CircleShape,
                onClick = {
                    onTakePicture()
//                    if (isStreaming) stopForegroundService() else startForegroundService()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_picture_mode),
                    contentDescription = "Button Icon",
                    tint = Color.Gray,
                    modifier = Modifier.size(30.dp)
                )
            }

            val validStreamUrl = streamUrl.isNotBlank() &&
                    (streamUrl.startsWith("rtmp://") || streamUrl.startsWith("rtmps://")) &&
                    !streamUrl.contains(" ") &&
                    streamUrl.split("/").filter { it.isNotBlank() }.size >= 4

            Log.d("CameraUI", "validStreamUrl: $streamUrl")

            Button(
                modifier = Modifier
                    .size(60.dp),
                contentPadding = PaddingValues(0.dp),
                enabled = validStreamUrl,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = CircleShape,
                onClick = {
                    onStartStreaming()
//                    if (isStreaming) stopForegroundService() else startForegroundService()
                }
            ) {
                Icon(
                    painter = if (isStreaming)
                        painterResource(id = R.drawable.ic_stop)
                    else
                        painterResource(id = R.drawable.ic_stream_mode),
                    contentDescription = "Button Icon",
                    tint = if (isStreaming)
                        Color.Red
                    else
                        Color.Gray,
                    modifier = Modifier.size(30.dp)
                )
            }

            Button(
                modifier = Modifier
                    .size(55.dp),
                contentPadding = PaddingValues(0.dp),
                enabled = !isWaiting.value,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = CircleShape,
                onClick = {
                    if (isRecording) {
                        onStartRecord()
                        isWaiting.value = true
                        coroutineScope.launch {
                            delay(2000L) // 5 seconds delay before re-enabling the record button.
                            isWaiting.value = false
                            snackbarHostState.showSnackbar("✅ Video Saved Successfully")
                        }
                    }
                    else {
                        onStartRecord()
                        isWaiting.value = true
                        coroutineScope.launch {
                            delay(2000L) // 5 seconds delay before re-enabling the record button.
                            isWaiting.value = false
                        }
                    }
//                    if (isStreaming) stopForegroundService() else startForegroundService()
                }
            ) {
                if (isWaiting.value) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp), color = CalypsoRed)
                } else {
                    Icon(
                        painter = if (isRecording)
                            painterResource(id = R.drawable.ic_stop)
                        else
                            painterResource(id = R.drawable.ic_record_mode),
                        contentDescription = "Record Button Icon",
                        tint = if (isRecording) Color.Red else Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
//            ModeButton(
//                modifier = Modifier.padding(top = 40.dp),
//                painter = painterResource(id = R.drawable.ic_stream_mode),
//                toggled = isStreamMode,
//                onToggle = { onToggleStreamMode() }
//            )
//            ModeButton(
//                modifier = Modifier,
//                painter = painterResource(id = R.drawable.ic_record_mode),
//                toggled = isRecordMode,
//                onToggle = { onToggleRecordMode() }
//            )
//            ModeButton(
//                modifier = Modifier.padding(bottom = 40.dp),
//                painter = painterResource(id = R.drawable.ic_picture_mode),
//                toggled = isPictureMode,
//                onToggle = { onTogglePictureMode() }
//            )
//        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 110.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(50.dp)
//                    .zIndex(3f)
            ) {
                // Place the battery indicator with an upward offset so it appears above the rocket button.
                BatteryIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = (-50).dp)  // Adjust this value as needed
                        .zIndex(3f)            // Higher zIndex so it's drawn on top
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .zIndex(3f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    AuxButton(
                        modifier = Modifier.size(50.dp),
                        painter = painterResource(id = R.drawable.ic_overlay_fast_menu),
                        onClick = { showOverlayMenu = !showOverlayMenu }
                    )

//                    Spacer(modifier = Modifier.width(12.dp))
//                    AuxButton(
//                        modifier = Modifier.size(50.dp),
//                        painter = painterResource(id = R.drawable.ic_rocket),
//                        onClick = { onShowApplyButton() }
//                    )
                }
                if (showReplayMenuBtn && isStreaming) {
                    AuxButton(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.CenterEnd)    // same horizontal alignment as the first
                            .offset(y = 60.dp),            // pushes it down 60dp (50dp height + 10dp spacing)
                        painter = painterResource(id = R.drawable.ic_rocket),
                        onClick = { showReplayMenu = !showReplayMenu }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showReplayMenu && isStreaming,
                    enter = slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(200)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        // moves the menu left of the rocket button, and down to match
                        .offset(x = (-60).dp, y = 60.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // put whatever extra buttons you want here
                        AuxButtonSquare(
                            modifier = Modifier.size(40.dp),
                            painter = painterResource(id = R.drawable.ic_view_replay),
                            onClick = { onTogglePlayReplay() }
                        )
                        AuxButtonSquare(
                            modifier = Modifier.size(40.dp),
                            painter = painterResource(id = R.drawable.ic_save_replay),
                            iconModifier = Modifier.fillMaxSize().scale(0.7f),
                            onClick = { onToggleSaveReplay() }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showOverlayMenu,
                    enter = slideInHorizontally(
                        // start fully off to the right of its final position
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutHorizontally(
                        // slide back to the right when hiding
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        // shift it left of the overlay button:
                        // 2 toggles × 50.dp each + 1 spacer of 12.dp = 112.dp
                        .offset(x = (-60).dp)
                ) {
                    Row(
                        modifier = Modifier
//                            .background(Color.Black.copy(alpha = 0.8f), shape = CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if(teamsSelected) {
                            ToggleAuxButtonSquare(
                                modifier = Modifier.size(40.dp),
                                painter = painterResource(id = R.drawable.ic_scoreboard),
                                toggled = showScoreboardOverlay,
                                onToggle = { onToggleScoreboardOverlay() },
                                toggledColor = CalypsoRed.copy(alpha = 0.7f)
                            )
                            ToggleAuxButtonSquare(
                                modifier = Modifier.size(40.dp),
                                painter = painterResource(id = R.drawable.ic_lineup),
                                toggled = showLineUpOverlay,
                                onToggle = { onToggleLineUpOverlay() },
                                toggledColor = CalypsoRed.copy(alpha = 0.7f)
                            )
                        }
                        AuxButtonSquare(
                            modifier = Modifier.size(40.dp),
                            painter = painterResource(id = R.drawable.ic_settings),
                            iconModifier = Modifier.fillMaxSize().scale(0.7f),
                            onClick = {
                                onShowApplyButton()
//                            isVolumeMenuVisible = !isVolumeMenuVisible
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

//            if (isStreamMode) {
//            val validStreamUrl = streamUrl.isNotBlank() &&
//                    (streamUrl.startsWith("rtmp://") || streamUrl.startsWith("rtmps://")) &&
//                    !streamUrl.contains(" ") &&
//                    streamUrl.split("/").filter { it.isNotBlank() }.size >= 4
//
//            Button(
//                modifier = Modifier
//                    .size(70.dp)
//                    .border(3.dp, Color.White, CircleShape),
//                enabled = validStreamUrl,
//                colors = if (validStreamUrl)
//                    ButtonDefaults.buttonColors(containerColor = Color.Red)
//                else
//                    ButtonDefaults.buttonColors(containerColor = Color.Gray),
//                shape = CircleShape,
//                onClick = {
//                    onStartStreaming()
////                    if (isStreaming) stopForegroundService() else startForegroundService()
//                }
//            ) {}
//            }
//            if (isRecordMode) {
//                Button(
//                    modifier = Modifier
//                        .size(70.dp)
//                        .border(15.dp, Color.White, CircleShape),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
//                    shape = CircleShape,
//                    onClick = {
//                        onStartRecording()
////                    if (isStreaming) stopForegroundService() else startForegroundService()
//                    }
//                ) {}
//            }
//            if (isPictureMode) {
//                Button(
//                    modifier = Modifier
//                        .size(70.dp)
//                        .border(3.dp, Color.White, CircleShape),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
//                    shape = CircleShape,
//                    onClick = {
//                        onTakePicture()
////                    if (isStreaming) stopForegroundService() else startForegroundService()
//                    }
//                ) {}
//            }

//            Spacer(modifier = Modifier.height(5.dp))
//
//            if (isStreaming) {
//                // Recording button
//                Box(
//                    modifier = Modifier
//                        .size(50.dp)
//                        .clip(CircleShape)
//                        .background(Color.Red.copy(alpha = 0.5f))
//                        .clickable {
//                            onRecordWhileStreaming()
////                            if (!isRecording) {
////                                recordVideoStreaming(context, genericStream) { }
////                                isRecording = true
////                            } else {
////                                recordVideoStreaming(context, genericStream) { }
////                                isRecording = false
////                            }
//                        },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        painter = painterResource(id = if (isRecording) R.drawable.ic_stop else R.drawable.ic_record),
//                        contentDescription = "Button Icon",
//                        tint = Color.White,
//                        modifier = Modifier.size(30.dp)
//                    )
//                }
//            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp)
                    .size(60.dp)
                    .zIndex(3f)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToggleAuxButtonSquare(
                        modifier = Modifier.size(50.dp),
                        painter = painterResource(id = R.drawable.ic_zoom),
                        toggled = showZoomSlider,
                        onToggle = {
                            onToggleShowZoomSlider()
//                            showZoomSlider = !showZoomSlider
                        }
                    )
                    AuxButtonSquare(
                        modifier = Modifier.size(50.dp),
                        iconModifier = Modifier.fillMaxSize().scale(0.9f),
                        painter = painterResource(id = if (isMicMuted) R.drawable.ic_volume_off else R.drawable.ic_volume),
                        onClick = {
                            onToggleVolumeMenuVisible()
//                            isVolumeMenuVisible = !isVolumeMenuVisible
                        }
                    )
                    if (isVolumeMenuVisible) {
                        ToggleAuxButtonSquare(
                            modifier = Modifier.size(50.dp),
                            painter = painterResource(id = R.drawable.ic_mute_letters),
                            toggled = isMicMuted,
                            onToggle = {
                                onToggleMuteUnmute()
//                                isMicMuted = !isMicMuted
//                                if(isMicMuted) {
//                                    micSource.mute()
//                                    externalAudioSource.mute()
//                                } else {
//                                    micSource.unMute()
//                                    externalAudioSource.unMute()
//                                }
                            }
                        )
                    }
                }

                AuxButton(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterEnd),
                    painter = painterResource(id = R.drawable.ic_settings),
                    onClick = {
                        onToggleSettingsMenu()
//                        showSettingsSubMenu = !showSettingsSubMenu
//                        // Reset all nested menus when toggling settings
//                        showCameraModeSelection = false
//                        showManualSubMenu = false
//                        showAutoSubMenu = false
//                        showIsoSlider = false
//                        showExposureCompensationSlider = false
//                        showExposureSlider = false
//                        showSensorExposureTimeSlider = false
                    }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showSettingsSubMenu && !showCameraModeSelection && !showManualSubMenu && !showAutoSubMenu,
                    enter = fadeIn(tween(200)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                    exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-100).dp)
                ) {
                    Row(
                        modifier = Modifier.wrapContentWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(1.1f),
                            painter = painterResource(id = R.drawable.ic_settings_camera),
                            onClick = {
                                onShowSettingsCameraMenu()
//                                showCameraModeSelection = !showCameraModeSelection
//                                // Reset any lower-level menus
//                                showManualSubMenu = false
//                                showAutoSubMenu = false
//                                showIsoSlider = false
//                                showExposureCompensationSlider = false
//                                showExposureSlider = false
//                                showSensorExposureTimeSlider = false
                            }
                        )
                        Spacer(modifier = Modifier.width(22.dp))
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(1.1f),
                            painter = painterResource(id = R.drawable.ic_settings_stream),
                            onClick = {
                                onShowSettingsStreamMenu()
//                                isSettingsMenuVisible = !isSettingsMenuVisible
                            }
                        )
                    }
                }

                // Camera mode selection: Manual or Auto
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCameraModeSelection,
                    enter = fadeIn(tween(200)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                    exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-100).dp)
                ) {
                    Row(
                        modifier = Modifier.wrapContentWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(1.1f),
                            painter = painterResource(id = R.drawable.ic_camera_settings_manual),
                            onClick = {
                                onShowSettingsCameraManualMenu()
//                                cameraMode = "MANUAL"
//                                showCameraModeSelection = false
//                                showManualSubMenu = true
//                                showAutoSubMenu = false
                            }
                        )
                        Spacer(modifier = Modifier.width(22.dp))
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(1.1f),
                            painter = painterResource(id = R.drawable.ic_camera_settings_auto),
                            onClick = {
                                onShowSettingsCameraAutoMenu()
//                                cameraMode = "AUTO"
//                                showCameraModeSelection = false
//                                showAutoSubMenu = true
//                                showManualSubMenu = false
//
//                                activeCameraSource.setIsoAuto()
//                                activeCameraSource.enableAutoExposure()
//                                val isAutoExposure = activeCameraSource.isAutoExposureEnabled()
//                                activeCameraSource.setExposure(defaultExposure)
//                                exposureLevel = defaultExposure.toFloat()
                            }
                        )
                        Spacer(modifier = Modifier.width(22.dp))
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(0.9f),
                            painter = painterResource(id = R.drawable.ic_settings_back),
                            onClick = { onCameraSettingsManualAutoBack() }
                        )
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showManualSubMenu,
                    enter = fadeIn(tween(200)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                    exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-100).dp)
                ) {
                    Row(
                        modifier = Modifier.wrapContentWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(1.1f),
                            painter = painterResource(id = R.drawable.ic_iso),
                            onClick = {
                                onShowIsoSlider()
//                                showIsoSlider = true
//                                showManualSubMenu = false
                            }
                        )
                        Spacer(modifier = Modifier.width(22.dp))
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(0.9f),
                            painter = painterResource(id = R.drawable.ic_exposure_time),
                            onClick = {
                                onShowSensorExposureTimeSlider()
//                                sensorExposureTimeMode = "MANUAL"
//                                showSensorExposureTimeSlider = true
//                                showManualSubMenu = false
                            }
                        )
                        Spacer(modifier = Modifier.width(22.dp))
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(0.9f),
                            painter = painterResource(id = R.drawable.ic_settings_back),
                            onClick = {
                                onCameraManualModeBack()
//                                showManualSubMenu = false
//                                showCameraModeSelection = true
                            }
                        )
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showAutoSubMenu,
                    enter = fadeIn(tween(200)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                    exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-100).dp)
                ) {
                    Row(
                        modifier = Modifier.wrapContentWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(0.9f),
                            painter = painterResource(id = R.drawable.ic_exposure_compensation),
                            onClick = {
                                onShowExposureCompensationSlider()
//                                showExposureCompensationSlider = true
//                                showAutoSubMenu = false
                            }
                        )
                        Spacer(modifier = Modifier.width(22.dp))
                        AuxButtonSquare(
                            iconModifier = Modifier.fillMaxSize().scale(0.9f),
                            painter = painterResource(id = R.drawable.ic_settings_back),
                            onClick = {
                                onCameraAutoModeBack()
//                                showAutoSubMenu = false
//                                showCameraModeSelection = true
                            }
                        )
                    }
                }
            }
        }

        if (showZoomSlider) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(start = 50.dp, bottom = 50.dp)) {
                // Assume zoomLevel is a mutable state.
                ZoomControls(
                    zoomLevel = zoomLevel,
                    onZoomDelta = { delta ->
                        // Update the zoom level smoothly and ensure it stays within your bounds (e.g. 1f to 5f)
                        onZoomLevelFunction(delta)
//                        zoomLevel = (zoomLevel + delta).coerceIn(1f, 5f)
//                        activeCameraSource.setZoom(zoomLevel)
                    },
                    modifier = Modifier
                        .padding(start = 40.dp, top = 50.dp) // adjust padding as needed
                        .align(Alignment.CenterStart)
                )
            }
        }
        if (isVolumeMenuVisible && !isMicMuted) {
            AnimatedVisibility(
                visible = isVolumeMenuVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 60.dp) // Ensure the slider is aligned with the shifted volume button
                            .padding(start = 40.dp), // Optional additional spacing if needed
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Decorative plus icon at the top (indicating maximum volume)
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = "Increase Volume",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var volume by remember { mutableStateOf(micSource.microphoneVolume) }
                        Slider(
                            value = volume,
                            onValueChange = { newValue ->
                                volume = newValue
                                micSource.microphoneVolume = newValue
                                externalAudioSource.microphoneVolume = newValue
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .width(150.dp)
                                .rotate(-90f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Decorative minus icon at the bottom (indicating minimum volume)
                        Icon(
                            painter = painterResource(id = R.drawable.ic_less),
                            contentDescription = "Decrease Volume",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        if (showIsoSlider) {
            val screenWidth = configuration.screenWidthDp.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp) // Adjust padding as needed
                        .width(screenWidth * 0.7f) // 70% of the screen width
                ) {
                    // Optionally add a title or label here (e.g. "ISO Adjustment")
                    IsoSlider(
                        isoValue = isoSliderValue,
                        onValueChange = { newValue ->
                            onIsoSliderValue(newValue)
//                            isoSliderValue = newValue
                        },
                        updateSensorSensitivity = { newIso ->
                            onUpdateSensorSensitivity(newIso)
//                            activeCameraSource.setSensorSensitivity(newIso)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Back button to return to the Manual submenu
                    AuxButton(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(id = R.drawable.ic_settings_back),
                        onClick = {
                            onIsoSliderBack()
//                            showIsoSlider = false
//                            showManualSubMenu = true
                        }
                    )
                }
            }
        }
        if (showSensorExposureTimeSlider) {
            val screenWidth = configuration.screenWidthDp.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .width(screenWidth * 0.7f)
                ) {
                    // Show sensor exposure controls based on the current mode.
                    if (sensorExposureTimeMode == "MANUAL") {
                        // Use a default slider value if no manual change has been made.
                        val sliderValue = sensorExposureTimeIndex ?: defaultSensorExposureIndex
                        Text(
                            text = "Sensor Exposure Time: ${sensorExposureTimeOptions[sliderValue.toInt()].first}",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        SensorExposureTimeSlider(
                            index = sliderValue,
                            onValueChange = { newIndex ->
                                onSensorExposureTimeSliderChange(newIndex)
//                                sensorExposureTimeIndex = newIndex
//                                val idx = newIndex.toInt().coerceIn(0, sensorExposureTimeOptions.size - 1)
//                                val newSensorTime = sensorExposureTimeOptions[idx].second
//                                activeCameraSource.setSensorExposureTime(newSensorTime)
//                                // Store the exact value for later reapplication.
//                                currentSensorExposureTime = newSensorTime
//                                // Update the normalized exposure slider value.
//                                exposureLevel = ((newSensorTime - minSensorExposure).toFloat() / (maxSensorExposure - minSensorExposure))
//                                baseExposureLevel = exposureLevel
                            },
                            modifier = Modifier.fillMaxWidth(),
                            exposureOptions = sensorExposureTimeOptions
                        )
                    } else {
                        Text(
                            text = "Sensor Exposure Time: AUTO",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    // Back button to return to the Manual submenu
                    AuxButton(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(id = R.drawable.ic_settings_back),
                        onClick = {
                            onSensorExposureTimeSliderBack()
//                            showSensorExposureTimeSlider = false
//                            showManualSubMenu = true
                        }
                    )
                }
            }
        }
        if (showExposureCompensationSlider) {
            val screenWidth = configuration.screenWidthDp.dp
            // Determine if exposure compensation is available.
            val isExposureCompAvailable = activeCameraSource.isExposureCompensationAvailable()

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp) // Adjust padding as needed
                        .width(screenWidth * 0.7f)  // 70% of the screen width
                ) {
                    if (isExposureCompAvailable) {
                        Text(
                            text = "Exposure Compensation",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        ExposureCompensationSlider(
                            compensation = activeCameraSource.getExposureCompensationManual(),
                            onValueChange = { newComp ->
                                activeCameraSource.setExposureCompensationManual(newComp)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                            textAlign = TextAlign.Center
                        )
                    }
                    // Back button to return to the Auto submenu
                    AuxButton(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(id = R.drawable.ic_settings_back),
                        onClick = {
                            onExposureCompensationBack()
//                            showExposureCompensationSlider = false
//                            showAutoSubMenu = true
                        }
                    )
                }
            }
        }

        // Add the SnackbarHost at the bottom center.
        Box(
            modifier = Modifier
                .fillMaxSize(), // This Box takes the full available size
            contentAlignment = Alignment.BottomCenter // Align content to bottom center
        ) {
            // Place the SnackbarHost with bottom padding
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .wrapContentWidth() // Let the Snackbar take its intrinsic width
                    .padding(bottom = 40.dp),
                snackbar = { data ->
                    // Custom Snackbar styling:
                    Snackbar(
                        modifier = Modifier
                            .widthIn(max = 250.dp) // Adjust max width as desired
                            .wrapContentWidth(),
                        containerColor = Color.DarkGray,
                        contentColor = Color.White,
                        shape = SnackbarDefaults.shape,
                        content = {
                            // Force single line by using maxLines = 1.
                            Text(
                                text = data.visuals.message,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun ModeButton(
    modifier: Modifier = Modifier.size(50.dp),
    painter: Painter,
    toggled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Choose background based on the passed toggled state.
    val tintColor = if (toggled)
        CalypsoRed
    else
        Color.White

    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable { onToggle(!toggled) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = "Toggle Button Icon",
            tint = tintColor,
            modifier = Modifier.size(30.dp)
        )
    }
}