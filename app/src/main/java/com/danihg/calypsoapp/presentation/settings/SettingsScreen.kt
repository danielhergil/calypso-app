package com.danihg.calypsoapp.presentation.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.ui.theme.Black
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import com.danihg.calypsoapp.ui.theme.SelectedField
import com.danihg.calypsoapp.ui.theme.UnselectedField

@SuppressLint("SourceLockedOrientationActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
    val scrollState = rememberScrollState()

    var selectedResolution by remember { mutableStateOf(sharedPreferences.getString("resolution", "1080p") ?: "1080p") }
    var selectedBitrate by remember { mutableStateOf(sharedPreferences.getInt("videoBitrate", 5000 * 1000)) }
    var selectedFPS by remember { mutableStateOf(sharedPreferences.getInt("videoFPS", 30)) }
    var selectedEndpoint by remember { mutableStateOf(sharedPreferences.getString("selectedEndpoint", "youtube")?.lowercase() ?: "youtube") }
    var streamKey by remember { mutableStateOf(TextFieldValue(sharedPreferences.getString("streamKey", "") ?: "")) }
    var customEndpoint by remember { mutableStateOf(TextFieldValue(sharedPreferences.getString("customEndpoint", "") ?: "")) }
    var showFPSMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingsSection("Video Settings") {
            SettingsItem("Resolution") {
                SegmentedButtons(
                    items = listOf("1080p", "720p"),
                    selectedItem = selectedResolution,
                    onItemSelected = {
                        selectedResolution = it
                        saveResolution(sharedPreferences, it)
                    }
                )
            }

            SettingsItem("Bitrate") {
                ModernDropdown(
                    items = listOf(3000 * 1000, 5000 * 1000, 7000 * 1000, 10000 * 1000, 12000 * 1000),
                    selectedValue = selectedBitrate,
                    displayMapper = { "${it / 1000 / 1000} Mbps" },
                    onValueChange = {
                        selectedBitrate = it
                        saveBitrate(sharedPreferences, it)
                    },
                )
            }

            SettingsItem("FPS") {
                SegmentedButtons(
                    items = listOf("30", "60"),
                    selectedItem = selectedFPS.toString(),
                    onItemSelected = {
                        if (it == "60") {
                            showFPSMessage = true
                        } else {
                            selectedFPS = it.toInt()
                            saveFPS(sharedPreferences, it.toInt())
                        }
                    }
                )
            }
        }

        SettingsSection("Audio Settings") {
            Text("Sample Rate: ${sharedPreferences.getInt("audioSampleRate", 32000)} Hz", color = Color.White)
            Text("Bitrate: ${sharedPreferences.getInt("audioBitrate", 128 * 1000) / 1000} kbps", color = Color.White)
            Text("Stereo: ${if (sharedPreferences.getBoolean("audioIsStereo", true)) "Yes" else "No"}", color = Color.White)
        }

        SettingsSection("Streaming Endpoint") {
            SegmentedButtons(
                items = listOf("YouTube", "Twitch", "Custom"),
                selectedItem = selectedEndpoint.capitalize(),
                onItemSelected = {
                    selectedEndpoint = it.lowercase()
                    saveEndpointSettings(sharedPreferences, selectedEndpoint, streamKey.text, customEndpoint.text)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedEndpoint) {
                "youtube", "twitch" -> {
                    OutlinedTextField(
                        value = streamKey,
                        onValueChange = { streamKey = it },
                        label = { Text("Stream Key") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CalypsoRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = CalypsoRed,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }
                "custom" -> {
                    OutlinedTextField(
                        value = customEndpoint,
                        onValueChange = { customEndpoint = it },
                        label = { Text("Custom Endpoint URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CalypsoRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = CalypsoRed,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                saveEndpointSettings(sharedPreferences, selectedEndpoint, streamKey.text, customEndpoint.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
        ) {
            Text("Apply", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showFPSMessage) {
        AlertDialog(
            onDismissRequest = { showFPSMessage = false },
            title = { Text("60 FPS Disabled") },
            text = { Text("60 fps is disabled for now. We are working on it. You can use 60 fps using USB camera.") },
            confirmButton = {
                TextButton(onClick = { showFPSMessage = false }) {
                    Text("OK")
                }
            },
            containerColor = Gray,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Gray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.LightGray),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

@Composable
fun SegmentedButtons(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(UnselectedField)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item.lowercase() == selectedItem.lowercase()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onItemSelected(item) }
                    .background(if (isSelected) SelectedField else Color.Transparent)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = if (isSelected) CalypsoRed else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (index < items.size - 1) {
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color.Gray)
                )
            }
        }
    }
}

@Composable
fun <T> ModernDropdown(
    items: List<T>,
    selectedValue: T,
    displayMapper: (T) -> String,
    onValueChange: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(UnselectedField)
            .clickable { expanded = true }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayMapper(selectedValue),
                color = Color.White,
                fontSize = 16.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Gray)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayMapper(item),
                            color = if (item == selectedValue) CalypsoRed else Color.White,
                            fontSize = 16.sp
                        )
                    },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun saveResolution(sharedPreferences: SharedPreferences, resolution: String) {
    sharedPreferences.edit().putString("resolution", resolution).apply()
}

private fun saveBitrate(sharedPreferences: SharedPreferences, bitrate: Int) {
    sharedPreferences.edit().putInt("videoBitrate", bitrate).apply()
}

private fun saveFPS(sharedPreferences: SharedPreferences, fps: Int) {
    sharedPreferences.edit().putInt("videoFPS", fps).apply()
}

private fun saveEndpointSettings(
    sharedPreferences: SharedPreferences,
    selectedEndpoint: String,
    streamKey: String,
    customEndpoint: String
) {
    sharedPreferences.edit().apply {
        putString("selectedEndpoint", selectedEndpoint.lowercase())
        putString("streamKey", streamKey)
        putString("customEndpoint", customEndpoint)
        apply()
    }
}