package com.kolee.composemusicexoplayer.presentation.music_screen

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.data.roomdb.MusicRepository
import com.kolee.composemusicexoplayer.utils.MusicUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class PlayerEnvironment @OptIn(UnstableApi::class)
@Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository
) {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _allMusics = MutableStateFlow(emptyList<MusicEntity>())
    private val allMusics = _allMusics.asStateFlow()

    private val _currentPlayedMusic = MutableStateFlow(MusicEntity.default)
    private val currentPlayedMusic = _currentPlayedMusic.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L)
    private val currentDuration = _currentDuration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    private val isPlaying = _isPlaying.asStateFlow()

    private val _hasStopped = MutableStateFlow(false)
    private val hasStopped = _hasStopped.asStateFlow()

    private val _isBottomMusicPlayerShowed = MutableStateFlow(false)
    private val isBottomMusicPlayerShowed = _isBottomMusicPlayerShowed.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.REPEAT_ALL)
    val playbackMode = _playbackMode.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded = _isPlayerExpanded.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    private var shuffledList: List<MusicEntity> = emptyList()
    private var isOriginalList: Boolean = true

    private val playerHandler = Handler(Looper.getMainLooper())

    private val exoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    when (playbackMode.value) {
                        PlaybackMode.REPEAT_ALL -> {
                            val nextSong = if (_isShuffleEnabled.value) {
                                getNextShuffledSong(currentPlayedMusic.value)
                            } else {
                                val currentIndex = allMusics.value.indexOfFirst {
                                    it.audioId == currentPlayedMusic.value.audioId
                                }
                                when {
                                    currentIndex == allMusics.value.lastIndex -> allMusics.value[0]
                                    currentIndex != -1 -> allMusics.value[currentIndex + 1]
                                    else -> allMusics.value[0]
                                }
                            }
                            scope.launch { play(nextSong) }
                        }
                        PlaybackMode.REPEAT_ONE -> {
                            scope.launch { play(currentPlayedMusic.value) }
                        }
                        PlaybackMode.REPEAT_OFF -> {
                            this@apply.stop()
                            _currentPlayedMusic.tryEmit(MusicEntity.default)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                _isPlaying.tryEmit(isPlaying)
            }
        })
    }

    init {
        scope.launch {
            val defaultMusic = MusicEntity(
                audioId = 100L,
                title = "intro (end of the world)",
                artist = "Ariana Grande",
                duration = 161000L,
                loved = true,
                albumPath = Uri.parse(
                    ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                            context.packageName + "/" + R.drawable.album_cover
                ).toString(),
                audioPath = RawResourceDataSource.buildRawResourceUri(R.raw.intro).toString()
            )

            val existing = musicRepository.getMusicById(100L)

            if (existing == null) {
                Log.d("MusicDebug", "Inserting default music")
                musicRepository.insertMusic(defaultMusic)
            } else {
                Log.d("MusicDebug", "Updating default music")

                val updated = existing.copy(
                    title = defaultMusic.title,
                    artist = defaultMusic.artist,
                    duration = defaultMusic.duration,
                    albumPath = defaultMusic.albumPath,
                    audioPath = defaultMusic.audioPath
                )
                musicRepository.updateMusic(updated)
            }

            musicRepository.getAllMusic()
                .distinctUntilChanged()
                .collect { musicList ->
                    _allMusics.emit(musicList)
                    musicList.forEach {
                        Log.d("AddSongDrawer", "Music in DB: $it") // Pastikan data lama masih ada
                    }
                }


        }
    }



    fun getAllMusic(): Flow<List<MusicEntity>> = allMusics
    fun isBottomMusicPlayerShowed(): Flow<Boolean> = isBottomMusicPlayerShowed
    fun getCurrentPlayedMusic(): Flow<MusicEntity> = currentPlayedMusic
    fun isPlaying(): Flow<Boolean> = isPlaying
    fun getCurrentDuration(): Flow<Long> = currentDuration

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        when (mode) {
            PlaybackMode.REPEAT_ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
            PlaybackMode.REPEAT_ALL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }
            PlaybackMode.REPEAT_OFF -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    fun togglePlaybackMode() {
        val newMode = when (playbackMode.value) {
            PlaybackMode.REPEAT_ONE -> PlaybackMode.REPEAT_ALL
            PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_OFF
            PlaybackMode.REPEAT_OFF -> PlaybackMode.REPEAT_ONE
        }
        setPlaybackMode(newMode)
    }


    suspend fun play(music: MusicEntity) {
        if (music.audioId != MusicEntity.default.audioId) {
            _hasStopped.emit(false)
            _currentPlayedMusic.emit(music)

            playerHandler.post {
                exoPlayer.setMediaItem(MediaItem.fromUri(music.audioPath.toUri()))
                exoPlayer.prepare()
                exoPlayer.play()
                startUpdatingProgress()
            }
        }
    }

    suspend fun updateMusic(music: MusicEntity) {
        musicRepository.updateMusic(music)

        val updatedList = allMusics.value.map {
            if (it.audioId == music.audioId) music else it
        }
        _allMusics.emit(updatedList)
    }


    fun snapTo(duration: Long, fromUser: Boolean = true) {
        _currentDuration.tryEmit(duration)
        if (fromUser) playerHandler.post { exoPlayer.seekTo(duration) }
    }

    suspend fun setShowButtonMusicPlayer(isShowed: Boolean) {
        _isBottomMusicPlayerShowed.emit(isShowed)
    }

    suspend fun updateMusicList(musicList: List<MusicEntity>) {
        _allMusics.emit(musicList)
    }

    suspend fun deleteMusic(music: MusicEntity) {
        musicRepository.deleteMusics(music)
    }

    suspend fun next() {
        val nextMusic = if (_isShuffleEnabled.value) {
            getNextShuffledSong(currentPlayedMusic.value)
        } else {
            val currentIndex = allMusics.value.indexOfFirst {
                it.audioId == currentPlayedMusic.value.audioId
            }
            when {
                currentIndex == allMusics.value.lastIndex -> allMusics.value[0]
                currentIndex != -1 -> allMusics.value[currentIndex + 1]
                else -> allMusics.value[0]
            }
        }
        play(nextMusic)
    }

    suspend fun previous() {
        val currentIndex = allMusics.value.indexOfFirst {
            it.audioId == currentPlayedMusic.value.audioId
        }
        val previousMusic = when {
            currentIndex == 0 -> allMusics.value[allMusics.value.lastIndex]
            currentIndex >= 1 -> allMusics.value[currentIndex - 1]
            else -> allMusics.value[0]
        }
        play(previousMusic)
    }

    suspend fun pause() {
        playerHandler.post { exoPlayer.pause() }
        startUpdatingProgress()
    }

    suspend fun resume() {
        if (hasStopped.value && currentPlayedMusic.value != MusicEntity.default) {
            play(currentPlayedMusic.value)
        } else {
            playerHandler.post { exoPlayer.play() }
        }
    }

    suspend fun addMusic(music: MusicEntity){
        musicRepository.insertMusics(music)
    }

    suspend fun addMusicAndRefresh(music: MusicEntity) {
        addMusic(music)

        val currentMusics = getAllMusic().first()

        val scannedMusics = MusicUtil.fetchMusicsFromDevice(context)

        val combinedList = (currentMusics + scannedMusics + music)
            .distinctBy { it.audioId }

        insertAllMusics(combinedList)
    }





    suspend fun refreshMusicList() {
        val scannedMusics = MusicUtil.fetchMusicsFromDevice(context)
//        insertAllMusics(scannedMusics)
    }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (exoPlayer.isPlaying) {
                _currentDuration.value = exoPlayer.currentPosition
                playerHandler.postDelayed(this, 1000)
            }
        }
    }

    fun updateCurrentDuration(newDuration: Long) {
        _currentDuration.value = newDuration
    }


    private fun startUpdatingProgress() {
        playerHandler.post(updateProgressRunnable)
    }

    private fun stopUpdatingProgress() {
        playerHandler.removeCallbacks(updateProgressRunnable)
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        if (_isShuffleEnabled.value) {
            shuffledList = allMusics.value.shuffled()
            isOriginalList = false
        } else {
            isOriginalList = true
        }
    }

    fun getNextShuffledSong(currentSong: MusicEntity): MusicEntity {
        return if (_isShuffleEnabled.value && shuffledList.isNotEmpty()) {
            val currentIndex = shuffledList.indexOfFirst { it.audioId == currentSong.audioId }
            if (currentIndex == -1 || currentIndex == shuffledList.lastIndex) {
                shuffledList[0]
            } else {
                shuffledList[currentIndex + 1]
            }
        } else {
            val currentIndex = allMusics.value.indexOfFirst { it.audioId == currentSong.audioId }
            if (currentIndex == allMusics.value.lastIndex) {
                allMusics.value[0]
            } else {
                allMusics.value[currentIndex + 1]
            }
        }
    }

    private suspend fun insertAllMusics(newMusicList: List<MusicEntity>) {
        val defaultMusicId = 100L

        val musicToInsert = newMusicList.filterNot { new ->
            allMusics.value.any { it.audioId == new.audioId }
        }
        val musicToDelete = allMusics.value.filterNot { stored ->
            newMusicList.any { it.audioId == stored.audioId } || stored.audioId == defaultMusicId
        }

        musicRepository.insertMusics(*musicToInsert.toTypedArray())
        musicRepository.deleteMusics(*musicToDelete.toTypedArray())
    }


    fun release() {
        exoPlayer.release()
        scope.cancel()
    }
}

enum class PlaybackMode {
    REPEAT_ONE,
    REPEAT_ALL,
    REPEAT_OFF
}