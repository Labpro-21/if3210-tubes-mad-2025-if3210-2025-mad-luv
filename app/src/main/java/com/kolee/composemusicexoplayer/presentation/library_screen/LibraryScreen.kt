package com.kolee.composemusicexoplayer.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet.MusicPlayerSheet
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerHeight
import com.kolee.composemusicexoplayer.presentation.component.BottomMusicPlayerImpl
import com.kolee.composemusicexoplayer.presentation.component.MusicItem
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

// ini baru nambahin liked songs buat ngetes doang, nanti bagian all songs tambahin aja yak
@Composable
fun LibraryScreen(
    playerVM: PlayerViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val musicUiState by playerVM.uiState.collectAsState()
    val likedSongs = musicUiState.musicList.filter { it.loved }

    val isMusicPlaying = musicUiState.currentPlayedMusic != MusicEntity.default
    var open = musicUiState.isPlayerExpanded

    LaunchedEffect(key1 = musicUiState.currentPlayedMusic) {
        val isShowed = musicUiState.currentPlayedMusic != MusicEntity.default
        playerVM.onEvent(PlayerEvent.SetShowBottomPlayer(isShowed))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Liked Songs",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (likedSongs.isEmpty()) {
                Text(
                    text = "No liked songs yet.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.body1
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = BottomMusicPlayerHeight.value),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(likedSongs) { music ->
                        MusicItem(
                            music = music,
                            selected = music.audioId == musicUiState.currentPlayedMusic.audioId,
                            isMusicPlaying = musicUiState.isPlaying,
                            isHorizontal = false,
                            onClick = {
                                playerVM.onEvent(PlayerEvent.Play(music))
                            }
                        )
                    }
                }
            }
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
                        playerVM.onEvent(PlayerEvent.PlayPause(isMusicPlaying))
                    },
                    onExpand = { playerVM.setPlayerExpanded(true) }
                )
            }
        }
    }
}
