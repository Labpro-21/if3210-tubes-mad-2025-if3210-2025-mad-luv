package com.kolee.composemusicexoplayer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.kolee.composemusicexoplayer.presentation.music_screen.MusicScreen
import com.kolee.composemusicexoplayer.presentation.library.LibraryScreen
import com.kolee.composemusicexoplayer.presentation.profil_screen.ProfileScreen
import com.kolee.composemusicexoplayer.presentation.soundcapsule_screen.TopArtistsDetailPage
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModel
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.presentation.online_song.OnlineSongsViewModel
import com.kolee.composemusicexoplayer.presentation.soundcapsule_screen.TimeDetailPage
import com.kolee.composemusicexoplayer.presentation.soundcapsule_screen.TopSongsDetailPage

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
        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel,
                playerViewModel = playerViewModel,
                authViewModel = authViewModel,
                networkSensing = networkSensing,
                onTopArtistsClick = { topArtists ->
                    // Simpan data ke PlayerViewModel untuk digunakan di TopArtistsDetailPage
                    playerViewModel.setTopArtistsForDetail(topArtists)
                    // Navigate ke TopArtistsDetailPage
                    navController.navigate("top_artists_detail")
                },
                onTopSongsClick = { topSongs ->
                    // Simpan data ke PlayerViewModel untuk digunakan di TopArtistsDetailPage
                    playerViewModel.setTopSongsForDetail(topSongs)
                    // Navigate ke TopArtistsDetailPage
                    navController.navigate("top_songs_detail")
                },
                onTimeDetailClick = { timeDetail ->
                    // Simpan data ke PlayerViewModel untuk digunakan di TopArtistsDetailPage
                    playerViewModel.setDailyListeningTimeForDetail(timeDetail)
                    // Navigate ke TopArtistsDetailPage
                    navController.navigate("time_daily_detail")
                },
            )
        }
        composable("top_artists_detail") {
            val topArtists by playerViewModel.topArtistsForDetail.collectAsState()
            TopArtistsDetailPage(
                topArtists = topArtists,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable("top_songs_detail") {
            val topSongs by playerViewModel.topSongsForDetail.collectAsState()
            TopSongsDetailPage(
                topSongs = topSongs,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable("time_daily_detail") {
            val timeDetail by playerViewModel.dailyListeningTimeForDetail.collectAsState()
            TimeDetailPage(
                dailyStats = timeDetail,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}