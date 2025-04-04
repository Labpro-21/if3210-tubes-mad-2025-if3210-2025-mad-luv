package com.kolee.composemusicexoplayer.data.roomdb

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class MusicEntity(
    @PrimaryKey
    val audioId: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumPath: String,
    val audioPath: String
) : Parcelable
