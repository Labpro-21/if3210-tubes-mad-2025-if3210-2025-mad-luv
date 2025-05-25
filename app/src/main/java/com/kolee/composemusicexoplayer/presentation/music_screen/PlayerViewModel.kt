package com.kolee.composemusicexoplayer.presentation.music_screen

import AudioDeviceManager
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.data.roomdb.MusicRepository
import com.kolee.composemusicexoplayer.presentation.Notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val environment: PlayerEnvironment,
    val musicRepository: MusicRepository,
    private val notificationHelper: NotificationHelper
) : StatefulViewModel<MusicUiState>(MusicUiState()) {

    data class RecommendationState(
        val dailyPlaylist: List<MusicEntity> = emptyList(),
        val topMixes: Map<String, List<MusicEntity>> = emptyMap(),
        val isLoading: Boolean = false
    )

    private val _recommendationState = mutableStateOf(RecommendationState())
    val recommendationState: State<RecommendationState> = _recommendationState

    private val audioDeviceManager = AudioDeviceManager(context)

    private val _availableDevices = MutableStateFlow<List<AudioDeviceManager.AudioDevice>>(emptyList())
    val availableDevicesFlow: StateFlow<List<AudioDeviceManager.AudioDevice>> = _availableDevices.asStateFlow()

    private val _currentDevice = MutableStateFlow<AudioDeviceManager.AudioDevice?>(null)
    val currentDeviceFlow: StateFlow<AudioDeviceManager.AudioDevice?> = _currentDevice.asStateFlow()

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        initializeAudioDevices()
        notificationHelper.createNotificationChannel()

        viewModelScope.launch {
            environment.getAllMusic().collect { musics ->
                updateState { copy(musicList = musics) }
                generateRecommendations()
            }
        }

        viewModelScope.launch {
            environment.getCurrentPlayedMusic().collect { music ->
                updateState { copy(currentPlayedMusic = music) }
                if (music != MusicEntity.default) {
                    showNotification(music, uiState.value.isPlaying)
                } else {
                    cancelNotification()
                }
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

    private fun initializeAudioDevices() {
        viewModelScope.launch {
            try {
                // Get available devices from AudioDeviceManager
                val devices = audioDeviceManager.availableDevices.value
                if (devices != null) {
                    _availableDevices.value = devices
                }

                // Set current device to the first available device or default
                val currentDevice = audioDeviceManager.currentDevice.value
                if (devices != null) {
                    _currentDevice.value = currentDevice ?: devices.firstOrNull()
                }

                if (devices != null) {
                    Log.d("PlayerViewModel", "Audio devices initialized: ${devices.size} devices found")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to initialize audio devices", e)
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

                    showNotification(updatedMusic, true)

                    val updatedList = uiState.value.musicList.map {
                        if (it.audioId == updatedMusic.audioId) updatedMusic else it
                    }
                    updateState { copy(musicList = updatedList) }
                }
            }

            is PlayerEvent.PlayPause -> {
                viewModelScope.launch {
                    if (event.isPlaying) {
                        environment.pause()
                        showNotification(uiState.value.currentPlayedMusic, false)
                    } else {
                        environment.resume()
                        showNotification(uiState.value.currentPlayedMusic, true)
                    }
                }
            }

            is PlayerEvent.Next -> {
                viewModelScope.launch {
                    environment.next()
                    showNotification(uiState.value.currentPlayedMusic, true)
                }
            }

            is PlayerEvent.Previous -> {
                viewModelScope.launch {
                    environment.previous()
                    showNotification(uiState.value.currentPlayedMusic, true)
                }
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
                        copy(musicList = updatedList)
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
                    environment.updateMusic(updatedMusic)
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

                    generateRecommendations()
                }
            }
        }
    }

    fun generateRecommendations() {
        viewModelScope.launch {
            _recommendationState.value = _recommendationState.value.copy(isLoading = true)

            val likedSongs = getLoved()
            val recentlyPlayed = getRecentlyPlayed()
            val allSongs = uiState.value.musicList

            val dailyPlaylist = generateDailyPlaylist(likedSongs, recentlyPlayed, allSongs)
            val topMixes = generateTopMixes(likedSongs)

            _recommendationState.value = RecommendationState(
                dailyPlaylist = dailyPlaylist,
                topMixes = topMixes,
                isLoading = false
            )
        }
    }

    private fun generateDailyPlaylist(
        likedSongs: List<MusicEntity>,
        recentlyPlayed: List<MusicEntity>,
        allSongs: List<MusicEntity>
    ): List<MusicEntity> {
        val combined = (likedSongs + recentlyPlayed).distinctBy { it.audioId }
        val weightedSongs = likedSongs.flatMap { song ->
            List(3) { song }
        } + recentlyPlayed

        return (weightedSongs + allSongs)
            .distinctBy { it.audioId }
            .sortedWith(compareByDescending<MusicEntity> {
                if (it.loved) 2 else if (recentlyPlayed.any { r -> r.audioId == it.audioId }) 1 else 0
            })
            .take(10)
            .ifEmpty {
                allSongs.shuffled().take(10) // Fallback
            }
    }

    private suspend fun generateTopMixes(
        likedSongs: List<MusicEntity>
    ): Map<String, List<MusicEntity>> {

        val supportedCountries = listOf("ID", "MY", "US", "GB", "CH", "DE", "BR")

        val allCountrySongs = supportedCountries.flatMap { countryCode ->
            musicRepository.getTopSongsByCountry(countryCode)
        }.distinctBy { it.audioId }

        val combinedSongs = (likedSongs + allCountrySongs)
            .distinctBy { it.audioId }

        val byCountry = combinedSongs
            .groupBy { it.country ?: "Global" }
            .filterKeys { it in supportedCountries || it == "Global" }

        val topCountries = if (likedSongs.isNotEmpty()) {
            likedSongs.asSequence()
                .filter { it.country in supportedCountries }
                .groupBy { it.country ?: "Global" }
                .entries
                .sortedByDescending { it.value.size }
                .take(3)
                .associate { (country, _) ->
                    val songs = byCountry[country]
                        ?.sortedByDescending { it.lastPlayedAt ?: 0 }
                        ?.take(5)
                        ?: emptyList()
                    country to songs
                }
        } else {
            byCountry.entries
                .sortedByDescending { it.value.size }
                .take(3)
                .associate { (country, songs) ->
                    country to songs.take(5)
                }
        }

        return topCountries.ifEmpty {
            mapOf(
                "Global" to allCountrySongs
                    .filter { it.country == null || it.country == "Global" }
                    .take(5)
                    .ifEmpty { combinedSongs.shuffled().take(5) }
            )
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

    fun selectAudioDevice(device: AudioDeviceManager.AudioDevice) {
        viewModelScope.launch {
            try {
                audioDeviceManager.selectDevice(device)
                _currentDevice.value = device
                Log.d("PlayerViewModel", "Audio device selected: ${device.name}")
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to select audio device", e)
            }
        }
    }

    fun refreshAudioDevices() {
        viewModelScope.launch {
            try {
                val devices = audioDeviceManager.availableDevices.value
                if (devices != null) {
                    _availableDevices.value = devices
                }
                if (devices != null) {
                    Log.d("PlayerViewModel", "Audio devices refreshed: ${devices.size} devices found")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to refresh audio devices", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioDeviceManager.cleanup()
        cancelNotification()
    }

    fun fetchAndPlaySharedSong(songId: String) {
        viewModelScope.launch {
            try {
                musicRepository.getAllMusic()
                val localSong = musicRepository.getSongById(songId.toLong())
                Log.d("PlayerVM", "Local Music $localSong")
                Log.d("PlayerVM", " Music ID $songId    ")

                if (localSong != null) {
                    onEvent(PlayerEvent.Play(localSong))
                } else {
                    val song = musicRepository.getSongById(songId.toLong())

                    if (!uiState.value.musicList.any { it.audioId == song.audioId }) {
                        environment.addMusicAndRefresh(song)
                    }

                    onEvent(PlayerEvent.Play(song))
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to fetch and play shared song", e)
            }
        }
    }

    fun showNotification(song: MusicEntity, isPlaying: Boolean) {
        notificationHelper.showNotification(song, isPlaying)
    }

    fun cancelNotification() {
        notificationHelper.cancelNotification()
    }
}