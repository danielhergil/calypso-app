package com.danihg.calypsoapp.presentation.camera.menus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.data.Team
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.utils.ModernDropdown

@Composable
fun OverlayMenu2(
    visible: Boolean,
    teams: List<Team>,
    selectedTeam1: String,
    onTeam1Change: (String) -> Unit,
    selectedTeam2: String,
    onTeam2Change: (String) -> Unit,
    onLeftLogoUrlChange: (String?) -> Unit,
    onRightLogoUrlChange: (String?) -> Unit,
    onAddTeam: () -> Unit,
    onClose: () -> Unit
) {
    // Get screen dimensions.
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    // Use a reference width and calculate horizontal padding if screen width is larger.
    val screenWidthDpReference = 788.dp
    val paddingFix = if (screenWidthDp > screenWidthDpReference) {
        (screenWidthDp - screenWidthDpReference) / 2
    } else {
        0.dp
    }

    // Track currently selected tab (default is tab 1)
    var selectedTab by remember { mutableStateOf(1) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
        ) {
            // Outer container with rounded corners, background, and border.
            Box(
                modifier = Modifier
                    .width(screenWidthDp)
                    .height(screenHeightDp)
                    .padding(horizontal = paddingFix)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.DarkGray.copy(alpha = 0.92f))
                    .border(2.dp, Color.White, shape = RoundedCornerShape(16.dp))
                    .align(Alignment.Center)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header row: five tabs plus a closing tab on the right.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Clip top corners to match the outer container.
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color.Gray)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Five normal tab items.
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TabItem(
                                label = "Teams",
                                isSelected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                            VerticalDivider()
                            TabItem(
                                label = "Tab 2",
                                isSelected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            )
                            VerticalDivider()
                            TabItem(
                                label = "Tab 3",
                                isSelected = selectedTab == 3,
                                onClick = { selectedTab = 3 }
                            )
                            VerticalDivider()
                            TabItem(
                                label = "Tab 4",
                                isSelected = selectedTab == 4,
                                onClick = { selectedTab = 4 }
                            )
                            VerticalDivider()
                            TabItem(
                                label = "Tab 5",
                                isSelected = selectedTab == 5,
                                onClick = { selectedTab = 5 }
                            )
                        }
                        // Divider between normal tabs and the close tab.
                        VerticalDivider(height = 32.dp)
                        // The closing tab item (a small square with the X).
                        CloseTab(onClick = onClose)
                    }
                    // Content placeholder for the selected tab.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (selectedTab) {
                            // When "Teams" tab is selected, display two dropdowns.
                            1 -> TeamsContent(
                                teams = teams,
                                selectedTeam1 = selectedTeam1,
                                selectedTeam2 = selectedTeam2,
                                onTeam1Change = onTeam1Change,
                                onTeam2Change = onTeam2Change,
                                onLeftLogoUrlChange = onLeftLogoUrlChange,
                                onRightLogoUrlChange = onRightLogoUrlChange,
                                onAddTeam = onAddTeam
                            )
                            else -> Text(
                                text = "Content for Tab $selectedTab",
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// TabItem is an extension on RowScope so that weight() is available.
@Composable
fun RowScope.TabItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            // The inner padding reduces the background indicator so it doesn't span fully
            .padding(horizontal = 4.dp)
            .background(
                color = if (isSelected) CalypsoRed else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}

// A fixed-size square tab for closing the overlay.
@Composable
fun CloseTab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .padding(start = 4.dp)
            .clickable(onClick = onClick)
            .background(
                color = Color.LightGray,
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = "Close",
            tint = Color.Black
        )
    }
}

// A customizable vertical divider used to separate tabs.
@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    thickness: Dp = 1.dp,
    height: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .width(thickness)
            .height(height)
            .background(color = color)
    )
}

// Content for the "Teams" tab with two dropdowns to select teams.
@Composable
fun TeamsContent(
    teams: List<Team>,
    selectedTeam1: String,
    selectedTeam2: String,
    onTeam1Change: (String) -> Unit,
    onTeam2Change: (String) -> Unit,
    onLeftLogoUrlChange: (String?) -> Unit,
    onRightLogoUrlChange: (String?) -> Unit,
    onAddTeam: () -> Unit
) {
    if (teams.isEmpty()) {
        // If there are no teams, display a message and an "Add New Teams" button.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No teams found",
                fontSize = 20.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddTeam,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                colors = ButtonDefaults.buttonColors(CalypsoRed)
            ) {
                Text(text = "Add New Teams")
            }
        }
    } else {
        // Define placeholder text.
        val teamPlaceholder = "Select a team"

        // Build items lists: include the placeholder only if no team is yet selected.
        val team1Items = if (selectedTeam1.isEmpty()) {
            listOf(teamPlaceholder) + teams.map { it.name }
        } else {
            teams.map { it.name }
        }
        // If no team is selected, show the placeholder.
        val team1SelectedValue = selectedTeam1.ifEmpty { teamPlaceholder }

        val team2Items = if (selectedTeam2.isEmpty()) {
            listOf(teamPlaceholder) + teams.map { it.name }
        } else {
            teams.map { it.name }
        }
        val team2SelectedValue = selectedTeam2.ifEmpty { teamPlaceholder }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Teams",
                fontSize = 20.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Column for Team 1
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModernDropdown(
                        items = team1Items,
                        selectedValue = team1SelectedValue,
                        displayMapper = { it },
                        onValueChange = { newValue ->
                            if (newValue == teamPlaceholder) return@ModernDropdown
                            onTeam1Change(newValue)
                            val team = teams.find { it.name == newValue }
                            onLeftLogoUrlChange(team?.logo)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    val team1LogoUrl = teams.find { it.name == selectedTeam1 }?.logo
                    if (!team1LogoUrl.isNullOrEmpty()) {
                        val painter = rememberAsyncImagePainter(model = team1LogoUrl)
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = "Team 1 Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                            if (painter.state is AsyncImagePainter.State.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = CalypsoRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Column for Team 2
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModernDropdown(
                        items = team2Items,
                        selectedValue = team2SelectedValue,
                        displayMapper = { it },
                        onValueChange = { newValue ->
                            if (newValue == teamPlaceholder) return@ModernDropdown
                            onTeam2Change(newValue)
                            val team = teams.find { it.name == newValue }
                            onRightLogoUrlChange(team?.logo)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    val team2LogoUrl = teams.find { it.name == selectedTeam2 }?.logo
                    if (!team2LogoUrl.isNullOrEmpty()) {
                        val painter = rememberAsyncImagePainter(model = team2LogoUrl)
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = "Team 2 Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                            if (painter.state is AsyncImagePainter.State.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = CalypsoRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}