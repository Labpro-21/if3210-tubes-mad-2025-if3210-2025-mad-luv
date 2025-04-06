package com.kolee.composemusicexoplayer.di

import android.content.Context
import com.kolee.composemusicexoplayer.data.roomdb.MusicDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMusicDB(@ApplicationContext context: Context): MusicDB =
        MusicDB.getInstance(context)
}