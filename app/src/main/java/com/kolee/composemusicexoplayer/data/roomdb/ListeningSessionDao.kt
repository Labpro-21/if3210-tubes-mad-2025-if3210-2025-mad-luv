package com.kolee.composemusicexoplayer.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kolee.composemusicexoplayer.data.model.ArtistStats
import com.kolee.composemusicexoplayer.data.model.DailyListeningTime
import com.kolee.composemusicexoplayer.data.model.DailySongPlay
import com.kolee.composemusicexoplayer.data.model.DailySongPlayWithAlbum
import com.kolee.composemusicexoplayer.data.model.SongStats


@Dao
interface ListeningSessionDao {

    @Insert
    suspend fun insertSession(session: ListeningSessionEntity)

    @Query("SELECT * FROM listening_sessions WHERE month = :month ORDER BY startTime DESC")
    suspend fun getSessionsByMonth(month: String): List<ListeningSessionEntity>

    @Query("SELECT SUM(duration) FROM listening_sessions WHERE month = :month")
    suspend fun getTotalListeningTimeForMonth(month: String): Long?

    @Query("SELECT date, SUM(duration) as dailyDuration FROM listening_sessions WHERE month = :month GROUP BY date ORDER BY date")
    suspend fun getDailyListeningTimeForMonth(month: String): List<DailyListeningTime>

    // Modified query dengan JOIN untuk mendapatkan albumPath
    @Query("""
        SELECT ls.artist, 
               COUNT(*) as playCount, 
               SUM(ls.duration) as totalDuration,
               m.albumPath as albumPath
        FROM listening_sessions ls
        LEFT JOIN MusicEntity m ON ls.audioId = m.audioId
        WHERE ls.month = :month 
        GROUP BY ls.artist 
        ORDER BY playCount DESC
    """)
    suspend fun getTopArtistsByMonth(month: String): List<ArtistStats>

    // Modified query dengan JOIN untuk mendapatkan albumPath
    @Query("""
        SELECT ls.audioId, 
               ls.title, 
               ls.artist, 
               COUNT(*) as playCount, 
               SUM(ls.duration) as totalDuration,
               m.albumPath as albumPath
        FROM listening_sessions ls
        LEFT JOIN MusicEntity m ON ls.audioId = m.audioId
        WHERE ls.month = :month 
        GROUP BY ls.audioId 
        ORDER BY playCount DESC
    """)
    suspend fun getTopSongsByMonth(month: String): List<SongStats>

    @Query("SELECT audioId, title, artist, date FROM listening_sessions WHERE month = :month GROUP BY audioId, date ORDER BY date")
    suspend fun getSongsByDateForMonth(month: String): List<DailySongPlay>

    @Query("""
    SELECT ls.audioId, 
           ls.title, 
           ls.artist, 
           ls.date,
           m.albumPath as albumPath
    FROM listening_sessions ls
    LEFT JOIN MusicEntity m ON ls.audioId = m.audioId
    WHERE ls.month = :month 
    GROUP BY ls.audioId, ls.date 
    ORDER BY ls.date
""")
    suspend fun getSongsByDateWithAlbumPath(month: String): List<DailySongPlayWithAlbum>
}
