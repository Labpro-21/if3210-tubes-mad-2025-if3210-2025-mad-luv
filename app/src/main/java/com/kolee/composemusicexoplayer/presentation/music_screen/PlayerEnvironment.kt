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
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.data.roomdb.MusicRepository
import com.kolee.composemusicexoplayer.utils.MusicUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import android.util.Log

class PlayerEnvironment @OptIn(UnstableApi::class)
@Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository
) {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val userPreferences = UserPreferences(context)

    private val _currentUserId = MutableStateFlow("1")
    private val currentUserId = _currentUserId.asStateFlow()

    private val _currentUserCountry = MutableStateFlow("GLOBAL")
    private val currentUserCountry = _currentUserCountry.asStateFlow()

    private var hasLoadedMusicForCurrentUser = false

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

            logAllMusicInDatabase()

            // Combine both flows to ensure they're active
            combine(
                userPreferences.getUserEmail,
                userPreferences.getUserCountry
            ) { email, country ->
                email to country
            }.collect { (email, country) ->
                val userId = email ?: "1"
                val userCountry = country ?: "GLOBAL"

                Log.d("PlayerEnv", "Email: $userId, Country: $userCountry")

                if (_currentUserId.value != userId) {
                    _currentUserId.value = userId
                    Log.d("PlayerEnv", "User ID updated to: $userId")
                }

                if (_currentUserCountry.value != userCountry) {
                    _currentUserCountry.value = userCountry
                    Log.d("PlayerEnv", "User Country updated to: $userCountry")
                    loadUserMusic(userId, userCountry)
                }
            }
        }
    }

    private suspend fun logAllMusicInDatabase() {
        musicRepository.getAllMusic().first().let { allMusicList ->
            Log.d("MusicOwners", "===== ALL MUSIC IN DATABASE =====")
            Log.d("MusicOwners", "Total music count: ${allMusicList.size}")

            val musicByOwner = allMusicList.groupBy { it.owner }

            musicByOwner.forEach { (owner, musicList) ->
                Log.d("MusicOwners", "Owner: $owner - Music count: ${musicList.size}")
                musicList.forEach { music ->
                    Log.d("MusicOwners", "  - ID: ${music.audioId}, Title: ${music.title}, Artist: ${music.artist}, Owner: ${music.owner}")
                }
            }

            Log.d("MusicOwners", "===== END OF ALL MUSIC =====")
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun loadUserMusic(userId: String, userCountry: String) {
        Log.d("MusicOwners", "Loading music for user: $userId")
        hasLoadedMusicForCurrentUser = true

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
            audioPath = RawResourceDataSource.buildRawResourceUri(R.raw.intro).toString(),
            owner = userId
        )

        val existing = musicRepository.getMusicById(100L)

        if (existing == null) {
            Log.d("MusicDebug", "Inserting default music for user: $userId")
            musicRepository.insertMusic(defaultMusic)
        } else {
            Log.d("MusicDebug", "Updating default music for user: $userId")
            Log.d("MusicOwners", "Default music current owner: ${existing.owner}, updating to: $userId")

            val updated = existing.copy(
                title = defaultMusic.title,
                artist = defaultMusic.artist,
                duration = defaultMusic.duration,
                albumPath = defaultMusic.albumPath,
                audioPath = defaultMusic.audioPath,
                owner = userId
            )
            musicRepository.updateMusic(updated)
        }

        try {
            combine(
                musicRepository.getMusicByOwner(userId),
                musicRepository.getMusicByOwner("GLOBAL"),
                musicRepository.getMusicByOwner(userCountry)
            ) { userMusic, globalMusic, countryMusic ->
                (userMusic + globalMusic + countryMusic).distinctBy { it.audioId }
            }
                .distinctUntilChanged()
                .collect { musicList ->
                    _allMusics.emit(musicList)
                    Log.d("MusicOwners", "===== MUSIC FOR USER: $userId =====")
                    Log.d("MusicOwners", "User music count: ${musicList.size}")
                    musicList.forEach {
                        Log.d("MusicOwners", "Music: ID=${it.audioId}, Title=${it.title}, Artist=${it.artist}, Owner=${it.owner}")
                    }
                    Log.d("MusicOwners", "===== END OF USER MUSIC =====")
                }
        } catch (e: Exception) {
            Log.e("MusicOwners", "Error loading music for user $userId: ${e.message}")
        }
    }

    suspend fun reloadUserMusic() {
        val userId = currentUserId.value
        val userCountry = currentUserCountry.value
        Log.d("MusicOwners", "Manually reloading music for user: $userId")
        loadUserMusic(userId,userCountry)
    }

    fun getAllMusic(): Flow<List<MusicEntity>> = allMusics
    fun isBottomMusicPlayerShowed(): Flow<Boolean> = isBottomMusicPlayerShowed
    fun getCurrentPlayedMusic(): Flow<MusicEntity> = currentPlayedMusic
    fun isPlaying(): Flow<Boolean> = isPlaying
    fun getCurrentDuration(): Flow<Long> = currentDuration
    fun getCurrentUserId(): Flow<String> = currentUserId

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
            Log.d("MusicOwners", "Playing music: ID=${music.audioId}, Title=${music.title}, Owner=${music.owner}")

            playerHandler.post {
                exoPlayer.setMediaItem(MediaItem.fromUri(music.audioPath.toUri()))
                exoPlayer.prepare()
                exoPlayer.play()
                startUpdatingProgress()
            }
        }
    }

    suspend fun updateMusic(music: MusicEntity) {
        val updatedMusic = music.copy(owner = currentUserId.value)
        Log.d("MusicOwners", "Updating music: ID=${music.audioId}, Title=${music.title}")
        Log.d("MusicOwners", "  - Original owner: ${music.owner}, New owner: ${updatedMusic.owner}")

        musicRepository.updateMusic(updatedMusic)

        val updatedList = allMusics.value.map {
            if (it.audioId == updatedMusic.audioId) updatedMusic else it
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
        val filteredList = musicList.filter { it.owner == currentUserId.value }
        Log.d("MusicOwners", "Updating music list - Total: ${musicList.size}, Filtered for user ${currentUserId.value}: ${filteredList.size}")
        _allMusics.emit(filteredList)
    }

    suspend fun deleteMusic(music: MusicEntity) {
        Log.d("MusicOwners", "Deleting music: ID=${music.audioId}, Title=${music.title}, Owner=${music.owner}")
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

    suspend fun addMusic(music: MusicEntity) {
        val musicWithOwner = music.copy(owner = currentUserId.value)
        Log.d("MusicOwners", "Adding music: ID=${music.audioId}, Title=${music.title}")
        Log.d("MusicOwners", "  - Original owner: ${music.owner}, Set owner: ${musicWithOwner.owner}")

        musicRepository.insertMusics(musicWithOwner)

        reloadUserMusic()
    }

    suspend fun addMusicAndRefresh(music: MusicEntity) {
        val musicWithOwner = music.copy(owner = currentUserId.value)
        Log.d("MusicOwners", "Adding music and refreshing: ID=${music.audioId}, Title=${music.title}, Owner set to: ${musicWithOwner.owner}")

        addMusic(musicWithOwner)

        val currentMusics = getAllMusic().first()
        val scannedMusics = MusicUtil.fetchMusicsFromDevice(context)
            .map { it.copy(owner = currentUserId.value) } // Set owner for scanned music

        Log.d("MusicOwners", "Scanned ${scannedMusics.size} music files from device, setting owner to: ${currentUserId.value}")

        val combinedList = (currentMusics + scannedMusics + musicWithOwner)
            .distinctBy { it.audioId }

        Log.d("MusicOwners", "Combined list size after deduplication: ${combinedList.size}")
        insertAllMusics(combinedList)
    }

    suspend fun refreshMusicList() {
        val scannedMusics = MusicUtil.fetchMusicsFromDevice(context)
            .map { it.copy(owner = currentUserId.value) }

        Log.d("MusicOwners", "Refreshing music list - Scanned ${scannedMusics.size} music files, setting owner to: ${currentUserId.value}")
        insertAllMusics(scannedMusics)

        logAllMusicInDatabase()
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
        val userId = currentUserId.value
        val userCountry = currentUserCountry.value

        val filteredNewMusicList = newMusicList.map { it.copy(owner = userId) }

        val currentUserMusic = allMusics.value.filter { it.owner == userId }
        val globalsongs = musicRepository.getTopGlobalSongs()
        val countrysongs = musicRepository.getTopSongsByCountry(userCountry)

        Log.d("MusicOwners", "NewMusic musics for user: $userId\n${filteredNewMusicList.joinToString("\n")}")
        Log.d("MusicOwners", "Current musics for user: $userId\n${currentUserMusic.joinToString("\n")}")
        Log.d("MusicOwners", "Inserting musics for user: $userId\n${currentUserMusic.joinToString("\n")}")
        Log.d("MusicOwners", "country musics for user: $userCountry\n${countrysongs.joinToString("\n")}")

        musicRepository.insertMusics(*currentUserMusic.toTypedArray())
        musicRepository.insertMusics(*globalsongs.toTypedArray())
        musicRepository.insertMusics(*countrysongs.toTypedArray())

        reloadUserMusic()
    }

    suspend fun logAllMusicOwners() {
        Log.d("MusicOwners", "===== MANUALLY LOGGING ALL MUSIC OWNERS =====")
        musicRepository.getAllMusic().first().let { allMusic ->
            Log.d("MusicOwners", "Total music in database: ${allMusic.size}")

            // Group by owner for better readability
            val byOwner = allMusic.groupBy { it.owner }
            byOwner.forEach { (owner, list) ->
                Log.d("MusicOwners", "Owner: $owner - Count: ${list.size}")
                list.forEach { music ->
                    Log.d("MusicOwners", "  - ID: ${music.audioId}, Title: ${music.title}, Artist: ${music.artist}")
                }
            }
        }
        Log.d("MusicOwners", "===== END OF MANUAL LOG =====")
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