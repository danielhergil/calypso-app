package com.danihg.calypsoapp.presentation.camera.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R

@Composable
fun BatteryIndicator(modifier: Modifier) {
    val context = LocalContext.current
    // Mutable state to hold the battery percentage.
    val batteryLevel = remember { mutableStateOf<Int?>(null) }

    // Register a BroadcastReceiver when the composable enters composition.
    DisposableEffect(Unit) {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                // Retrieve battery level and scale, then calculate percentage.
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) {
                    batteryLevel.value = (level * 100 / scale)
                } else {
                    batteryLevel.value = null
                }
            }
        }
        // Register the receiver.
        context.registerReceiver(batteryReceiver, intentFilter)
        onDispose {
            // Unregister when the composable is disposed.
            context.unregisterReceiver(batteryReceiver)
        }
    }

    // Display the battery percentage or a fallback message.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = batteryLevel.value?.let { "$it%" } ?: "Unknown",
            color = Color.White,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_battery),
            contentDescription = "Button Icon",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
//            modifier = iconModifier
        )
    }
}
