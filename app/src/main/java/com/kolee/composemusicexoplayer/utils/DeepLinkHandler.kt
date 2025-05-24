package com.kolee.composemusicexoplayer.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object DeepLinkHandler {
    private const val TAG = "DeepLinkHandler"
    private const val SCHEME = "purrytify"
    private const val HOST_SONG = "song"

    // Base URL untuk server Purrytify
    private const val BASE_URL = "https://your-purrytify-server.com" // Ganti dengan URL server Anda

    /**
     * Creates a deep link URI for a song (untuk internal app use)
     * @param songId The ID of the song to link to
     * @return A URI string in the format purrytify://song/{songId}
     */
    fun createSongDeepLink(songId: Long): String {
        return "$SCHEME://$HOST_SONG/$songId"
    }

    /**
     * Creates a shareable URL that redirects to the app
     * @param songId The ID of the song to link to
     * @return A URL string that can be opened in browser and redirects to app
     */
    fun createShareableURL(songId: Long): String {
        return "$BASE_URL/song/$songId"
    }

    /**
     * Extracts the song ID from a deep link URI
     * @param uri The deep link URI
     * @return The song ID or null if the URI is invalid
     */
    fun extractSongIdFromUri(uri: Uri?): Long? {
        if (uri == null) return null

        return try {
            if (uri.scheme == SCHEME && uri.host == HOST_SONG) {
                val path = uri.pathSegments
                if (path.isNotEmpty()) {
                    path[0].toLongOrNull()
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting song ID from URI: $uri", e)
            null
        }
    }

    /**
     * Extracts song ID from shareable URL
     * @param url The shareable URL
     * @return The song ID or null if the URL is invalid
     */
    fun extractSongIdFromURL(url: String): Long? {
        return try {
            val uri = Uri.parse(url)
            if (uri.host?.contains("purrytify") == true) {
                val pathSegments = uri.pathSegments
                if (pathSegments.size >= 2 && pathSegments[0] == "song") {
                    pathSegments[1].toLongOrNull()
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting song ID from URL: $url", e)
            null
        }
    }

    /**
     * Shares a song via the Android share sheet using shareable URL
     * @param context The context
     * @param songId The ID of the song to share
     * @param songTitle Optional song title for share text
     * @param artistName Optional artist name for share text
     */
    fun shareSong(context: Context, songId: Long, songTitle: String? = null, artistName: String? = null) {
        val shareableURL = createShareableURL(songId)

        val shareText = if (songTitle != null && artistName != null) {
            "Check out \"$songTitle\" by $artistName on Purrytify! $shareableURL"
        } else if (songTitle != null) {
            "Check out \"$songTitle\" on Purrytify! $shareableURL"
        } else {
            "Check out this song on Purrytify! $shareableURL"
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Song")
        context.startActivity(chooserIntent)
    }

    /**
     * Generates and shares a QR code for a song using shareable URL
     * @param context The context
     * @param songId The ID of the song to share
     * @param songTitle Optional song title to display with QR
     * @param artistName Optional artist name to display with QR
     */
    fun shareQRCode(context: Context, songId: Long, songTitle: String? = null, artistName: String? = null) {
        try {
            // Create the shareable URL (not deep link)
            val shareableURL = createShareableURL(songId)

            // Generate QR code bitmap with song info
            val qrBitmap = if (songTitle != null) {
                generateQRCodeWithInfo(shareableURL, 512, songTitle, artistName)
            } else {
                generateQRCode(shareableURL, 512)
            }

            // Save bitmap to cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()

            val fileName = "purrytify_song_$songId.png"
            val stream = FileOutputStream("$cachePath/$fileName")
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // Get the file URI using FileProvider
            val fileProviderAuthority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(
                context,
                fileProviderAuthority,
                File(cachePath, fileName)
            )

            // Create share intent for the image
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)

                // Add song info if available
                if (songTitle != null) {
                    val shareText = if (artistName != null) {
                        "Check out \"$songTitle\" by $artistName on Purrytify!"
                    } else {
                        "Check out \"$songTitle\" on Purrytify!"
                    }
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                type = "image/png"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Song QR Code")
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing QR code", e)
            Toast.makeText(context, "Failed to share QR code", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a QR code bitmap from a string
     * @param content The content to encode in the QR code
     * @param size The size of the QR code in pixels
     * @return A Bitmap containing the QR code
     */
    private fun generateQRCode(content: String, size: Int): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
        }

        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    /**
     * Generates a QR code bitmap with song information below it
     * @param content The content to encode in the QR code
     * @param size The size of the QR code in pixels
     * @param songTitle The title of the song
     * @param artistName The name of the artist (optional)
     * @return A Bitmap containing the QR code and song information
     */
    private fun generateQRCodeWithInfo(
        content: String,
        size: Int,
        songTitle: String,
        artistName: String? = null
    ): Bitmap {
        // Generate the QR code
        val qrBitmap = generateQRCode(content, size)

        // Create a new bitmap with extra space for text
        val textHeight = if (artistName != null) 120 else 80
        val resultBitmap = Bitmap.createBitmap(size, size + textHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw white background
        canvas.drawColor(Color.WHITE)

        // Draw QR code
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        // Draw song title
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val titleX = size / 2f
        val titleY = size + 40f
        canvas.drawText(songTitle, titleX, titleY, titlePaint)

        // Draw artist name if available
        if (artistName != null) {
            val artistPaint = Paint().apply {
                color = Color.GRAY
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }

            val artistX = size / 2f
            val artistY = size + 80f
            canvas.drawText(artistName, artistX, artistY, artistPaint)
        }

        return resultBitmap
    }
}
