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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.danihg.calypsoapp.data.Team
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import com.danihg.calypsoapp.utils.ColorDropdown
import com.danihg.calypsoapp.utils.ModernDropdown
import com.danihg.calypsoapp.utils.SectionSubtitle

@Composable
fun OverlayMenu(
    visible: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    teams: List<Team>,
    selectedTeam1: String,
    onTeam1Change: (String) -> Unit,
    selectedTeam2: String,
    onTeam2Change: (String) -> Unit,
    onLeftLogoUrlChange: (String?) -> Unit, // New parameter to update logo URL
    onRightLogoUrlChange: (String?) -> Unit, // New parameter to update logo URL
    showScoreboardOverlay: Boolean,
    onToggleScoreboard: (Boolean) -> Unit,
    selectedTeamsOverlayDuration: String,
    onTeamsOverlayDurationChange: (String) -> Unit,
    showLineUpOverlay: Boolean,
    onToggleLineUp: (Boolean) -> Unit,
    showReplays: Boolean,
    onToggleReplays: (Boolean) -> Unit,
    selectedReplaysDuration: String,
    onReplaysDurationChange: (String) -> Unit,
    selectedLeftColor: String,
    onLeftColorChange: (String) -> Unit,
    selectedRightColor: String,
    onRightColorChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val colorOptions = listOf(
        "Black" to Color.Black,
        "Blue" to Color.Blue,
        "Cyan" to Color.Cyan,
        "Green" to Color.Green,
        "Orange" to Color(0xFFFFA500),
        "Pink" to Color(0xFFFF69B4),
        "Purple" to Color(0xFF800080),
        "Red" to Color.Red,
        "Soft Blue" to Color(0xFF87CEFA),
        "White" to Color.White,
        "Yellow" to Color.Yellow
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        val cameraPreviewHeight = screenHeight
        val cameraPreviewWidth = cameraPreviewHeight * (16f / 9f)

        // Center horizontally.
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
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close Button (X)
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

                    // Scoreboard Toggle (Header)
                    Text(
                        text = "Scoreboard Configuration",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Scoreboard",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = showScoreboardOverlay,
                            onCheckedChange = onToggleScoreboard,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CalypsoRed,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Team Selection Layout (Side by Side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Team 1")
                            ModernDropdown(
                                items = teams.map { it.name },
                                selectedValue = selectedTeam1,
                                displayMapper = { it },
                                onValueChange = {
                                    onTeam1Change(it)
                                    val team = teams.find { team -> team.name == it }
                                    onLeftLogoUrlChange(team?.logo) // Save the new logo URL
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Team 2")
                            ModernDropdown(
                                items = teams.map { it.name },
                                selectedValue = selectedTeam2,
                                displayMapper = { it },
                                onValueChange = {
                                    onTeam2Change(it)
                                    val team = teams.find { team -> team.name == it }
                                    onRightLogoUrlChange(team?.logo) // Save the new logo URL
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Selection Layout (Side by Side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Left Color")
                            ColorDropdown(
                                colorOptions = colorOptions,
                                selectedColorName = selectedLeftColor,
                                onColorChange = onLeftColorChange
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionSubtitle("Right Color")
                            ColorDropdown(
                                colorOptions = colorOptions,
                                selectedColorName = selectedRightColor,
                                onColorChange = onRightColorChange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Line Up Config
                    Text(
                        text = "Line Up Configuration",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Line Up",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = showLineUpOverlay,
                            onCheckedChange = onToggleLineUp,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CalypsoRed,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Line Up Duration",
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.align(
                                Alignment.CenterVertically
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ModernDropdown(
                            items = listOf("5s", "10s", "15s", "20s", "25s", "30s"),
                            selectedValue = selectedTeamsOverlayDuration,
                            displayMapper = { it },
                            onValueChange = {
                                onTeamsOverlayDurationChange(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Replays Config
                    Text(
                        text = "Replays Configuration",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Replays",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = showReplays,
                            onCheckedChange = onToggleReplays,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CalypsoRed,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Replays Duration",
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.align(
                                Alignment.CenterVertically
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ModernDropdown(
                            items = listOf("5s", "10s", "15s", "20s"),
                            selectedValue = selectedReplaysDuration,
                            displayMapper = { it },
                            onValueChange = {
                                onReplaysDurationChange(it)
                            }
                        )
                    }
                }
            }
        }
    }
}