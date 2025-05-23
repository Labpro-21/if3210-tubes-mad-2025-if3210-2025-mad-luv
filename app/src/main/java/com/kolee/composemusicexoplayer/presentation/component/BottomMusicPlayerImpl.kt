package com.kolee.composemusicexoplayer.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kolee.composemusicexoplayer.presentation.music_screen.MusicUiState
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

@Composable
fun BoxScope.BottomMusicPlayerImpl(
    musicUiState: MusicUiState,
    onPlayPauseClicked: (isPlaying: Boolean) -> Unit,
    playerVM: PlayerViewModel,
    onExpand: () -> Unit
) {
    AnimatedVisibility(
        visible = musicUiState.isBottomPlayerShow,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
    ) {
        BottomMusicPlayer(
            currentMusic = musicUiState.currentPlayedMusic,
            currentDuration = musicUiState.currentDuration,
            isPlaying = musicUiState.isPlaying,
            onClick = onExpand,
            playerVM = playerVM,
            onPlayPauseClicked = onPlayPauseClicked,
        )
    }
}
