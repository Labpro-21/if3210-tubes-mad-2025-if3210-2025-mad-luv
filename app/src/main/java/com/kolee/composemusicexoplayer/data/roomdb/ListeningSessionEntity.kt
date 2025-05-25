package com.kolee.composemusicexoplayer.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_sessions")
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val audioId: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val date: String, // Format: yyyy-MM-dd
    val month: String, // Format: yyyy-MM
    val title: String,
    val artist: String
)