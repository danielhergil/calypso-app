package com.danihg.calypsoapp.presentation.settings

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Preview
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)

    // Video settings
    var selectedResolution by remember { mutableStateOf(sharedPreferences.getString("resolution", "1080p") ?: "1080p") }
    var selectedBitrate by remember { mutableStateOf(sharedPreferences.getInt("videoBitrate", 5000 * 1000)) }
    var selectedFPS by remember { mutableStateOf(sharedPreferences.getInt("videoFPS", 30)) }

    // Audio settings (read-only)
    val audioSampleRate = sharedPreferences.getInt("audioSampleRate", 32000)
    val audioBitrate = sharedPreferences.getInt("audioBitrate", 128 * 1000)
    val audioIsStereo = sharedPreferences.getBoolean("audioIsStereo", true)

    // Streaming endpoint settings
    var selectedEndpoint by remember { mutableStateOf(sharedPreferences.getString("selectedEndpoint", "youtube")?.lowercase() ?: "youtube") }
    var streamKey by remember { mutableStateOf(TextFieldValue(sharedPreferences.getString("streamKey", "") ?: "")) }
    var customEndpoint by remember { mutableStateOf(TextFieldValue(sharedPreferences.getString("customEndpoint", "") ?: "")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(16.dp)
    ) {
        // Title Section
        Text(
            "Settings",
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Video Settings Section
        SectionTitle("Video Settings")

        ModernCard {
            // Resolution Radio Buttons
            SectionSubtitle("Resolution")
            Row {
                ModernRadioButton("1080p", selectedResolution, onClick = {
                    selectedResolution = "1080p"
                    saveResolution(sharedPreferences, "1080p")
                })
                ModernRadioButton("720p", selectedResolution, onClick = {
                    selectedResolution = "720p"
                    saveResolution(sharedPreferences, "720p")
                })
            }

            SectionSubtitle("Bitrate")
            ModernDropdown(
                items = listOf(3000 * 1000, 5000 * 1000, 7000 * 1000, 10000 * 1000),
                selectedValue = selectedBitrate,
                displayMapper = { "${it / 1000 / 1000} Mbps" },
                onValueChange = {
                    selectedBitrate = it
                    saveBitrate(sharedPreferences, it)
                }
            )

            SectionSubtitle("FPS")
            Row {
                ModernRadioButton("30", if (selectedFPS == 30) "30" else "60", onClick = {
                    selectedFPS = 30
                    saveFPS(sharedPreferences, 30)
                })
                ModernRadioButton("60", if (selectedFPS == 60) "60" else "30", onClick = {
                    selectedFPS = 60
                    saveFPS(sharedPreferences, 60)
                })
            }
        }

        // Audio Settings (Read-only)
        SectionTitle("Audio Settings")
        ModernCard {
            Text("Sample Rate: $audioSampleRate Hz", color = Color.White)
            Text("Bitrate: ${audioBitrate / 1000} kbps", color = Color.White)
            Text("Stereo: ${if (audioIsStereo) "Yes" else "No"}", color = Color.White)
        }

        // Streaming Endpoint Section
        SectionTitle("Streaming Endpoint")
        ModernCard {
            Row {
                ModernRadioButton("YouTube", selectedEndpoint, onClick = {
                    selectedEndpoint = "youtube"
                    saveEndpointSettings(sharedPreferences, selectedEndpoint, streamKey.text, customEndpoint.text)
                })
                ModernRadioButton("Twitch", selectedEndpoint, onClick = {
                    selectedEndpoint = "twitch"
                    saveEndpointSettings(sharedPreferences, selectedEndpoint, streamKey.text, customEndpoint.text)
                })
                ModernRadioButton("Custom", selectedEndpoint, onClick = {
                    selectedEndpoint = "custom"
                    saveEndpointSettings(sharedPreferences, selectedEndpoint, streamKey.text, customEndpoint.text)
                })
            }

            if (selectedEndpoint == "youtube" || selectedEndpoint == "twitch") {
                Text("Stream Key", color = Color.White)
                BasicTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SelectedField, CircleShape)
                        .padding(8.dp)
                )
            } else if (selectedEndpoint == "custom") {
                Text("Custom Endpoint URL", color = Color.White)
                BasicTextField(
                    value = customEndpoint,
                    onValueChange = { customEndpoint = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SelectedField, CircleShape)
                        .padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Apply Button
        Button(
            onClick = {
                saveEndpointSettings(sharedPreferences, selectedEndpoint, streamKey.text, customEndpoint.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CalypsoRed,
                contentColor = Color.White
            ),
            shape = CircleShape
        ) {
            Text("Apply", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModernCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gray)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ModernRadioButton(text: String, selectedValue: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 16.dp)
            .clickable(onClick = onClick)
    ) {
        RadioButton(
            selected = selectedValue.lowercase() == text.lowercase(),
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = CalypsoRed, unselectedColor = UnselectedField)
        )
        Text(text, color = Color.White)
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

    Box(modifier = Modifier.fillMaxWidth().background(UnselectedField, CircleShape).padding(8.dp)) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayMapper(selectedValue),
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp)
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

@Composable
fun SectionTitle(title: String) {
    Text(title, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White), modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun SectionSubtitle(title: String) {
    Text(title, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.LightGray), modifier = Modifier.padding(vertical = 4.dp))
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