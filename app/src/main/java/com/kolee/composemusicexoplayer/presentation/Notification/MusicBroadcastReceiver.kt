package com.kolee.composemusicexoplayer.presentation.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

class MusicBroadcastReceiver : BroadcastReceiver() {
    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action") ?: return

        Log.d("MusicBroadcastReceiver", "Received action from notification: $action")

        when (action) {
            "PLAY" -> {
                Log.d("MusicBroadcastReceiver", "Play button clicked")
                // lakukan aksi play
            }
            "PAUSE" -> {
                Log.d("MusicBroadcastReceiver", "Pause button clicked")
                // lakukan aksi pause
            }
            else -> {
                Log.d("MusicBroadcastReceiver", "Unknown action: $action")
            }
        }

        val mainIntent = Intent("com.kolee.composemusicexoplayer.NOTIFICATION_ACTION").apply {
            putExtra("action", action)
        }
        context.sendBroadcast(mainIntent)
    }
}
