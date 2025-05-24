package com.kolee.composemusicexoplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.material.*
import com.kolee.composemusicexoplayer.data.auth.AuthViewModel
import com.kolee.composemusicexoplayer.data.auth.AuthViewModelFactory
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.presentation.navigation.BottomNavItem
import com.kolee.composemusicexoplayer.presentation.navigation.Navigation
import com.kolee.composemusicexoplayer.presentation.navigation.ResponsiveNavigationBar
import com.kolee.composemusicexoplayer.presentation.permission.CheckAndRequestPermissions
import com.kolee.composemusicexoplayer.ui.theme.ComposeMusicExoPlayerTheme
import LoginScreen
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.google.accompanist.navigation.material.BottomSheetNavigator
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModel
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModelFactory
import com.kolee.composemusicexoplayer.presentation.online_song.OnlineSongsViewModel


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(applicationContext)
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(UserPreferences(applicationContext))
    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleDeepLink(intent)
        setContent {
            ComposeMusicExoPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CheckAndRequestPermissions() {
                        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                        val sheetState = rememberModalBottomSheetState(
                            initialValue = ModalBottomSheetValue.Hidden
                        )
                        val bottomSheetNavigator = remember { BottomSheetNavigator(sheetState) }
                        val navController = rememberNavController(bottomSheetNavigator)
                        val networkSensing = remember { NetworkSensing(applicationContext) }

                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        val bottomNavItems = listOf(
                            BottomNavItem("Home", "home", Icons.Filled.Home),
                            BottomNavItem("Library", "library", Icons.Filled.LibraryMusic),
                            BottomNavItem("Profile", "profile", Icons.Filled.Person)
                        )

                        ModalBottomSheetLayout(
                            bottomSheetNavigator = bottomSheetNavigator
                        ) {
                            if (isLoggedIn) {
                                val configuration = LocalConfiguration.current
                                val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT


                                val playerViewModel: PlayerViewModel = hiltViewModel()
                                val onlineSongsViewModel: OnlineSongsViewModel = hiltViewModel()
                                val scaffoldContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
                                    if (isPortrait) {
                                        Box(
                                            modifier = Modifier
                                                .padding(innerPadding)
                                                .fillMaxSize()
                                        ) {
                                            Navigation(
                                                navController = navController,
                                                authViewModel = authViewModel,
                                                playerViewModel = playerViewModel,
                                                profileViewModel = profileViewModel,
                                                onlineSongsVM= onlineSongsViewModel,
                                                networkSensing = networkSensing
                                            )
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            ResponsiveNavigationBar(
                                                items = bottomNavItems,
                                                currentRoute = currentRoute ?: "home",
                                                onItemClick = { route ->
                                                    if (route != currentRoute) {
                                                        navController.navigate(route) {
                                                            popUpTo(navController.graph.startDestinationId) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .padding(innerPadding)
                                                    .fillMaxSize()
                                            ) {
                                                Navigation(
                                                    navController = navController,
                                                    authViewModel = authViewModel,
                                                    playerViewModel = playerViewModel,
                                                    profileViewModel = profileViewModel,
                                                    onlineSongsVM= onlineSongsViewModel,
                                                    networkSensing = networkSensing
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isPortrait) {
                                    Scaffold(
                                        bottomBar = {
                                            ResponsiveNavigationBar(
                                                items = bottomNavItems,
                                                currentRoute = currentRoute ?: "home",
                                                onItemClick = { route ->
                                                    if (route != currentRoute) {
                                                        navController.navigate(route) {
                                                            popUpTo(navController.graph.startDestinationId) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                        content = scaffoldContent
                                    )
                                } else {
                                    Scaffold(content = scaffoldContent)
                                }
                            } else {
                                LoginScreen(
                                    context = this@MainActivity,
                                    onLoginSuccess = {
                                        authViewModel.setLoggedIn(true)
                                    },
                                    networkSensing = networkSensing
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                if (uri.scheme == "purrytify" && uri.host == "song") {
                    val songId = uri.lastPathSegment ?: return

                    val playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
                    playerViewModel.fetchAndPlaySharedSong(songId)
                }
            }
        }
    }
}
