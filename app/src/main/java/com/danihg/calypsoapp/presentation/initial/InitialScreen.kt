package com.danihg.calypsoapp.presentation.initial

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.data.FirestoreManager
import com.danihg.calypsoapp.ui.theme.BackgroundButton
import com.danihg.calypsoapp.ui.theme.Black
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import com.danihg.calypsoapp.ui.theme.SelectedField
import com.danihg.calypsoapp.ui.theme.ShapeButton
import com.danihg.calypsoapp.ui.theme.UnselectedField
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Preview
@Composable
fun InitialScreen(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    navigateToHome: () -> Unit = {},
    navigateToSignup: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()

    var isLoginView by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(sharedPreferences.getString("email", "") ?: "") }
    var password by remember { mutableStateOf(sharedPreferences.getString("password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(sharedPreferences.getBoolean("rememberMe", false)) }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }

    var googleSignInClient by remember { mutableStateOf<GoogleSignInClient?>(null) }
    val firestoreManager = remember { FirestoreManager() }

    LaunchedEffect(Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleSignIn", "Google sign in succeeded")
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).await()
                Log.d("GoogleSignIn", "Firebase auth succeeded")
                firestoreManager.initializeUserData()
                navigateToHome()
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Google sign in failed", e)
                loginError = ""
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Firebase auth failed", e)
                loginError = "Authentication failed. Please try again."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 900f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.logo_calypso),
            contentDescription = "Calypso Logo",
            modifier = Modifier.size(300.dp, 150.dp),
            colorFilter = ColorFilter.tint(CalypsoRed)
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

        if (isLoginView) {
            // Login View
            TextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .background(UnselectedField, RoundedCornerShape(8.dp)),
                placeholder = { Text(text = "Enter your email", color = Color.White.copy(alpha = 0.7F)) },
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
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 36.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .background(UnselectedField, RoundedCornerShape(8.dp)),
                placeholder = { Text(text = "Enter your password", color = Color.White.copy(alpha = 0.7F)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.White,
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                    )
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = UnselectedField,
                    focusedContainerColor = SelectedField
                )
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CalypsoRed,
                        uncheckedColor = Color.White
                    )
                )
                Text("Remember me", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = true
                    } else {
                        emailError = false
                        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (rememberMe) {
                                    // Save credentials if "Remember me" is checked
                                    sharedPreferences.edit().apply {
                                        putString("email", email)
                                        putString("password", password)
                                        putBoolean("rememberMe", true)
                                        apply()
                                    }
                                } else {
                                    // Clear saved credentials if "Remember me" is unchecked
                                    sharedPreferences.edit().clear().apply()
                                }
                                coroutineScope.launch {
                                    firestoreManager.initializeUserData()
                                    navigateToHome()
                                }
                            } else {
                                loginError = "Invalid email or password. Please try again."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
            ) {
                Text(text = "Login", color = Black, fontWeight = FontWeight.Bold)
            }
            if (loginError.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = loginError,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Return to sign up",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(24.dp)
                    .clickable { isLoginView = false }
            )
        } else {
            // Sign Up View
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
            CustomButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            googleSignInClient?.signOut()?.await()
                            Log.d("GoogleSignIn", "Signed out of previous Google account")
                            googleSignInClient?.signInIntent?.let { googleSignInLauncher.launch(it) }
                        } catch (e: Exception) {
                            Log.e("GoogleSignIn", "Error during sign out or launch", e)
                            loginError = "Failed to start Google Sign-In. Please try again."
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Log In",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(24.dp)
                    .clickable { isLoginView = true }
            )
        }
//        if (loginError.isNotEmpty()) {
//            Spacer(Modifier.height(8.dp))
//            Text(
//                text = loginError,
//                color = Color.Red,
//                fontSize = 14.sp,
//                modifier = Modifier.fillMaxWidth(),
//                textAlign = TextAlign.Center
//            )
//        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CustomButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 32.dp)
            .background(BackgroundButton)
            .border(2.dp, ShapeButton, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painterResource(id = R.drawable.google_icon),
            contentDescription = "Google Icon",
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp, 24.dp)
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
