package com.kolee.composemusicexoplayer.data.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    @Query("SELECT * FROM MusicEntity")
    fun getAllMusics(): Flow<List<MusicEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg music: MusicEntity)

    @Delete
    suspend fun delete(vararg music: MusicEntity)

    @Update
    suspend fun updateMusic(music: MusicEntity)

    @Query("SELECT * FROM MusicEntity WHERE audioId = :audioId LIMIT 1")
    suspend fun getMusicById(audioId: Long): MusicEntity?

    @Query("SELECT * FROM MusicEntity WHERE owner LIKE '%' || :owner || '%'")
    fun getMusicByOwner(owner: String): Flow<List<MusicEntity>>

    @Query("SELECT * FROM MusicEntity WHERE isDownloaded = 1")
    fun getDownloadedMusic(): Flow<List<MusicEntity>>


    @Query("SELECT albumPath FROM MusicEntity WHERE audioId = :audioId LIMIT 1")
    suspend fun getAlbumPathById(audioId: Long): String?


}