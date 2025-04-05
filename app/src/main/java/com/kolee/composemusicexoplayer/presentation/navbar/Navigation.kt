package com.kolee.composemusicexoplayer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kolee.composemusicexoplayer.presentation.music_screen.MusicScreen
import com.kolee.composemusicexoplayer.presentation.library.LibraryScreen
import com.kolee.composemusicexoplayer.presentation.profile.ProfileScreen
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel

@Composable
fun Navigation(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MusicScreen(viewModel = authViewModel)
        }
        composable("library") {
            LibraryScreen()
        }
        composable("profile") {
            ProfileScreen()
        }
    }
}