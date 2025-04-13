package com.danihg.calypsoapp.presentation.camera.menus

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.data.FirestoreManager
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import com.danihg.calypsoapp.utils.ModernDropdown
import com.danihg.calypsoapp.utils.SectionSubtitle
import kotlinx.coroutines.launch

// Helper data class for RTMP configurations.
data class RTMPConfig(
    val alias: String,
    val rtmpUrl: String,
    val streamKey: String
) {
    // Constructed URL as "rtmpUrl/streamKey", ensuring no trailing slash on the URL.
    val constructedUrl: String
        get() = rtmpUrl.trim().removeSuffix("/") + "/" + streamKey.trim()
}

// Placeholder functions for JSON conversion.
// In a real project, use a proper JSON library (like kotlinx.serialization or Gson).
private fun toJson(list: List<RTMPConfig>): String {
    // For simplicity, join each config as "alias|rtmpUrl|streamKey" separated by newlines.
    return list.joinToString(separator = "\n") { "${it.alias}|${it.rtmpUrl}|${it.streamKey}" }
}

private fun parseRTMPConfigs(jsonString: String): List<RTMPConfig> {
    return jsonString.lines().mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size == 3) {
            RTMPConfig(parts[0], parts[1], parts[2])
        } else null
    }
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
    rtmpUrl: String,
    onRtmpUrlChange: (String) -> Unit,
    streamKey: String,
    onStreamKeyChange: (String) -> Unit,
    availableVideoCodecs: List<String>,
    availableAudioCodecs: List<String>,
    onApply: (constructedStreamUrl: String, newRtmpUrl: String, newStreamKey: String) -> Unit,
    onClose: () -> Unit
) {

    // Validate RTMP URL: must not contain spaces and must start with "rtmp://" or "rtmps://"
    val isRtmpUrlValid = rtmpUrl.isNotBlank() &&
            !rtmpUrl.contains(" ") &&
            (rtmpUrl.startsWith("rtmp://") || rtmpUrl.startsWith("rtmps://"))

    // Helper function to verify that an RTMP configuration is valid.
    fun isValidRtmpConfig(config: RTMPConfig?) =
        config != null &&
                config.rtmpUrl.isNotBlank() &&
                config.streamKey.isNotBlank() &&
                (config.rtmpUrl.startsWith("rtmp://") || config.rtmpUrl.startsWith("rtmps://"))

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Local state list for RTMP configurations (using the single RTMPConfig type).
    val rtmpConfigs = remember { mutableStateListOf<RTMPConfig>() }
    var selectedRtmpConfig by remember { mutableStateOf<RTMPConfig?>(null) }

    // New RTMP configuration input fields.
    var newAlias by remember { mutableStateOf("") }
    var newRtmpUrl by remember { mutableStateOf("") }
    var newStreamKey by remember { mutableStateOf("") }
    val newRtmpUrlValid = newRtmpUrl.isNotBlank() &&
            !newRtmpUrl.contains(" ") &&
            (newRtmpUrl.startsWith("rtmp://") || newRtmpUrl.startsWith("rtmps://"))

    // Load saved RTMP configurations from Firestore when the composable appears.
    LaunchedEffect(Unit) {
        // Fetch the configurations from Firestore (data type)
        val configs = FirestoreManager().getRTMPConfigs() // Returns List<com.danihg.calypsoapp.data.RTMPConfig>
        rtmpConfigs.clear()
        // Map each configuration to the UI type
        rtmpConfigs.addAll(configs.map { dataConfig ->
            // Assuming the UI type (RTMPConfig) has the same properties,
            // you create a new instance with the same values:
            com.danihg.calypsoapp.presentation.camera.menus.RTMPConfig(
                alias = dataConfig.alias,
                rtmpUrl = dataConfig.rtmpUrl,
                streamKey = dataConfig.streamKey
            )
        })
        if (rtmpConfigs.isNotEmpty() && selectedRtmpConfig == null) {
            selectedRtmpConfig = rtmpConfigs.first()
        }
    }

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
                    // RTMP configuration section.
                    SectionSubtitle("RTMP Configuration")
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Input fields for new RTMP configuration.
                    OutlinedTextField(
                        value = newAlias,
                        onValueChange = { newAlias = it },
                        label = { Text("Alias") },
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
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newRtmpUrl,
                        onValueChange = { newRtmpUrl = it },
                        label = { Text("RTMP URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isStreaming,
                        isError = newRtmpUrl.isNotBlank() && !newRtmpUrlValid,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.Red,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    if (newRtmpUrl.isNotBlank() && !newRtmpUrlValid) {
                        Text(
                            text = "Invalid RTMP URL. Must start with rtmp:// or rtmps://.",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newStreamKey,
                        onValueChange = { newStreamKey = it },
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
                    Spacer(modifier = Modifier.height(8.dp))
                    // Save Configuration button.
                    Button(
                        onClick = {
                            if (newAlias.isNotBlank() && newRtmpUrlValid && newStreamKey.isNotBlank()) {
                                // Create the UI type configuration.
                                val uiConfig = RTMPConfig(
                                    alias = newAlias.trim(),
                                    rtmpUrl = newRtmpUrl.trim().removeSuffix("/"),
                                    streamKey = newStreamKey.trim()
                                )
                                // Add it to the UI list.
                                rtmpConfigs.add(uiConfig)
                                coroutineScope.launch {
                                    // Convert the UI type into the data type expected by FirestoreManager.
                                    val dataConfig = com.danihg.calypsoapp.data.RTMPConfig(
                                        alias = uiConfig.alias,
                                        rtmpUrl = uiConfig.rtmpUrl,
                                        streamKey = uiConfig.streamKey
                                    )
                                    FirestoreManager().saveRTMPConfig(dataConfig)
                                }
                                if (selectedRtmpConfig == null) {
                                    selectedRtmpConfig = uiConfig
                                }
                                newAlias = ""
                                newRtmpUrl = ""
                                newStreamKey = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isStreaming && newAlias.isNotBlank() && newRtmpUrlValid && newStreamKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalypsoRed,
                            contentColor = Color.White
                        ),
                        shape = CircleShape
                    ) {
                        Text("Save Configuration", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (rtmpConfigs.isNotEmpty()) {
                        SectionSubtitle("Saved RTMP Configurations")
                        ModernDropdown(
                            items = rtmpConfigs,
                            selectedValue = selectedRtmpConfig ?: rtmpConfigs.first(),
                            displayMapper = { it.alias },
                            onValueChange = { selectedRtmpConfig = it },
                            enabled = !isStreaming
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                selectedRtmpConfig?.let { configToDelete ->
                                    coroutineScope.launch {
                                        val success = FirestoreManager().deleteRTMPConfig(configToDelete.alias)
                                        if (success) {
                                            rtmpConfigs.remove(configToDelete)
                                            selectedRtmpConfig = rtmpConfigs.firstOrNull()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isStreaming && selectedRtmpConfig != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.White
                            ),
                            shape = CircleShape
                        ) {
                            Text("Delete Selected", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // Apply button: enabled only if a valid configuration is selected.
                    Button(
                        onClick = {
                            selectedRtmpConfig?.let { config ->
                                val finalConstructedUrl = config.constructedUrl.trim()
                                onApply(finalConstructedUrl, config.rtmpUrl, config.streamKey)
                            } ?: run {
                                // For example, if no config exists, apply the current manual values:
                                onApply(rtmpUrl.trim(), rtmpUrl.trim(), streamKey.trim())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        enabled = !isStreaming,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalypsoRed,
                            contentColor = Color.White
                        ),
                        shape = CircleShape
                    ) {
                        Text("Apply", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}