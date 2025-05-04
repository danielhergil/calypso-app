package com.danihg.calypsoapp

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.danihg.calypsoapp.data.FirestoreManager
import com.danihg.calypsoapp.model.AddTeamViewModel
import com.danihg.calypsoapp.presentation.addteam.AddTeamScreen
import com.danihg.calypsoapp.presentation.home.HomeScreen
import com.danihg.calypsoapp.presentation.initial.InitialScreen
import com.danihg.calypsoapp.presentation.login.LoginScreen
import com.danihg.calypsoapp.presentation.signup.SignupScreen
import com.danihg.calypsoapp.presentation.camera.CameraScreen
import com.danihg.calypsoapp.presentation.settings.SettingsScreen
import com.danihg.calypsoapp.presentation.splash.SplashScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper (navHostController: NavHostController, auth: FirebaseAuth) {

    NavHost(navController = navHostController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navHostController.navigate("initial") {
                        popUpTo("splash") { inclusive = true } // remove splash from backstack
                    }
                }
            )
        }
        composable("initial") {
            InitialScreen(
                navigateToHome = { navHostController.navigate("home") },
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
                navigateToSettings = { navHostController.navigate("settings") },
                navigateToOverlay = { navHostController.navigate("overlay") }
            )
        }
        composable(
            route = "camera",
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            CameraScreen(navHostController = navHostController)
        }
        composable("addTeam") {
            val firestoreManager = FirestoreManager()
            AddTeamScreen(firestoreManager)
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}