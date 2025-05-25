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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent

object DeepLinkHandler {
    private const val TAG = "DeepLinkHandler"
    private const val SCHEME = "purrytify"
    private const val HOST_SONG = "song"
    private const val BASE_URL = "https://purrytify.vercel.app"

    fun createShareableURL(songId: Long): String {
        return "$BASE_URL/song/$songId"
    }

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
     * Validates if the scanned QR code is a valid Purrytify link
     * Shows detailed logging of the extraction process
     */
    fun isValidPurrytifyQR(content: String): Boolean {
        return try {
            Log.d(TAG, "🔍 Validating QR content: $content")

            val uri = Uri.parse(content)
            Log.d(TAG, "📋 Parsed URI - Scheme: ${uri.scheme}, Host: ${uri.host}, Path: ${uri.path}")

            val isWebURL = uri.host?.contains("purrytify") == true &&
                    uri.pathSegments.size >= 2 &&
                    uri.pathSegments[0] == "song"

            val isDeepLink = uri.scheme == SCHEME && uri.host == HOST_SONG

            Log.d(TAG, "✅ Validation result - Web URL: $isWebURL, Deep Link: $isDeepLink")

            val isValid = isWebURL || isDeepLink

            if (isValid) {
                val songId = if (isWebURL) extractSongIdFromURL(content) else extractSongIdFromUri(uri)
                Log.d(TAG, "🎵 Extracted Song ID: $songId")
            } else {
                Log.w(TAG, "❌ Invalid Purrytify QR code detected")
            }

            isValid
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error validating QR content: $content", e)
            false
        }
    }

    fun convertToDeepLink(url: String): String? {
        val songId = extractSongIdFromURL(url)
        return if (songId != null) {
            "$SCHEME://$HOST_SONG/$songId"
        } else null
    }

    suspend fun handleDeepLink(
        context: Context,
        uri: Uri,
        playerViewModel: PlayerViewModel
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔗 Handling deep link: $uri")

                val songId = extractSongIdFromUri(uri)
                if (songId != null) {
                    Log.d(TAG, "🎵 Loading song with ID: $songId")

                    withContext(Dispatchers.Main) {
                        // Use the existing fetchAndPlaySharedSong method from PlayerViewModel
                        playerViewModel.fetchAndPlaySharedSong(songId.toString())

                        Toast.makeText(
                            context,
                            "🎵 Song loaded successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    true
                } else {
                    Log.e(TAG, "❌ Failed to extract song ID from URI: $uri")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "❌ Invalid song link",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error handling deep link", e)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "❌ Error loading song: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                false
            }
        }
    }

    /**
     * Generates a QR code bitmap from a string
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
