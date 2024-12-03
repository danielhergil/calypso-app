package com.danihg.calypsoapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.danihg.calypsoapp.presentation.home.HomeScreen
import com.danihg.calypsoapp.presentation.initial.InitialScreen
import com.danihg.calypsoapp.presentation.login.LoginScreen
import com.danihg.calypsoapp.presentation.signup.SignupScreen
import com.danihg.calypsoapp.presentation.camera.CameraScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper (navHostController: NavHostController, auth: FirebaseAuth) {

    NavHost(navController = navHostController, startDestination = "initial") {
        composable("initial"){
            InitialScreen(
                navigateToLogin = { navHostController.navigate("login") },
                navigateToSignup = { navHostController.navigate("signup") }
            )
        }
        composable("login"){
            LoginScreen(auth, navigateToHome = { navHostController.navigate("home") })
        }
        composable("signup"){
            SignupScreen(auth, navigateToHome = { navHostController.navigate("home") }, navigateToLogin = { navHostController.navigate("login") })
        }
        composable("home"){
            HomeScreen(
                navigateToCamera = { navHostController.navigate("camera") },
                navigateToAddTeam = { navHostController.navigate("addTeam") },
                navigateToLibrary = { navHostController.navigate("library") },
                navigateToDeploy = { navHostController.navigate("deploy") },
                navigateToOverlay = { navHostController.navigate("overlay") }
            )
        }
        composable("camera") {
            CameraScreen()
        }
    }
}