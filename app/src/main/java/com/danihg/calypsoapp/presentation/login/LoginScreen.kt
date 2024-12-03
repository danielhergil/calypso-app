package com.danihg.calypsoapp.presentation.login

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.ui.theme.Black
import com.danihg.calypsoapp.ui.theme.SelectedField
import com.danihg.calypsoapp.ui.theme.UnselectedField
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(auth: FirebaseAuth, navigateToHome: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Login",
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        TextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(UnselectedField, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && email.isNotEmpty()) {
                        emailError = !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    }
                },
            placeholder = { Text(text = "Enter your email", color = White.copy(alpha = 0.7F)) },
            singleLine = true,
            isError = emailError,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = UnselectedField,
                focusedContainerColor = SelectedField
            )
        )
        if (emailError) {
            Text(
                text = "Please enter a valid email address.",
                color = androidx.compose.ui.graphics.Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        TextField(
            value = password, onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(UnselectedField, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            placeholder = { Text(text = "Enter your password", color = White.copy(alpha = 0.7F)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon =
                    if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = White,
                    modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                )
            },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = UnselectedField,
                focusedContainerColor = SelectedField
            )
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailError = true
                } else {
                    emailError = false
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            navigateToHome()
                            Log.i("aris", "LOGIN OK")
                        } else {
                            loginError = when (task.exception?.message) {
                                "The supplied auth credential is incorrect, malformed or has expired." ->
                                    "Invalid email or password. Please try again."
                                else -> "Login failed. Please check your credentials and try again."
                            }
                            Log.i("aris", "LOGIN KO: ${task.exception?.message}")
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = White)
        ) {
            Text(
                text = "Login",
                color = Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        // Login Error Message
        if (loginError.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = loginError,
                color = androidx.compose.ui.graphics.Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}