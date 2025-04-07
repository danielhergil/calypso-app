package com.danihg.calypsoapp.presentation.camera.ui

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.sources.CameraCalypsoSource
import com.danihg.calypsoapp.utils.AuxButton
import com.danihg.calypsoapp.utils.AuxButtonSquare
import com.danihg.calypsoapp.utils.ExposureCompensationSlider
import com.danihg.calypsoapp.utils.IsoSlider
import com.danihg.calypsoapp.utils.SensorExposureTimeSlider
import com.danihg.calypsoapp.utils.ToggleAuxButtonSquare
import com.danihg.calypsoapp.utils.ZoomControls
import com.pedro.encoder.input.sources.audio.MicrophoneSource

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
    onExposureCompensationBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(70.dp)
                .background(Color.Red)
                .align(Alignment.CenterEnd)
        ) {
            // (Optional content for the red column)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 80.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(50.dp)
                    .zIndex(3f)
            ) {
                // Rocket button (this is the reference position)
                AuxButton(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterEnd)
                        .zIndex(2f),
                    painter = painterResource(id = R.drawable.ic_rocket),
                    onClick = {
                        onShowApplyButton()
//                        showApplyButton = !showApplyButton
                    }
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            val validStreamUrl = streamUrl.isNotBlank() &&
                    (streamUrl.startsWith("rtmp://") || streamUrl.startsWith("rtmps://")) &&
                    !streamUrl.contains(" ") &&
                    streamUrl.split("/").filter { it.isNotBlank() }.size >= 4

            Button(
                modifier = Modifier
                    .size(70.dp)
                    .border(3.dp, Color.White, CircleShape),
                enabled = validStreamUrl,
                colors = if (validStreamUrl)
                    ButtonDefaults.buttonColors(containerColor = Color.Red)
                else
                    ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = CircleShape,
                onClick = {
                    onStartStreaming()
//                    if (isStreaming) stopForegroundService() else startForegroundService()
                }
            ) {}

            Spacer(modifier = Modifier.height(5.dp))

            if (isStreaming) {
                // Recording button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.5f))
                        .clickable {
                            onRecordWhileStreaming()
//                            if (!isRecording) {
//                                recordVideoStreaming(context, genericStream) { }
//                                isRecording = true
//                            } else {
//                                recordVideoStreaming(context, genericStream) { }
//                                isRecording = false
//                            }
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
            val configuration = LocalConfiguration.current
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
            val configuration = LocalConfiguration.current
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

    }
}