package com.kolee.composemusicexoplayer.data.roomdb

import com.kolee.composemusicexoplayer.data.model.MonthlyAnalytics
import com.kolee.composemusicexoplayer.data.network.ApiClient
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import com.kolee.composemusicexoplayer.data.model.OnlineSong
import android.util.Log

class MusicRepository @Inject constructor(
    db: MusicDB,
    private val analyticsRepository: AnalyticsRepository
) {
    private val musicDao = db.musicDao()
    private val api = ApiClient.apiService

    fun getAllMusic(): Flow<List<MusicEntity>> = musicDao.getAllMusics()
    suspend fun insertMusic(music: MusicEntity) = musicDao.insert(music)
    suspend fun insertMusics(vararg music: MusicEntity) = musicDao.insert(*music)
    suspend fun deleteMusics(vararg music: MusicEntity) = musicDao.delete(*music)
    suspend fun updateMusic(music: MusicEntity) = musicDao.updateMusic(music)
    suspend fun getMusicById(audioId: Long): MusicEntity? = musicDao.getMusicById(audioId)
    fun getMusicByOwner(owner: String): Flow<List<MusicEntity>> = musicDao.getMusicByOwner(owner)
    suspend fun getDownloadedMusic(): Flow<List<MusicEntity>> = musicDao.getDownloadedMusic()

    // Analytics methods
    suspend fun recordListeningSession(
        audioId: Long,
        startTime: Long,
        endTime: Long,
        title: String,
        artist: String
    ) = analyticsRepository.recordListeningSession(audioId, startTime, endTime, title, artist)

    suspend fun getMonthlyAnalytics(month: String): MonthlyAnalytics =
        analyticsRepository.getMonthlyAnalytics(month)

    suspend fun getTopGlobalSongs(): List<MusicEntity> {
        return api.getTopGlobalSongs().map { it.toMusicEntity() }
    }

    suspend fun getTopSongsByCountry(code: String): List<MusicEntity> {
        return api.getTopSongsByCountry(code).map { it.toMusicEntity() }
    }
    suspend fun getSongById(songId: Long): MusicEntity {
        // First check if the song exists in the local database
        val localSong = musicDao.getMusicById(songId)
        if (localSong != null) {
            Log.d("DeepLink", "Found song in local database: ${localSong.title}")
            return localSong
        }

        // If not found locally, fetch from API
        try {
            Log.d("DeepLink", "Fetching song from API: $songId")
            val onlineSong = api.getSongById(songId)
            val musicEntity = onlineSong.toMusicEntity()
            // Cache the song in the local database
            insertMusic(musicEntity)
            return musicEntity
        } catch (e: Exception) {
            Log.e("DeepLink", "Error fetching song from API: ${e.message}")
            // Return a default entity if the song can't be fetched
            return MusicEntity.default
        }
    }

    suspend fun getAlbumPathByAudioId(audioId: Long): String? = musicDao.getAlbumPathById(audioId)

    private fun OnlineSong.toMusicEntity(): MusicEntity {
        return MusicEntity(
            audioId = this.id.toLong(),
            title = this.title,
            artist = this.artist,
            duration = durationToMillis(this.duration),
            albumPath = this.artwork,
            audioPath = this.url,
            owner = listOf(this.country),
            country = this.country,
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