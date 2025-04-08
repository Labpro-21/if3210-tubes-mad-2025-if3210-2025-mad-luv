package com.kolee.composemusicexoplayer.data.roomdb

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class MusicRepository @Inject constructor(
    db: MusicDB
) {
    private val musicDao = db.musicDao()

    fun getAllMusic(): Flow<List<MusicEntity>> = musicDao.getAllMusics()
    suspend fun insertMusic(music: MusicEntity) = musicDao.insert(music)
    suspend fun insertMusics(vararg music: MusicEntity) = musicDao.insert(*music)
    suspend fun deleteMusics(vararg music: MusicEntity) = musicDao.delete(*music)
    suspend fun updateMusic(music: MusicEntity) = musicDao.updateMusic(music)

}