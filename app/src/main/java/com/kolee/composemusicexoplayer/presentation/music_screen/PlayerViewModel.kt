package com.kolee.composemusicexoplayer.presentation.music_screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kolee.composemusicexoplayer.data.model.ArtistStats
import com.kolee.composemusicexoplayer.data.model.DailyListeningTime
import com.kolee.composemusicexoplayer.data.model.MonthlyAnalytics
import com.kolee.composemusicexoplayer.data.model.SongStats
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.data.roomdb.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val environment: PlayerEnvironment,
    private val musicRepository: MusicRepository
) : StatefulViewModel<MusicUiState>(MusicUiState()) {

    // Analytics state
    private val _monthlyAnalytics = MutableStateFlow<MonthlyAnalytics?>(null)
    val monthlyAnalytics = _monthlyAnalytics.asStateFlow()

    private val _topArtistsForDetail = MutableStateFlow<List<ArtistStats>>(emptyList())
    val topArtistsForDetail = _topArtistsForDetail.asStateFlow()

    private val _topSongsForDetail = MutableStateFlow<List<SongStats>>(emptyList())
    val topSongsForDetail = _topSongsForDetail.asStateFlow()

    private val _dailyListeningTimeForDetail = MutableStateFlow<List<DailyListeningTime>>(emptyList())
    val dailyListeningTimeForDetail = _dailyListeningTimeForDetail.asStateFlow()

    fun setTopArtistsForDetail(artists: List<ArtistStats>) {
        _topArtistsForDetail.value = artists
    }

    fun setTopSongsForDetail(songs: List<SongStats>) {
        _topSongsForDetail.value = songs
    }

    fun setDailyListeningTimeForDetail(daily: List<DailyListeningTime>) {
        _dailyListeningTimeForDetail.value = daily
    }

    // Real-time session tracking
    private var currentSessionStartTime: Long = 0L
    private var lastAnalyticsUpdate: Long = 0L

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

                // Handle session tracking
                if (isPlaying && currentSessionStartTime == 0L) {
                    currentSessionStartTime = System.currentTimeMillis()
                } else if (!isPlaying && currentSessionStartTime > 0L) {
                    recordCurrentSession()
                }
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

        // Load current month analytics
        viewModelScope.launch {
            loadCurrentMonthAnalytics()
        }

        // Real-time analytics update every 1 minute during playback
        viewModelScope.launch {
            while (true) {
                delay(60000) // 1 minute
                val currentTime = System.currentTimeMillis()

                // Update analytics if playing and at least 1 minute has passed
                if (uiState.value.isPlaying &&
                    currentTime - lastAnalyticsUpdate > 60000) {

                    // Record partial session for real-time updates
                    recordPartialSession()
                    loadCurrentMonthAnalytics()
                    lastAnalyticsUpdate = currentTime
                }
            }
        }
    }

    private suspend fun recordCurrentSession() {
        val currentMusic = uiState.value.currentPlayedMusic
        if (currentMusic != MusicEntity.default && currentSessionStartTime > 0L) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - currentSessionStartTime

            // Only record if listened for more than 1 minute
            if (duration > 60000) {
                musicRepository.recordListeningSession(
                    audioId = currentMusic.audioId,
                    startTime = currentSessionStartTime,
                    endTime = endTime,
                    title = currentMusic.title,
                    artist = currentMusic.artist
                )

                // Update analytics immediately after recording
                loadCurrentMonthAnalytics()
            }
        }
        currentSessionStartTime = 0L
    }

    private suspend fun recordPartialSession() {
        val currentMusic = uiState.value.currentPlayedMusic
        if (currentMusic != MusicEntity.default &&
            currentSessionStartTime > 0L &&
            uiState.value.isPlaying) {

            val currentTime = System.currentTimeMillis()
            val duration = currentTime - currentSessionStartTime

            // Record partial session if more than 1 minute
            if (duration > 60000) {
                musicRepository.recordListeningSession(
                    audioId = currentMusic.audioId,
                    startTime = currentSessionStartTime,
                    endTime = currentTime,
                    title = currentMusic.title,
                    artist = currentMusic.artist
                )

                // Reset start time for next partial session
                currentSessionStartTime = currentTime
            }
        }
    }


    private suspend fun loadCurrentMonthAnalytics() {
        try {
            // PERBAIKAN: Gunakan format yang sama
            val currentDate = java.util.Calendar.getInstance()
            val currentMonth = currentDate.get(java.util.Calendar.MONTH) + 1 // Convert to 1-based
            val currentYear = currentDate.get(java.util.Calendar.YEAR)
            val monthString = String.format("%04d-%02d", currentYear, currentMonth)

            println("DEBUG: Loading current month analytics for: $monthString") // Debug log
            val analytics = musicRepository.getMonthlyAnalytics(monthString)
            _monthlyAnalytics.value = analytics
        } catch (e: Exception) {
            println("DEBUG: Error loading current analytics: ${e.message}") // Debug log
            _monthlyAnalytics.value = null
        }
    }


    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Play -> {
                viewModelScope.launch {
                    // Record previous session if any
                    recordCurrentSession()

                    val updatedMusic = event.musicEntity.copy(lastPlayedAt = System.currentTimeMillis())
                    environment.play(updatedMusic)
                    environment.setShowButtonMusicPlayer(true)

                    // Start new session
                    currentSessionStartTime = System.currentTimeMillis()

                    val updatedList = uiState.value.musicList.map {
                        if (it.audioId == updatedMusic.audioId) updatedMusic else it
                    }
                    updateState { copy(musicList = updatedList) }
                }
            }

            is PlayerEvent.PlayPause -> {
                viewModelScope.launch {
                    if (event.isPlaying) {
                        // Pausing - record current session
                        recordCurrentSession()
                        environment.pause()
                    } else {
                        // Resuming - start new session
                        currentSessionStartTime = System.currentTimeMillis()
                        environment.resume()
                    }
                }
            }

            is PlayerEvent.Next -> {
                viewModelScope.launch {
                    recordCurrentSession()
                    environment.next()
                }
            }

            is PlayerEvent.Previous -> {
                viewModelScope.launch {
                    recordCurrentSession()
                    environment.previous()
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
                viewModelScope.launch {
                    environment.refreshMusicList()
                    // Also refresh analytics
                    loadCurrentMonthAnalytics()
                }
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

                    // Refresh analytics after deletion
                    loadCurrentMonthAnalytics()
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

    suspend fun getMonthAnalyticsForMonth(month: Int, year: Int) {
        try {
//            _monthlyAnalytics.value = null
            delay(100)
            // PERBAIKAN: Format month ke 1-based (month + 1) karena Calendar.MONTH menggunakan 0-based indexing
            val monthString = String.format("%04d-%02d", year, month + 1)
            println("DEBUG: Fetching analytics for monthString: $monthString") // Debug log
            val analytics = musicRepository.getMonthlyAnalytics(monthString)
            println("DEBUG: Analytics result: ${analytics?.totalMinutes} minutes") // Debug log
            _monthlyAnalytics.value = analytics
        } catch (e: Exception) {
            println("DEBUG: Error fetching analytics: ${e.message}") // Debug log
            _monthlyAnalytics.value = null
        }
    }

    // Analytics-specific methods
    fun refreshAnalytics() {
        viewModelScope.launch {
            loadCurrentMonthAnalytics()
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