package com.danihg.calypsoapp.presentation.signup

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
fun SignupScreen(auth: FirebaseAuth, navigateToHome: () -> Unit, navigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verifyPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var verifyPasswordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var signupError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Sign Up",
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        // Email TextField
        TextField(
            value = email,
            onValueChange = { email = it },
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
                focusedContainerColor = SelectedField,
                errorContainerColor = SelectedField,
                errorIndicatorColor = White
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

        // Password TextField
        TextField(
            value = password,
            onValueChange = { password = it },
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

        Spacer(Modifier.height(16.dp))

        // Verify Password TextField
        TextField(
            value = verifyPassword,
            onValueChange = { verifyPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(UnselectedField, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            placeholder = { Text(text = "Verify your password", color = White.copy(alpha = 0.7F)) },
            singleLine = true,
            visualTransformation = if (verifyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon =
                    if (verifyPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = if (verifyPasswordVisible) "Hide password" else "Show password",
                    tint = White,
                    modifier = Modifier.clickable { verifyPasswordVisible = !verifyPasswordVisible }
                )
            },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = UnselectedField,
                focusedContainerColor = SelectedField
            )
        )

        Spacer(Modifier.height(32.dp))

        // Sign Up Button
        Button(
            onClick = {
                // Basic field validations
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailError = true
                } else if (password != verifyPassword) {
                    passwordError = true
                    signupError = "Passwords do not match."
                } else {
                    emailError = false
                    passwordError = false

                    // Check if the email already exists.
                    auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val signInMethods = task.result?.signInMethods
                            // If the list is not empty, then there is already an account registered with this email.
                            if (signInMethods != null && signInMethods.isNotEmpty()) {
                                signupError = "This email is already in use."
                            } else {
                                // Otherwise, continue with the account creation.
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { createTask ->
                                        if (createTask.isSuccessful) {
                                            navigateToHome()
                                            Log.i("aris", "SIGNUP OK")
                                        } else {
                                            signupError = createTask.exception?.message ?: "Sign up failed. Please try again."
                                            Log.i("aris", "SIGNUP KO: ${createTask.exception?.message}")
                                        }
                                    }
                            }
                        } else {
                            signupError = "An error occurred. Please try again."
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
                text = "Sign Up",
                color = Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Signup Error Message
        if (signupError.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = signupError,
                color = androidx.compose.ui.graphics.Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        // Go to Login
        Text(
            text = "Already have an account? Login",
            color = White,
            modifier = Modifier.clickable { navigateToLogin() },
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}
