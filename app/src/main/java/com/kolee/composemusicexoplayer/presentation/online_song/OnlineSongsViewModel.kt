package com.kolee.composemusicexoplayer.presentation.online_song

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kolee.composemusicexoplayer.data.model.OnlineSong
import com.kolee.composemusicexoplayer.data.network.ApiClient
import com.kolee.composemusicexoplayer.data.network.ApiClient.apiService
import com.kolee.composemusicexoplayer.data.network.ApiService
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class OnlineSongsViewModel @Inject constructor(
) : ViewModel() {
    private val apiService = ApiClient.apiService
    var globalSongs by mutableStateOf<List<MusicEntity>>(emptyList())
        private set

    var countrySongs by mutableStateOf<List<MusicEntity>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadGlobalSongs() {
        viewModelScope.launch {
            try {
                val response = apiService.getTopGlobalSongs()
                globalSongs = response.map { it.toMusicEntity() }
            } catch (e: Exception) {
                errorMessage = "Failed to load global songs: ${e.localizedMessage}"
            }
        }
    }

    fun loadCountrySongs(code: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getTopSongsByCountry(code)
                countrySongs = response.map { it.toMusicEntity() }
            } catch (e: Exception) {
                errorMessage = "Failed to load country songs: ${e.localizedMessage}"
            }
        }
    }

    private fun OnlineSong.toMusicEntity(): MusicEntity {
        return MusicEntity(
            audioId = this.id.toLong(),
            title = this.title,
            artist = this.artist,
            duration = durationToMillis(this.duration),
            albumPath = this.artwork,
            audioPath = this.url,
            owner = this.country,
            lastPlayedAt = 0L,
            loved = false
        )
    }

    private fun durationToMillis(duration: String): Long {
        val parts = duration.split(":")
        val minutes = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val seconds = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        return (minutes * 60 + seconds) * 1000
    }
}
