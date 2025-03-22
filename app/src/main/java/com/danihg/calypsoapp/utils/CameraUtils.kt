package com.danihg.calypsoapp.utils

// CameraUtils.kt
import android.content.Context
import android.media.MediaCodecList
import android.os.PowerManager
import android.util.Log
import android.view.MotionEvent
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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
@Composable
fun ToggleAuxButton(
    modifier: Modifier = Modifier.size(50.dp),
    painter: Painter,
    toggled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Choose background based on the passed toggled state.
    val backgroundColor = if (toggled)
        Color.DarkGray.copy(alpha = 0.7f)  // Darker when toggled (pressed)
    else
        Color.Gray.copy(alpha = 0.5f)      // Default appearance

    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onToggle(!toggled) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = "Toggle Button Icon",
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

// A new composable to handle continuous press behavior
@Composable
fun ContinuousZoomButton(
    modifier: Modifier = Modifier,
    painter: Painter,
    zoomSpeed: Float, // Zoom change per second; use positive for zooming in, negative for zooming out.
    onZoomDelta: (Float) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    // Launch a frame-based loop when the button is pressed.
    LaunchedEffect(isPressed) {
        if (isPressed) {
            var lastFrameTime = withFrameNanos { it }
            while (isPressed) {
                val currentFrameTime = withFrameNanos { it }
                // Calculate elapsed time in seconds
                val deltaTime = (currentFrameTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = currentFrameTime
                onZoomDelta(zoomSpeed * deltaTime)
            }
        }
    }

    // A Box mimicking the AuxButton styling while handling press using pointerInput.
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease() // Wait until the press is released.
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = "Button Icon",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ZoomControls(
    zoomLevel: Float,
    onZoomDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Zoom In button with a positive zoom speed (adjust speed as needed).
        ContinuousZoomButton(
            painter = painterResource(id = R.drawable.ic_add), // Replace with your plus icon resource.
            zoomSpeed = 1f, // Adjust the speed (units per second) for zooming in.
            onZoomDelta = onZoomDelta
        )
        Spacer(modifier = Modifier.height(30.dp))
        // Zoom Out button with a negative zoom speed.
        ContinuousZoomButton(
            painter = painterResource(id = R.drawable.ic_less), // Replace with your minus icon resource.
            zoomSpeed = -1f, // Adjust the speed for zooming out.
            onZoomDelta = onZoomDelta
        )
    }
}

@Composable
fun IsoSlider(
    isoIndex: Float,
    onValueChange: (Float) -> Unit,
    isoOptions: List<Int> = listOf(100, 200, 400, 800, 1600, 3200),
    modifier: Modifier = Modifier
) {
    // The slider’s range is from 0 to (number of options - 1)
    val minIndex = 0f
    val maxIndex = (isoOptions.size - 1).toFloat()

    // Use a Slider with discrete steps. Steps is the number of values between the min and max (excluding endpoints).
    Slider(
        value = isoIndex,
        onValueChange = { value ->
            // Snap to the nearest integer index
            val newIndex = value.toInt().toFloat()
            onValueChange(newIndex)
        },
        valueRange = minIndex..maxIndex,
        steps = isoOptions.size - 2,
        modifier = modifier
    )

    Text(
        text = "ISO: ${isoOptions[isoIndex.toInt()]}",
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}


@Composable
fun ExposureSlider(
    exposureLevel: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -55f..55f
) {
    // A standard horizontal slider.
    Slider(
        value = exposureLevel,
        onValueChange = onValueChange,
        valueRange = valueRange,  // For example, from -5 to +5 stops. Adjust as needed.
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

@Composable
fun ExposureCompensationSlider(
    compensation: Int, // Current exposure compensation value (-2, -1, 0, 1, or 2)
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Define the allowed discrete values.
    val sliderPositions = listOf(-2, -1, 0, 1, 2)
    // Determine the initial index for the slider (if compensation not found, default to 0)
    val initialIndex = sliderPositions.indexOf(compensation).takeIf { it != -1 } ?: 2
    // Local state to track the slider's current float position (which corresponds to an index in sliderPositions)
    var sliderValue by remember { mutableStateOf(initialIndex.toFloat()) }

    // Layout that shows the text label and the slider.
    Column(modifier = modifier) {
        // Show the current compensation value as text.
        Text(
            text = "${sliderPositions[sliderValue.toInt()]}",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        // Slider with a value range corresponding to the indices [0, 4]
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                // When the user stops dragging, round the slider value and report the discrete value.
                val newIndex = sliderValue.toInt()
                onValueChange(sliderPositions[newIndex])
            },
            valueRange = 0f..4f, // Because we have 5 discrete positions (0,1,2,3,4)
            steps = 3, // 4 intervals between 5 positions, so steps = intervals - 1
            modifier = Modifier.fillMaxWidth()
        )
    }
}


//@Composable
//fun ExposureCompensationSlider(
//    compensation: Float,
//    onValueChange: (Float) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Slider(
//        value = compensation,
//        onValueChange = onValueChange,
//        valueRange = -3f..3f,
//        steps = 6,
//        modifier = modifier,
//        colors = SliderDefaults.colors(
//            thumbColor = MaterialTheme.colorScheme.primary,
//            activeTrackColor = MaterialTheme.colorScheme.primary,
//            inactiveTrackColor = Color.LightGray
//        )
//    )
//}

@Composable
fun SensorExposureTimeSlider(
    index: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val exposureTimeOptions = listOf(
        "1/30" to 33333333L,
        "1/40" to 25000000L,
        "1/50" to 20000000L,
        "1/60" to 16666667L,
        "1/120" to 8333333L,
        "1/250" to 4000000L,
        "1/500" to 2000000L
    )
    Slider(
        value = index,
        onValueChange = onValueChange,
        valueRange = 0f..(exposureTimeOptions.size - 1).toFloat(),
        steps = exposureTimeOptions.size - 2,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.LightGray
        )
    )
}

@Composable
fun SensorExposureTimeSlider(
    index: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    exposureOptions: List<Pair<String, Long>>
) {
    Slider(
        value = index,
        onValueChange = onValueChange,
        valueRange = 0f..(exposureOptions.size - 1).toFloat(),
        steps = exposureOptions.size - 1,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.LightGray
        ),
        modifier = modifier
    )
}

@Composable
fun SensorExposureTimeModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FilterChip(
            selected = selectedMode == "AUTO",
            onClick = { onModeChange("AUTO") },
            label = { Text("Auto") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == "MANUAL",
            onClick = { onModeChange("MANUAL") },
            label = { Text("Manual") },
            modifier = Modifier.weight(1f)
        )
    }
}