package com.kolee.composemusicexoplayer.presentation.Notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "music_channel"
    private val notificationId = 1

    fun buildNotification(song: MusicEntity, isPlaying: Boolean): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_previous_filled_rounded,
                "Previous",
                getActionPendingIntent("PREVIOUS")
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause_filled_rounded else R.drawable.ic_play_filled_rounded,
                if (isPlaying) "Pause" else "Play",
                getActionPendingIntent(if (isPlaying) "PAUSE" else "PLAY")
            )
            .addAction(
                R.drawable.ic_next_filled_rounded,
                "Next",
                getActionPendingIntent("NEXT")
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .build()
    }

    private fun getActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(context, MusicBroadcastReceiver::class.java).apply {
            putExtra("action", action)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }



    fun showNotification(song: MusicEntity, isPlaying: Boolean) {
        createNotificationChannel()
        notificationManager.notify(notificationId, buildNotification(song, isPlaying))
    }

    fun cancelNotification() {
        notificationManager.cancel(notificationId)
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music player controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
