package com.kolee.composemusicexoplayer.presentation.Notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.kolee.composemusicexoplayer.MainActivity
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "music_channel"
    private val notificationId = 1
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val contentIntent: PendingIntent by lazy {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("from_notification", true)
        }
        PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }


    private val stopIntent: PendingIntent by lazy {
        val intent = Intent(context, MusicBroadcastReceiver::class.java).apply {
            putExtra("action", "STOP")
        }
        PendingIntent.getBroadcast(
            context,
            "STOP".hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun buildNotification(
        song: MusicEntity,
        isPlaying: Boolean,
        progress: Long = 0L,
        duration: Long = 0L
    ): Notification {
        try {
            // Debug log
            android.util.Log.d("NotificationHelper", "Building notification for: ${song.title}")
            android.util.Log.d("NotificationHelper", "Album path: ${song.albumPath}")

            val albumArt = loadAlbumArtSync(song.albumPath)
            val roundedAlbumArt = createRoundedBitmap(albumArt)

            return NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.music_player_icon)
                .setLargeIcon(roundedAlbumArt)
                .setContentTitle(song.title ?: "Unknown Title")
                .setContentText(song.artist ?: "Unknown Artist")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setDeleteIntent(stopIntent)
                .setAutoCancel(false)
                .setColor(0xFF1DB954.toInt())
                .setColorized(false)

                .addAction(createNotificationAction(
                    R.drawable.ic_previous_filled_rounded,
                    "Previous",
                    "PREVIOUS"
                ))
                .addAction(createNotificationAction(
                    if (!isPlaying) R.drawable.ic_play_filled_rounded else R.drawable.ic_pause_filled_rounded,
                    if (isPlaying) "Pause" else "Play",
                    if (isPlaying) "PAUSE" else "PLAY"
                ))
                .addAction(createNotificationAction(
                    R.drawable.ic_next_filled_rounded,
                    "Next",
                    "NEXT"
                ))
                .addAction(createNotificationAction(
                    R.drawable.ic_close,
                    "Stop",
                    "STOP"
                ))

                .apply {
                    if (duration > 0) {
                        setProgress(duration.toInt(), progress.toInt(), false)
                    }
                }

                .setStyle(
                    MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(null)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopIntent)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setShowWhen(false)
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(false)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error building notification", e)

            return createFallbackNotification(song, isPlaying)
        }
    }

    private fun createFallbackNotification(song: MusicEntity, isPlaying: Boolean): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.music_player_icon)
            .setLargeIcon(getDefaultAlbumArt())
            .setContentTitle(song.title ?: "Unknown Title")
            .setContentText(song.artist ?: "Unknown Artist")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .addAction(createNotificationAction(
                if (!isPlaying) R.drawable.ic_play_filled_rounded else R.drawable.ic_pause_filled_rounded,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) "PAUSE" else "PLAY"
            ))
            .setStyle(MediaStyle()
                .setShowActionsInCompactView(0)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopIntent)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
    }

    private fun loadAlbumArtSync(albumPath: String?): Bitmap {
        return try {
            if (albumPath.isNullOrEmpty()) {
                return getDefaultAlbumArt()
            }

            android.util.Log.d("NotificationHelper", "Trying to load album art from: $albumPath")

            var originalBitmap: Bitmap? = null

            when {
                albumPath.startsWith("http://") || albumPath.startsWith("https://") -> {
                    originalBitmap = getBitmapFromCache(albumPath)
                    if (originalBitmap == null) {

                        loadBitmapFromUrlAsync(albumPath)
                        return getDefaultAlbumArt()
                    }
                }

                albumPath.startsWith("android.resource://") -> {
                    originalBitmap = loadBitmapFromResourceUri(albumPath)
                }

                albumPath.startsWith("content://") -> {
                    try {
                        context.contentResolver.openInputStream(android.net.Uri.parse(albumPath))?.use { inputStream ->
                            originalBitmap = BitmapFactory.decodeStream(inputStream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("NotificationHelper", "Failed to load from content URI: ${e.message}")
                    }
                }

                albumPath.startsWith("android_asset/") -> {
                    try {
                        val assetPath = albumPath.removePrefix("android_asset/")
                        context.assets.open(assetPath).use { inputStream ->
                            originalBitmap = BitmapFactory.decodeStream(inputStream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("NotificationHelper", "Failed to load from assets: ${e.message}")
                    }
                }

                else -> {
                    originalBitmap = BitmapFactory.decodeFile(albumPath)
                }
            }

            if (originalBitmap != null && !originalBitmap!!.isRecycled) {
                android.util.Log.d("NotificationHelper", "Album art loaded successfully: ${originalBitmap!!.width}x${originalBitmap!!.height}")

                val size = (256 * context.resources.displayMetrics.density).toInt()
                return Bitmap.createScaledBitmap(originalBitmap!!, size, size, true).also {
                    if (originalBitmap != it) originalBitmap!!.recycle()
                }
            } else {
                android.util.Log.w("NotificationHelper", "Failed to decode album art from path: $albumPath")
                return getDefaultAlbumArt()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error loading album art: ${e.message}", e)
            getDefaultAlbumArt()
        }
    }

    private fun loadBitmapFromResourceUri(resourceUri: String): Bitmap? {
        return try {
            android.util.Log.d("NotificationHelper", "Loading bitmap from resource URI: $resourceUri")

            val uri = android.net.Uri.parse(resourceUri)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    android.util.Log.d("NotificationHelper", "Successfully loaded bitmap from resource URI")
                } else {
                    android.util.Log.w("NotificationHelper", "Failed to decode bitmap from resource URI")
                }
                bitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error loading bitmap from resource URI: ${e.message}", e)
            null
        }
    }

    private fun loadBitmapFromUrlAsync(url: String) {
        coroutineScope.launch {
            try {
                android.util.Log.d("NotificationHelper", "Loading bitmap from URL async: $url")

                val connection = java.net.URL(url).openConnection().apply {
                    connectTimeout = 5000
                    readTimeout = 10000
                    doInput = true
                }
                connection.connect()

                connection.getInputStream().use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        android.util.Log.d("NotificationHelper", "Successfully downloaded bitmap from URL")
                        saveBitmapToCache(url, bitmap)
                    } else {
                        android.util.Log.w("NotificationHelper", "Failed to decode bitmap from URL")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Error loading bitmap from URL: ${e.message}", e)
            }
        }
    }

    companion object {
        private val bitmapCache = ConcurrentHashMap<String, WeakReference<Bitmap>>()
        private const val MAX_CACHE_SIZE = 20
    }

    private fun getBitmapFromCache(url: String): Bitmap? {
        val weakRef = bitmapCache[url]
        val bitmap = weakRef?.get()
        if (bitmap == null || bitmap.isRecycled) {
            bitmapCache.remove(url)
            return null
        }
        return bitmap
    }

    private fun saveBitmapToCache(url: String, bitmap: Bitmap) {
        cleanupCache()

        if (bitmapCache.size >= MAX_CACHE_SIZE) {
            val firstKey = bitmapCache.keys.firstOrNull()
            if (firstKey != null) {
                bitmapCache.remove(firstKey)
            }
        }
        bitmapCache[url] = WeakReference(bitmap)
    }

    private fun cleanupCache() {
        val iterator = bitmapCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val bitmap = entry.value.get()
            if (bitmap == null || bitmap.isRecycled) {
                iterator.remove()
            }
        }
    }

    private fun getDefaultAlbumArt(): Bitmap {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_music_unknown)
                ?: createDefaultAlbumArtBitmap()
        } catch (e: Exception) {
            createDefaultAlbumArtBitmap()
        }
    }

    private fun createDefaultAlbumArtBitmap(): Bitmap {
        val size = (128 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = 0xFF2E2E2E.toInt()
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        paint.color = 0xFF1DB954.toInt()
        val noteSize = size * 0.4f
        val centerX = size / 2f
        val centerY = size / 2f
        canvas.drawOval(
            centerX - noteSize/2,
            centerY - noteSize/2,
            centerX + noteSize/2,
            centerY + noteSize/2,
            paint
        )

        return bitmap
    }

    private fun createRoundedBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.isRecycled) {
            return getDefaultAlbumArt()
        }

        return try {
            val size = bitmap.width.coerceAtMost(bitmap.height)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            val rect = Rect(0, 0, size, size)
            val rectF = RectF(rect)
            val roundPx = size * 0.1f

            canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)

            output
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error creating rounded bitmap", e)
            bitmap // Return original if rounding fails
        }
    }

    private fun createNotificationAction(iconRes: Int, title: String, action: String): NotificationCompat.Action {
        val intent = Intent(context, MusicBroadcastReceiver::class.java).apply {
            putExtra("action", action)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Action.Builder(iconRes, title, pendingIntent).build()
    }

    fun showNotification(song: MusicEntity, isPlaying: Boolean) {
        try {
            createNotificationChannel()

            coroutineScope.launch {
                val albumArt = if (!song.albumPath.isNullOrEmpty() &&
                    (song.albumPath.startsWith("http://") || song.albumPath.startsWith("https://"))) {
                    loadBitmapFromUrlAsync(song.albumPath)
                    getBitmapFromCache(song.albumPath)
                } else {
                    withContext(Dispatchers.IO) {
                        loadAlbumArtSync(song.albumPath)
                    }
                }

                val finalAlbumArt = albumArt?.let { createRoundedBitmap(it) } ?: createRoundedBitmap(getDefaultAlbumArt())

                withContext(Dispatchers.Main) {
                    val notification = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.music_player_icon)
                        .setLargeIcon(finalAlbumArt)
                        .setContentTitle(song.title ?: "Unknown Title")
                        .setContentText(song.artist ?: "Unknown Artist")
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(contentIntent)
                        .setDeleteIntent(stopIntent)
                        .setColor(0xFF1DB954.toInt())
                        .setColorized(false)
                        .addAction(createNotificationAction(
                            R.drawable.ic_previous_filled_rounded,
                            "Previous",
                            "PREVIOUS"
                        ))
                        .addAction(createNotificationAction(
                            if (!isPlaying) R.drawable.ic_play_filled_rounded else R.drawable.ic_pause_filled_rounded,
                            if (isPlaying) "Pause" else "Play",
                            if (isPlaying) "PAUSE" else "PLAY"
                        ))
                        .addAction(createNotificationAction(
                            R.drawable.ic_next_filled_rounded,
                            "Next",
                            "NEXT"
                        ))
                        .addAction(createNotificationAction(
                            R.drawable.ic_close,
                            "Stop",
                            "STOP"
                        ))
                        .apply {
                            if (song.duration > 0) {
                                setProgress(song.duration.toInt(), 0, false)
                            }
                        }
                        .setStyle(
                            MediaStyle()
                                .setShowActionsInCompactView(0, 1, 2)
                                .setMediaSession(null)
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(stopIntent)
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setOnlyAlertOnce(true)
                        .setOngoing(isPlaying)
                        .setAutoCancel(false)
                        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                        .setShowWhen(false)
                        .build()

                    notificationManager.notify(notificationId, notification)
                }
            }

            // Show immediate notification with default album art
            val immediateNotification = buildNotification(song, isPlaying)
            notificationManager.notify(notificationId, immediateNotification)

        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error showing notification", e)
        }
    }

    fun updateNotification(song: MusicEntity, isPlaying: Boolean, progress: Long = 0L, duration: Long = 0L) {
        try {
            createNotificationChannel()
            notificationManager.notify(
                notificationId,
                buildNotification(song, isPlaying, progress, duration)
            )
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error updating notification", e)
        }
    }

    fun cancelNotification() {
        try {
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error canceling notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                setBypassDnd(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun cleanup() {
        coroutineScope.cancel()
        cleanupCache()
    }
}