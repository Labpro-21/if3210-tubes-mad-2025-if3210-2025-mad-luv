package com.kolee.composemusicexoplayer.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.R

object MusicUtil {

    fun fetchMusicsFromDevice(
        context: Context,
        isTracksSmallerThan100KBSkipped: Boolean = true,
        isTracksShorterThan60SecondsSkipped: Boolean = true
    ): List<MusicEntity> {
        val musicList = mutableListOf<MusicEntity>()
        val audioUriExternal = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicProjection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE
        )

        val songCursor = context.contentResolver.query(
            audioUriExternal,
            musicProjection,
            null,
            null,
            null
        )

        songCursor?.use { cursor ->
            val cursorIndexSongId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val cursorIndexSongTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val cursorIndexSongArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val cursorIndexSongDuration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val cursorIndexSongAlbumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val cursorIndexSongSize = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val audioId = cursor.getLong(cursorIndexSongId)
                val title = cursor.getString(cursorIndexSongTitle)
                val artist = cursor.getString(cursorIndexSongArtist)
                val duration = cursor.getLong(cursorIndexSongDuration)
                val albumId = cursor.getString(cursorIndexSongAlbumId)
                val size = cursor.getInt(cursorIndexSongSize)

                val albumPath = Uri.withAppendedPath(Uri.parse("content://media/external/audio/albumart"), albumId)
                val musicPath = Uri.withAppendedPath(audioUriExternal, audioId.toString())

                val durationGreaterThan60Sec = duration / 1000 > 60
                val sizeGreaterThan100KB = (size / 1024) > 100

                val music = MusicEntity(
                    audioId = audioId,
                    title = title,
                    artist = if (artist.equals("<unknown>", true)) context.getString(R.string.unknown) else artist,
                    duration = duration,
                    albumPath = albumPath.toString(),
                    audioPath = musicPath.toString()
                )

                when {
                    isTracksSmallerThan100KBSkipped && isTracksShorterThan60SecondsSkipped -> {
                        if (sizeGreaterThan100KB && durationGreaterThan60Sec) musicList.add(music)
                    }
                    !isTracksSmallerThan100KBSkipped && isTracksShorterThan60SecondsSkipped -> {
                        if (durationGreaterThan60Sec) musicList.add(music)
                    }
                    isTracksSmallerThan100KBSkipped && !isTracksShorterThan60SecondsSkipped -> {
                        if (sizeGreaterThan100KB) musicList.add(music)
                    }
                    !isTracksSmallerThan100KBSkipped && !isTracksShorterThan60SecondsSkipped -> {
                        musicList.add(music)
                    }
                }
            }
        }

        return musicList
    }
}
