package com.kolee.composemusicexoplayer.data.roomdb


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MusicEntity(
    @PrimaryKey
    val audioId: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumPath: String,
    val audioPath: String,
    val lastPlayedAt: Long = 0L,
    val loved: Boolean = false,
    val owner: List<String> = listOf("1"),
    val playedAt: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = false,
    val country: String = "LOCAL"
) {
    companion object {
        val default = MusicEntity(
            audioId = -1,
            title = "",
            artist = "<unknown>",
            duration = 0L,
            albumPath = "",
            audioPath = "",
            owner = listOf("1"),
        )
    }
}