package com.kolee.composemusicexoplayer.presentation.music_screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val environment: PlayerEnvironment
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
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Play -> {
                viewModelScope.launch {
                    environment.play(event.musicEntity)
                    environment.setShowButtonMusicPlayer(true)}
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

            is PlayerEvent.RefreshMusicList -> {
                viewModelScope.launch { environment.refreshMusicList() }
            }

            is PlayerEvent.SnapTo -> {
                viewModelScope.launch { environment.snapTo(event.duration) }
            }

            is PlayerEvent.updateMusicList -> {
                viewModelScope.launch { environment.updateMusicList(event.musicList) }
            }
        }
    }
}
