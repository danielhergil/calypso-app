package com.danihg.calypsoapp.presentation.initial

import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.BackgroundButton
import com.danihg.calypsoapp.ui.theme.Black
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import com.danihg.calypsoapp.ui.theme.Green
import com.danihg.calypsoapp.ui.theme.ShapeButton

@Preview
@Composable
fun InitialScreen(navigateToLogin: () -> Unit = {}, navigateToSignup: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 900f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.calypso_logo),
            contentDescription = "",
            modifier = Modifier.size(300.dp, 150.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Calypso",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Sports Streaming", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Thin
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { navigateToSignup() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
        ) {
            Text(text = "Sign up free", color = Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        CustomButton(Modifier.clickable {  })
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Log In", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp).clickable { navigateToLogin() })
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CustomButton(modifier:Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 32.dp)
            .background(BackgroundButton)
            .border(2.dp, ShapeButton, CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painterResource(id = R.drawable.google_icon),
            contentDescription = "",
            modifier = Modifier.padding(start = 16.dp).size(24.dp, 24.dp)
        )
        Text(
            text = "Continue with Google",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

    }
}