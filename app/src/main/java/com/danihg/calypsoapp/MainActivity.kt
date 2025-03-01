package com.danihg.calypsoapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.danihg.calypsoapp.ui.theme.CalypsoAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

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

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()

        auth = Firebase.auth
        requestPermissions()
        setContent {
            val navHostController = rememberNavController()
            val isInitialLaunch = remember { mutableStateOf(true) } // Tracks if this is the first launch

//            // Restore the last route if available
//            val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)
//            val lastRoute = sharedPreferences.getString("lastRoute", null)
//            if (!lastRoute.isNullOrEmpty()) {
//                lifecycleScope.launch {
//                    navHostController.navigate(lastRoute) {
//                        popUpTo(0) // Clear any previous routes
//                    }
//                }
//            }
            CalypsoAppTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) {
                    NavigationWrapper(navHostController, auth)
                }
            }

            // Observe lifecycle and handle navigation
            HandleLifecycleNavigation(navHostController, isInitialLaunch)

//            // Save the current route when the app is paused
//            DisposableEffect(navHostController) {
//                val lifecycleObserver = LifecycleEventObserver { _, event ->
//                    if (event == Lifecycle.Event.ON_PAUSE) {
//                        val currentRoute = navHostController.currentBackStackEntry?.destination?.route
//                        sharedPreferences.edit().putString("lastRoute", currentRoute).apply()
//                    }
//                }
//                lifecycle.addObserver(lifecycleObserver)
//                onDispose { lifecycle.removeObserver(lifecycleObserver) }
//            }
        }
    }

    @Composable
    private fun HandleLifecycleNavigation(
        navHostController: NavHostController,
        isInitialLaunch: MutableState<Boolean>
    ) {
        val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)
        val lastRoute = sharedPreferences.getString("lastRoute", null)

        DisposableEffect(Unit) {
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (isInitialLaunch.value) {
                            isInitialLaunch.value = false // Mark the first launch as handled
                            navHostController.currentBackStackEntry?.destination?.let {
                                // Navigate to the appropriate screen only if the graph is ready
                                if (auth.currentUser == null && lastRoute.isNullOrEmpty()) {
                                    navHostController.navigate("initial") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else if (!lastRoute.isNullOrEmpty() && navHostController.currentDestination?.route != lastRoute) {
                                    navHostController.navigate(lastRoute) {
                                        popUpTo(0)
                                    }
                                }
                            }
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        val currentRoute = navHostController.currentBackStackEntry?.destination?.route
                        sharedPreferences.edit().putString("lastRoute", currentRoute).apply()
                    }
                    else -> {}
                }
            }

            lifecycle.addObserver(lifecycleObserver)
            onDispose { lifecycle.removeObserver(lifecycleObserver) }
        }
    }

    override fun onStart() {
        super.onStart()
        window.decorView.post {
            val currentUser = auth.currentUser

            // Only navigate to "initial" if the user is not logged in and no route is set
            if (currentUser == null) {
                val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)
                val lastRoute = sharedPreferences.getString("lastRoute", null)

                if (lastRoute.isNullOrEmpty()) {
                    setContent {
                        val navHostController = rememberNavController()
                        CalypsoAppTheme {
                            NavigationWrapper(navHostController, auth)
                            navHostController.navigate("initial") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Add Android 14+ specific permissions
        if (Build.VERSION.SDK_INT >= 34) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
        }

        permissionsLauncher.launch(permissions.toTypedArray())
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
