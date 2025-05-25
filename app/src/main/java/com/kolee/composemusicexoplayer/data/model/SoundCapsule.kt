package com.kolee.composemusicexoplayer.data.model

data class DailyListeningTime(
    val date: String,
    val dailyDuration: Long
)

data class ArtistStats(
    val artist: String,
    val playCount: Int,
    val totalDuration: Long,
    val albumPath: String? = null
)

data class SongStats(
    val audioId: Long,
    val title: String,
    val artist: String,
    val playCount: Int,
    val totalDuration: Long,
    val albumPath: String? = null
)

data class DailySongPlay(
    val audioId: Long,
    val title: String,
    val artist: String,
    val date: String
)

data class StreakData(
    val songTitle: String,
    val artist: String,
    val streakDays: Int,
    val startDate: String,
    val endDate: String,
    val albumPath: String? = null
)

data class MonthlyAnalytics(
    val totalMinutes: Int,
    val dailyStats: List<DailyListeningTime>,
    val topArtists: List<ArtistStats>,
    val topSongs: List<SongStats>,
    val longestStreak: StreakData?
)

data class DailySongPlayWithAlbum(
    val audioId: Long,
    val title: String,
    val artist: String,
    val date: String,
    val albumPath: String? = null
)