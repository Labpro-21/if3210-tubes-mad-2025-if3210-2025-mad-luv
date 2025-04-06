package com.kolee.composemusicexoplayer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.kolee.composemusicexoplayer.presentation.music_screen.MusicScreen
import com.kolee.composemusicexoplayer.presentation.library.LibraryScreen
import com.kolee.composemusicexoplayer.presentation.profil_screen.ProfileScreen
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun Navigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MusicScreen(playerVM = playerViewModel, navController)
        }
        composable("library") {
            LibraryScreen()
        }
        composable("profile") {
            ProfileScreen(viewModel = authViewModel)
        }
        bottomSheet("music_player") {
            MusicPlayerSheet(playerVM = playerViewModel, navController)
        }
    }
}