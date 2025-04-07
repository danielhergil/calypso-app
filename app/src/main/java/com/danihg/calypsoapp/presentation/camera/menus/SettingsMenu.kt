package com.danihg.calypsoapp.presentation.camera.menus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import com.danihg.calypsoapp.utils.ModernDropdown
import com.danihg.calypsoapp.utils.SectionSubtitle

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
    rtmpUrl: String,
    onRtmpUrlChange: (String) -> Unit,
    streamKey: String,
    onStreamKeyChange: (String) -> Unit,
    availableVideoCodecs: List<String>,
    availableAudioCodecs: List<String>,
    onApply: (constructedStreamUrl: String, newRtmpUrl: String, newStreamKey: String) -> Unit,
    onClose: () -> Unit
) {

    // Validate RTMP URL: must start with "rtmp://" or "rtmps://"
    // and must not contain any spaces.
    val isRtmpUrlValid = rtmpUrl.isNotBlank() &&
            !rtmpUrl.contains(" ") &&
            (rtmpUrl.startsWith("rtmp://") || rtmpUrl.startsWith("rtmps://"))

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
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.3f)
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
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.3f)
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
                        items = listOf("1440p", "1080p", "720p"),
                        selectedValue = selectedResolution,
                        displayMapper = { it },
                        onValueChange = onResolutionChange,
                        enabled = !isStreaming
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionSubtitle("RTMP URL")
                    OutlinedTextField(
                        value = rtmpUrl,
                        onValueChange = onRtmpUrlChange,
                        label = { Text("RTMP URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isStreaming,
                        isError = !isRtmpUrlValid,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.Red,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    if (!isRtmpUrlValid) {
                        Text(
                            text = "Invalid RTMP URL. Must start with rtmp:// or rtmps://.",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionSubtitle("Stream Key")
                    OutlinedTextField(
                        value = streamKey,
                        onValueChange = onStreamKeyChange,
                        label = { Text("Stream Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isStreaming,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.Red,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Remove any trailing slash from the RTMP URL.
                            val sanitizedRtmpUrl = rtmpUrl.trim().removeSuffix("/")
                            // Construct final stream URL.
                            val constructedStreamUrl = "$sanitizedRtmpUrl/$streamKey"
                            onApply(constructedStreamUrl.trim(), sanitizedRtmpUrl, streamKey)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        enabled = !isStreaming && isRtmpUrlValid && streamKey.isNotBlank(),
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