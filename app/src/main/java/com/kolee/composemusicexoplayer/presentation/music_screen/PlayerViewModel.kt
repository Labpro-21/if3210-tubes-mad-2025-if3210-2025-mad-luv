package com.kolee.composemusicexoplayer.presentation.music_screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.data.roomdb.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val environment: PlayerEnvironment,
    private val musicRepository: MusicRepository
) : StatefulViewModel<MusicUiState>(MusicUiState()) {

    init {
        viewModelScope.launch {
            environment.getAllMusic().collect { musics ->
                updateState { copy(musicList = musics) }
            }
        }

        viewModelScope.launch {
            environment.getCurrentPlayedMusic().collect { music ->
                updateState { copy(currentPlayedMusic = music) }
            }
        }

        viewModelScope.launch {
            environment.isPlaying().collect { isPlaying ->
                updateState { copy(isPlaying = isPlaying) }
            }
        }

        viewModelScope.launch {
            environment.isBottomMusicPlayerShowed().collect { isShowed ->
                updateState { copy(isBottomPlayerShow = isShowed) }
            }
        }

        viewModelScope.launch {
            environment.getCurrentDuration().collect { currentPos ->
                updateState { copy(currentDuration = currentPos) }
            }
        }

        viewModelScope.launch {
            environment.playbackMode.collect { mode ->
                updateState { copy(playbackMode = mode) }
            }
        }


    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Play -> {
                viewModelScope.launch {
                    val updatedMusic = event.musicEntity.copy(lastPlayedAt = System.currentTimeMillis())
                    environment.play(updatedMusic)
                    environment.setShowButtonMusicPlayer(true)

                    val updatedList = uiState.value.musicList.map {
                        if (it.audioId == updatedMusic.audioId) updatedMusic else it
                    }
                    updateState { copy(musicList = updatedList) }
                }
            }

            is PlayerEvent.PlayPause -> {
                viewModelScope.launch {
                    if (event.isPlaying) environment.pause()
                    else environment.resume()
                }
            }

            is PlayerEvent.Next -> {
                viewModelScope.launch { environment.next() }
            }

            is PlayerEvent.Previous -> {
                viewModelScope.launch { environment.previous() }
            }

            is PlayerEvent.SetShowBottomPlayer -> {
                viewModelScope.launch {
                    environment.setShowButtonMusicPlayer(event.isShowed)
                }
            }

            is PlayerEvent.UpdateProgress -> {
                viewModelScope.launch {
                    environment.updateCurrentDuration(event.newDuration)
                    updateState { copy(currentDuration = event.newDuration) }
                }
            }

            is PlayerEvent.RefreshMusicList -> {
                viewModelScope.launch { environment.refreshMusicList() }
            }

            is PlayerEvent.SnapTo -> {
                viewModelScope.launch { environment.snapTo(event.duration) }
            }

            is PlayerEvent.updateMusicList -> {
                viewModelScope.launch { environment.updateMusicList(event.musicList) }
            }

            is PlayerEvent.addMusic -> {
                viewModelScope.launch { environment.addMusicAndRefresh(event.music) }
            }

            is PlayerEvent.EditMusic -> {
                viewModelScope.launch {
                    environment.updateMusic(event.updatedMusic)

                    val updatedList = uiState.value.musicList.map {
                        if (it.audioId == event.updatedMusic.audioId) event.updatedMusic else it
                    }

                    updateState {
                        copy(
                            musicList = updatedList,
                            currentPlayedMusic = if (currentPlayedMusic.audioId == event.updatedMusic.audioId)
                                event.updatedMusic
                            else currentPlayedMusic
                        )
                    }
                }
            }

            is PlayerEvent.DeleteMusic -> {
                viewModelScope.launch {
                    environment.deleteMusic(event.music)

                    val updatedList = uiState.value.musicList.filterNot {
                        it.audioId == event.music.audioId
                    }

                    updateState {
                        copy(
                            musicList = updatedList,

                        )
                    }
                }
            }

            is PlayerEvent.SetPlaybackMode -> {
                viewModelScope.launch {
                    environment.setPlaybackMode(event.mode)
                }
            }

            is PlayerEvent.TogglePlaybackMode -> {
                viewModelScope.launch {
                    environment.togglePlaybackMode()
                }
            }

            is PlayerEvent.ToggleShuffle -> {
                viewModelScope.launch {
                    environment.toggleShuffle()
                    updateState { copy(isShuffleEnabled = environment.isShuffleEnabled.value) }
                }
            }

            is PlayerEvent.ToggleLoved -> {
                viewModelScope.launch {
                    val updatedMusic = event.music.copy(loved = !event.music.loved)

                    val updatedList = uiState.value.musicList.map {
                        if (it.audioId == updatedMusic.audioId) updatedMusic else it
                    }

                    updateState {
                        copy(
                            musicList = updatedList,
                            currentPlayedMusic = if (currentPlayedMusic.audioId == updatedMusic.audioId)
                                updatedMusic
                            else currentPlayedMusic
                        )
                    }
                }
            }
        }
    }

    fun getRecentlyPlayed(): List<MusicEntity> {
        return uiState.value.musicList
            .sortedByDescending { it.lastPlayedAt }
    }

    fun getLoved(): List<MusicEntity> {
        return uiState.value.musicList
            .filter { it.loved }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        updateState { copy(isPlayerExpanded = expanded) }
    }

    fun getListenedSongs(): List<MusicEntity> {
        return uiState.value.musicList
            .filter { (it.lastPlayedAt ?: 0) > 0 }
    }
}
