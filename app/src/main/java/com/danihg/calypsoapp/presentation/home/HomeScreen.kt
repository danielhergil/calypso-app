package com.danihg.calypsoapp.presentation.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.Black
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import kotlinx.coroutines.delay

@SuppressLint("SourceLockedOrientationActivity")
@Preview
@Composable
fun HomeScreen(
    navigateToCamera: () -> Unit = {},
    navigateToAddTeam: () -> Unit = {},
    navigateToLibrary: () -> Unit = {},
    navigateToSettings: () -> Unit = {},
    navigateToOverlay: () -> Unit = {}
) {

    var isNavigatingToCamera by remember { mutableStateOf(false) }

    LaunchedEffect(isNavigatingToCamera) {
        if (isNavigatingToCamera) {
            // Delay the actual navigation to allow for smooth transition
            delay(100)
            navigateToCamera()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ModernCard(
            title = "Camera",
            icon = R.drawable.ic_camera,
            onClick = {
                isNavigatingToCamera = true
            },
            modifier = Modifier.fillMaxWidth()
        )
        ModernCard(
            title = "Add Team",
            icon = R.drawable.ic_add_2,
            onClick = navigateToAddTeam,
            modifier = Modifier.fillMaxWidth()
        )
//        ModernCard(
//            title = "Library",
//            icon = R.drawable.ic_library,
//            onClick = navigateToLibrary,
//            modifier = Modifier.fillMaxWidth()
//        )
//        ModernCard(
//            title = "Settings",
//            icon = R.drawable.ic_settings,
//            onClick = navigateToSettings,
//            modifier = Modifier.fillMaxWidth()
//        )
//        ModernCard(
//            title = "Overlay",
//            icon = R.drawable.ic_overlay,
//            onClick = navigateToOverlay,
//            modifier = Modifier.fillMaxWidth()
//        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ModernCard(title: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            // Before the height was 130.dp
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Black,
            contentColor = White
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            White.copy(alpha = 0.05f),
                            White.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = title,
                    modifier = Modifier.size(100.dp),
                    tint = CalypsoRed
                )
                Text(
                    text = title,
                    color = White,
                    fontSize = 24.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}