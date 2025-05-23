package com.kolee.composemusicexoplayer.data.roomdb

import com.kolee.composemusicexoplayer.data.network.ApiClient
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import com.kolee.composemusicexoplayer.data.model.OnlineSong

class MusicRepository @Inject constructor(
    db: MusicDB
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
    suspend fun getTopGlobalSongs(): List<MusicEntity> {
        return api.getTopGlobalSongs().map { it.toMusicEntity() }
    }

    suspend fun getTopSongsByCountry(code: String): List<MusicEntity> {
        return api.getTopSongsByCountry(code).map { it.toMusicEntity() }
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