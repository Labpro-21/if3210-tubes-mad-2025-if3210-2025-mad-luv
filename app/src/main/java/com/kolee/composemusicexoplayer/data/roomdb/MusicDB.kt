package com.kolee.composemusicexoplayer.data.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MusicEntity::class, ListeningSessionEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(OwnersConverter::class)
abstract class MusicDB : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun listeningSessionDao(): ListeningSessionDao

    companion object {
        @Volatile
        private var instance: MusicDB? = null

        fun getInstance(context: Context): MusicDB {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): MusicDB {
            return Room.databaseBuilder(
                context,
                MusicDB::class.java,
                "music_db_1129"
            )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
        }
    }
}