package com.kolee.composemusicexoplayer.presentation.music_screen

import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import kotlin.time.Duration

sealed interface PlayerEvent {
    data class Play(val musicEntity: MusicEntity) : PlayerEvent
    data class PlayPause(val isPlaying: Boolean) : PlayerEvent
    data class SetShowBottomPlayer(val isShowed: Boolean) : PlayerEvent
    object RefreshMusicList : PlayerEvent
    object Next: PlayerEvent
    object Previous: PlayerEvent
    data class ToggleLoved(val music: MusicEntity) : PlayerEvent
    data class SnapTo(val duration: Long):PlayerEvent
    data class updateMusicList(val musicList: List<MusicEntity>): PlayerEvent
}
