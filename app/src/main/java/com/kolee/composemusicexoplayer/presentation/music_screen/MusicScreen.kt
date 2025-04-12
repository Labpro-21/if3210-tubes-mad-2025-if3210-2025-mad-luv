
package com.kolee.composemusicexoplayer.presentation.music_screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerHeight
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerImpl
import com.kolee.composemusicexoplayer.presentation.component.MusicItem
import com.kolee.composemusicexoplayer.presentation.component.NetworkSensingScreen

private const val TAG = "MusicScreen"

@Composable
fun MusicScreen(
    playerVM: PlayerViewModel = hiltViewModel(),
    navController: NavHostController,
    networkSensing: NetworkSensing
) {
    val context = LocalContext.current
    val musicUiState by playerVM.uiState.collectAsState()
    var open = musicUiState.isPlayerExpanded
    val isMusicPlaying = musicUiState.currentPlayedMusic != MusicEntity.default

    LaunchedEffect(key1 = musicUiState.currentPlayedMusic) {
        val isShowed = (musicUiState.currentPlayedMusic != MusicEntity.default)
        playerVM.onEvent(PlayerEvent.SetShowBottomPlayer(isShowed))

    }

    NetworkSensingScreen(
        networkSensing = networkSensing,
//        showFallbackPage = !isConnected && profileState == null
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MusicSection(
                    title = "New Songs",
                    musicList = musicUiState.musicList.reversed(),
                    isHorizontal = true,
                    musicUiState = musicUiState,
                    onSelectedMusic = { playerVM.onEvent(PlayerEvent.Play(it)) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recently Played
                MusicSection(
                    title = "Recently Played",
                    musicList = playerVM.getRecentlyPlayed(),
                    isHorizontal = false,
                    musicUiState = musicUiState,
                    onSelectedMusic = { playerVM.onEvent(PlayerEvent.Play(it)) }
                )


            }

            if (isMusicPlaying) {
                if (open) {
                    MusicPlayerSheet(
                        playerVM = playerVM,
                        navController = navController,
                        onCollapse = { playerVM.setPlayerExpanded(false) }
                    )
                } else {
                    BottomMusicPlayerImpl(
                        playerVM = playerVM,
                        musicUiState = musicUiState,
                        onPlayPauseClicked = {
                            playerVM.onEvent(PlayerEvent.PlayPause(musicUiState.isPlaying))
                        },
                        onExpand = {playerVM.setPlayerExpanded(true) }
                    )
                }
            }
        }
    }

    ComposableLifeCycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                Log.d(TAG, "MusicScreen: ON_RESUME")
                playerVM.onEvent(PlayerEvent.RefreshMusicList)
            }

            Lifecycle.Event.ON_PAUSE -> {
                Log.d(TAG, "MusicScreen: ON_PAUSE")
            }

            else -> {}
        }
    }
}

@Composable
fun MusicSection(
    title: String,
    musicList: List<MusicEntity>,
    isHorizontal: Boolean,
    musicUiState: MusicUiState,
    onSelectedMusic: (MusicEntity) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isHorizontal) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(25.dp)
            ) {
                items(musicList) { music ->
                    MusicItem(
                        music = music,
                        selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                        isMusicPlaying = musicUiState.isPlaying,
                        isHorizontal = isHorizontal,
                        onClick = {

                            onSelectedMusic(music) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = BottomMusicPlayerHeight.value),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                items(musicList) { music ->
                    MusicItem(
                        music = music,
                        selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                        isMusicPlaying = musicUiState.isPlaying,
                        isHorizontal = false,
                        onClick = { onSelectedMusic(music) }
                    )
                }
            }
        }
    }
}

@Composable
fun ComposableLifeCycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (source: LifecycleOwner, event: Lifecycle.Event) -> Unit
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            onEvent(source, event)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
