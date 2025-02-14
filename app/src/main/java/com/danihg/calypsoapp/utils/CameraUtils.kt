package com.danihg.calypsoapp.utils

// CameraUtils.kt
import android.content.Context
import android.media.MediaCodecList
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import com.danihg.calypsoapp.ui.theme.CalypsoRed

@Composable
fun rememberToast(): (String) -> Unit {
    val context = LocalContext.current
    return remember { { message: String ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } }
}

@Composable
fun PreventScreenLock() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
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

@Composable
fun AuxButton(modifier: Modifier = Modifier.size(50.dp), painter: Painter, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter,
            contentDescription = "Button Icon",
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

fun getSupportedVideoCodecs(): List<String> {
    val supportedCodecs = mutableListOf<String>()

    for (codecInfo in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
        if (!codecInfo.isEncoder) continue // We only need encoders

        for (type in codecInfo.supportedTypes) {
            if (type.startsWith("video/")) {
                supportedCodecs.add(type)
            }
        }
    }

    Log.d("CodecCheck", "Supported Video Codecs: $supportedCodecs")
    return supportedCodecs
}

fun getSupportedAudioCodecs(): List<String> {
    val supportedCodecs = mutableListOf<String>()

    for (codecInfo in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
        if (!codecInfo.isEncoder) continue // We only need encoders

        for (type in codecInfo.supportedTypes) {
            if (type.startsWith("audio/")) {
                supportedCodecs.add(type)
            }
        }
    }

    Log.d("CodecCheck", "Supported Audio Codecs: $supportedCodecs")
    return supportedCodecs
}

/**
 * Returns a list of video codecs that are supported both by the device and the streaming library.
 */
fun getAvailableVideoCodecs(): List<String> {
    val supportedVideoCodecs = getSupportedVideoCodecs()

    val codecMapping = mapOf(
        "video/avc" to "H264",
        "video/hevc" to "H265",
        "video/av1" to "AV1"
    )

    val availableCodecs = codecMapping.filter { (deviceCodec, _) ->
        supportedVideoCodecs.contains(deviceCodec)
    }.values.toList()

    Log.d("CodecCheck", "Available Video Codecs: $availableCodecs")
    return availableCodecs
}

/**
 * Returns a list of audio codecs that are supported both by the device and the streaming library.
 */
fun getAvailableAudioCodecs(): List<String> {
    val supportedAudioCodecs = getSupportedAudioCodecs()

    val codecMapping = mapOf(
        "audio/mp4a-latm" to "AAC",
        "audio/opus" to "OPUS",
        "audio/g711-alaw" to "G711",
        "audio/g711-ulaw" to "G711"
    )

    val availableCodecs = codecMapping.filter { (deviceCodec, _) ->
        supportedAudioCodecs.contains(deviceCodec)
    }.values.toSet().toList() // Using a Set to avoid duplicate "G711"

    Log.d("CodecCheck", "Available Audio Codecs: $availableCodecs")
    return availableCodecs
}

@Composable
fun ZoomSlider(
    zoomLevel: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // A horizontal slider rotated to appear vertical.
    // The fixed width (150.dp) becomes the vertical length after rotation,
    // and the fixed height (40.dp) sets the track thickness.
    Slider(
        value = zoomLevel,
        onValueChange = onValueChange,
        valueRange = 1f..5f,  // Adjust the max zoom as needed.
        // Remove steps for a smooth continuous slider.
        modifier = modifier
            .width(150.dp)  // This becomes the vertical length after rotation.
            .height(40.dp)  // Track thickness.
            .rotate(-90f),
        colors = SliderDefaults.colors(
            thumbColor = CalypsoRed,        // Customize as needed.
            activeTrackColor = CalypsoRed,
            inactiveTrackColor = Color.Gray
        )
    )
}

@Composable
fun ExposureSlider(
    exposureLevel: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // A standard horizontal slider.
    Slider(
        value = exposureLevel,
        onValueChange = onValueChange,
        valueRange = -35f..25f,  // For example, from -5 to +5 stops. Adjust as needed.
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = CalypsoRed,
            activeTrackColor = CalypsoRed,
            inactiveTrackColor = Color.Gray
        )
    )
}

@Composable
fun ExposureModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedMode == "AUTO",
            onClick = { onModeChange("AUTO") },
            label = { Text("AUTO") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == "MANUAL",
            onClick = { onModeChange("MANUAL") },
            label = { Text("MANUAL") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WhiteBalanceModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedMode == "AUTO",
            onClick = { onModeChange("AUTO") },
            label = { Text("AUTO") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == "MANUAL",
            onClick = { onModeChange("MANUAL") },
            label = { Text("MANUAL") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ManualWhiteBalanceSlider(
    temperature: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Let’s assume a temperature range from 2000K to 8000K.
    Slider(
        value = temperature,
        onValueChange = onValueChange,
        valueRange = 2000f..8000f,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.LightGray
        )
    )
}

@Composable
fun OpticalStabilizationModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedMode == "ENABLE",
            onClick = { onModeChange("ENABLE") },
            label = { Text("ENABLE") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == "DISABLE",
            onClick = { onModeChange("DISABLE") },
            label = { Text("DISABLE") },
            modifier = Modifier.weight(1f)
        )
    }
}
