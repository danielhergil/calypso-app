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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter

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