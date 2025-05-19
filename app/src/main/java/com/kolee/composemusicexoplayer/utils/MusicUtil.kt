package com.kolee.composemusicexoplayer.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
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
            audioUriExternal, musicProjection, null, null, null
        )

        songCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val audioId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val albumId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))

                val albumPath = Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )
                val musicPath = Uri.withAppendedPath(audioUriExternal, audioId.toString())

                val music = MusicEntity(
                    audioId = audioId,
                    title = title,
                    artist = if (artist.equals("<unknown>", true)) context.getString(R.string.unknown) else artist,
                    duration = duration,
                    albumPath = albumPath.toString(),
                    audioPath = musicPath.toString(),
                    owner = "1"
                )

                val isValid = (!isTracksSmallerThan100KBSkipped || size / 1024 > 100) &&
                        (!isTracksShorterThan60SecondsSkipped || duration / 1000 > 60)

                if (isValid) {
                    musicList.add(music)
                    Log.d("DEBUG_FETCH", "🎵 Added: $title (${duration / 1000}s, ${size / 1024} KB)")
                }
            }
        }

        return musicList
    }
}
