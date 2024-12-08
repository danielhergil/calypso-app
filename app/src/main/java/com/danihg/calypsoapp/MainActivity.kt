package com.danihg.calypsoapp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.danihg.calypsoapp.ui.theme.CalypsoAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private lateinit var navHostController: NavHostController
    private lateinit var auth: FirebaseAuth

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }

        if (deniedPermissions.isNotEmpty()) {
            // Handle denied permissions
            showToast("Some permissions were denied: ${deniedPermissions.keys.joinToString()}")
        } else {
            // All permissions granted
            showToast("All permissions granted")
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()

        auth = Firebase.auth
        requestPermissions()
        setContent {
            navHostController = rememberNavController()
            CalypsoAppTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) {
                    NavigationWrapper(navHostController, auth)
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        window.decorView.post {
            val currentUser = auth.currentUser
            if (::navHostController.isInitialized && currentUser != null) {
                navHostController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    private fun requestPermissions() {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Hides system UI (status bar and navigation bar) using WindowInsetsControllerCompat.
     */
    private fun hideSystemUI() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
