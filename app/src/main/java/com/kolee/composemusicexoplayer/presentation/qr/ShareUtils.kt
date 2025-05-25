package com.kolee.composemusicexoplayer.presentation.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import java.io.File
import java.io.FileOutputStream

object ShareUtils {
    private const val DEEP_LINK_BASE = "https\n://purrytify.vercel.app/song/"
    fun generateSongDeepLink(songId: Long): String {
        return "$DEEP_LINK_BASE$songId"
    }

    fun shareSongViaUrl(context: Context, song: MusicEntity) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, generateSongDeepLink(song.audioId))
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Song"))
    }

//    suspend fun shareSongViaQR(context: Context, song: MusicEntity) {
//        val qrBitmap = generateQRCodeBitmap(generateSongDeepLink(song.audioId), 400)
//        val uri = saveBitmapToCache(context, qrBitmap)
//
//        val shareIntent = Intent().apply {
//            action = Intent.ACTION_SEND
//            putExtra(Intent.EXTRA_STREAM, uri)
//            type = "image/png"
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//        context.startActivity(Intent.createChooser(shareIntent, "Share Song via QR"))
//    }

//    private fun generateQRCodeBitmap(content: String, size: Int): Bitmap {
//        val hints = mapOf(
//            EncodeHintType.MARGIN to 1,
//            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L
//        )
//
//        val bitMatrix = QRCodeWriter().encode(
//            content,
//            BarcodeFormat.QR_CODE,
//            size,
//            size,
//            hints
//        )
//
//        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
//        for (x in 0 until size) {
//            for (y in 0 until size) {
//                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.Black else Color.White)
//            }
//        }
//        return bitmap
//    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        val file = File(context.cacheDir, "qr_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}