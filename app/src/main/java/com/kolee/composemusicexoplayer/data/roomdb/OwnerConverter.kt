package com.kolee.composemusicexoplayer.data.roomdb

import androidx.room.TypeConverter

class OwnersConverter {
    @TypeConverter
    fun fromOwnersList(owners: List<String>): String {
        return owners.joinToString(separator = "|")
    }

    @TypeConverter
    fun toOwnersList(data: String): List<String> {
        return if (data.isEmpty()) emptyList() else data.split("|")
    }
}