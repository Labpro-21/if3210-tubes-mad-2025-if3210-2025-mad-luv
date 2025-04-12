package com.kolee.composemusicexoplayer.presentation.music_screen

import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity

data class MusicUiState(
    val musicList: List<MusicEntity> = emptyList(),
    val currentPlayedMusic: MusicEntity = MusicEntity.default,
    val currentDuration: Long = 0L,
    val isPlaying: Boolean = false,
    val isBottomPlayerShow: Boolean = false,
    val isPlayerExpanded : Boolean = true,
    val playbackMode: PlaybackMode = PlaybackMode.REPEAT_ALL,
    val isShuffleEnabled: Boolean = false
)
