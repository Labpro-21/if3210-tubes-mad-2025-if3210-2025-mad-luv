package com.kolee.composemusicexoplayer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.kolee.composemusicexoplayer.presentation.music_screen.MusicScreen
import com.kolee.composemusicexoplayer.presentation.library.LibraryScreen
import com.kolee.composemusicexoplayer.presentation.profil_screen.ProfileScreen
import com.kolee.composemusicexoplayer.presentation.qr.QRScanScreen
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModel
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.presentation.online_song.OnlineSongsViewModel

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun Navigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel,
    profileViewModel: ProfileViewModel,
    onlineSongsVM: OnlineSongsViewModel,
    networkSensing: NetworkSensing
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MusicScreen(playerVM = playerViewModel, onlineSongsVM, navController, profileViewModel, networkSensing)
        }
        composable("library") {
            LibraryScreen(playerVM = playerViewModel, navController, networkSensing)
        }
        composable("scan_qr") {
            QRScanScreen(
                playerViewModel = playerViewModel,
                navController = navController,
                networkSensing = networkSensing
            )
        }
        composable("profile") {
            ProfileScreen(viewModel = profileViewModel, playerViewModel, authViewModel, networkSensing)
        }
    }
}
