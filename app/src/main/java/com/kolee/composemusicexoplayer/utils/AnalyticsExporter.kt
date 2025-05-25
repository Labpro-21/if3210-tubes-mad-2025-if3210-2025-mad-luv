package com.kolee.composemusicexoplayer.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kolee.composemusicexoplayer.data.model.MonthlyAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsExporter(private val context: Context) {

    companion object {
        private const val CSV_SEPARATOR = ","
        private const val LINE_SEPARATOR = "\n"
        private const val FILE_PROVIDER_AUTHORITY = "com.kolee.composemusicexoplayer.fileprovider"
    }

    suspend fun exportAnalytics(
        analytics: MonthlyAnalytics,
        username: String,
        format: ExportFormat = ExportFormat.CSV
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                when (format) {
                    ExportFormat.CSV -> exportToCSV(analytics, username)
                    ExportFormat.PDF -> exportToPDF(analytics, username)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                false
            }
        }
    }

    private suspend fun exportToCSV(analytics: MonthlyAnalytics, username: String): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "MusicAnalytics_${username}_$timestamp.csv"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        return try {
            FileWriter(file).use { writer ->
                // Header Information
                writer.append("MUSIC ANALYTICS REPORT$LINE_SEPARATOR")
                writer.append("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}$LINE_SEPARATOR")
                writer.append("User: $username$LINE_SEPARATOR")
                writer.append("Period: ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}$LINE_SEPARATOR")
                writer.append("$LINE_SEPARATOR")

                // Summary Section
                writer.append("LISTENING SUMMARY$LINE_SEPARATOR")
                writer.append("Total Minutes Listened${CSV_SEPARATOR}${analytics.totalMinutes}$LINE_SEPARATOR")
                writer.append("Total Hours Listened${CSV_SEPARATOR}${String.format("%.2f", analytics.totalMinutes / 60.0)}$LINE_SEPARATOR")
                writer.append("$LINE_SEPARATOR")

                // Daily Listening Stats
                if (analytics.dailyStats.isNotEmpty()) {
                    writer.append("DAILY LISTENING BREAKDOWN$LINE_SEPARATOR")
                    writer.append("Date${CSV_SEPARATOR}Duration (Minutes)${CSV_SEPARATOR}Duration (Hours)$LINE_SEPARATOR")

                    analytics.dailyStats.forEach { dailyStat ->
                        val minutes = (dailyStat.dailyDuration / 1000 / 60).toInt()
                        val hours = String.format("%.2f", minutes / 60.0)
                        writer.append("${dailyStat.date}${CSV_SEPARATOR}$minutes${CSV_SEPARATOR}$hours$LINE_SEPARATOR")
                    }
                    writer.append("$LINE_SEPARATOR")
                }

                // Top Artists Section
                if (analytics.topArtists.isNotEmpty()) {
                    writer.append("TOP ARTISTS$LINE_SEPARATOR")
                    writer.append("Rank${CSV_SEPARATOR}Artist Name${CSV_SEPARATOR}Play Count${CSV_SEPARATOR}Total Duration (Minutes)$LINE_SEPARATOR")

                    analytics.topArtists.forEachIndexed { index, artist ->
                        val minutes = (artist.totalDuration / 1000 / 60).toInt()
                        writer.append("${index + 1}${CSV_SEPARATOR}${artist.artist}${CSV_SEPARATOR}${artist.playCount}${CSV_SEPARATOR}$minutes$LINE_SEPARATOR")
                    }
                    writer.append("$LINE_SEPARATOR")
                }

                // Top Songs Section
                if (analytics.topSongs.isNotEmpty()) {
                    writer.append("TOP SONGS$LINE_SEPARATOR")
                    writer.append("Rank${CSV_SEPARATOR}Song Title${CSV_SEPARATOR}Artist${CSV_SEPARATOR}Play Count${CSV_SEPARATOR}Duration (Minutes)$LINE_SEPARATOR")

                    analytics.topSongs.forEachIndexed { index, song ->
                        val minutes = (song.totalDuration / 1000 / 60).toInt()
                        writer.append("${index + 1}${CSV_SEPARATOR}\"${song.title}\"${CSV_SEPARATOR}${song.artist}${CSV_SEPARATOR}${song.playCount}${CSV_SEPARATOR}$minutes$LINE_SEPARATOR")
                    }
                    writer.append("$LINE_SEPARATOR")
                }

                // Streak Information
                analytics.longestStreak?.let { streak ->
                    writer.append("LISTENING STREAK$LINE_SEPARATOR")
                    writer.append("Streak Duration (Days)${CSV_SEPARATOR}${streak.streakDays}$LINE_SEPARATOR")
                    writer.append("Song Title${CSV_SEPARATOR}\"${streak.songTitle}\"$LINE_SEPARATOR")
                    writer.append("Artist${CSV_SEPARATOR}${streak.artist}$LINE_SEPARATOR")
                    writer.append("Start Date${CSV_SEPARATOR}${streak.startDate}$LINE_SEPARATOR")
                    writer.append("End Date${CSV_SEPARATOR}${streak.endDate}$LINE_SEPARATOR")
                    writer.append("$LINE_SEPARATOR")
                }

                // Analytics Metadata
                writer.append("EXPORT METADATA$LINE_SEPARATOR")
                writer.append("Export Format${CSV_SEPARATOR}CSV$LINE_SEPARATOR")
                writer.append("File Generated${CSV_SEPARATOR}$fileName$LINE_SEPARATOR")
                writer.append("App Version${CSV_SEPARATOR}${getAppVersion()}$LINE_SEPARATOR")
            }

            // Share the file
            shareFile(file, "text/csv")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Analytics exported to: $fileName", Toast.LENGTH_LONG).show()
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun exportToPDF(analytics: MonthlyAnalytics, username: String): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "MusicAnalytics_${username}_$timestamp.pdf"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yPosition = 80f
            val leftMargin = 50f
            val lineSpacing = 25f

            // Title Paint
            val titlePaint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }

            // Header Paint
            val headerPaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }

            // Normal Paint
            val normalPaint = Paint().apply {
                textSize = 12f
                color = android.graphics.Color.BLACK
            }

            // Title
            canvas.drawText("MUSIC ANALYTICS REPORT", leftMargin, yPosition, titlePaint)
            yPosition += lineSpacing * 2

            // Report Info
            canvas.drawText("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}", leftMargin, yPosition, normalPaint)
            yPosition += lineSpacing
            canvas.drawText("User: $username", leftMargin, yPosition, normalPaint)
            yPosition += lineSpacing
            canvas.drawText("Period: ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}", leftMargin, yPosition, normalPaint)
            yPosition += lineSpacing * 2

            // Summary Section
            canvas.drawText("LISTENING SUMMARY", leftMargin, yPosition, headerPaint)
            yPosition += lineSpacing
            canvas.drawText("Total Minutes Listened: ${analytics.totalMinutes}", leftMargin, yPosition, normalPaint)
            yPosition += lineSpacing
            canvas.drawText("Total Hours Listened: ${String.format("%.2f", analytics.totalMinutes / 60.0)}", leftMargin, yPosition, normalPaint)
            yPosition += lineSpacing * 2

            // Top Artists
            if (analytics.topArtists.isNotEmpty()) {
                canvas.drawText("TOP ARTISTS", leftMargin, yPosition, headerPaint)
                yPosition += lineSpacing

                analytics.topArtists.take(5).forEachIndexed { index, artist ->
                    val minutes = (artist.totalDuration / 1000 / 60).toInt()
                    canvas.drawText("${index + 1}. ${artist.artist} - ${artist.playCount} plays ($minutes min)", leftMargin, yPosition, normalPaint)
                    yPosition += lineSpacing
                }
                yPosition += lineSpacing
            }

            // Top Songs
            if (analytics.topSongs.isNotEmpty()) {
                canvas.drawText("TOP SONGS", leftMargin, yPosition, headerPaint)
                yPosition += lineSpacing

                analytics.topSongs.take(5).forEachIndexed { index, song ->
                    val minutes = (song.totalDuration / 1000 / 60).toInt()
                    canvas.drawText("${index + 1}. ${song.title} by ${song.artist} - ${song.playCount} plays", leftMargin, yPosition, normalPaint)
                    yPosition += lineSpacing
                }
                yPosition += lineSpacing
            }

            // Streak Info
            analytics.longestStreak?.let { streak ->
                canvas.drawText("LISTENING STREAK", leftMargin, yPosition, headerPaint)
                yPosition += lineSpacing
                canvas.drawText("${streak.streakDays}-day streak playing \"${streak.songTitle}\" by ${streak.artist}", leftMargin, yPosition, normalPaint)
                yPosition += lineSpacing
                canvas.drawText("From ${streak.startDate} to ${streak.endDate}", leftMargin, yPosition, normalPaint)
                yPosition += lineSpacing * 2
            }

            // Daily Stats Summary
            if (analytics.dailyStats.isNotEmpty()) {
                canvas.drawText("RECENT DAILY ACTIVITY", leftMargin, yPosition, headerPaint)
                yPosition += lineSpacing

                analytics.dailyStats.take(7).forEach { dailyStat ->
                    val minutes = (dailyStat.dailyDuration / 1000 / 60).toInt()
                    val hours = String.format("%.1f", minutes / 60.0)
                    canvas.drawText("${dailyStat.date}: ${minutes} minutes (${hours}h)", leftMargin, yPosition, normalPaint)
                    yPosition += lineSpacing
                }
            }

            pdfDocument.finishPage(page)

            file.outputStream().use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            // Share the file
            shareFile(file, "application/pdf")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Analytics exported to: $fileName", Toast.LENGTH_LONG).show()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun shareFile(file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Analytics Report")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            // Fallback: show file location
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "File saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Extension function to format analytics data for display
    fun getAnalyticsSummary(analytics: MonthlyAnalytics): String {
        val totalHours = String.format("%.1f", analytics.totalMinutes / 60.0)
        val topArtist = analytics.topArtists.firstOrNull()?.artist ?: "N/A"
        val topSong = analytics.topSongs.firstOrNull()?.title ?: "N/A"
        val streakInfo = analytics.longestStreak?.let { "${it.streakDays} days" } ?: "No streak"

        return """
            📊 Your Music Analytics Summary:
            🎵 Total Listening Time: ${analytics.totalMinutes} minutes ($totalHours hours)
            🎤 Top Artist: $topArtist
            🎶 Top Song: $topSong
            🔥 Longest Streak: $streakInfo
            📅 Period: ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}
        """.trimIndent()
    }
}

enum class ExportFormat {
    CSV, PDF
}