package com.kolee.composemusicexoplayer.data.roomdb

import com.kolee.composemusicexoplayer.data.model.MonthlyAnalytics
import com.kolee.composemusicexoplayer.data.model.StreakData
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    db: MusicDB
) {
    private val listeningSessionDao = db.listeningSessionDao()

    suspend fun recordListeningSession(
        audioId: Long,
        startTime: Long,
        endTime: Long,
        title: String,
        artist: String
    ) {
        val duration = endTime - startTime
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startTime))
        val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(startTime))

        val session = ListeningSessionEntity(
            audioId = audioId,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            date = date,
            month = month,
            title = title,
            artist = artist
        )

        listeningSessionDao.insertSession(session)
    }

    suspend fun getMonthlyAnalytics(month: String): MonthlyAnalytics {
        val totalDuration = listeningSessionDao.getTotalListeningTimeForMonth(month) ?: 0L
        val dailyStats = listeningSessionDao.getDailyListeningTimeForMonth(month)
        val topArtists = listeningSessionDao.getTopArtistsByMonth(month)
        val topSongs = listeningSessionDao.getTopSongsByMonth(month)
        val longestStreak = calculateLongestStreak(month)

        return MonthlyAnalytics(
            totalMinutes = (totalDuration / 1000 / 60).toInt(),
            dailyStats = dailyStats,
            topArtists = topArtists,
            topSongs = topSongs,
            longestStreak = longestStreak
        )
    }

    private suspend fun calculateLongestStreak(month: String): StreakData? {
        val songsByDate = listeningSessionDao.getSongsByDateWithAlbumPath(month)
        val songDateMap = songsByDate.groupBy { it.audioId }

        var longestStreak: StreakData? = null
        var maxStreakDays = 0

        songDateMap.forEach { (audioId, plays) ->
            val sortedDates = plays.map { it.date }.distinct().sorted()
            var currentStreak = 1
            var streakStart = sortedDates.first()

            for (i in 1 until sortedDates.size) {
                val currentDate = LocalDate.parse(sortedDates[i])
                val previousDate = LocalDate.parse(sortedDates[i - 1])

                if (ChronoUnit.DAYS.between(previousDate, currentDate) == 1L) {
                    currentStreak++
                } else {
                    if (currentStreak >= 2 && currentStreak > maxStreakDays) {
                        maxStreakDays = currentStreak
                        longestStreak = StreakData(
                            songTitle = plays.first().title,
                            artist = plays.first().artist,
                            streakDays = currentStreak,
                            startDate = streakStart,
                            endDate = sortedDates[i - 1],
                            albumPath = plays.first().albumPath
                        )
                    }
                    currentStreak = 1
                    streakStart = sortedDates[i]
                }
            }

            // Check final streak
            if (currentStreak >= 2 && currentStreak > maxStreakDays) {
                maxStreakDays = currentStreak
                longestStreak = StreakData(
                    songTitle = plays.first().title,
                    artist = plays.first().artist,
                    streakDays = currentStreak,
                    startDate = streakStart,
                    endDate = sortedDates.last(),
                    albumPath = plays.first().albumPath
                )
            }
        }

        return longestStreak
    }
}