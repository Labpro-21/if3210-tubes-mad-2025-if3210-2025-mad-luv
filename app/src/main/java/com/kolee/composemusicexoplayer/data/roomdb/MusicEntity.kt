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
    val owner: String,
) {
    companion object {
        val default = MusicEntity(
            audioId = -1,
            title = "",
            artist = "<unknown>",
            duration = 0L,
            albumPath = "",
            audioPath = "",
            owner = "1",
        )
    }
}